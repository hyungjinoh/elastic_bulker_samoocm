package com.rayful.bulk.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;

import com.google.gson.Gson;
import com.rayful.bulk.attach.IAttachFileHelper;
import com.rayful.bulk.concurrent.BlockingRejectedExecutionHandler;
import com.rayful.bulk.dao.JdbcDAO;
import com.rayful.bulk.ocr.OcrClient;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.CodecUtil;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.DataLogVO;
import com.rayful.bulk.vo.df.ColumnItemVO;
import com.rayful.bulk.vo.df.QueryItemVO;

import static com.rayful.bulk.util.Utils.deleteAllChildren;

/**
 *  색인 데이터 수집 관련 데이터 조회 및 첨부파일 처리 관련 메서드를 정의한 
 *  IBulkerService 인터페이스의 함수 중 함수를 일부 구현한 추상클래스
 */
public abstract class BaseBulkerService implements IBulkerService{
	private static final Logger logger = LoggerFactory.getLogger(BaseBulkerService.class);
	private static final Logger errLogger = LoggerFactory.getLogger("FILE_ERRO_LOGGER");
	
	public static final String KEYINC_QUERY_LASTINDEXDATE = "#data_source@last_doc_indexdate#";
	public static final String FILE_SEPERATOR = "\n\n";
	
	protected final JdbcDAO jdbcDAO;
	protected JdbcCursorItemReader<Map<String, Object>> cursorItemReader;
	protected List<Map<String,Object>> keyDataList = null;
	protected int keyReadCount = -1;

	protected final ExecutorService executor ;
	
	protected final Properties prop;
	protected final DFLoadVO dfVO;

	protected final Path resultReadyPath;
	
	protected final IEtcReader etcReader;
	protected final IAttachFileHelper fileHelper;
	
	protected final int resultFileLimitSize;
	protected final long allowFileMaxSize;
	protected Map<String, String> allowExtMap = null;
	protected final boolean isExecDrmDecode;
	
	// 2026.11.18 OCR(이미지/스캔 문서 텍스트 인식) 설정
	protected Map<String, String> ocrExtMap = null;   // OCR 처리 대상 확장자(대문자). null이면 미설정
	protected final OcrClient ocrClient;              // null이면 OCR 비활성화
	
	protected final int maxRows;
	protected final boolean printDetail;
	protected final int printInterval;
	
	protected final File lastKeyFile;
	
	/**
	 * 생성자
	 * 
	 * @param dfVO DF 정보
	 * @param prop 설정파일 정보
	 * @throws Exception EtcClass 생성 실패 시
	 * @throws FileNotFoundException 설정에 정의된 경로가 존재하지 않을 때
	 * @throws IllegalArgumentException  설정이 존재하지 않을 때
	 */
	public BaseBulkerService(DFLoadVO dfVO, Properties prop) throws Exception {
		this.dfVO = dfVO;
		this.prop = prop;
		this.jdbcDAO = JdbcDAO.getInstance(dfVO, prop);
		
		String resultBasePath = this.prop.getProperty("BULKER.PATH.RESULT");
		if(resultBasePath == null) {
			logger.error("\"BULKER.PATH.RESULT\" setting is not found.");
			throw new IllegalArgumentException("\"BULKER.PATH.RESULT\" setting is not found.");
		} else if (!Files.exists(Paths.get(resultBasePath))) {
			logger.error("Result base path does not exist: {}", resultBasePath);
			throw new FileNotFoundException("Result base path does not exist");
		}
		this.resultReadyPath = Paths.get(resultBasePath, "ready");
		
		//  2025.10.04 정충열 덤프파일의 저장 사이즈 제한 
		String maxFileSize = null;
		maxFileSize = this.prop.getProperty("BULKER.RESULT.LIMITSIZE");
		if(maxFileSize == null) {
			this.resultFileLimitSize = 10485760;	// 기본값 : 10M
		} else {
			this.resultFileLimitSize = Integer.parseInt(maxFileSize);
		}
		//  2025.09.29 정충열 텍스트 필터링 대상 첨부 파일 사이즈 제한 추가
		maxFileSize = this.prop.getProperty("BULKER.ALLOWED.FILEMAXSIZE");
		if(maxFileSize == null) {
			this.allowFileMaxSize = 5368709120L;	// 기본값 : 5G
		} else {
			this.allowFileMaxSize = Long.parseLong(maxFileSize);
		}
				
		String allowExt = this.prop.getProperty("BULKER.ALLOWED.FILEFORMAT");
		if(allowExt != null) {
			this.allowExtMap = new HashMap<String, String>();
			String [] arrExt = allowExt.toUpperCase().split(",");
			for(String ext : arrExt) {
				this.allowExtMap.put(ext, null);
			}
		}
		
		// 2023.08.07 정충열 설정 추가 - 복호화 수행여부
		if("1".equals(this.prop.getProperty("BULKER.DRM.DEOCDE.EXEC"))) {
			isExecDrmDecode = true;
		} else {
			isExecDrmDecode = false;
		}
		
		// 2026.11.18 OCR 대상 확장자 + OCR 클라이언트 구성
		// - BULKER.OCR.ALLOWED_EXT 에 명시된 확장자는 외부 텍스트 필터(snf_exe/Tika) 대신 OCR API로 처리한다.
		// - BULKER.OCR.URL 이 비어있으면 OCR 기능 자체를 비활성화한다.
		String ocrExt = this.prop.getProperty("BULKER.OCR.ALLOWED_EXT");
		if(ocrExt != null && ocrExt.trim().length() > 0) {
			this.ocrExtMap = new HashMap<String, String>();
			for(String ext : ocrExt.toUpperCase().split(",")) {
				if(ext.trim().length() > 0) {
					this.ocrExtMap.put(ext.trim(), null);
				}
			}
		}
		String ocrUrl = this.prop.getProperty("BULKER.OCR.URL");
		if(ocrUrl != null && ocrUrl.trim().length() > 0 && this.ocrExtMap != null) {
			this.ocrClient = new OcrClient(this.prop);
		} else {
			this.ocrClient = null;
			if(this.ocrExtMap != null) {
				logger.warn("\"BULKER.OCR.ALLOWED_EXT\" is set but \"BULKER.OCR.URL\" is empty. OCR is disabled.");
			}
		}
		
		// 2024.09.01 max rows 설정 추가
		this.maxRows = (Integer) this.prop.get("__RUNTIME.MAX_ROWS");
		
		// 2024.08.23 print detail 설정 추가
		this.printDetail = (Boolean) this.prop.get("__RUNTIME.PRINT_DETAIL");
		this.printInterval = (Integer) this.prop.get("__RUNTIME.PRINT_INTERVAL");
		
		this.etcReader = BulkerUtils.newInstanceEtcClass(dfVO, prop);
		this.fileHelper = BulkerUtils.newInstanceFileHelperClass(dfVO, prop);
		
		// 2026.03.04
		// BaseEtcReader를 사용하는 경우에만 fileHelper를 주입한다.
		// (하위 호환성을 위해 instanceof 체크)
		if(this.etcReader instanceof BaseEtcReader) {
		    ((BaseEtcReader) this.etcReader).setFileHelper(this.fileHelper);
		}
		
		int nThread = this.dfVO.custom.getBulkerSerivce() == null ? 1: this.dfVO.custom.getBulkerSerivce().getnThread();
		// 2025.09.23 스레드 풀 추가
		this.executor = new ThreadPoolExecutor(
				nThread,                      // core
				nThread,                      // max
			    0L,
			    TimeUnit.MILLISECONDS,
			    new ArrayBlockingQueue<>(nThread),          // 큐 객체
			    new BlockingRejectedExecutionHandler()      // 큐가 빌때까지 대기
			);
		
		// 2025.09.24 lastKey 저장 파일 객체
		String dfPath = prop.getProperty("BULKER.PATH.DF");
		if(dfPath == null) {
			logger.error("\"BULKER.PATH.DF\" setting is not found.");
			throw new IllegalArgumentException("\"BULKER.PATH.DF\" setting is not found.");
		} else if (!Files.exists(Paths.get(dfPath))) {
			logger.error("LastKeyFile path does not exist: {}", dfPath);
			throw new FileNotFoundException("LastKeyFile path does not exist");
		}
		this.lastKeyFile = new File(dfPath, dfVO.getDfName() + ".lastkey.txt");
	}
	
