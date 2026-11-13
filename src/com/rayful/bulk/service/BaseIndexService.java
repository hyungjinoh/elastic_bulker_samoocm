package com.rayful.bulk.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * 색인 요청을 담당하는 클래스의 공통 함수 중 함수를 일부 구현한 추상클래스
 */
public abstract class BaseIndexService implements IIndexService{
	private static final Logger logger = LoggerFactory.getLogger(BaseIndexService.class);
	
	protected final Properties prop;
	protected final DFLoadVO dfVO;
	
	protected final Path resultReadyPath;
	protected final Path resultSuccessPath;
	protected final Path resultFailPath;
	
	protected final boolean isDeleteAfterSuccess;
	protected final boolean isBackupAfterSuccess;
	protected final boolean isBackupAfterFail;
	
	protected final ExecutorService executorService;
	protected RestTemplate restTemplate = null;
	
	/**
	 * 벌크 파일 한 건의 검색엔진 요청을 담당할 쓰레드를 위한 Inner 클래스 정의
	 * @author 정충열
	 */
	class BulkIndexTask  implements Runnable {
		private final String dfName;
		private final String indexName;
		private final Path bulkFile;
		private final BulkerLogVO bulkerLogVO;
		
		public BulkIndexTask(Path bulkFile, String dfName, String indexName, BulkerLogVO bulkerLogVO) {
			this.dfName = dfName;
			this.indexName = indexName;
			this.bulkFile = bulkFile;
			this.bulkerLogVO = bulkerLogVO;
		}
		
		public BulkIndexTask(File bulkFile, String dfName, String indexName, BulkerLogVO bulkerLogVO) {
			this.dfName = dfName;
			this.indexName = indexName;
			this.bulkFile = bulkFile.toPath();
			this.bulkerLogVO = bulkerLogVO;
		}
	
		@Override
		public void run() {
			boolean isSuccess = false;
			String logFileName = null;
			Path logFile = null;
			Path destBulkFile = null;
			Path destLogFile = null;
			
			Path source = resultReadyPath.resolve(dfName);
			Path targetFail = resultFailPath.resolve(dfName);
			Path targetSuccess = resultSuccessPath.resolve(dfName).resolve(BulkerUtils.getSuccessDatetimeFolderName(prop));
			
			CountLogVO logVO = bulkerLogVO.index;
			logFileName = Utils.getFileNameWithoutExt(bulkFile.getFileName().toString()) + ".log";
			logFile = bulkFile.getParent().resolve(logFileName);
			
			try {
				if(!Files.exists(logFile)) {
					isSuccess = requestIndex(bulkFile, logFile, indexName, bulkerLogVO);
					
					if(isSuccess) {
						logVO.increaseSuccessCount();
					} else {
						logVO.increaseFailCount("Server response failed. [" + bulkFile.toString() + "]");
					}
				} else {
					isSuccess = false;
					logVO.increaseWarnCount("Aleady exists log file. [" + bulkFile.toString() + "]");
				}
				
				if(logger.isDebugEnabled())
					logger.debug("[INDEX] bulkFile: {}, exists logFile: {}", bulkFile.getFileName(), Files.exists(logFile));
				
				if(isSuccess) {
					if(isDeleteAfterSuccess) {
						// 성공 후에 모든 관련 파일 삭제
						Files.delete(bulkFile);
						logger.debug("[INDEX] Delete successed bulkFile: {}", bulkFile);
						if(Files.exists(logFile)) {
							Files.delete(logFile);
							logger.debug("[INDEX] Delete successed logFile: {}", logFile);
						}
					} else if(isBackupAfterSuccess) {
						if(!Files.exists(targetSuccess)) {
							Files.createDirectories(targetSuccess);
						}
						
						// 성공 후에 모든 관련 파일 "_success" 폴더로 이동
						destBulkFile = targetSuccess.resolve(source.relativize(bulkFile));
						destLogFile = targetSuccess.resolve(source.relativize(logFile));
						
						Files.move(bulkFile, destBulkFile);
						logger.debug("[INDEX] Move successed bulkFile: {} -> {}", bulkFile, destBulkFile);
						Files.move(logFile, destLogFile);
						logger.debug("[INDEX] Move successed logFile: {} -> {}", logFile, destLogFile);
					}
				} else {
					if(isBackupAfterFail) {
						if(!Files.exists(targetFail)) {
							Files.createDirectories(targetFail);
						}
						
						// 실패 후에 모든 관련 파일 "_fail" 폴더로 이동
						destBulkFile = targetFail.resolve(source.relativize(bulkFile));
						destLogFile = targetFail.resolve(source.relativize(logFile));
						
						Files.move(bulkFile, destBulkFile);
						logger.debug("[INDEX] Move failed bulkFile: {} -> {}", bulkFile, destBulkFile);
						Files.move(logFile, destLogFile);
						logger.debug("[INDEX] Move failed logFile: {} -> {}", logFile, destLogFile);
					}
				}
			
			} catch(Exception e) {
				logger.error("{}", e.getMessage());
				logVO.increaseFailCount(e.toString());
				isSuccess = false;
			}
		}
	}
	
