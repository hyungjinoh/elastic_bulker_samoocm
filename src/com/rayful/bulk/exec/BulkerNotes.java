package com.rayful.bulk.exec;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.df.DFLoader;
import com.rayful.bulk.service.IBulkerNotesService;
import com.rayful.bulk.service.IEtcProcessor;
import com.rayful.bulk.service.IIndexService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.SpringBeanUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.NotesNsfItemVO;
import com.rayful.elastic.batch.common.service.BatchLogService;

import lotus.domino.Document;

public class BulkerNotes {
	private static final Logger logger = LoggerFactory.getLogger(BulkerNotes.class);
	private static final String INDEX_TYPE = "Index";	// 색인 데이터 추출(index) 또는 색인 데이터 삭제(delete)
	
	private static BulkerNotes INSTANCE = new BulkerNotes();
	
	private final Properties prop;
	
	/**
	 * 생성자
	 */
	public BulkerNotes() {
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
		IBulkerNotesService bulkerService = null;
		IIndexService indexService = null;
		BatchLogService batchLogService = null;		// 2025.07.10 배치로그 관련 추가
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
		
		List<BulkerLogVO> bulkerLogList = null;
		BulkerLogVO bulkerLogVO = null;
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
					bulkerService = BulkerUtils.newInstanceBulkerNotesServiceClass(dfVO, prop);
					indexService = BulkerUtils.newInstanceIndexServiceClass(dfVO, prop);
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
						
						bulkerLogList = new ArrayList<> ();

						if(dfVO.nsfList.hasListFile()) {
							// nsf 목록을 파일로 정의했다면 loadFile 함수를 먼저 호출하여 파일로 부터 목록을 세팅한다.
							dfVO.nsfList.loadFile();
							/* nsf 목록 파일 형식
							 	{
									"nsf_list": [
										{"dataSource": "db_conn1", "name": "egate/global/eap1/aprv/repository/doc/repository20242.nsf" },
										{"dataSource": "db_conn1", "name": "egate/global/eap1/aprv/repository/doc/repository20243.nsf" }
									]
								}
							*/
						}
						List<NotesNsfItemVO> nsfList = dfVO.nsfList.getList();
						List<Document> keyDataList = null;
						List<Map<String, Object>> dataList = null;
						List<Object> resultDataList = null;
						int keyNo = 0;
						boolean bResult = false;
						
						for(NotesNsfItemVO nsfItem : nsfList) {
							bulkerLogVO = new BulkerLogVO(nsfItem.getName());
							bulkerLogList.add(bulkerLogVO);
							
							keyNo = 1;
							logger.info("\n\n");
							logger.info( BulkerUtils.LOG_SUMMARY_LINE );
							logger.info("### NSF Name: {} ", nsfItem.getName());
							logger.info( BulkerUtils.LOG_SUMMARY_LINE );
							
							// 쿼리를 수행한다. 
							bResult = bulkerService.getKeyData(indexMode, nsfItem);
							if(!bResult) {
								bulkerLogVO.nsf.setCompletFail("Can't connect nsf");
								logger.error("Can't connect nsf: {}", nsfItem.getName());
								continue;
							}
							
							// 쿼리 결과를 루프를 돌면서 일정 개수 씩  받는다. 
							bulkerLogVO.query.setTotalCount(bulkerService.getDocCount());
							File bulkFile = null;
							
							while((keyDataList = bulkerService.nextKeyData()) != null) {
								dataList = bulkerService.getData(keyDataList, indexMode, bulkerLogVO, keyNo);
								keyNo += keyDataList.size();
								
								// 덤프 파일 저장 형식으로 변경
								resultDataList = bulkerService.getResultData(dataList, indexMode);
								
								// 색인 덤프 파일 생성
								bulkFile = bulkerService.makeJson(resultDataList, INDEX_TYPE, bulkerLogVO);
								if(indexService != null && bulkFile != null) {
									if(dfVO.custom.getIndexSerivce().isAsync()) {
										logger.info("Async - IndexService");
										indexService.requestSingleBulkIndex(bulkFile, dfName, dfVO.getIndexName(), bulkerLogVO);
									}
								}
							}

							bulkerLogVO.nsf.setCompleted();
							
							// indexService가 DF에 정의되면 ... 색인 덤프 파일(json)들에 대해 벌크 색인요청을 수행
							if(indexService != null) {
								if(dfVO.custom.getIndexSerivce().isAsync() == false) {
									logger.info("No async - IndexService");
									indexService.requestBulkIndex(dfName, dfVO.getIndexName(), bulkerLogVO);
								}
							}
						} // for(NotesNsfItemVO nsfItem ...
						
						// 벌커 요약 로그를 출력
						int completedCount = 0;
						for(BulkerLogVO bulkerLog : bulkerLogList) {
							BulkerUtils.printBulkerLog(bulkerLog);
							
							// nsf 완료 처리 개수
							if(bulkerLog.nsf.isCompleted()) {
								completedCount ++;
							}
						}
						
						// 모든 nsf 처리를 완료한 경우 
						if(completedCount == nsfList.size()) {
							// DF의 last_doc_indexdate 업데이트
							dfLoader.updateIndexDate(dfName, startTime, INDEX_TYPE);
							
							// 모든 인덱스 요청이 완료될때 까지 대기
							indexService.waitForComplete();
							
							// delete_not_indexed 수행
							if(indexService != null) {
								if("all".equals(indexMode) && dfVO.datasource.isDeleteNotIndexed()) {
									if(maxRows > 0) {
										logger.info("DeleteNotIndexed not executed. because maxrows option > 0 (TEST MODE)");
									} else {
										indexService.requestDeleteNotIndexed(dfVO.getIndexName());
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

					} else {
						// 결과 파일을 전송만을 수행...
						bulkerLogVO = new BulkerLogVO("--- SEND Option");
						
						// indexService가 DF에 정의되면 ... 색인 덤프 파일(json)들에 대해 벌크 색인요청을 수행
						if(indexService != null) {
							indexService.requestBulkIndex(dfName, dfVO.getIndexName(), bulkerLogVO);
						}
						
						BulkerUtils.printBulkerLog(bulkerLogVO);
					}
					
					if (batchLogService != null ) {
						batchLogService.updateStatusToSuccess();
					}
					
				} catch (Throwable t) {
					logger.error(t.getMessage(), t);
					
					if (batchLogService != null ) {
						batchLogService.updateStatusToFail();
					}
				
				} finally {
					// Bulker Servie 후처리 수행
					if(bulkerService != null ) bulkerService.destroy();	
					
					// Lock 파일 삭제
					BulkerUtils.deleteLockFile(lockFile);
				}
				
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
	}
}