	@Override
	public void init() throws IOException {
		if(this.fileHelper != null) {
			this.fileHelper.init();
		}
	}
	
	@Override
	public void destroy() {
		if(this.fileHelper != null) {
			this.fileHelper.destroy();
		}
		
		// 2026.11.18 OCR 클라이언트 종료
		if(this.ocrClient != null) {
			this.ocrClient.close();
		}
		
		if(this.cursorItemReader != null) {
			this.cursorItemReader.close();
		}
		
		// 2025.09.23 스레드 풀 추가
		if(this.executor != null) {
			try {
				this.executor.shutdown();
				logger.info("Wait for completing to processs attach file ...#1");
				
				while(!this.executor.awaitTermination(1, TimeUnit.MINUTES)) {
					// Do nothing.
					logger.info("Wait for completing to processs attach file.");
				}
			} catch (Exception e) {
				logger.warn("Failed to terminate executor - {}", e);
			}
		}
	}
	
	@Override
	public int getDocCount () {
		String indexMode = this.prop.getProperty("__RUNTIME.INDEX_MODE");
		QueryItemVO queryItem = null;
		if("inc".equals(indexMode)) {
			queryItem = this.dfVO.query.getKeyIncQuery();
		} else {
			queryItem = this.dfVO.query.getKeyQuery();
		}
		
		if(!queryItem.isCursorUse()) {
			return this.keyDataList != null ? this.keyDataList.size() : 0;
		} else {
			return -1;
		}
	}
	
	@Override
	public void getKeyData(String indexMode) {
		if(this.keyDataList == null && this.cursorItemReader == null) {
			String sql = null;
			QueryItemVO queryItem = null;
			if("all".equalsIgnoreCase(indexMode)) {
				queryItem = this.dfVO.query.getKeyQuery();
				if(queryItem == null) {
					logger.error("key_query is null");
					throw new NullPointerException("key_query is null");
				}
				
				sql = queryItem.getSql();
			} else {
				queryItem = this.dfVO.query.getKeyIncQuery();
				if(queryItem == null) {
					logger.error("key_inc_query is null");
					throw new NullPointerException("key_inc_query is null");
				} 
				
				sql = queryItem.getSql();
				sql = sql.replace(KEYINC_QUERY_LASTINDEXDATE, "'" + this.dfVO.datasource.getLastIndexDate() + "'");
			}
			
			this.keyReadCount = 0;
			if(!queryItem.isCursorUse()) {
				// 서버 커서를 사용하지 않는 경우
				this.keyDataList = this.jdbcDAO.selectKeyQuery(this.dfVO, sql, queryItem.getConnectionId());
			} else {
				// 서버 커서를 사용하는 경우
				this.cursorItemReader = this.jdbcDAO.getItemReader(this.dfVO, sql, queryItem.getConnectionId());
				this.cursorItemReader.open(new ExecutionContext());
			}
			
		} else {
			throw new IllegalAccessError("Must call this mehtod one");
		}
	}
	
