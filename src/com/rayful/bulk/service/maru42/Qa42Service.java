package com.rayful.bulk.service.maru42;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.opencsv.CSVWriter;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * QS42 솔루션 관련 덤프파일 생성 및 색인요청 등을 담당하는 클래스
 */
public class Qa42Service {
	private static final Logger logger = LoggerFactory.getLogger(Qa42Service.class);
	public static final String FILE_TIME_FORMAT = "yyyMMdd-HHmmss.S";
	public static final String FOLDER_TIME_FORMAT = "yyyMMdd";
	
	public static String FIELD_SEPARATOR = null;
	public static String MULTI_SEPARATOR = null;
	
	
	private final RestTemplate restTemplate;
	protected final Gson gson;
	
	protected final Path qaImsiResultBasePath;
	protected final Path qaDestResultBasePath;
	protected final String qaDateFileName;
	protected final String qaLockFileName;

	protected final List<String> sourceFieldList;
	protected final List<String> targetFieldList;
	
	protected final String apiRequestIndexUrl;
	protected final String apiProjectKey;
	protected final Map<String, String> dateFieldMap;
	
	private int fileSequence = 0;
	private int dataCount = 0;
	
	/**
	 * 생성자 
	 * 
	 * @param prop 설정파일 정보
	 * @throws FileNotFoundException 설정에 정의된 경로가 존재하지 않을 때
	 * @throws IllegalArgumentException 설정이 존재하지 않을 때
	 */
	public Qa42Service(Properties prop) throws FileNotFoundException {
		this.restTemplate = BulkerUtils.getInstanceRestTemplate();
		this.gson = BulkerUtils.getGsonS24();
		
		String imsiBasePath = prop.getProperty("BULKER.QA42.PATH.RESULT.IMSI");
		if(imsiBasePath == null || imsiBasePath.trim().length() == 0) {
			logger.error("[QA] Not found setting ... BULKER.QA42.PATH.RESULT.IMSI");
			throw new IllegalArgumentException("[QA] Not found setting ... BULKER.QA42.PATH.RESULT.IMSI");
		} else if (!Files.exists(Paths.get(imsiBasePath))) {
			logger.error("[QA] Not exist imsi result path: {}", imsiBasePath);
			throw new FileNotFoundException("[QA] Not exist imsi result path:" + imsiBasePath);
		}
		this.qaImsiResultBasePath = Paths.get(imsiBasePath.trim());
		
		String destBasePath = prop.getProperty("BULKER.QA42.PATH.RESULT.DEST");
		if(destBasePath == null || destBasePath.trim().length() == 0) {
			logger.error("[QA] Not found setting ... BULKER.QA42.PATH.RESULT.DEST");
			throw new IllegalArgumentException("[QA] Not found setting ... BULKER.QA42.PATH.RESULT.DEST");
		} else if (!Files.exists(Paths.get(destBasePath))) {
			logger.error("[QA] Not exist dest result path: {}", destBasePath);
			throw new FileNotFoundException("[QA] Not exist dest result path:" + destBasePath);
		}
		this.qaDestResultBasePath = Paths.get(destBasePath.trim());
		
		this.qaDateFileName = prop.getProperty("BULKER.QA42.DATE.FILENAME");
		if(this.qaDateFileName == null || this.qaDateFileName.trim().length() == 0) {
			logger.error("[QA] Not found setting ... BULKER.QA42.DATE.FILENAME");
			throw new IllegalArgumentException("[QA] Not found setting ... BULKER.QA42.DATE.FILENAME");
		}
		
		this.qaLockFileName = prop.getProperty("BULKER.QA42.LOCK.FILENAME");
		if(this.qaLockFileName == null || this.qaLockFileName.trim().length() == 0) {
			logger.error("[QA] Not found setting ... BULKER.QA42.LOCK.FILENAME");
			throw new IllegalArgumentException("[QA] Not found setting ... BULKER.QA42.LOCK.FILENAME");
		}
		
		Qa42Service.FIELD_SEPARATOR = prop.getProperty("BULKER.QA42.DUMP.SEPARATOR.FIELD", ",");
		Qa42Service.MULTI_SEPARATOR = prop.getProperty("BULKER.QA42.DUMP.SEPARATOR.MULI", ";");
		
		String dumpFields = prop.getProperty("BULKER.QA42.DUMP.FIELDS");
		if(dumpFields == null ||dumpFields.trim().length() == 0) {
			logger.error("[QA] Not found setting ... BULKER.QA42.DUMP.FIELDS");
			throw new IllegalArgumentException("[QA] Not found setting ... BULKER.QA42.DUMP.FIELDS");
		}
		
		this.sourceFieldList = new ArrayList<String>();
		this.targetFieldList = new ArrayList<String>();
		String [] arrField = dumpFields.split(",");
		String [] arrFieldMap = null;
		for(String fieldMapping : arrField) {
			arrFieldMap = fieldMapping.split(":");
			if(arrFieldMap.length == 2) {
				this.sourceFieldList.add(arrFieldMap[0]);
				this.targetFieldList.add(arrFieldMap[1].toLowerCase());
			} else if(arrFieldMap.length == 1) {
				this.sourceFieldList.add(fieldMapping);
				this.targetFieldList.add(fieldMapping.toLowerCase());
			} else {
				throw new IllegalArgumentException("[QA] Invalid foramt ... BULKER.QA42.DUMP.FIELDS");
			}
		}
		logger.debug("sourceFieldList={}", sourceFieldList);
		logger.debug("targetFieldList={}", targetFieldList);
		
		this.apiRequestIndexUrl = prop.getProperty("QA42.API.REQUEST_INDEX.URL");
		if(this.apiRequestIndexUrl == null || this.apiRequestIndexUrl.length() == 0) {
			logger.error("[QA] Not found setting ... QA42.API.REQUEST_INDEX.URL");
			throw new IllegalArgumentException("[QA] Not found setting ... QA42.API.REQUEST_INDEX.URL");	
		}
		
		this.apiProjectKey = prop.getProperty("QA42.API.PROJECT_KEY");
		if(this.apiProjectKey == null || this.apiProjectKey.length() == 0) {
			logger.error("[QA] Not found setting ... QA42.API.PROJECT_KEY");
			throw new IllegalArgumentException("[QA] Not found setting ... QA42.API.PROJECT_KEY");	
		}
		logger.debug("apiRequestIndexUrl={}, apiProjectKey={}", this.apiRequestIndexUrl, this.apiProjectKey);
		
		String dateFields = prop.getProperty("BULKER.QA42.DUMP.DATE.FIELDS");
		this.dateFieldMap = new HashMap<String, String>();
		if(dateFields != null && dateFields.trim().length() > 0) {
			String [] arrDateField = dateFields.split(",");
			for(String field : arrDateField) {
				this.dateFieldMap.put(field, null);
			}
		}
		logger.debug("dateFieldMap={}", dateFieldMap);
		
	}
	
