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
import com.rayful.bulk.service.IIndexService;
import com.rayful.bulk.service.maru42.entity.EntityExtractorService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;

public class BulkerEntity {
	private static final Logger logger = LoggerFactory.getLogger(BulkerEntity.class);
	private static final String INDEX_TYPE = "Update";
	private static BulkerEntity INSTANCE = new BulkerEntity();
	
	
	private final Properties prop;
	
	/**
	 * 생성자
	 */
	public BulkerEntity() {
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
		EntityExtractorService entityService = null;
		DFLoader dfLoader = new DFLoader();
		File lockFile = null;
		
		// 유효성 체크
		Map<String, Object> paramMap = BulkerUtils.validParams(args, prop);
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
					
					bulkerService = BulkerUtils.newInstanceBulkerServiceClass(dfVO, prop);
					indexService = BulkerUtils.newInstanceIndexServiceClass(dfVO, prop);
					entityService = BulkerUtils.newInstanceEntityServiceClass(dfVO, prop);
					if(entityService == null) {
						throw new NullPointerException("entityService is null");
					}
					
					List<Map<String, Object>> keyDataList = null;
					List<Map<String, Object>> dataList = null;
					List<Object> resultDataList = null;
					int keyNo = 0;
					
					// KeyQuery 실행 후 데이터 세팅
					bulkerService.getKeyData(indexMode);
					bulkerLogVO.query.setTotalCount(bulkerService.getDocCount());
					
					while((keyDataList = bulkerService.nextKeyData()) != null) {
						// content_query 실행
						dataList = entityService.getEntityData(keyDataList, indexMode,  bulkerLogVO, keyNo + 1);
						keyNo += keyDataList.size();
						
						// Result data 조회
						resultDataList = bulkerService.getResultData(dataList, indexMode);
						
						// 결과 파일 생성
						bulkerService.makeJson(resultDataList, INDEX_TYPE, bulkerLogVO);
					}
					
					// indexService가 DF에 정의되면 ... 색인 덤프 파일(json)들에 대해 벌크 색인요청을 수행
					if(indexService != null) {
						indexService.requestBulkIndex(dfName, dfVO.getIndexName(), bulkerLogVO);
					}

				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					
				} finally {
					if(bulkerService != null ) bulkerService.destroy();	
					if(entityService != null ) try { entityService.destroy(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }	
					
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
		logger.info(" Bulker Entity Start");
		logger.info("------------------------------------------");
		INSTANCE.run(args);
		stopWatch.stop();
		logger.info("------------------------------------------");
		logger.info(" Bulker Entity End (" + Utils.getMsToTime(stopWatch.getTime()) + ")");
		logger.info("------------------------------------------");
	}
}