	@Override
	public List<Map<String, Object>> nextKeyData() throws Exception {
		// DF에 정의한 maxRows 만큼 sub KeyDataList 만들어 리턴
		List<Map<String, Object>> rtnKeyDataList = null;
		
		String indexMode = this.prop.getProperty("__RUNTIME.INDEX_MODE");
		QueryItemVO queryItem = null;
		if("inc".equals(indexMode)) {
			queryItem = this.dfVO.query.getKeyIncQuery();
		} else {
			queryItem = this.dfVO.query.getKeyQuery();
		}
		
		if(this.keyReadCount < 0) {
			throw new IllegalStateException("Must call getKeyData() first.");
		}
		
		if(!queryItem.isCursorUse()) {
			// 서버 커서를 사용하지 않는 경우
			if(this.keyReadCount < this.keyDataList.size()) {
				int lastPostion = Math.min(this.keyReadCount + this.dfVO.getMaxRows(), this.keyDataList.size());
				if(this.maxRows > 0) {
					// 2024.03.07 정충열 - maxrows 옵션 사용 시 무한 대기(루프) 현상 문제 수정.
					if(this.keyReadCount < this.maxRows) {
						lastPostion = Math.min(lastPostion, this.maxRows);
					} else {
						lastPostion = -1;
					}
				} 
				
				if(lastPostion > 0) {
					rtnKeyDataList = new ArrayList<Map<String,Object>>(this.keyDataList.subList(this.keyReadCount, lastPostion));
					this.keyReadCount += rtnKeyDataList.size();
				}
			}
		} else {
			// 서버 커서를 사용하는 경우
			List<Map<String, Object>> resutlList = new ArrayList<>();
			Map<String, Object> keyData = null;
			int fetchSize = this.dfVO.getMaxRows();
			for(int index = 0; index < fetchSize; index ++) {
				if(this.maxRows > 0 && this.keyReadCount >=  this.maxRows) {
					break;
				}
				
				keyData = (Map<String, Object>) this.cursorItemReader.read();
				if(keyData == null) {
					break;
				}
				resutlList.add(keyData);
				
				this.keyReadCount ++;

			}
			rtnKeyDataList = (resutlList.size() > 0)? resutlList : null;
		}
		
		return rtnKeyDataList;
	}
	