	/**
	 * 덤프파일 형식으로 데이터를 재구성한다.
	 * @param dataList 조회 데이터 목록
	 * @param dfVO DF 정보
	 * @return 재구성 데이터
	 */
	protected Map<String, List<Object>> getResultData(List<Map<String, Object>> dataList, DFLoadVO dfVO) {
		Map<String, List<Object>> resultMap = new HashMap<String, List<Object>> ();
		
		List<Object> indexList = new ArrayList<Object> ();
		List<Object> updateList = new ArrayList<Object> ();
		List<Object> deleteList = new ArrayList<Object> ();
		
		resultMap.put("I", indexList);
		resultMap.put("U", updateList);
		resultMap.put("D", deleteList);
		
		if(dataList != null && dataList.size() > 0) {
			List<Object> resultList = null;
			String indexAction = null;
			Map<String, String> outputDataMap = null;
			Object objectData = null;
			StringBuilder sbBody = new StringBuilder();
			String description = null;
			String key = null;
			
			for(Map<String, Object> dataMap: dataList) {
				outputDataMap = new LinkedHashMap<String, String> ();
				key = (String) dataMap.get(dfVO.column.getKeyColumnName());
				indexAction = (String)dataMap.get(dfVO.column.getIndexActionColumnName());
				if(indexAction == null) {
					throw new NullPointerException("indexAction is required.");
				}
				
				if("U".equals(indexAction)) {
					resultList = updateList;
				} else if("D".equals(indexAction)) {
					resultList = deleteList;
				} else {
					resultList = indexList;
				}
				
				if(!"D".equals(indexAction)) {
					// 삭제 액션이 아니라면 ... body process.
					
					String strValue = null;
					for(int index = 0; index < this.sourceFieldList.size(); index ++) {
						if(index > 0) {
							sbBody.append(FIELD_SEPARATOR);
						}
						objectData = dataMap.get(this.sourceFieldList.get(index));
						
						if(objectData instanceof List) {
							// .......... Multi value (List)
							sbBody = new StringBuilder();
							@SuppressWarnings("unchecked")
							List<String> multiValues = (List<String>) objectData;
							for(int subIndex = 0; subIndex < multiValues.size(); subIndex ++) {
								if(subIndex > 0) {
									sbBody.append(MULTI_SEPARATOR);
								}
								sbBody.append(multiValues.get(subIndex));
							}
							strValue = sbBody.toString();
						} else if(objectData instanceof String []) {
							// .......... Multi value (STring [])
							sbBody = new StringBuilder();
							String [] multiValues = (String []) objectData;
							for(int subIndex = 0; subIndex < multiValues.length; subIndex ++) {
								if(subIndex > 0) {
									sbBody.append(MULTI_SEPARATOR);
								}
								sbBody.append(multiValues[subIndex]);
							}
							strValue = sbBody.toString();
						} else {
							// .......... String value
							
							if(this.dateFieldMap.containsKey(this.sourceFieldList.get(index))) {
								if(objectData instanceof String) {
									// Convert to date format string
									strValue = (String)objectData;
									strValue = strValue.replace("/", "").replace("-", "").replace(".", "").replace(":", "");
									if(strValue.length() >= 14) {
										strValue = strValue.substring(0, 4)
												+ "-" + strValue.substring(4, 6)
												+ "-" + strValue.substring(6, 8)
												+ " " + strValue.substring(8, 10)
												+ ":" + strValue.substring(10, 12)
												+ ":" + strValue.substring(12, 14);
									} else {
										strValue = strValue.substring(0, 4)
												+ "-" + strValue.substring(4, 6)
												+ "-" + strValue.substring(6, 8)
												+ " 00:00:01";
									}
								} else {
									strValue = Utils.objToString(objectData);
								}
							} else {
								strValue = Utils.objToString(objectData);
							}
						}
						
						outputDataMap.put(this.targetFieldList.get(index), strValue);
					} // for(int index = 0 ...
					
					description = outputDataMap.get("description");
					if(description == null || description.trim().isEmpty()) {
						logger.warn("Description is empty: key={}", key);
						continue;
					}
					
					outputDataMap.put("agent_apply", "X");
					outputDataMap.put("agent_apply_detail", "NULL");
					outputDataMap.put("agent_apply_num", "0");
				} else {
					// 삭제 액션이라면 ... 
					String id = Utils.objToString(dataMap.get(dfVO.column.getKeyColumnName()));
					
					// [@@@ 주의사항 @@@]targetFieldList 의 첫번째는 ID 필드여야 함.
					outputDataMap.put(this.targetFieldList.get(0), id);
					outputDataMap.put("agent_apply", "NULL");
					outputDataMap.put("agent_apply_detail", "NULL");
					outputDataMap.put("agent_apply_num", "0");
				}
				resultList.add(outputDataMap);
			} // for(Map<String, Object> dataMap ...
		}
		
		return resultMap;
	}
	
