package com.rayful.bulk.service.es;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rayful.bulk.service.BaseBulkerService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.ColumnVO;

/**
 * ElasticSearch를 위한 색인 데이터 수집 관련 데이터 조회 및 첨부파일 처리를 담당하는 서비스 클래스
 */
public class BulkerService extends BaseBulkerService{
	private static final Logger logger = LoggerFactory.getLogger(BulkerService.class);
	
	/**
	 * 생성자
	 * 
	 * @param dfVO DF 정보
	 * @param prop 설정파일 정보
	 * @throws Exception EtcClass 생성 실패 시
	 * @throws FileNotFoundException 설정에 정의된 경로가 존재하지 않을 때
	 * @throws IllegalArgumentException  설정이 존재하지 않을 때
	 */
	public BulkerService(DFLoadVO dfVO, Properties prop) throws Exception {
		super(dfVO, prop);
	}
	
	@Override
	public List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode) {
		List<Object> resultList = new ArrayList<Object>();
		if(dataList != null && dataList.size() > 0) {
			String keyId = this.dfVO.column.getKeyColumnName();
			String indexAction = null;
			Map<String, Object> outputDataMap = null;
			Map<String, Object> updateDataMap = null;
			List<String> noOutputColumnNameList = null;
			
			List<Map<String, Object>> fileDataList = null;
			List<Map<String, Object>> outputFileDataList = null;
			Map<String, Object> outputFileDataMap = null;
			String curIndexName = null;
			String indexPatternColumnValue = null;
			
			for (Map<String, Object> dataMap : dataList) {
				Map<String, Map<String, Object>> keyMap = new HashMap<String, Map<String, Object>>();
				Map<String, Object> subKeyMap = new HashMap<String, Object>();
				
				if(this.dfVO.isIncludeIndexname()) { //index name을  bulk file에 추가
					indexPatternColumnValue = Utils.objToString(dataMap.get(dfVO.column.getIndexPatternColumnName()));
					if(indexPatternColumnValue == null) {
						throw new NullPointerException("\"" + dfVO.column.getIndexPatternColumnName() + "\" column's value must be not null" );
					}
					curIndexName = BulkerUtils.applyIndexNamePattern(dfVO.getIndexName(), indexPatternColumnValue) ;
					subKeyMap.put("_index", curIndexName);
				}
				
				indexAction = (String) dataMap.get(this.dfVO.column.getIndexActionColumnName());
				
				/*
				 * ---------------------------------
				 * Delete
				 * ---------------------------------
				 * {"delete" : {"_id" : "keyId"}} 
				 */
				if("D".equalsIgnoreCase(indexAction)) {
					//subKeyMap.put("_index", dfVO.getIndexName());
					subKeyMap.put("_id", dataMap.get(keyId));
					keyMap.put("delete", subKeyMap);
					resultList.add(keyMap);
				
				} else {
					outputDataMap = new HashMap<String, Object>();
					// Shallow copy.
					outputDataMap.putAll(dataMap);

					// Remove No output field. - dataQuery.
					noOutputColumnNameList = this.dfVO.column.getNoOutputColumnNameList(ColumnVO.QUERY_DATA);
					for(String noOutputColumnName : noOutputColumnNameList) {
						if(outputDataMap.remove(noOutputColumnName) != null) {
							logger.debug("Data Column({})'s data is removed.", noOutputColumnName);
						}
					}
					
					// Remove No output field. - fileQuery
					if (this.dfVO.field.isIndexFileInfo()) {
						fileDataList = (List<Map<String, Object>>) outputDataMap.get(this.dfVO.field.getFileInfoFieldName());
						if(fileDataList != null && fileDataList.size() > 0) {
							outputFileDataList = new ArrayList<Map<String, Object>>();
							for(Map<String, Object> fileDataMap: fileDataList) {
								outputFileDataMap = new HashMap<String, Object>();
								outputFileDataMap.putAll(fileDataMap);
								noOutputColumnNameList = this.dfVO.column.getNoOutputColumnNameList(ColumnVO.QUERY_FILE);
								for(String noOutputColumnName : noOutputColumnNameList) {
									if(outputFileDataMap.remove(noOutputColumnName) != null) {
										logger.debug("File Column({})'s data is removed.", noOutputColumnName);
									}
								}
								outputFileDataList.add(outputFileDataMap);
							}
							outputDataMap.put(this.dfVO.field.getFileInfoFieldName(), outputFileDataList);
						}
					}
					
					/*
					 * ---------------------------------
					 * Update
					 * ---------------------------------
					 * {"update" : {"_id" : "keyId"}}
					 * {"doc" : {"fieldName1" : "value1"}}
					 */
					if("U".equalsIgnoreCase(indexAction)) {
						subKeyMap.put("_id", dataMap.get(keyId));
						keyMap.put("update", subKeyMap);
						
						updateDataMap = new HashMap<String, Object>();
						updateDataMap.put("doc", outputDataMap);
						
						resultList.add(keyMap);
						resultList.add(updateDataMap);
					
					/*
					 * ---------------------------------
					 * Index
					 * ---------------------------------
					 * {"index" : {"_id" : "keyId"}}
					 * {"fieldName1" : "value1", "fieldName2" : "value2", ...}
					 */
					} else {
						subKeyMap.put("_id", dataMap.get(keyId));
						keyMap.put("index", subKeyMap);
						resultList.add(keyMap);
						resultList.add(outputDataMap);
					}
				}
			}
		}
		
		return resultList;
	}
	
	@Override
	public List<Object> getDeleteResultData(List<Map<String, Object>> dataList) {
		List<Object> resultList = new ArrayList<Object>();
		
		if(dataList != null && dataList.size() > 0) {
			logger.info("------------------------------------------");
			Map<String, Map<String, Object>> keyMap = null;
			Map<String, Object> keyMapSub = null;
			for (Map<String, Object> dataMap : dataList) {
				keyMap = new HashMap<String, Map<String, Object>>();
				keyMapSub = new HashMap<String, Object>();
				keyMapSub.put("_id", dataMap.get(this.dfVO.column.getKeyColumnName()));
				keyMap.put("delete", keyMapSub);
				resultList.add(keyMap);
				logger.info("- ID : " + dataMap.get(this.dfVO.column.getKeyColumnName()));
			}
		}
		
		return resultList;
	}
	
	@Override
	public File makeJson(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws IOException {
		String fileBaseName = this.dfVO.getResultFileName();
		String filePath = this.resultReadyPath + File.separator + this.dfVO.getDfName();
		String fileName = null;
		File file = null;
		BufferedWriter bw = null;
		CountLogVO logVO = bulkerLogVO.dump;
		Gson gson = BulkerUtils.getGson();
		
		
		if(resultDataList != null && resultDataList.size() > 0) {
			if("index".equalsIgnoreCase(indexType)) {
				fileName = "index_" + fileBaseName + "_" + Utils.getNow() + ".json";
			} else if("update".equalsIgnoreCase(indexType)) {
				fileName = "update_" + fileBaseName + "_" + Utils.getNow() + ".json";
			} else {
				fileName = "delete_" + fileBaseName + "_" + Utils.getNow() + ".json";
			}
			
			file = new File(filePath, fileName);
			
			// 부모 폴더 생성
			if(!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			try {
				bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
				
				for (int i = 0; i < resultDataList.size(); i++) {
					bw.write(gson.toJson(resultDataList.get(i)));
					bw.write("\r\n");
				}
				
				logVO.increaseSuccessCount();
			} catch (IOException e) {
				logVO.increaseFailCount(e.toString());
				throw e;
			} finally {
				if(bw != null) { 
					try { bw.close(); } catch (IOException e) { logger.error(e.getMessage(), e); } 
				}
			}
		}
		
		return file;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<File> makeJsonLimtSize(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO)
			throws Exception {
		String fileBaseName = this.dfVO.getResultFileName();
		String filePath = this.resultReadyPath + File.separator + this.dfVO.getDfName();
		String fileName = null;
		File file = null;
		BufferedWriter bw = null;
		CountLogVO logVO = bulkerLogVO.dump;
		Gson gson = BulkerUtils.getGson();
		int maxFileSize = this.resultFileLimitSize;	// 벌크파일의 사이즈 제한 - 기본값(10M)
		int checkCount = 10; 
 		List<File> jsonFileList = new ArrayList<>();
		
		
		if(resultDataList != null && resultDataList.size() > 0) {
			fileName = BulkerUtils.getJsonFileName(indexType, fileBaseName);
			file = new File(filePath, fileName);
			jsonFileList.add(file);
			
			// 부모 폴더 생성
			if(!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			try {
				boolean isPassCheckCount = false; 
				bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
				Map<String, Object> dataMap = null;
				String mapKey = null;
				for (int i = 0; i < resultDataList.size(); i++) {
					dataMap = (Map<String, Object>)resultDataList.get(i);
					bw.write(gson.toJson(dataMap));
					bw.write("\r\n");
					
					if ((i + 1) % checkCount == 0 || isPassCheckCount) {
						if(dataMap.size() == 1) {
							// 파일에서 저장한 데이터가 헤더 정보고, index, update 헤더라면...
							mapKey = dataMap.entrySet().iterator().next().getKey();
							if("index".equals(mapKey) || "update".equals(mapKey)) {
								isPassCheckCount = true;	 // 다음 데이터 까지 파일에 기록하도록 함.
								continue;
							}
						}
						
						// 리스트의 마지막 엔트리가 아니라면...
						if((i + 1) != resultDataList.size()) {
							bw.flush();
							isPassCheckCount = false;
							
							// file size 체크
			                if(file.length() >= maxFileSize) {
			                	bw.close();
			                	
			                	// 신규 Json 파일을 생성한다.
			                	fileName = BulkerUtils.getJsonFileName(indexType, fileBaseName);
			        			file = new File(filePath, fileName);
			        			jsonFileList.add(file);
			        			
			        			bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
			                }
						}
		            }
				}
				
				logVO.increaseSuccessCount();
			} catch (IOException e) {
				logVO.increaseFailCount(e.toString());
				throw e;
			} finally {
				if(bw != null) { 
					try { bw.close(); } catch (IOException e) { logger.error(e.getMessage(), e); } 
				}
			}
		}
		
		return jsonFileList;
	}
}
