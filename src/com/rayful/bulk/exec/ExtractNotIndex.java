package com.rayful.bulk.exec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.rayful.bulk.util.Utils;


public class ExtractNotIndex {
	private static final Logger logger = LoggerFactory.getLogger(ExtractNotIndex.class);
	private static ExtractNotIndex INSTANCE = new ExtractNotIndex();


	private final Properties prop;
	
	/**
	 * Constructor
	 */
	public ExtractNotIndex() {
		prop = new Properties();
		
		try {
			Reader reader = Resources.getResourceAsReader("config.properties");
			prop.load(reader);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * run
	 * @param indexName
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public void run(String indexName) throws IOException {
		String indexResultPath = prop.getProperty("BULKER.PATH.RESULT") + File.separator + indexName;
		
		// 색인 및 로그 파일 추출
		List<File> indexFileList = getFileList(indexResultPath, "index");
		List<File> logFileList = getFileList(indexResultPath, "log");
		
		// 색인 성공, 실패 카운트 및 실패 상태
		Map<String, Object> indexStatusMap = getIndexStatus(logFileList);
		
		// 미색인 리스트
		Map<String, Object> notIndexMap = getNotIndex(indexFileList, logFileList);
		
		// 미색인 파일 복사
		copyFiles(indexResultPath, (List<String>) notIndexMap.get("notIndexFileNameList"));
		
		// 삭제 파일 추출
		List<File> deleteFileList = getFileList(indexResultPath, "delete");
		List<File> deleteLogFileList = getFileList(indexResultPath, "deleteLog");
		
		// 삭제 추출 카운트
		int deleteTotalCnt = getTotalCnt(deleteFileList, "delete");
		
		// 삭제 성공, 실패 카운트 및 실패 상태
		Map<String, Object> deleteStatusMap = getDeleteStatus(deleteLogFileList);
		
		// 미색인 (삭제) 리스트
		Map<String, Object> notIndexDeleteMap = getNotIndex(deleteFileList, deleteLogFileList);
		
		// 미색인 파일 복사
		copyFiles(indexResultPath, (List<String>) notIndexDeleteMap.get("notIndexFileNameList"));
		
		// 결과 로그 파일 쓰기
		indexReport(indexName, indexResultPath
				, indexStatusMap, notIndexMap
				, deleteTotalCnt, deleteStatusMap, notIndexDeleteMap);
		
	}
	
	/**
	 * 색인 결과(Create, Update, Delete)를 log 파일로 생성
	 * config.properties에 정의된 BULKER.PATH.RESULT 경로에 생성
	 * @param indexName
	 * @param totalCnt
	 * @param indexStatusMap
	 * @param notIndexList
	 * @param indexResultPath
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void indexReport(String indexName, String indexResultPath
			, Map<String, Object> indexStatusMap, Map<String, Object> notIndexMap
			, int deleteTotalCnt, Map<String, Object> deleteStatusMap
			, Map<String, Object> notIndexDeleteMap) throws IOException {
		
		File file = new File(indexResultPath, "_indexReport.log");
		BufferedWriter bw = null;
		int iNotIndexCnt = 0;
		int iNotUpdateCnt = 0;
		int iDeleteNotIndexCnt = 0;
		long took = 0;
		
		if(indexStatusMap.get("took") != null) {
			took = (long) indexStatusMap.get("took");
		}
		
		try {
			List<String> createFailList = (List<String>) indexStatusMap.get("createFailList");
			List<String> notIndexIdList = (List<String>) notIndexMap.get("notIndexIdList");
			List<String> notIndexFileNameList = (List<String>) notIndexMap.get("notIndexFileNameList");
			
			bw = new BufferedWriter(new FileWriter(file));
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" [INDEX] " + indexName);
			bw.newLine();
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" - Index time : " +  Utils.getNow2());
			bw.newLine();
			bw.write(" - Index running time : " + Utils.getLongToTime(took));
			bw.newLine();
			bw.write(" - Index total count : " + indexStatusMap.get("createTotalCnt"));
			bw.newLine();
			bw.write(" - Index sccess count : " + indexStatusMap.get("createCnt"));
			bw.newLine();
			bw.write(" - Index fail count : " + indexStatusMap.get("createFailCnt"));
			bw.newLine();
			
			if(createFailList != null && createFailList.size() > 0) {
				bw.write(" - Index fail list : " + indexStatusMap.get("createFailList"));
				bw.newLine();
			}
			
			if(notIndexIdList != null) {
				iNotIndexCnt = notIndexIdList.size();
			}
			
			bw.write(" - Unexecuted count : " + iNotIndexCnt);
			bw.newLine();
			
			if(notIndexIdList != null && notIndexIdList.size() > 0) {
				bw.write(" - Unexecuted file name : " + notIndexFileNameList);
				bw.newLine();
				bw.write(" - Unexecuted ID : " + notIndexIdList);
				bw.newLine();
			}
			
			List<String> updateFailList = (List<String>) indexStatusMap.get("updateFailList");
			List<String> notUpdateIdList = (List<String>) notIndexMap.get("notUpdateIdList");
			
			bw.newLine();
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" [UPDATE] " + indexName);
			bw.newLine();
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" - Update total count : " + indexStatusMap.get("updateTotalCnt"));
			bw.newLine();
			bw.write(" - Update sccess count : " + indexStatusMap.get("updateCnt"));
			bw.newLine();
			bw.write(" - Update fail count : " + indexStatusMap.get("updateFailCnt"));
			bw.newLine();
			
			if(updateFailList != null && updateFailList.size() > 0) {
				bw.write(" - Update fail list : " + indexStatusMap.get("updateFailList"));
				bw.newLine();
			}
			
			if(notUpdateIdList != null) {
				iNotUpdateCnt = notUpdateIdList.size();
			}
			
			bw.write(" - Unexecuted count : " + iNotUpdateCnt);
			bw.newLine();
			
			if(notUpdateIdList != null && notUpdateIdList.size() > 0) {
				bw.write(" - Unexecuted file name : " + notIndexFileNameList);
				bw.newLine();
				bw.write(" - Unexecuted ID : " + notUpdateIdList);
				bw.newLine();
			}
			
			List<String> deleteFailList = (List<String>) deleteStatusMap.get("failList");
			List<String> deleteNotIndexIdList = (List<String>) notIndexDeleteMap.get("notIndexIdList");
			List<String> deleteNotIndexFileNameList = (List<String>) notIndexDeleteMap.get("notIndexFileNameList");
			
			long deleteTook = 0;
			
			if(deleteStatusMap.get("took") != null) {
				deleteTook = (long) deleteStatusMap.get("took");
			}
			
			bw.newLine();
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" [DELETE] " + indexName);
			bw.newLine();
			bw.write("------------------------------------------");
			bw.newLine();
			bw.write(" - Delete time : " +  Utils.getNow2());
			bw.newLine();
			bw.write(" - Delete running time : " + Utils.getLongToTime(deleteTook));
			bw.newLine();
			bw.write(" - Delete total count : " + deleteTotalCnt);
			bw.newLine();
			bw.write(" - Delete sccess count : " + deleteStatusMap.get("successCnt"));
			bw.newLine();
			bw.write(" - Delete fail count : " + deleteStatusMap.get("failCnt"));
			bw.newLine();
			
			if(deleteStatusMap != null && deleteFailList.size() > 0) {
				bw.write(" - Delete fail list : " + deleteStatusMap.get("failList"));
				bw.newLine();
			}
			
			if(deleteNotIndexIdList != null) {
				iDeleteNotIndexCnt = deleteNotIndexIdList.size();
			}
			
			bw.write(" - Unexecuted count : " + iDeleteNotIndexCnt);
			bw.newLine();
			
			if(deleteNotIndexIdList != null && deleteNotIndexIdList.size() > 0) {
				bw.write(" - Unexecuted file name : " + deleteNotIndexFileNameList);
				bw.newLine();
				bw.write(" - Unexecuted ID : " + deleteNotIndexIdList);
				bw.newLine();
			}
			
			bw.newLine();
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		
		} finally {
			if(bw != null) {
				bw.flush();
				bw.close();
			}
		}
	}
	
	/**
	 * result 파일 복사
	 * bulk로 색인 시 elastic return 값을 log 파일로 생성하는데, 해당 log 파일이 없을 경우 
	 * result json 파일을 .tmp 파일로 복사하여 result 폴더에 남겨 놓도록 함
	 * @param indexFileList
	 * @param logFileList
	 */
	public void copyFiles(String indexResultPath, List<String> notIndexFileNameList) {
		if(notIndexFileNameList != null) {
			for (String notIndexFileName : notIndexFileNameList) {
				File notIndexFile = new File(indexResultPath, notIndexFileName);
				File newFile = new File(indexResultPath, notIndexFileName + ".tmp");
				
				try {
					Files.copy(notIndexFile, newFile);
				
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * 색인 키 조회
	 * @param indexFile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getIndexId(File indexFile, String indexType) {
		List<String> resultList = new ArrayList<String>();
		ObjectMapper mapper = new ObjectMapper();
		BufferedReader reader = null;
		String strJson = null;
		Map<String, Object> keyMap = null;
		String key = null;
		
		try {
			reader = new BufferedReader(new FileReader(indexFile));
			
			while ((strJson = reader.readLine()) != null) {
				Map<String, Object> jsonMap = mapper.readValue(strJson, Map.class);
				
				if(jsonMap != null) {
					if(jsonMap.get(indexType) != null) {
						keyMap = (Map<String, Object>) jsonMap.get(indexType);
						
						try {
							key = (String) keyMap.get("_id");
						} catch (Exception e) {
							key = keyMap.get("_id").toString();
						}
						
						resultList.add(key);
					}
				}
			}
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return resultList;
	}
	
	/**
	 * 미색인 리스트
	 * @param indexFileList
	 * @param logFileList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getNotIndex(List<File> indexFileList, List<File> logFileList) throws IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<String> notIndexFileNameList = new ArrayList<String>();
		List<String> notIndexIdList = new ArrayList<String>();
		List<String> notUpdateIdList = new ArrayList<String>();
		List<String> notDeleteIdList = new ArrayList<String>();
		
		String indexFileName = null;
		int notExistsCnt = 0;
		List<String> indexIdList = null;
		List<String> updateIdList = null;
		List<String> deleteIdList = null;
		boolean isIndex = true;
		BufferedReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		
		for (File indexFile : indexFileList) {
			indexFileName = indexFile.getName();
			File file = new File(indexFile.getParent(), indexFileName + ".log");
			
			if(!file.exists()) {
				isIndex = false;
			
			} else {
				try {
					reader = new BufferedReader(new FileReader(file));
					String strJson = null;
					isIndex = false;
					
					while ((strJson = reader.readLine()) != null) {
						Map<String, Object> jsonMap = mapper.readValue(strJson, Map.class);
						
						if(jsonMap != null && jsonMap.size() > 0) {
							isIndex = true;
						}
					}
				
				} catch (Exception e) {
					isIndex = false;
				
				} finally {
					if(reader != null) { reader.close(); }
				}
			}
			
			if(!isIndex) {
				notIndexFileNameList.add(indexFileName);
				indexIdList = getIndexId(indexFile, "index");
				updateIdList = getIndexId(indexFile, "update");
				deleteIdList = getIndexId(indexFile, "delete");
				notExistsCnt = notExistsCnt + 1;
				notIndexIdList.addAll(indexIdList);
				notUpdateIdList.addAll(updateIdList);
				notDeleteIdList.addAll(deleteIdList);
			}
		}
		
		resultMap.put("notIndexFileNameList", notIndexFileNameList);
		resultMap.put("notIndexIdList", notIndexIdList);
		resultMap.put("notUpdateIdList", notUpdateIdList);
		resultMap.put("notDeleteIdList", notDeleteIdList);
		
		return resultMap;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getIndexStatus(List<File> logFileList) throws IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		BufferedReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> indexFailList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> updateFailList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> deleteFailList = new ArrayList<Map<String,Object>>();
		
		int indexTotalCnt = 0;
		int indexCnt = 0;
		int indexFailCnt = 0;
		int updateTotalCnt = 0;
		int updateCnt = 0;
		int updateFailCnt = 0;
		int deleteTotalCnt = 0;
		int deleteCnt = 0;
		int deleteFailCnt = 0;
		int status = 0;
		long took = 0;
		
		for (File logFile : logFileList) {
			reader = new BufferedReader(new FileReader(logFile));
			String strJson = null;
			
			try {
				while ((strJson = reader.readLine()) != null) {
					Map<String, Object> jsonMap = mapper.readValue(strJson, Map.class);
					
					if(jsonMap.get("took") != null && jsonMap.get("items") != null) {
						List<Map<String, Object>> itemList = (List<Map<String, Object>>) jsonMap.get("items");
						took = took + (int) jsonMap.get("took");
						
						if(itemList != null) {
							for (Map<String, Object> item : itemList) {
								// Create
								if(item.containsKey("index")) {
									Map<String, Object> itemMap = (Map<String, Object>) item.get("index");
									indexTotalCnt = indexTotalCnt + 1;
									status = (int) itemMap.get("status");
									
									if(status <= 200 && status >= 299) {
										indexCnt = indexCnt + 1;
									
									} else {
										indexFailList.add((Map<String, Object>) itemMap.get("error"));
										indexFailCnt = indexFailCnt + 1;
									}
									
								// Update
								} else if (item.containsKey("update")) {
									Map<String, Object> itemMap = (Map<String, Object>) item.get("update");
									updateTotalCnt = updateTotalCnt + 1;
									status = (int) itemMap.get("status");
									
									if(status <= 200 && status >= 299) {
										updateCnt = updateCnt + 1;
									
									} else {
										updateFailList.add((Map<String, Object>) itemMap.get("error"));
										updateFailCnt = updateFailCnt + 1;
									}
									
								// Delete
								} else if (item.containsKey("delete")) {
									Map<String, Object> itemMap = (Map<String, Object>) item.get("delete");
									deleteTotalCnt = deleteTotalCnt + 1;
									status = (int) itemMap.get("status");
									
									if(status <= 200 && status >= 299) {
										deleteCnt = deleteCnt + 1;
									
									} else {
										deleteFailList.add((Map<String, Object>) itemMap.get("error"));
										deleteFailCnt = deleteFailCnt + 1;
									}
								}
							}
						}
					}
				}
			
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			
			} finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
		
		resultMap.put("indexTotalCnt", indexTotalCnt);
		resultMap.put("indexCnt", indexCnt);
		resultMap.put("indexFailCnt", indexFailCnt);
		resultMap.put("indexFailList", indexFailList);
		resultMap.put("updateTotalCnt", updateTotalCnt);
		resultMap.put("updateCnt", updateCnt);
		resultMap.put("updateFailCnt", updateFailCnt);
		resultMap.put("updateFailList", updateFailList);
		resultMap.put("deleteTotalCnt", deleteTotalCnt);
		resultMap.put("deleteCnt", deleteCnt);
		resultMap.put("deleteFailCnt", deleteFailCnt);
		resultMap.put("deleteFailList", deleteFailList);
		resultMap.put("took", took);
		
		return resultMap;
	}
		
	
	/**
	 * 색인 상태
	 * @param logFileList
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getIndexStatus2(List<File> logFileList) throws IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		BufferedReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> createFailList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> updateFailList = new ArrayList<Map<String,Object>>();
		
		int createTotalCnt = 0;
		int createCnt = 0;
		int createFailCnt = 0;
		int updateTotalCnt = 0;
		int  updateCnt = 0;
		int updateFailCnt = 0;
		long took = 0;
		
		for (File logFile : logFileList) {
			reader = new BufferedReader(new FileReader(logFile));
			String strJson = null;
			
			try {
				while ((strJson = reader.readLine()) != null) {
					Map<String, Object> jsonMap = mapper.readValue(strJson, Map.class);
					
					if(jsonMap.get("took") != null && jsonMap.get("items") != null) {
						List<Map<String, Object>> itemList = (List<Map<String, Object>>) jsonMap.get("items");
						took = took + (int) jsonMap.get("took");
						
						if(itemList != null) {
							for (Map<String, Object> item : itemList) {
								// Create
								if(item.containsKey("index")) {
									createTotalCnt = createTotalCnt + 1;
									Map<String, Object> itemMap = (Map<String, Object>) item.get("index");
									
									if("created".equalsIgnoreCase((String) itemMap.get("result")) 
											|| "updated".equalsIgnoreCase((String) itemMap.get("result"))) {
										createCnt = createCnt + 1;
									
									} else {
										createFailList.add((Map<String, Object>) itemMap.get("error"));
										createFailCnt = createFailCnt + 1;
									}
									
								// Update
								} else if (item.containsKey("update")) {
									updateTotalCnt = updateTotalCnt + 1;
									Map<String, Object> itemMap = (Map<String, Object>) item.get("update");
									
									if("updated".equalsIgnoreCase((String) itemMap.get("result")) 
											|| "noop".equalsIgnoreCase((String) itemMap.get("result"))) {
										updateCnt = updateCnt + 1;
									
									} else {
										updateFailList.add((Map<String, Object>) itemMap.get("error"));
										updateFailCnt = updateFailCnt + 1;
									}
								}
							}
						}
					}
				}
			
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			
			} finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
		
		resultMap.put("createTotalCnt", createTotalCnt);
		resultMap.put("createCnt", createCnt);
		resultMap.put("createFailCnt", createFailCnt);
		resultMap.put("createFailList", createFailList);
		resultMap.put("updateTotalCnt", updateTotalCnt);
		resultMap.put("updateCnt", updateCnt);
		resultMap.put("updateFailCnt", updateFailCnt);
		resultMap.put("updateFailList", updateFailList);
		resultMap.put("took", took);
		
		return resultMap;
	}
	
	/**
	 * 색인 상태 (삭제)
	 * @param deleteLogFileList
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getDeleteStatus(List<File> deleteLogFileList) throws IOException {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<Map<String, Object>> failList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> itemList = null;
		BufferedReader reader = null;
		String strJson = null;
		ObjectMapper mapper = new ObjectMapper();
		long took = 0;
		int successCnt = 0;
		int failCnt = 0;
		int iStatus = 0;
		
		for (File deleteFile : deleteLogFileList) {
			try {
				reader = new BufferedReader(new FileReader(deleteFile));
				
				while ((strJson = reader.readLine()) != null) {
					Map<String, Object> jsonMap = mapper.readValue(strJson, Map.class);
					
					if(jsonMap != null) {
						if(jsonMap.get("took") != null) {
							took = took + (int) jsonMap.get("took");
						}
						
						itemList = (List<Map<String, Object>>) jsonMap.get("items");
						
						for (Map<String, Object> item : itemList) {
							Map<String, Object> itemMap = (Map<String, Object>) item.get("delete");
							Map<String, Object> failMap = new HashMap<String, Object>();
							iStatus = (int) itemMap.get("status");
							
							if(iStatus == 200) {
								successCnt = successCnt + 1;
							
							} else {
								failCnt = failCnt + 1;
								failMap.put("id", itemMap.get("_id"));
								failMap.put("result", itemMap.get("result"));
								failList.add(failMap);
							}
						}
					}
				}
				
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		
		resultMap.put("successCnt", successCnt);
		resultMap.put("failCnt", failCnt);
		resultMap.put("failList", failList);
		resultMap.put("took", took);
		
		return resultMap;
	}
	
	/**
	 * 색인 대상 추출 카운트
	 * @param indexFileList
	 * @return
	 * @throws IOException 
	 */
	public int getTotalCnt(List<File> indexFileList, String indexType) throws IOException {
		int resultCnt = 0;
		LineNumberReader reader = null;
		
		for (File indexFile : indexFileList) {
			try {
				reader = new LineNumberReader(new FileReader(indexFile.getPath()));
				while (reader.readLine() != null);
				int lineCount = reader.getLineNumber();
				
				resultCnt = resultCnt + lineCount;

			} catch (Exception e) {
				logger.error(e.getMessage(), e);

			} finally {
				if(reader != null) { reader.close(); }
			}
		}
		
		if("index".equalsIgnoreCase(indexType) && resultCnt > 0) {
			resultCnt = resultCnt / 2;
		}
		
		return resultCnt;
	}
	
	/**
	 * resultType에 따라 벌커 result 경로의 파일 리스트 리턴 
	 * resultType이 index 일 경우 json 파일 리스트
	 * resultType이 log 일 경우 log 파일 리스트
	 * @param indexResultPath
	 * @param resultType
	 * @return
	 */
	public List<File> getFileList(String indexResultPath, String resultType) {
		List<File> resultList = new ArrayList<File>();
		File resultPath = new File(indexResultPath);
		File[] resultFiles = resultPath.listFiles();
		String fileName = null;
		String fileExt = null;
		
		if(resultPath.exists()) {
			for (File file : resultFiles) {
				if(!file.isDirectory()) {
					fileName = file.getName();
					fileExt = FilenameUtils.getExtension(fileName);
					
					if("index".equalsIgnoreCase(resultType)) {
						if(fileName.startsWith("index_") && "json".equalsIgnoreCase(fileExt)) {
							resultList.add(file);
						}

					} else if("log".equalsIgnoreCase(resultType)) {
						if(fileName.startsWith("index_") && "log".equalsIgnoreCase(fileExt)) {
							resultList.add(file);
						}
						
					} else if("delete".equalsIgnoreCase(resultType)) {
						if(fileName.startsWith("delete_") && "json".equalsIgnoreCase(fileExt)) {
							resultList.add(file);
						}
						
					} else if("deleteLog".equalsIgnoreCase(resultType)) {
						if(fileName.startsWith("delete_") && "log".equalsIgnoreCase(fileExt)) {
							resultList.add(file);
						}
					}
				}
			}
		
		} else {
			logger.info("Not exists path. " + indexResultPath);
		}
		
		return resultList;
	}
	
	/**
	 * Main
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		logger.info("Start ExtracNotIndex..");
		String indexName = args[0];
		INSTANCE.run(indexName);
		logger.info("End ExtracNotIndex..");
	}

}
