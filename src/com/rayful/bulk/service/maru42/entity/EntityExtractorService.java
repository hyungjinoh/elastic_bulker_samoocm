package com.rayful.bulk.service.maru42.entity;

import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.dao.JdbcDAO;
import com.rayful.bulk.service.es.BulkerService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.DataLogVO;
import com.rayful.bulk.vo.df.QueryItemVO;

public class EntityExtractorService {
	private static final Logger logger = LoggerFactory.getLogger(BulkerService.class);
	protected final JdbcDAO jdbcDAO;
	protected final Properties prop;
	protected final DFLoadVO dfVO;
	
	protected ExecutorService executorService = null;
	protected final int poolthreadCount;
	protected final int poolShutdownTimeout;
	protected final EntityExtractorApi entityExtractorApi;
	
	public EntityExtractorService(DFLoadVO dfVO, Properties prop) throws Exception {
		this.dfVO = dfVO;
		this.prop = prop;
		this.jdbcDAO = JdbcDAO.getInstance(dfVO, prop);
		
		if(this.dfVO.column.getBodyColumnName() == null) {
			logger.error("DF must be specify cloumn(ROLE=body)");
			throw new IllegalArgumentException("DF must be specify cloumn(ROLE=body)");
		}
		
		if(this.dfVO.field.getEntityFieldName() == null) {
			logger.error("DF must be specify field(ROLE=entity)");
			throw new IllegalArgumentException("DF must be specify field(ROLE=entity)");
		}
		
		try {
			this.poolthreadCount = Integer.parseInt(this.prop.getProperty("BULKER.EXTRACT_ENTITY.THREADPOOL.THREAD.COUNT", "3"));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("\"BULKER.EXTRACT_ENTITY.THREADPOOL.THREAD.COUNT\" setting is not number foramt.");
		}
		
		try {
			this.poolShutdownTimeout = Integer.parseInt(this.prop.getProperty("BULKER.EXTRACT_ENTITY.THREADPOOL.SHUTDOWN.TIMEOUT", "60"));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("\"BULKER.EXTRACT_ENTITY.THREADPOOL.SHUTDOWN.TIMEOUT\" setting is not number foramt.");
		}
		
		this.executorService = Executors.newFixedThreadPool(this.poolthreadCount);
		this.entityExtractorApi = new EntityExtractorApi(this.prop);
		
		logger.info("poolthreadCount={}, poolShutdownTimeout={}", this.poolthreadCount, this.poolShutdownTimeout);
	}
	
	public void destroy() throws InterruptedException {
		this.executorService.shutdown();
		this.executorService.awaitTermination(poolShutdownTimeout, TimeUnit.SECONDS);
		this.executorService = null;
	}
	
