package com.rayful.bulk.exec;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.df.DFLoader;
import com.rayful.bulk.service.IBulkerService;
import com.rayful.bulk.service.IEtcProcessor;
import com.rayful.bulk.service.IIndexService;
import com.rayful.bulk.service.maru42.Qa42Service;
import com.rayful.bulk.service.maru42.entity.EntityExtractorService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.SpringBeanUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.elastic.batch.common.service.BatchLogService;

public class Bulker {
	private static final Logger logger = LoggerFactory.getLogger(Bulker.class);
	private static final String INDEX_TYPE = "Index";	// 색인 데이터 추출(index) 또는 색인 데이터 삭제(delete)
	
	private static Bulker INSTANCE = new Bulker();
	
	private final Properties prop;
	
	/**
	 * 생성자
	 */
	public Bulker() {
		prop = new Properties();
		
		try {
			Reader reader = Resources.getResourceAsReader("config.properties");
			prop.load(reader);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Bulker 실행
	 */
	public void run(String[] args) {
		IBulkerService bulkerService = null;
		Qa42Service qa42Service = null;
		IIndexService indexService = null;
		BatchLogService batchLogService = null;		// 2025.07.10 배치로그 관련 추가
		EntityExtractorService entityService = null;
		IEtcProcessor etcProcessor = null;
		
		DFLoader dfLoader = new DFLoader();
		DFLoadVO dfVO = null;
		File lockFile = null;
		String startTime = Utils.getExecuteTime((String) prop.get("BULKER.TIMEZONE"));
		
		// 유효성 체크
		Map<String, Object> paramMap = BulkerUtils.validParams(args, prop);
		logger.info("parameters={}", paramMap);
		String indexMode = (String) paramMap.get("indexmode");
		String dfName = (String) paramMap.get("dfname");
		int maxRows = (Integer) paramMap.get("maxrows");
		
	
		BulkerLogVO bulkerLogVO = new BulkerLogVO();
		if(paramMap.size() > 0) {
			// Lock 파일 조회 및 생성
			lockFile = BulkerUtils.getLockFile(dfName, INDEX_TYPE, prop);
			boolean hasLock = BulkerUtils.hasLockFile(lockFile);
			
			
			if(!hasLock) {
				try {
					// 배치 로그 서비스 클래스를 로드
					SpringBeanUtils.init(prop);
					batchLogService = SpringBeanUtils.newInstanceBatchLogService(
							dfName, indexMode,
							(Boolean)paramMap.get("batchlog"), (String)paramMap.get("dupcheck"));
					
					// 배치 이중화 설정 시 중복 실행 방지 로직
					if (batchLogService != null ) {
						if(!batchLogService.checkBatchProceed((String)paramMap.get("dupcheck"))) {
						    logger.warn("Batch was already executed.");
						    BulkerUtils.deleteLockFile(lockFile);
						    System.exit(100);
						}
					}
					
					// DF 로드
					dfVO = dfLoader.load(dfName, indexMode, INDEX_TYPE);
					logger.debug("dfVO={}", dfVO);
					
					// RUNTIME 환경변수 세팅 (주의: 아래 커스텀 클래스 로드 전에 설정)
					prop.setProperty("__RUNTIME.START_ITME", startTime);
					prop.setProperty("__RUNTIME.INDEX_MODE", indexMode);
					prop.put("__RUNTIME.MAX_ROWS", maxRows);
					prop.put("__RUNTIME.PRINT_DETAIL", (Boolean) paramMap.get("printdetail"));
					prop.put("__RUNTIME.PRINT_INTERVAL", (Integer) paramMap.get("printinterval"));
					
					// DF에 정의된 커스텀 클래스들을 로드
					bulkerService = BulkerUtils.newInstanceBulkerServiceClass(dfVO, prop);
					qa42Service = BulkerUtils.newInstanceQa42ServiceClass(dfVO, prop);
					indexService = BulkerUtils.newInstanceIndexServiceClass(dfVO, prop);
					entityService = BulkerUtils.newInstanceEntityServiceClass(dfVO, prop);
					etcProcessor = BulkerUtils.newInstancEtcProcessorClass(dfVO, prop);
					
					if(!"send".equals(indexMode)) {
						// Bulker Servie 전처리 수행
						bulkerService.init();
						
						// ------------------------------------
						// 전처리 로직 호출 ...
						// ------------------------------------
						if(etcProcessor != null) {
							etcProcessor.preProcess();
						}
						
						List<Map<String, Object>> keyDataList = null;
						List<Map<String, Object>> dataList = null;
						List<Object> resultDataList = null;
						int keyNo = 0;
						
						// KeyQuery 실행 후 데이터 세팅
						bulkerService.getKeyData(indexMode); // key query 수행
						bulkerLogVO.query.setTotalCount(bulkerService.getDocCount());
						List<File> bulkFileList = null;
						
						while((keyDataList = bulkerService.nextKeyData()) != null) {
							// DataQuery, FileQuery, EtcQuery 실행
							//dataList = bulkerService.getData(keyDataList, indexMode,  bulkerLogVO, keyNo + 1);
							dataList = bulkerService.getDataAsync(keyDataList, indexMode,  bulkerLogVO, keyNo + 1);
							keyNo += keyDataList.size();
							
							// 개체명을 추출한다.
							if(entityService != null && "inc".equalsIgnoreCase(indexMode)) {
								entityService.setEntity(dataList);
							}
							
							// 덤프 파일 저장 형식으로 변경
							resultDataList = bulkerService.getResultData(dataList, indexMode);
							
							// 색인 덤프 파일 생성
							bulkFileList = bulkerService.makeJsonLimtSize(resultDataList, INDEX_TYPE, bulkerLogVO);
							if(indexService != null && bulkFileList.size() > 0) {
								if(dfVO.custom.getIndexSerivce().isAsync()) {
									logger.info("Async - IndexService");
									for(File bulkFile : bulkFileList) {
										indexService.requestSingleBulkIndex(bulkFile, dfName, dfVO.getIndexName(), bulkerLogVO);
									}
								}
							}
							
							// qa 덤프 결과 파일 생성
							if(qa42Service != null) {
								qa42Service.makeCsv(dfName, startTime, dataList, dfVO, bulkerLogVO);
							}
							
							// 큐 데이터를 업데이트 (내부에서 증분일 경우만 수행)
							bulkerService.updateQueueData(keyDataList, indexMode);
						}
						
						// DF의 last_doc_indexdate 업데이트
						dfLoader.updateIndexDate(dfName, startTime, INDEX_TYPE);
						
						// qa42Service가 DF에 정의되면 ... qa 덤프 파일(csv)의 색인요청을 수행
						if(qa42Service != null && keyNo > 0) {
							qa42Service.updateIndexDate(dfName, startTime);
							if(qa42Service.moveToQaFolder(dfName, indexMode)) {
								qa42Service.requestIndex(dfName, startTime, indexMode);
							}
						}
					}
					
					// indexService가 DF에 정의되면 ... 색인 덤프 파일(json)들에 대해 벌크 색인요청을 수행
					if(indexService != null) {
						if("send".equals(indexMode) || dfVO.custom.getIndexSerivce().isAsync() == false) {
							logger.info("No async - IndexService");
							indexService.requestBulkIndex(dfName, dfVO.getIndexName(), bulkerLogVO);
						}	
						// 모든 인덱스 요청이 완료될때 까지 대기
						indexService.waitForComplete();
						
						// delete_not_indexed 수행
						if("all".equals(indexMode) && dfVO.datasource.isDeleteNotIndexed()) {
							if(maxRows > 0) {
								logger.info("DeleteNotIndexed not executed. because maxrows option > 0 (TEST MODE)");
							} else {
								if(bulkerLogVO.query.getSuccessCount() > 0
										&& bulkerLogVO.query.getSuccessCount() == bulkerLogVO.indexResult.getTotalCount()
										&& bulkerLogVO.indexResult.getErrorCount() == 0) {
									indexService.requestDeleteNotIndexed(dfVO.getIndexName());
								} else {
									logger.warn("DeleteNotIndexed not executed.");
									logger.warn("Data Process: {}, Index Request File: {}, Index Response Error: {}"
											, bulkerLogVO.query.getSuccessCount()
											, bulkerLogVO.index.getTotalCount()
											, bulkerLogVO.indexResult.getErrorCount()
									);
								}
							}
						}
					}
					
					// ------------------------------------
					// 후처리 로직 호출 ...
					// ------------------------------------
					if(etcProcessor != null) {
						etcProcessor.postProcess();
					}
					
					if (batchLogService != null ) {
						batchLogService.updateStatusToSuccess();
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					
					if (batchLogService != null ) {
						batchLogService.updateStatusToFail();
					}
				} finally {
					// Bulker Servie 후처리 수행
					if(bulkerService != null ) 	
						try { bulkerService.destroy(); } 
						catch (Exception e) { logger.warn("fail to bulkerService.destroy() - {}", e.getMessage());}	
					if(entityService != null ) 
						try { entityService.destroy(); } 
						catch (Exception e) { logger.warn("fail to entityService.destroy() - {}", e.getMessage()); }	
					
					// Lock 파일 삭제
					try { BulkerUtils.deleteLockFile(lockFile); }
					catch (Exception e) { logger.warn("Can't delete lock file. - {}", e.getMessage()); }	
				}
				
				// 벌커 요약 로그를 출력
				BulkerUtils.printBulkerLog(bulkerLogVO);
				
			} else {
				logger.info("- " + dfName + " is already running. ");
				logger.info("- Lock file path : " + lockFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * Main
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			logger.info("------------------------------------------");
			logger.info(" Bulker Start");
			logger.info("------------------------------------------");
			INSTANCE.run(args);
			stopWatch.stop();
			logger.info("------------------------------------------");
			logger.info(" Bulker End (" + Utils.getMsToTime(stopWatch.getTime()) + ")");
			logger.info("------------------------------------------");
		} catch(Throwable t) {
			logger.error("Fatil ERROR", t);
		}
	}
}