	@Override
	public List<Map<String, Object>> getData(List<Map<String, Object>> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo) {
		List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
		
		if(keyDataList.size() > 0) {
			Map<String, Object> dataMap = null;
			List<Map<String, Object>> fileDataList = null;
			DataLogVO dataLogVO = null;
			int idx = keyNo;
			String indexAction = null;
			String startTime = null;
			String keyValue = null;
			Map<String, Object> lastSussessKeyMap = null;
			String indexPatternColumnValue = null;
			
			// 전체 키쿼리, 증분 키쿼리 결과가 key Column 존재여부 체크 (ROLE: key)
			if(!keyDataList.get(0).containsKey(this.dfVO.column.getKeyColumnName())) {
				throw new NullPointerException("Key Query (inc) must contain ROLE='key' column - " + this.dfVO.column.getKeyColumnName());
			}
			// 전체 키쿼리, 증분 키쿼리 결과가 IndexPatern Column 존재여부 체크 (ROLE: index_pattern)
			if(this.dfVO.isIncludeIndexname()) { 
				if(!keyDataList.get(0).containsKey(this.dfVO.column.getIndexPatternColumnName())) {
					throw new NullPointerException("Key Query (inc) must contain ROLE='index_pattern' column - " + this.dfVO.column.getIndexPatternColumnName());
				}
			}
				
			for (Map<String, Object> keyMap : keyDataList) {
				indexAction = (String) keyMap.get(this.dfVO.column.getIndexActionColumnName());
				dataLogVO = new DataLogVO(keyMap, keyDataList.size(), idx, indexAction);
				if("D".equals(indexAction)) {
					dataMap = new HashMap<String, Object>();
					
					// ROLE: index_action
					dataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
					
					// ROLE: key
					dataMap.put(this.dfVO.column.getKeyColumnName(), keyMap.get(this.dfVO.column.getKeyColumnName()));
					
					//index name을  bulk file에 추가  설정인 경우 
					if(this.dfVO.isIncludeIndexname()) { 
						indexPatternColumnValue = Utils.objToString(keyMap.get(this.dfVO.column.getIndexPatternColumnName()));
						dataMap.put(this.dfVO.column.getIndexPatternColumnName(), indexPatternColumnValue);
					}
					
					// 데이터를 결과 목록에 담는다.
					dataList.add(dataMap);
					bulkerLogVO.query.increaseSuccessCount();
				} else {
					try {
						QueryItemVO queryItemVO = this.dfVO.query.getDataQuery();
						if(queryItemVO != null ) {
							// DataQuery 실행
							dataMap = this.jdbcDAO.selecyQueryMap(this.dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());
						} else {
							// 조회없이 키 데이터를 사용
							dataMap = keyMap;
						}
						
						if(dataMap != null && dataMap.size() > 0) {
							if(indexAction == null) {
								indexAction = "I";
							}
							
							// ROLE: key
							keyValue = Utils.objToString(dataMap.get(this.dfVO.column.getKeyColumnName()));
							if( keyValue == null || keyValue.trim().length() == 0) {
								throw new NullPointerException("key column value must be not empty");
							}
							
							// ROLE: index_action
							dataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
							
							// ROLE: index_time
							if(this.dfVO.column.getIndexTimeColumnName() != null) {
								startTime = this.prop.getProperty("__RUNTIME.START_ITME");
								if(startTime != null) {
									dataMap.put(this.dfVO.column.getIndexTimeColumnName(), startTime.replace(" ","T"));
								} else {
									dataMap.put(this.dfVO.column.getIndexTimeColumnName(), null);
								}
							}
							
							// DataQuery 결과를 컨버팅한다. (커스텀 로직 반영 가능)
							this.convertDataQueryReult(dataMap);
							
							// FileQuerey 실행
							if((this.dfVO.field.isIndexFileInfo() || this.dfVO.field.isIndexFileData())
									&& "I".equals(indexAction)) {
								fileDataList = this.getFileData(keyMap);
								
								if(fileDataList != null && fileDataList.size() > 0) {
									// es_file_info 의 값이 Y 일 경우
									if(this.dfVO.field.isIndexFileInfo()) {
										dataMap.put(this.dfVO.field.getFileInfoFieldName(), fileDataList);
									}
									
									// es_file_data 의 값이 Y 일 경우 (파일 내용 추출)
									if(this.dfVO.field.isIndexFileData() && this.fileHelper != null) {
										this.mergeFileContentData(fileDataList, dataMap, dataLogVO.fileLog);
									}
								}
							}
							
							// EtcQuery 실행
							if(this.etcReader != null) {
								this.etcReader.executeEtc(keyMap, dataMap, indexMode, dataLogVO.etcLog);
							}
							
							// 데이터를 결과 목록에 담는다.
							dataList.add(dataMap);
							bulkerLogVO.query.increaseSuccessCount();
							lastSussessKeyMap = keyMap;
						} else {
							logger.error("Fail to execute data query: key-{}", keyMap);
							dataLogVO.setErrorMessage("Fail to execute data query: key-" + keyMap.toString());
							bulkerLogVO.query.increaseWarnCount("Fail to execute data query: key-" + keyMap.toString());
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						dataLogVO.setErrorMessage(e.toString());
						bulkerLogVO.query.increaseFailCount(e.toString());
					}
				}
				
				this.printLog(idx, bulkerLogVO, dataLogVO, indexAction);
				idx ++;
			} // for
			
			if(!this.printDetail) {
				if((idx-1) % this.printInterval != 0) {
					this.printSimpleLog(bulkerLogVO.query.getTotalCount(), idx-1);
				}
			}
			
			// 2025.09.24 성공한 데이터 조회 결과 목록의 마지막 키 값을 저장
			if( ("all".equalsIgnoreCase(indexMode) && this.dfVO.query.getKeyQuery().isRelayUse())
					|| ("inc".equalsIgnoreCase(indexMode) && this.dfVO.query.getKeyIncQuery().isRelayUse()) ) {
				if(dataList.size() > 0) {
					// 마지막 키를 파일에 저장
					Gson gson = BulkerUtils.getGson();
					try {
						FileUtils.write(this.lastKeyFile, gson.toJson(lastSussessKeyMap), StandardCharsets.UTF_8.name(), false);
						logger.info("Success to save last key: {}", lastSussessKeyMap);
					} catch (IOException e) {
						logger.warn("Fail to save last key: {}", e);
					}
				}
			}
		}
		
		return dataList;
	}
	
	@Override
	public List<Map<String, Object>> getDataAsync(List<Map<String, Object>> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo) {
		List<Map<String, Object>> dataList = new CopyOnWriteArrayList<Map<String,Object>>();
		
		if(keyDataList.size() > 0) {
			Map<String, Object> dataMap = null;
			DataLogVO dataLogVO = null;
			int idx = keyNo;
			String indexAction = null;
			String startTime = null;
			String keyValue = null;
			final CountDownLatch latch = new CountDownLatch(keyDataList.size());
			List<DataLogVO> dataLogList = new ArrayList<> ();
			String indexPatternColumnValue = null;
			
			// 전체 키쿼리, 증분 키쿼리 결과가 key Column 존재여부 체크 (ROLE: key)
			if(!keyDataList.get(0).containsKey(this.dfVO.column.getKeyColumnName())) {
				throw new NullPointerException("Key Query (inc) must contain ROLE='key' column - " + this.dfVO.column.getKeyColumnName());
			}
			// 전체 키쿼리, 증분 키쿼리 결과가 IndexPatern Column 존재여부 체크 (ROLE: index_pattern)
			if(this.dfVO.isIncludeIndexname()) { 
				if(!keyDataList.get(0).containsKey(this.dfVO.column.getIndexPatternColumnName())) {
					throw new NullPointerException("Key Query (inc) must contain ROLE='index_pattern' column - " + this.dfVO.column.getIndexPatternColumnName());
				}
			}
				
			for (Map<String, Object> keyMap : keyDataList) {
				indexAction = (String) keyMap.get(this.dfVO.column.getIndexActionColumnName());
				dataLogVO = new DataLogVO(keyMap, keyDataList.size(), idx, indexAction);
				dataLogList.add(dataLogVO);
				
				if("D".equals(indexAction)) {
					dataMap = new HashMap<String, Object>();
					
					// ROLE: index_action
					dataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
					
					// ROLE: key
					dataMap.put(this.dfVO.column.getKeyColumnName(), keyMap.get(this.dfVO.column.getKeyColumnName()));
										
					//index name을  bulk file에 추가  설정인 경우 
					if(this.dfVO.isIncludeIndexname()) { 
						indexPatternColumnValue = Utils.objToString(keyMap.get(this.dfVO.column.getIndexPatternColumnName()));
						dataMap.put(this.dfVO.column.getIndexPatternColumnName(), indexPatternColumnValue);
					}
					
					// 데이터를 결과 목록에 담는다.
					dataList.add(dataMap);
					bulkerLogVO.query.increaseSuccessCount();
					
					this.printLog(idx, bulkerLogVO, dataLogVO, indexAction);
					latch.countDown();
				} else {
					try {
						QueryItemVO queryItemVO = this.dfVO.query.getDataQuery();
						if(queryItemVO != null ) {
							// DataQuery 실행
							dataMap = this.jdbcDAO.selecyQueryMap(this.dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());
						} else {
							// 조회없이 키 데이터를 사용
							dataMap = keyMap;
						}
						
						if(dataMap == null || dataMap.size() == 0) {
							logger.error("Fail to execute data query: key-{}", keyMap);
							dataLogVO.setErrorMessage("Fail to execute data query: key-" + keyMap.toString());
							bulkerLogVO.query.increaseWarnCount("Fail to execute data query: key-" + keyMap.toString());
							
							this.printLog(idx, bulkerLogVO, dataLogVO, indexAction);
							latch.countDown();
						} else {

							if(indexAction == null) {
								indexAction = "I";
							}
							
							// ROLE: key
							keyValue = Utils.objToString(dataMap.get(this.dfVO.column.getKeyColumnName()));
							if( keyValue == null || keyValue.trim().length() == 0) {
								throw new NullPointerException("key column value must be not empty");
							}
							
							// ROLE: index_action
							dataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
							
							// ROLE: index_time
							if(this.dfVO.column.getIndexTimeColumnName() != null) {
								startTime = this.prop.getProperty("__RUNTIME.START_ITME");
								if(startTime != null) {
									dataMap.put(this.dfVO.column.getIndexTimeColumnName(), startTime.replace(" ","T"));
								} else {
									dataMap.put(this.dfVO.column.getIndexTimeColumnName(), null);
								}
							}
							
							// DataQuery 결과를 컨버팅한다. (커스텀 로직 반영 가능)
							this.convertDataQueryReult(dataMap);
							
							
							// 첨부파일 처리 스레드에 의해서 참조되므로 HashMap => synchronizedMap으로 변경함. 
							// [즈의] ConcurrentHashMap은 null value를 허용하지 못해 사요할 수 없음.
							final Map<String, Object> tDataMap = Collections.synchronizedMap(dataMap);
							// 데이터를 결과 목록에 담는다.
							dataList.add(tDataMap);
							
							// [주의] 여기서 부터는 dataMap 참조하면 안됨
							dataMap = null;
							
							final String tIndexAction = indexAction;
							final DataLogVO tDataLogVO = dataLogVO;
							final int tIdx = idx;
							this.executor.submit(() -> {
								try {
									// FileQuerey 실행
									if((this.dfVO.field.isIndexFileInfo() || this.dfVO.field.isIndexFileData())
											&& "I".equals(tIndexAction)) {
										final List<Map<String, Object>> fileDataList = this.getFileData(keyMap);
										
										if(fileDataList != null && fileDataList.size() > 0) {
											// es_file_info 의 값이 Y 일 경우
											if(this.dfVO.field.isIndexFileInfo()) {
												tDataMap.put(this.dfVO.field.getFileInfoFieldName(), fileDataList);
											}
											
											// es_file_data 의 값이 Y 일 경우 (파일 내용 추출)
											if(this.dfVO.field.isIndexFileData() && this.fileHelper != null) {
												this.mergeFileContentData(fileDataList, tDataMap, tDataLogVO.fileLog);
											}
										}
									}
									
									// EtcQuery 실행
									if(this.etcReader != null) {
										this.etcReader.executeEtc(keyMap, tDataMap, indexMode, tDataLogVO.etcLog);
									}
									
									bulkerLogVO.query.increaseSuccessCount();
									
								} catch(Exception ex) {
									logger.error(ex.getMessage(), ex);
						    		tDataLogVO.setErrorMessage(ex.toString());
									bulkerLogVO.query.increaseFailCount(ex.toString());
									
									// 데이터를 결과 목록에 담는다.
									dataList.remove(tDataMap);
								}
								
								this.printLog(tIdx, bulkerLogVO, tDataLogVO, tIndexAction);
								latch.countDown();
						    });
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						dataLogVO.setErrorMessage(e.toString());
						bulkerLogVO.query.increaseFailCount(e.toString());
						
						this.printLog(idx, bulkerLogVO, dataLogVO, indexAction);
						latch.countDown();
					}		
				}
				
				idx ++;
			} // for
				
			// 2025.09.23 모든 작업이 완료할때 까지 대기
			try {
				latch.await();
			} catch (InterruptedException e) {
				logger.warn("Terminate abnormaly - latch.await()");
			}
			
			if(!this.printDetail) {
				if((idx-1) % this.printInterval != 0) {
					this.printSimpleLog(bulkerLogVO.query.getTotalCount(), idx-1);
				}
			}
			
			// 2025.09.24 성공한 데이터 조회 결과 목록의 마지막 키 값을 저장
			if( ("all".equalsIgnoreCase(indexMode) && this.dfVO.query.getKeyQuery().isRelayUse())
					|| ("inc".equalsIgnoreCase(indexMode) && this.dfVO.query.getKeyIncQuery().isRelayUse()) ) {
				if(dataList.size() > 0) {
					// 마지막 데이터 수집이 성공한 키 정보를 찾는다. 
					Map<String, Object> lastSussessKeyMap = null;
					DataLogVO imsiDataLogVO = null;
					 for (int index = dataLogList.size() - 1; index >= 0; index --) {
						 imsiDataLogVO = dataLogList.get(index);
						 if(imsiDataLogVO.getErrorMessage() == null) {
							 lastSussessKeyMap = imsiDataLogVO.getKey();
							 break;
						 }
					 }
					
					 // 마지막 키를 파일에 저장
					Gson gson = BulkerUtils.getGson();
					try {
						FileUtils.write(this.lastKeyFile, gson.toJson(lastSussessKeyMap), StandardCharsets.UTF_8.name(), false);
						logger.info("Success to save last key: {}", lastSussessKeyMap);
					} catch (IOException e) {
						logger.warn("Fail to save last key: {}", e);
					}
				}
			}
		}
		
		return dataList;
	}
	
	
	@Override
	abstract public List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode);
	
	@Override
	abstract public List<Object> getDeleteResultData(List<Map<String, Object>> dataList);
	
	@Override
	@Deprecated
	abstract public File makeJson(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws IOException;
	
	@Override
	abstract public List<File> makeJsonLimtSize(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws Exception; 
	
	@Override
	public boolean updateQueueData(List<Map<String, Object>> keyDataList, String indexMode) {
		boolean isSuccess = false;
		int affectedRows = 0;
		
		if("inc".equalsIgnoreCase(indexMode)) {
			QueryItemVO queryItemVO = this.dfVO.query.getUpdateQueueQuery();
			if(queryItemVO != null) {
				affectedRows = this.jdbcDAO.batchUpdate(this.dfVO, queryItemVO.getSql(), keyDataList, queryItemVO.getConnectionId());
				
				if(keyDataList.size() == affectedRows) {
					isSuccess = true;
					logger.info("Queue data is updated. (affected rows:{})", affectedRows);
				} else {
					isSuccess = false;
					logger.info("Queue data is updated. (affected rows:{}, keyList size:{})", affectedRows, keyDataList.size());
				}
			}
		}
		
		return isSuccess;
	}
	
	/**
	 * 파일정보 조회 쿼리를 수행한다.
	 * 
	 * @return 파일정보 목록
	 */
	protected List<Map<String, Object>> getFileData(Map<String, Object> keyMap) {
		List<Map<String, Object>> fileDataList = null;
		QueryItemVO queryItemVO = this.dfVO.query.getFileQuery();
		
		if(queryItemVO != null && keyMap != null) {
			fileDataList = this.jdbcDAO.selectQueryList(this.dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());
			
			/* 2023.07.06 정충열 불필한 로직으로 판단되어 주석처리
			if(fileDataList.size() == 1 && fileDataList.get(0).get(this.dfVO.column.getFileKeyColumnName()) == null) {
				fileDataList = null;
			}
			*/
		}
		
		return fileDataList;
	}
	
	/**
	 * 조회한 데이터를 변경처리한다.
	 * <p/>
	 * separator를 정의한 컬럼은 해당 구분자로 해서 배열 형식으로 변경한다. <br/>
	 * dataMap에 변경결과를 반영한다. <br/>
	 * 
	 */
	protected void convertDataQueryReult(Map<String, Object> dataMap) {
		List<ColumnItemVO> columnList = this.dfVO.column.getColumnList();
		String columnName = null;
		
		// separator
		String separator = null;
		String columnData = null;
		String [] splitData = null;
		List<String> listData = null; 
		// copy_to
		String [] copyColumns = null;
		// rename_to
		String renameColumn = null;
		
		for(ColumnItemVO columnItemVO : columnList) {
			columnName = columnItemVO.getName();
			
			///////// html tag 삭제 처리
			if(columnItemVO.isRemoveHtml()) {
				columnData = Utils.removeHtmlTag( String.valueOf(dataMap.get(columnName)) );
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// 불필요한 공잭 제거  처리
			if(columnItemVO.isRemoveSpace()) {
				columnData = Utils.removeSpace( String.valueOf(dataMap.get(columnName)) );
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// 해쉬함수를 적용한 값으로 대체
			if(columnItemVO.isEncodeHash()) {
				columnData = CodecUtil.encodeHash(String.valueOf(dataMap.get(columnName)));
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// BASE64 디코딩을 적용한 값으로 대체
			if(columnItemVO.isDecodeBase64()) {
				columnData = CodecUtil.decodeBase64(String.valueOf(dataMap.get(columnName)));
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
				
			///////// split 처리 
			separator = columnItemVO.getSeparator();
			columnData = Utils.objToString( dataMap.get(columnName));
			if (separator != null && columnData != null) {
				listData = new ArrayList<> ();
				splitData = columnData.split(separator);
				
				// trim 처리
				for(int index = 0; index < splitData.length; index++) {
					if( splitData[index].trim().length() > 0) {
						listData.add(splitData[index].trim());
					}
				}
				
				// split 처리한 데이터를 다시 dataMap에 저장한다.
				dataMap.put(columnName, listData);
			}
			
			///////// copy_to 처리 
			copyColumns = columnItemVO.getCopyTo();
			if(copyColumns != null) {
				for(String copyToColumnName : copyColumns) {
					if(dataMap.containsKey(columnName)) {
						if(dataMap.containsKey(copyToColumnName)) {
							logger.warn("copyto ({} -> {}) is overwrited, copyto's column already exists.", columnName, copyToColumnName);
						}
						dataMap.put(copyToColumnName, dataMap.get(columnName));
					} else {
						logger.warn("copyto ({} -> {}) process is canceled. column's vaule = null", columnName, copyToColumnName);
					}
				}
			}
			
			///////// rename_to 처리 
			renameColumn = columnItemVO.getRenameTo();
			if(renameColumn != null) {
				if(!dataMap.containsKey(columnName)) {
					logger.warn("renameto ({} -> {}) process is canceled. source column({}) not exists.", columnName, renameColumn, columnName);
				} else if (dataMap.containsKey(renameColumn)) {
					logger.warn("renameto ({} -> {}) process is canceled. dest column({}) already exists.", columnName, renameColumn, renameColumn);
				} else {
					dataMap.put(renameColumn, dataMap.remove(columnName));
				}
			}
		}
	}
	
	/**
	 * fileList에 해당하는 파일을 다운(복사)받고 필터링하여 처리한 결과를 resultMap에 merge 한다.
	 * 
	 * @param fileDataList 파일정보 목록
	 * @param resultMap 색인 데이터 맵 (파일 처리 결과도 담음)
	 * @param fileLogVO 로깅 객체
	 * @throws IOException
	 */
	protected void mergeFileContentData(List<Map<String, Object>> fileDataList, Map<String, Object> resultMap, CountLogVO fileLogVO) throws IOException {
		StringBuilder sBuilder = null;
		
		String remoteFilePath = null;
		String showFileName = null;
		String downFileName = null;
		File downloadFile = null;
		File filteredFile = null;
		File sourceFile = null;
		long fileSize;
		
		boolean isAllowExt = false;
		
		if(fileDataList != null && fileDataList.size() > 0) {
			sBuilder = new StringBuilder();
			String fileSizeColumnName = this.dfVO.column.getFileSizeColumnName();
	
			fileLogVO.setTotalCount(fileDataList.size());
			for (Map<String, Object> fileMap : fileDataList) {
				// 2026.11.18 반복 간 임시파일 참조가 남아 finally에서 이전 파일을 다시 건드리지 않도록 초기화
				downloadFile = null;
				sourceFile = null;
				filteredFile = null;
				
				remoteFilePath = (String) fileMap.get(this.dfVO.column.getFilePathColumnName());
				if(remoteFilePath == null) {
					throw new NullPointerException("Can't read remoteFilePath: columnName=" + this.dfVO.column.getFilePathColumnName());
				}
				
				showFileName = (String) fileMap.get(this.dfVO.column.getFileNameColumnName()); // show 파일명
				if(showFileName == null) {
					throw new NullPointerException("Can't read showFileName: columnName=" + this.dfVO.column.getFileNameColumnName());
				}
				
				downFileName = Utils.getFileName(remoteFilePath); //물리 파일명
				
				if(fileSizeColumnName != null) {
					fileSize = Utils.objToLong(fileMap.get(fileSizeColumnName)) ;
					if(this.allowFileMaxSize < fileSize ) {
						logger.warn("Exceed max file size. [{} - {}]", showFileName, fileSize);
						fileLogVO.increaseFailCount("Exceed max file size. [" + showFileName +  " - " + fileSize + "]");
						continue;
					}
				}
				
				// 사용가능 확장자. OCR 대상 확장자(이미지/스캔)는 일반 필터 허용목록에 없어도 통과시킨다.
				isAllowExt = allowExt(showFileName) || isOcrTarget(showFileName);
				if(isAllowExt) {
					try {
						downloadFile = this.fileHelper.download(remoteFilePath, downFileName);
						if(downloadFile.exists()) {
							// 2023.08.07 정충열 : DRM 복호화를 수행한다.
							if(this.isExecDrmDecode) {
								sourceFile = this.fileHelper.decodeDrm(downloadFile);
							} else {
								sourceFile = downloadFile;
							}
							
							if(isOcrTarget(showFileName)) {
								// 2026.11.18 이미지/스캔 문서 → OCR API로 텍스트 인식 후 fileContent에 색인
								//  (복호화가 끝난 sourceFile의 실제 바이트를 그대로 전송)
								String ocrText = this.ocrClient.extractText(sourceFile, showFileName);
								if(ocrText != null && ocrText.length() > 0) {
									logger.debug("Success to OCR text from file. [{}]", showFileName);
									sBuilder.append(ocrText);
									sBuilder.append(FILE_SEPERATOR);
									fileLogVO.increaseSuccessCount();
								} else {
									logger.warn("OCR returned empty text. [{}]", showFileName);
									fileLogVO.increaseWarnCount("OCR returned empty text. [" + showFileName + "]");
								}
							} else {
								filteredFile = new File(sourceFile.getPath() + ".txt");
								
								// 파일로 부터 텍스트를 추출한다.
								this.fileHelper.filterText(sourceFile, Utils.getFileExt(showFileName), filteredFile);
								if(filteredFile.exists()) {
									logger.debug("Success to Extract text from file.");
									Utils.getFileText(filteredFile.getAbsolutePath(), "\n", sBuilder);
									sBuilder.append(FILE_SEPERATOR);
									fileLogVO.increaseSuccessCount();

								} else {
									logger.warn("Fail to Extract text from file [{}]", filteredFile.getPath());
									fileLogVO.increaseWarnCount("Fail to Extract text from file " + "[" + filteredFile.getPath() + "]");
								}
							}
						} else {
							logger.warn("Can't find downloaded(copied) file [{}]", downloadFile.getPath());
							fileLogVO.increaseFailCount("Can't find downloaded(copied) file " + "[" + downloadFile.getPath() + "]");
						}
						
					} catch (Throwable t) {
						logger.error("Occur exception: {} [{}]", t.toString(), remoteFilePath, t);
						fileLogVO.increaseFailCount(t.toString() + " [" + remoteFilePath + "]");
					} finally {
						if(sourceFile != null && sourceFile.exists()) try { sourceFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };
						if(downloadFile != null && downloadFile.exists()) try { downloadFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };
						if(filteredFile != null && filteredFile.exists()) try { filteredFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };

//////////////////////////////////////////////
						// 다운로드 받은 파일 경로 하위의 모든 것 삭제
						// (download() 자체가 실패(예: 원본 파일이 존재하지 않음)하면 downloadFile이 null일 수 있으므로 방어)
						if(downloadFile != null) {
							logger.info("다음 경로의 하위 경로내용 모든것 삭제 수행: {} ", Paths.get(downloadFile.getParentFile().getAbsolutePath()));
							deleteAllChildren(Paths.get(downloadFile.getParentFile().getAbsolutePath()));
						}
			/////////////////////////////////////////////////////////////
					}
					
				} else {
					logger.warn("Unsupported file extension. [{}]", showFileName);
					fileLogVO.increaseWarnCount("Unsupported file extension. [" + showFileName + "]");
				}
			} // for
			
			if(sBuilder.length() > 0) {
				resultMap.put(this.dfVO.field.getFileDataFieldName(), sBuilder.toString());
			}
		}
	}
	
	/**
	 * 파일 필터링 처리 대상 확장자인지를 알아낸다.
	 * 
	 * @param fileName 파일명(논리 파일명)
	 * @return 대상확장자면 true, 아니면 false
	 */
	protected boolean allowExt(String fileName) {
		boolean isResult = false;
		if(this.allowExtMap == null) {
			// 설정되지 않았다면 무조건 대상 확장자로 간주
			isResult = true;
		} else {
			// 설정되지 되었더면 허용확장자 목록에 해당되는지 조사
			String fileExt = Utils.getFileExt(fileName);
			if(fileExt != null) {
				isResult = this.allowExtMap.containsKey(fileExt.toUpperCase());
			}
		}
		
		return isResult;
	}
	
	/**
	 * OCR(이미지/스캔 문서 텍스트 인식) 처리 대상 확장자인지 판단한다.
	 * <p/>
	 * OCR 클라이언트가 활성화되어 있고(BULKER.OCR.URL 설정), 논리 파일명의 확장자가
	 * BULKER.OCR.ALLOWED_EXT 목록에 포함될 때만 true.
	 * 
	 * @param fileName 파일명(논리 파일명)
	 * @return OCR 대상이면 true, 아니면 false
	 * @since 2026.11.18
	 */
	protected boolean isOcrTarget(String fileName) {
		if(this.ocrClient == null || this.ocrExtMap == null) {
			return false;
		}
		String fileExt = Utils.getFileExt(fileName);
		if(fileExt == null) {
			return false;
		}
		return this.ocrExtMap.containsKey(fileExt.toUpperCase());
	}
	
	protected void printLog(int current, BulkerLogVO bulkerLogVO, DataLogVO dataLogVO, String indexAction) {
		if(!this.printDetail) {
			if(current % this.printInterval == 0) {
				this.printSimpleLog(bulkerLogVO.query.getTotalCount(), current);
			}
		} else {
			this.printDetailLog(dataLogVO, bulkerLogVO.query.getTotalCount(), current, indexAction);
		}
		
		// 2025.10.04 파일 에러 로그만 별도 기록
		if(this.fileHelper != null) {
			CountLogVO logVO = dataLogVO.fileLog;
			
			if(logVO.getFailCount() > 0) {
				errLogger.info("{}\tFAIL count={}\t{}", dataLogVO.getKey(), logVO.getFailCount(), logVO.getFailMessage());
			}
			if(logVO.getWarnCount() > 0) {
				errLogger.debug("{}\tWARN count={}\t{}", dataLogVO.getKey(), logVO.getWarnCount(), logVO.getWarnMessage());
			}
		}
	}
	
	
	@Deprecated
	protected void printLog(int total, int idx) {
		logger.info("{} / {} data is fetched.", idx, total);
	}
	
	/**
	 * 색인수집 처리 로그를 간략히 출력한다.
	 * @param total
	 * @param idx
	 */
	protected synchronized void printSimpleLog(int total, int idx) {
		logger.info("{} / {} data is fetched.", idx, total);
	}
	/**
	 * 색인수집 처리 로그를 자세히 출력한다.
	 * 
	 * @param dataLogVO 로깅 객체
	 * @param idx 색인 키 데이터 카운트
	 * @param idx 색인 키 데이터 내 순번
	 * @param indexAction 색인 액션
	 */
	protected synchronized void printDetailLog(DataLogVO dataLogVO, int total, int idx, String indexAction) {
		String dataMsg = null;
		CountLogVO logVO = null;
		
		if(dataLogVO.getErrorMessage() == null) {
			dataMsg = "Success";
		} else {
			dataMsg = "Fail, Message : " + dataLogVO.getErrorMessage();
		}
		
		logger.info(BulkerUtils.LOG_DATA_LINE);
		logger.info("- [{}/{} - IndexAction: {}]  ", idx, total, indexAction);
		logger.info("- [key {}]  ", dataLogVO.getKey());
		logger.info("- [Data] {}", dataMsg);
		
		if(this.fileHelper != null) {
			// file 처리 로그 출력
			logVO = dataLogVO.fileLog;
			
			if(logVO.getTotalCount() > 0) {
				logger.info("- [File] Total: {}, Success: {}", logVO.getTotalCount(), logVO.getSuccessCount());
				if(logVO.getWarnCount() > 0) {
					logger.info("         Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
				}
				if(logVO.getFailCount() > 0) {
					logger.info("         Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
				}
			}
		}
		
		if(this.etcReader != null) {
			// etcReader 로그 출력
			logVO = dataLogVO.etcLog;
			
			if(logVO.getTotalCount() > 0) {
				logger.info("- [Etc ] Total: {}, Success: {}", logVO.getTotalCount(), logVO.getSuccessCount());
				if(logVO.getWarnCount() > 0) {
					logger.info("         Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
				}
				if(logVO.getFailCount() > 0) {
					logger.info("         Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
				}
			}
		}
		logger.info(BulkerUtils.LOG_DATA_LINE);
	}
}