	/**
	 * 전체 색인 시 개체명
	 * <p/>
	 * content_query의 결과에서 본문컬럼의 내용으로 NER 요청을 하여 개체명 추출결과를 부분업데이트 할 수있는 데이터를 조회한다.
	 * 
	 * @param keyDataList
	 * @param indexMode
	 * @param bulkerLogVO
	 * @param keyNo
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> getEntityData(List<Map<String, Object>> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo) throws Exception{
		List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
		
		QueryItemVO queryItemVO = this.dfVO.query.getEtcQuery("content_query");
		if(queryItemVO != null && keyDataList.size() > 0) {
			int idx = keyNo;
			String indexAction = "U";
			
			CountDownLatch countDownLatch = new CountDownLatch(keyDataList.size());
			
			CompletionHandler<List<String>, DataLogVO> callback = new CompletionHandler<List<String>, DataLogVO>() {
				@Override
				public void completed(List<String> result, DataLogVO attachment) {
					bulkerLogVO.query.increaseSuccessCount();
					printDataLog(attachment);
				}

				@Override
				public void failed(Throwable exc, DataLogVO attachment) {
					bulkerLogVO.query.increaseFailCount(exc.getMessage());
					attachment.setErrorMessage(exc.getMessage());
					printDataLog(attachment);
				}
			};
			
			String bodyColumnName = this.dfVO.column.getBodyColumnName();
			String entityFieldName = this.dfVO.field.getEntityFieldName();
			for (Map<String, Object> keyMap : keyDataList) {
				DataLogVO dataLogVO = new DataLogVO(keyMap, idx, bulkerLogVO.query.getTotalCount(), indexAction);
				try {
					// DataQuery 실행
					Map<String, Object> dataMap = this.jdbcDAO.selecyQueryMap(this.dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());
					if(dataMap != null && dataMap.size() > 0) {
						Map<String, Object> outputDataMap = new HashMap<String, Object>();
						outputDataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
						outputDataMap.put(this.dfVO.column.getKeyColumnName(), keyMap.get(this.dfVO.column.getKeyColumnName()));
						dataList.add(outputDataMap);
						
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								List<String> resultList = null;
								try {
									String content = (String) dataMap.get(bodyColumnName);
									resultList = entityExtractorApi.getEntity(content);
									
									callback.completed(resultList, dataLogVO);
								} catch(Exception e) {
									resultList = new ArrayList<String>();
									callback.failed(e, dataLogVO);
								} finally {
									// 데이터를 결과 목록에 담는다.
									outputDataMap.put(entityFieldName, resultList);
									countDownLatch.countDown();
								}
							}
						};
						
						executorService.submit(runnable);
					} else {
						logger.error("Fail to data query: key-{}", keyMap);
						dataLogVO.setErrorMessage("Fail to data query: key-" + keyMap.toString());
						bulkerLogVO.query.increaseWarnCount("Fail to data query: key-" + keyMap.toString());
						printDataLog(dataLogVO);
						countDownLatch.countDown();
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					dataLogVO.setErrorMessage(e.toString());
					bulkerLogVO.query.increaseFailCount(e.toString());
					printDataLog(dataLogVO);
					countDownLatch.countDown();
				}

				idx ++;
			} // for
			
			countDownLatch.await();  // 0가 될때 까지 대기
		} else {
			logger.warn("Can't find query: content_query");
		}
		
		return dataList;
	}
	
	public void printDataLog(DataLogVO dataLogVO) {
		String dataMsg = null;

		if(dataLogVO.getErrorMessage() == null) {
			dataMsg = "Success";
		} else {
			dataMsg = "Fail, Messge : " + dataLogVO.getErrorMessage();
		}
		
		synchronized(this) {
			logger.info(BulkerUtils.LOG_DATA_LINE);
			logger.info("- [{}/{} - IndexAction: {}]  ", dataLogVO.getCurrent(), dataLogVO.getTotal(), dataLogVO.getIndexAction());
			logger.info("- [key {}]  ", dataLogVO.getKey());
			logger.info("- [Data] {}", dataMsg);
			logger.info(BulkerUtils.LOG_DATA_LINE);
		}
	}
	
	
	public void setEntity(List<Map<String, Object>> dataList) throws Exception{
		if(dataList != null && dataList.size() > 0) {
			logger.info("Start to process extracting entity.");
			CountDownLatch countDownLatch = new CountDownLatch(dataList.size());
			String keyColumnName = this.dfVO.column.getKeyColumnName();
			
			CompletionHandler<List<String>, Map<String, Object>> callback = new CompletionHandler<List<String>, Map<String, Object>>() {
				@Override
				public void completed(List<String> result, Map<String, Object> attachment) {
					String key = (String) attachment.get(keyColumnName);
					if(logger.isDebugEnabled()) {
						logger.debug("Success to extract entity: key={}, count={}", key, result.size());
					}
				}
	
				@Override
				public void failed(Throwable exc, Map<String, Object> attachment) {
					String key = (String) attachment.get(keyColumnName);
					logger.error("Fatil to extract entity: key={}, error={}", key, exc.toString());
				}
			};
			
			String indexAction = null;
			String bodyColumnName = this.dfVO.column.getBodyColumnName();
			String entityFieldName = this.dfVO.field.getEntityFieldName();
			for (Map<String, Object> dataMap : dataList) {
				indexAction = (String) dataMap.get(this.dfVO.column.getIndexActionColumnName());
				if ("I".equalsIgnoreCase(indexAction)) {
	
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							List<String> resultList = null;
							try {
								String content = (String) dataMap.get(bodyColumnName);
								resultList = entityExtractorApi.getEntity(content);
								
								callback.completed(resultList, dataMap);
							} catch(Exception e) {
								callback.failed(e, dataMap);
								resultList = new ArrayList<String>();
							} finally {
								// 데이터를 결과 목록에 담는다.
								dataMap.put(entityFieldName, resultList);
								countDownLatch.countDown();
							}
						}
					};
					
					executorService.submit(runnable);
				} else {
					logger.warn("IndexAction is not I (index): {}", indexAction);
					countDownLatch.countDown();
				}
			} // for
			
			logger.info("Wait to process extracting engity");
			countDownLatch.await();  // 0가 될때 까지 대기
			logger.info("End to process extracting engity");
		} else {
			logger.warn("dataList is empty");
		}
	}
}