	/**
	 * csv 형식으로 덤프파일을 생성한다.
	 * @param dfName DF파일명
	 * @param indexDate 색인일시
	 * @param dataList 
	 * @param dfVO
	 * @param bulkerLogVO
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void makeCsv(String dfName, String indexDate, List<Map<String, Object>> dataList, DFLoadVO dfVO, BulkerLogVO bulkerLogVO) throws IOException {
		String fileName = null;
		Map<String, List<Object>> resultMap = this.getResultData(dataList, dfVO);
		List<Object> resultList = null;
		int maxRows = dfVO.getMaxRows();
		final String [] arrStr = new String [1];
		
		Path parentDfFolder = null;
		Path parentDateFolder = null;
		Path destFile = null;
		
		BufferedWriter bw = null;
		CSVWriter csvWriter = null;
		
		int count = 0;
		int dataRows = 0;
		String dateStr = null;
		
		Map<String, String> headerMap = null;
		Map<String, String> dataMap = null;
		
		CountLogVO logVO = bulkerLogVO.qadump;
		
		//indexAction : I or U or D
		for(String indexAction: resultMap.keySet()) {
			resultList = resultMap.get(indexAction);
			if(resultList != null && resultList.size() > 0) {
				parentDfFolder = Paths.get(this.qaImsiResultBasePath.toString(), dfName);
				parentDateFolder = Paths.get(parentDfFolder.toString(), indexDate.replace("-", "").substring(0, 8));
				destFile = null;
				if(!Files.exists(parentDateFolder)) {
					Files.createDirectories(parentDateFolder);
				}
				
				try {
					count = 0;
					dataRows = 0;
					// 날짜 포맷: yyyyMMdd-HHmmssz
					dateStr = indexDate.replace("-", "").replace(" ", "-").replace(":", "") + "001";
					
					for(int index = 0; index < resultList.size(); index ++) {
						if(bw == null) {
							fileName = dateStr
									+ "_POST_"
									+ String.format("%06d", ++ this.fileSequence) + "_"
									+ String.format("%06d", Math.min(resultList.size() - dataRows, maxRows)) + "_"
									+ indexAction
									+ ".csv";
							destFile = Paths.get(parentDateFolder.toString(), fileName);
							bw = Files.newBufferedWriter(destFile, StandardCharsets.UTF_8
									, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
							csvWriter = new CSVWriter(bw);
							
							headerMap = (Map<String, String>) resultList.get(0);
							csvWriter.writeNext(headerMap.keySet().toArray(arrStr));
							
							logVO.increaseSuccessCount();
						}
						
						dataMap = (Map<String, String>) resultList.get(index);
						csvWriter.writeNext(dataMap.values().toArray(arrStr));
						
						count ++;
						if(count == maxRows) {
							if(csvWriter != null) try { csvWriter.close(); } catch(IOException ioe) { /* close 실패 무시(자원정리) */ }
							if(bw != null) try { bw.close(); } catch(IOException ioe) { /* close 실패 무시(자원정리) */ }
							csvWriter = null;
							bw = null;
							
							dataRows += count;
							logger.info("[QA] success to save QA dump file: {}, row count: {}", destFile, count);
							count = 0;
						}
					} // for(int index ...
					dataRows += count;
					this.dataCount += dataRows;
					if(count > 0) {
						logger.info("[QA] success to save QA dump file: {}, row count: {}", destFile, count);
					}
				} catch (IOException e) {
					logVO.increaseFailCount(e.toString());
					throw e;
				} finally {
					if(csvWriter != null) try { csvWriter.close(); } catch(IOException ioe) { /* close 실패 무시(자원정리) */ }
					if(bw != null) try { bw.close(); } catch(IOException ioe) { /* close 실패 무시(자원정리) */ }
					csvWriter = null;
					bw = null;
				}
			}
		} // for(String indexAction ...
	}
	
	public void updateIndexDate(String dfName, String indexDate) throws Exception {
		try {
			Path parentDfFolder = Paths.get(this.qaImsiResultBasePath.toString(), dfName);
			if(Files.exists(parentDfFolder)) {
				Path destFile = Paths.get(parentDfFolder.toString(), this.qaDateFileName);
				try(BufferedWriter bw = Files.newBufferedWriter(destFile, StandardCharsets.UTF_8
						, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					bw.append(indexDate);
				} 
	
				logger.info("[QA] Success to update index date in file of imsi folder: {}", destFile);
			} else {
				logger.warn("[QA] Fail to update index date in file of imsi folder: Not exists folder- {}", parentDfFolder);
			}
		} catch(Exception e) {
			logger.error("[QA] Fail to update index date in file of imsi folder: {}", e.toString());
			throw e;
		}
	}
	
	public boolean moveToQaFolder(String dfName, String indexMode) {
		boolean isSuccess = false;
		
		String middleFolder = null;
		if("all".equals(indexMode)) {
			middleFolder = "full_dump";
		} else {
			middleFolder = "increment_dump";
		}
		
		try {
			Path sourceFolder = Paths.get(this.qaImsiResultBasePath.toString(), dfName);
			Path destFolder = Paths.get(this.qaDestResultBasePath.toString(), middleFolder, this.apiProjectKey, "IN_PROGRESS", dfName);
			Path lockFilePath = destFolder.resolve(this.qaLockFileName);
			
			if(!Files.exists(lockFilePath)) {
				if(Files.exists(sourceFolder)) {
					if(!Files.exists(destFolder)) {
						Files.createDirectories(destFolder);
					}
					
					// Move folders and files (날짜 폴더 및 덤프파일)
					logger.info("[QA] Move QA dump: {} -> {}", sourceFolder, destFolder);
					Utils.moveDirectory(sourceFolder, destFolder);
					isSuccess = true;
				}
			} else {
				logger.warn("[QA] Can't Move QA dump. because exists QA lock file: {}", lockFilePath);
			}
		} catch(Exception e) {
			logger.error("[QA] Can't Move QA dump.: {}", e.toString());
		}
		
		return isSuccess;
	}
	
	public boolean requestIndex(String dfName, String indexDate, String indexMode) {
		boolean isSuccess = false;
		
		try {
			// URL
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(this.apiRequestIndexUrl);
			String url = builder.build().toUriString();
			
			// HTTP HEADER 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			
			// REQUEST BODY 설정
			Map<String, Object> bodyMap = new HashMap<String, Object>();
			bodyMap.put("project_id", this.apiProjectKey);
			bodyMap.put("source_id", dfName);
			bodyMap.put("source_name", dfName);
			bodyMap.put("source_type", "PASSAGE");
			bodyMap.put("increment", "ID");
			// 날짜 포맷: yyyyMMddHHmmss.z
			bodyMap.put("date", indexDate.replace("-", "").replace(" ", "").replace(":", "") + ".001");
			bodyMap.put("dump_file_count", this.fileSequence);
			bodyMap.put("dump_data_count", this.dataCount);
			bodyMap.put("attach_file_count", 0);
			if("all".equals(indexMode)) {
				bodyMap.put("dump_type", "FULL");
			} else {
				bodyMap.put("dump_type", "INCREMENT");
			}
			
			String requestBody = this.gson.toJson(bodyMap);
			
			logger.info("[QA] =======================================================");
			logger.info("[QA] [REQUEST URL] {}", url);
			logger.info("[QA] [REQUEST HEADER] Content-Type={} (Api-Key masked)", headers.getContentType());
			logger.info("[QA] [REQUEST BODY] {}", requestBody);
			logger.info("[QA] =======================================================");
			
			HttpEntity<String> entity = new HttpEntity<String> (requestBody, headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> responseMap = this.restTemplate.postForObject(url, entity, Map.class);
			
			logger.info("[ENTITY] [RESPONSE] {}", responseMap);
			if(responseMap.containsKey("response_code")) {
				isSuccess = "C20000".equals((String) responseMap.get("response_code"));
				logger.info("[QA] Success to Request Index to QA Server.");
			} else {
				logger.warn("[QA] Fail to Request Index to QA Server: fail");
			}
		} catch(Exception e) {
			logger.error("[QA] Fail to Request Index to QA Server: {}", e.toString());
		}
		
		return isSuccess;
	}
}
