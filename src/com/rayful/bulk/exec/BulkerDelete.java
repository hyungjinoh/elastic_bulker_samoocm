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
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;

public class BulkerDelete {
	private static final Logger logger = LoggerFactory.getLogger(BulkerDelete.class);
	private static final String INDEX_TYPE = "Delete";
	
	private static BulkerDelete INSTANCE = new BulkerDelete();
	
	
	private final Properties prop;
	
	/**
	 * 생성자
	 */
	public BulkerDelete() {
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
		IIndexService indexService = null;
		IEtcProcessor etcProcessor = null;
		
		DFLoader dfLoader = new DFLoader();
		File lockFile = null;
		String startTime = Utils.getExecuteTime((String) prop.get("BULKER.TIMEZONE"));
		
		// 유효성 체크
		Map<String, Object> paramMap = BulkerUtils.validParams(args, prop);
		logger.info("parameters={}", paramMap);
		String dfName = (String) paramMap.get("dfname");
		String indexMode = (String) paramMap.get("indexmode");
		
		BulkerLogVO bulkerLogVO = new BulkerLogVO();
		if(paramMap.size() > 0) {
			// Lock 파일 조회 및 생성
			lockFile = BulkerUtils.getLockFile(dfName, INDEX_TYPE, prop);
			boolean hasLock = BulkerUtils.hasLockFile(lockFile);
			
			if(!hasLock) {
				try {
					// DF 로드
					DFLoadVO dfVO = dfLoader.load(dfName, indexMode, INDEX_TYPE);
					
					// RUNTIME 환경변수 세팅 (주의: 아래 커스텀 클래스 로드 전에 설정)
					prop.setProperty("__RUNTIME.START_ITME", startTime);
					prop.setProperty("__RUNTIME.INDEX_MODE", indexMode);
					prop.put("__RUNTIME.PRINT_DETAIL", (Boolean) paramMap.get("printdetail"));
					prop.put("__RUNTIME.PRINT_INTERVAL", (Integer) paramMap.get("printinterval"));
					
					// DF에 정의된 커스텀 클래스들을 로드
					bulkerService = BulkerUtils.newInstanceBulkerServiceClass(dfVO, prop);
					indexService = BulkerUtils.newInstanceIndexServiceClass(dfVO, prop);
					etcProcessor = BulkerUtils.newInstancEtcProcessorClass(dfVO, prop);
					
					// ------------------------------------
					// 전처리 로직 호출 ...
					// ------------------------------------
					if(etcProcessor != null) {
						etcProcessor.preProcess();
					}
					
					List<Map<String, Object>> keyDataList = null;
					List<Object> resultDataList = null;
					
					// KeyQuery 실행 후 데이터 세팅
					bulkerService.getKeyData(indexMode);
					bulkerLogVO.query.setTotalCount(bulkerService.getDocCount());
					
					while((keyDataList = bulkerService.nextKeyData()) != null) {
						// Result data 조회
						resultDataList = bulkerService.getDeleteResultData(keyDataList);
						
						// 결과 파일 생성
						bulkerService.makeJson(resultDataList, INDEX_TYPE, bulkerLogVO);
					}

					// indexService가 DF에 정의되면 ... 색인 덤프 파일(json)들에 대해 벌크 색인요청을 수행
					if(indexService != null) {
						indexService.requestBulkIndex(dfName, dfVO.getIndexName(), bulkerLogVO);
					}
					
					// ------------------------------------
					// 후처리 로직 호출 ...
					// ------------------------------------
					if(etcProcessor != null) {
						etcProcessor.postProcess();
					}
					
					// DF last_doc_indexdate 업데이트
					dfLoader.updateIndexDate(dfName, startTime, INDEX_TYPE);
					
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					
				} finally {
					// Lock 파일 삭제
					BulkerUtils.deleteLockFile(lockFile);
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
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		logger.info("------------------------------------------");
		logger.info(" Bulker Delete Start");
		logger.info("------------------------------------------");
		INSTANCE.run(args);
		stopWatch.stop();
		logger.info("------------------------------------------");
		logger.info(" Bulker Delete End (" + Utils.getMsToTime(stopWatch.getTime()) + ")");
		logger.info("------------------------------------------");
	}
}