	public BaseIndexService(DFLoadVO dfVO, Properties prop) {
		this.executorService = Executors.newSingleThreadExecutor();
		
		this.dfVO = dfVO;
		this.prop = prop;
		
		String resultBasePath = prop.getProperty("BULKER.PATH.RESULT");
		if(resultBasePath == null) {
			logger.error("[INDEX] Not found setting ... BULKER.PATH.RESULT");
			throw new IllegalArgumentException("[INDEX] Not found setting ... BULKER.PATH.RESULT");
		} else if (!Files.exists(Paths.get(resultBasePath))){
			logger.error("[INDEX] Not exist result basepath: {}", resultBasePath);
			throw new IllegalArgumentException("\"[INDEX] Not exist result basepath: " + resultBasePath);
		}
		this.resultReadyPath = Paths.get(resultBasePath, "ready");
		this.resultSuccessPath = Paths.get(resultBasePath, "_success");
		this.resultFailPath = Paths.get(resultBasePath, "_fail");
		
		// 성공 시 벌크,로그파일 삭제
		this.isDeleteAfterSuccess = prop.getProperty("BULKER.RESULT.SUCCESS.DELETE", "1").equals("0")? false: true;
		// 성공 시 벌크,로그파일을 "_success" 폴더로 이동
		this.isBackupAfterSuccess = prop.getProperty("BULKER.RESULT.SUCCESS.BACKUP", "1").equals("0")? false: true;
		// 실패 시 벌크,로그파일을 "_fail" 폴더로 이동
		this.isBackupAfterFail = prop.getProperty("BULKER.RESULT.FAIL.BACKUP", "1").equals("0")? false: true;
	}
	
	@Override
	public void requestSingleBulkIndex(File bulkFile, String dfName, String indexName, BulkerLogVO bulkerLogVO) throws Exception {
		this.executorService.submit(new BulkIndexTask(bulkFile, dfName, indexName, bulkerLogVO));
	}
	
