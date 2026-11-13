package com.rayful.bulk.service.maru42;

import java.io.BufferedWriter;
import java.io.File;
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
 * Search42를 위한 색인 데이터 수집 관련 데이터 조회 및 첨부파일 처리를 담당하는 서비스 클래스
 */
public class Bulker4S42Service extends BaseBulkerService{
	private static final Logger logger = LoggerFactory.getLogger(Bulker4S42Service.class);

	/**
	 * 생성자 
	 * 
	 * @param dfVO DF 정보
	 * @param prop 설정파일 정보
	 * @throws Exception
	 */
	public Bulker4S42Service(DFLoadVO dfVO, Properties prop) throws Exception {
		super(dfVO, prop);
	}
	
	@Override
	public List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode) {
		List<Object> resultList = new ArrayList<Object>();
		
		if(dataList != null && dataList.size() > 0) {
			String keyId = this.dfVO.column.getKeyColumnName();
			String indexAction = null;
			Map<String, Object> s42Format = null;
			Map<String, Object> outputDataMap = null;
			List<String> noOutputColumnNameList = null;
			
			List<Map<String, Object>> fileDataList = null;
			List<Map<String, Object>> outputFileDataList = null;
			Map<String, Object> outputFileDataMap = null;
			
			for (Map<String, Object> dataMap : dataList) {
				indexAction = (String) dataMap.get(this.dfVO.column.getIndexActionColumnName());

				/*
				 * ---------------------------------
				 * Delete
				 * ---------------------------------
				 * {"_id": "keyId", "_op_type": "delete"}
				 */
				if("D".equalsIgnoreCase(indexAction)) {
					s42Format = new HashMap<String, Object>();
					s42Format.put("_id",  dataMap.get(keyId));
					s42Format.put("_op_type", "delete");

					resultList.add(s42Format);
				
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
					 * {"_id": "keyId", "_op_type": "update",  "doc" : {"fieldName1": "value1"}}
					 *}
					 */
					if("U".equalsIgnoreCase(indexAction)) {
						s42Format = new HashMap<String, Object>();
						s42Format.put("_id",  dataMap.get(keyId));
						s42Format.put("_op_type", "update");
						s42Format.put("doc", outputDataMap);
						
						resultList.add(s42Format);
					/*
					 * ---------------------------------
					 * Index
					 * ---------------------------------
					 * {"_id" : "keyId", "fieldName1": "value1", "fieldName2": "value2", ...}
					 */
					} else {
						outputDataMap.put("_id", dataMap.get(keyId));
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
			Map<String, Object> s42Format = null;
			for (Map<String, Object> dataMap : dataList) {
				s42Format = new HashMap<String, Object>();
				s42Format.put("_id",  dataMap.get(this.dfVO.column.getKeyColumnName()));
				s42Format.put("_op_type", "delete");

				resultList.add(s42Format);
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
				bw.write(BulkerUtils.getGsonS24().toJson(resultDataList));
				
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