	@Override
	public void requestBulkIndex(String dfName, String indexName, BulkerLogVO bulkerLogVO) throws Exception {
		Path source = this.resultReadyPath.resolve(dfName);
		Path targetFail = this.resultFailPath.resolve(dfName);
		Path targetSuccess = this.resultSuccessPath.resolve(dfName).resolve(BulkerUtils.getSuccessDatetimeFolderName(this.prop));
		
		logger.debug("[INDEX] bulk ready folder: {}", source);
		logger.debug("[INDEX] bulk success folder: {}", targetSuccess);
		logger.debug("[INDEX] bulk fail folder: {}", targetFail);
		
		CountLogVO logVO = bulkerLogVO.index;
		try {
			if(Files.exists(source)) {
			
				if(!Files.exists(targetSuccess)) {
					Files.createDirectories(targetSuccess);
				}
				if(!Files.exists(targetFail)) {
					Files.createDirectories(targetFail);
				}
				
				List<Path> bulkFileList = null;
				
				//파일이름 오름차순 정렬
				final Comparator<Path> byName = (p1, p2)->p1.getFileName().toString().toLowerCase().compareTo(p2.getFileName().toString().toLowerCase());
				//파일 목록 조회
				try (Stream<Path> walk = Files.walk(source)) {
					bulkFileList = walk
							.filter(Files::isRegularFile)
							.filter(f->f.getFileName().toString().toLowerCase().endsWith(".json"))
							.sorted(byName)
							.collect(Collectors.toList());
				}
				
				logVO.setTotalCount(bulkFileList.size());
				/*
				boolean isSuccess = false;
				String logFileName = null;
				Path logFile = null;
				Path destBulkFile = null;
				Path destLogFile = null;
				*/
				for(Path bulkFile : bulkFileList) {
					this.executorService.submit(new BulkIndexTask(bulkFile, dfName, indexName, bulkerLogVO));
					/*
					logFileName = Utils.getFileNameWithoutExt(bulkFile.getFileName().toString()) + ".log";
					logFile = bulkFile.getParent().resolve(logFileName);
					
					try {
						if(!Files.exists(logFile)) {
							isSuccess = this.requestIndex(bulkFile, logFile, indexName, bulkerLogVO);
							
							if(isSuccess) {
								logVO.increaseSuccessCount();
							} else {
								logVO.increaseFailCount("Server response failed. [" + bulkFile.toString() + "]");
							}
						} else {
							isSuccess = false;
							logVO.increaseWarnCount("Aleady exists log file. [" + bulkFile.toString() + "]");
						}
						
						if(logger.isDebugEnabled())
							logger.debug("[INDEX] bulkFile: {}, exists logFile: {}", bulkFile.getFileName(), Files.exists(logFile));
						
						if(isSuccess) {
							if(this.isDeleteAfterSuccess) {
								// 성공 후에 모든 관련 파일 삭제
								Files.delete(bulkFile);
								logger.debug("[INDEX] Delete successed bulkFile: {}", bulkFile);
								if(Files.exists(logFile)) {
									Files.delete(logFile);
									logger.debug("[INDEX] Delete successed logFile: {}", logFile);
								}
							} else if(this.isBackupAfterSuccess) {
								// 성공 후에 모든 관련 파일 "_success" 폴더로 이동
								destBulkFile = targetSuccess.resolve(source.relativize(bulkFile));
								destLogFile = targetSuccess.resolve(source.relativize(logFile));
								
								Files.move(bulkFile, destBulkFile);
								logger.debug("[INDEX] Move successed bulkFile: {} -> {}", bulkFile, destBulkFile);
								Files.move(logFile, destLogFile);
								logger.debug("[INDEX] Move successed logFile: {} -> {}", logFile, destLogFile);
							}
						} else {
							if(this.isBackupAfterFail) {
								// 실패 후에 모든 관련 파일 "_fail" 폴더로 이동
								destBulkFile = targetFail.resolve(source.relativize(bulkFile));
								destLogFile = targetFail.resolve(source.relativize(logFile));
								
								Files.move(bulkFile, destBulkFile);
								logger.debug("[INDEX] Move failed bulkFile: {} -> {}", bulkFile, destBulkFile);
								Files.move(logFile, destLogFile);
								logger.debug("[INDEX] Move failed logFile: {} -> {}", logFile, destLogFile);
							}
						}
					
					} catch(Exception e) {
						logger.error("{}", e.getMessage());
						logVO.increaseFailCount(e.toString());
						isSuccess = false;
					}
					 */
				} 
			} else {
				logger.warn("No json file to request index.");
			}
		} catch (Exception e) {
			logVO.increaseFailCount(e.toString());
			throw e;
		} finally {
			// 빈 폴더들을 삭제
			if(Files.exists(source) && Utils.isEmptyDirectory(source)) {
				Files.delete(source);
			}
				
			if(Files.exists(targetSuccess) && Utils.isEmptyDirectory(targetSuccess)) {
				Files.delete(targetSuccess);
				if(Utils.isEmptyDirectory(targetSuccess.getParent())) {
					Files.delete(targetSuccess.getParent());
				}
			}
				
			if(Files.exists(targetFail) && Utils.isEmptyDirectory(targetFail)) {
				Files.delete(targetFail);
			}
		}
		
	}
	
	@Override
	public boolean requestDeleteNotIndexed(String indexName) throws Exception {
		boolean isCompleted = true;
		
		logger.info("### Try to delete not indexed document...");
		
		String indexTimeColumn = this.dfVO.column.getIndexTimeColumnName();
		String indexStartTime = this.prop.getProperty("__RUNTIME.START_ITME");
		if(indexTimeColumn == null) {
			throw new NullPointerException("Not Define - indexTimeColumn in DF");
		}
		if(indexStartTime == null) {
			throw new NullPointerException("indexStartTime == null");
		}
		
		String deleteQuery = "{\"query\": {\"range\" : {\"###field_name###\": {\"lt\": \"###last_index_datetime###\"}}}}";
		deleteQuery = deleteQuery.replace("###field_name###", indexTimeColumn);
		deleteQuery = deleteQuery.replace("###last_index_datetime###", indexStartTime.replace(" ", "T"));
		
		int waitTimeSeconds = 0;
		try {
			waitTimeSeconds = Integer.parseInt(this.prop.getProperty("DELETE.NOTINDEXED.WAITTIME", "5"));
		} catch (NumberFormatException nfe) {
			logger.warn("Not Number format: \"BULKER.PATH.RESULT\" property");
			waitTimeSeconds = 5;
		}
		
		logger.info("Wait for deleting document ... ");
		Thread.sleep(waitTimeSeconds * 1000L);
		
		// 삭제대상 건수를 구한다 : indexed_dtm < lastUpdateTime
		long deletingCnt = this.requestDeletingCount(indexName, deleteQuery);
		
		// 삭제대상이 있으면 삭제수행, 없으면 바로 종료
		if (deletingCnt > 0) {
			String taskId = this.requestDeleteByQuery(indexName, deleteQuery);
			
			// task가 완료할 때 까지 무한 대기
			isCompleted = this.requestTaskCompleted(taskId);
		}
		
		if(isCompleted) {
			logger.info("### Success to delete not indexed document: {}", deletingCnt);
		} else {
			logger.info("### Fail to delete not indexed document...");
		}
		
		return isCompleted;
	}
	
	@Override
	public void waitForComplete() throws InterruptedException {
		
		this.executorService.shutdown();
		logger.info("Wait for completing all bulke index request...#1");
		
		while(!this.executorService.awaitTermination(1, TimeUnit.MINUTES)) {
			// Do nothing.
			logger.info("Wait for completing all bulke index request.");
		}
	}

	/**
	 * bulkFile의 내용을 웹 서비스 API를 이용하여 색인요청을 수행한다. 
	 * <p/>
	 * 색인 요청 응답결과는 logFile에 저장한다. 
	 * 
	 * @param bulkFile 벌크 파일
	 * @param logFile 색인요청 응답결과를 저장할 로그 파일
	 * @param indexName 인덱스 명
	 * @param bulkerLogVO 벌커 로그 객체
	 * 
	 * @return 색인요청이 모두 처리되면 true, 그렇지 않으면 false
	 * @throws IOException
	 */
	abstract protected boolean requestIndex(Path bulkFile, Path logFile, String indexName, BulkerLogVO bulkerLogVO) throws IOException;
	
	abstract protected int requestDeletingCount(String indexName, String requestBody) throws IOException;
	
	abstract protected String requestDeleteByQuery(String indexName, String requestBody) throws IOException;
	
	abstract protected boolean requestTaskCompleted(String taskId) throws IOException, InterruptedException;
}
