package com.rayful.bulk.service.es;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.rayful.bulk.service.BaseIndexService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * ElasticSearch에 색인 요청을 담당하는 클래스(IndexService)의 필요한 함수를 모두 구현한 클래스
 */
public class IndexService extends BaseIndexService{
	private static final Logger logger = LoggerFactory.getLogger(IndexService.class);

	private final String apiBaseUrl;
	private String apiAuth4User;
	private String apiAuth4Apikey;
	
	public IndexService(DFLoadVO dfVO, Properties prop) throws Exception {
		super(dfVO, prop);
		
		this.apiBaseUrl = prop.getProperty("ES.API.BASE.URL");
		this.apiAuth4User = com.rayful.bulk.util.CryptUtils.resolve(prop.getProperty("ES.API.AUTH.USER"));
		this.apiAuth4Apikey = com.rayful.bulk.util.CryptUtils.resolve(prop.getProperty("ES.API.AUTH.APIKEY"));
		
		if(prop.getProperty("ES.API.BULK.URL") != null && prop.getProperty("ES.API.BULK.URL").length() > 0) {
			// Deprecated Setting Error.
			logger.error("[INDEX] ES.API.BULK.URL is deprecated. Use ES.API.BASE.URL");
			throw new IllegalArgumentException("[INDEX] ES.API.BULK.URL is deprecated. Use ES.API.BASE.URL");
		}
		
		if(this.apiBaseUrl == null || this.apiBaseUrl.length() == 0) {
			logger.error("[INDEX] Not found setting ... ES.API.BASE.URL");
			throw new IllegalArgumentException("[INDEX] Not found setting ... ES.API.BASE.URL");
		}
		
		if(this.apiAuth4Apikey == null || this.apiAuth4Apikey.length() == 0) {
			this.apiAuth4Apikey = null;
		}
		
		if(this.apiAuth4User == null || this.apiAuth4User.length() == 0) {
			this.apiAuth4User = null;
		} else {
			if(this.apiAuth4User.indexOf(":") < 0) {
				logger.error("[INDEX] Invalid format ... ES.API.AUTH.USER");
				throw new IllegalArgumentException("[INDEX] Invalid format ... ES.API.AUTH.USER");
			}
		}
		
		/*
		if(this.apiAuth4Apikey == null && this.apiAuth4User == null) {
			logger.error("[INDEX] Not found setting ... ES.API.AUTH.USER or ES.API.AUTH.APIKEY");
			throw new IllegalArgumentException("[INDEX] Not found setting ... ES.API.AUTH.USER or ES.API.AUTH.APIKEY");
		}
		*/
		
		logger.debug("[INDEX] apiBaseUrl={}, apiAuth4User={}, apiAuth4Apikey={}", this.apiBaseUrl,
				com.rayful.bulk.util.SecurityUtils.mask(this.apiAuth4User),
				com.rayful.bulk.util.SecurityUtils.mask(this.apiAuth4Apikey));
		
		// 부모클래스의 멤버 변수 restTemplate을 설정 
		if(this.apiBaseUrl.startsWith("https")) {
			this.restTemplate = BulkerUtils.getInstanceRestTemplateSsl(prop);
		} else {
			this.restTemplate = BulkerUtils.getInstanceRestTemplate();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean requestIndex(Path bulkFile, Path logFile, String indexName, BulkerLogVO bulkerLogVO) throws IOException {
		boolean isSuccess = false;
		
		// URL
		String url = null;
		if(this.dfVO.isIncludeIndexname()) { //index name을 bulk file에 추가
			url = this.apiBaseUrl + "/_bulk";
		} else {
			url =  this.apiBaseUrl + "/" + indexName + "/_bulk";
		}
		logger.info("Index request URL: {}", url);

		// HTTP HEADER 설정
		HttpHeaders headers = new HttpHeaders();
		if(this.apiAuth4Apikey != null) {
			headers.add("Authorization", "ApiKey " + this.apiAuth4Apikey);
		} else if(this.apiAuth4User != null) {
			String [] userInfo = this.apiAuth4User.split(":");
			headers.setBasicAuth(userInfo[0], userInfo[1]);
		}
		
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		
		// REQUEST BODY 설정
		String requestBody = Utils.getFileText(bulkFile.toString(), "\r\n");
		
		logger.debug("[INDEX] =======================================================");
		logger.debug("[INDEX] [REQUEST URL] {}", url);
		logger.debug("[INDEX] [REQUEST HEADER] Content-Type={} (Authorization masked)", headers.getContentType());
		logger.debug("[INDEX] [REQUEST BODY SIZE] {}", requestBody.length());
		logger.debug("[INDEX] =======================================================");
		logger.info("[INDEX] Request Index: bulkFile ({}/{})", bulkFile.getParent().getFileName(), bulkFile.getFileName());
		
		HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
		//@SuppressWarnings({ "unchecked", "rawtypes" })
		//ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity) this.restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ResponseEntity<Map<String, Object>> responseEntity = null;
		try {
			responseEntity = (ResponseEntity) this.restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
			// [DEBUG ADD]
			logger.debug("[DEBUG] restTemplate.exchange completed successfully.");
		} catch (Exception e) {
			logger.error("[DEBUG] Exception in restTemplate.exchange: {}", e.getMessage(), e);
			return false;
		}
		
		logger.debug("[INDEX] Response: {}", responseEntity);
		
		Map<String, Object> responseBody = responseEntity.getBody();
		
		// [DEBUG ADD]
		logger.debug("[DEBUG] Response received. Status={}, hasBody={}", responseEntity.getStatusCode(), responseBody != null);
		
		try(BufferedWriter bw = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8)) {
			if(responseBody != null) {
				bw.write(responseBody.toString());
			} else {
				// HTTP 상태 정보를 로그파일에 기록 : 2025.10.04 정충열
				bw.write(responseEntity.toString());
			}
			bw.flush();
			// [DEBUG ADD]
			logger.debug("[DEBUG] Log file write complete: {}", logFile);
		} catch (Exception e) {
			logger.error("[DEBUG] Log file write failed: {}", e.getMessage(), e);
		}

		if(responseEntity.getStatusCodeValue() == HttpStatus.SC_OK) {
			if(responseBody.containsKey("errors")) {
				isSuccess = !((Boolean) responseBody.get("errors"));
				
				// [DEBUG ADD]
				logger.debug("[DEBUG] Response 'errors' key found. isSuccess={}", isSuccess);
			}
			
			// 개별 결과를  확인하여 색인결과 카운트에 반영한다.
			if(responseBody.containsKey("items")) {
				List<Map<String, Object>> itemList = (List<Map<String, Object>>)responseBody.get("items");
				Map<String, Object> itemData = null;
				Map<String, Object> errorMap = null;
				String result = null;
				
				// [DEBUG ADD]
				logger.debug("[DEBUG] items size = {}", itemList.size());
				
				for(Map<String, Object> itemMap : itemList) {
					if(itemMap.containsKey("index")) {
						itemData = (Map<String, Object>) itemMap.get("index");
					} else if(itemMap.containsKey("update")) {
						itemData = (Map<String, Object>) itemMap.get("update");
					} else if(itemMap.containsKey("delete")) {
						itemData = (Map<String, Object>) itemMap.get("delete");
					} else {
						itemData = null;
						logger.warn("Can't find item's key : index or delete or update");
					}
					
					if(itemData != null) {
						if(itemData.containsKey("result")) {
							result = (String)itemData.get("result");
							
							// [DEBUG ADD]
							logger.trace("[DEBUG] item result = {}", result);
							
							if("created".equals(result)) {
								bulkerLogVO.indexResult.increaseCreateCount();
							} else if("updated".equals(result)) {
								bulkerLogVO.indexResult.increaseUpdateCount();
							} else if("deleted".equals(result)) {
								bulkerLogVO.indexResult.increaseDeleteCount();
							} else if("noop".equals(result)) {
								bulkerLogVO.indexResult.increasePartialCount();
							} else {
								bulkerLogVO.indexResult.increaseEtcCount(result);
							}
						} else if(itemData.containsKey("error")) {
							errorMap = (Map<String, Object>)itemData.get("error");
							bulkerLogVO.indexResult.increaseErrorCount((String) errorMap.get("reason"));
							logger.error("[DEBUG] item error reason: {}", errorMap.get("reason")); // [DEBUG ADD]
						} else {
							logger.warn("Can't find itemData's key : result or error");
						}
					}
				}
				
				if(itemList.size() == 0) {
					logger.warn("Empty itemList");
				} 
			} else {
				logger.warn("Can't find key : items");
			}
		} else {
			logger.error("[INDEX] Response: {}", responseEntity);
		}
		
		// [DEBUG ADD]
		logger.debug("[DEBUG] >>> requestIndex END: isSuccess={}", isSuccess);
		
		return isSuccess;
	}

	@Override
	protected int requestDeletingCount(String indexName, String requestBody) throws IOException {
		int count = -1;
		
		// URL
		String url =  this.apiBaseUrl + "/" + indexName + "/_count";

		// HTTP HEADER 설정
		HttpHeaders headers = new HttpHeaders();
		if(this.apiAuth4Apikey != null) {
			headers.add("Authorization", "ApiKey " + this.apiAuth4Apikey);
		} else if(this.apiAuth4User != null) {
			String [] userInfo = this.apiAuth4User.split(":");
			headers.setBasicAuth(userInfo[0], userInfo[1]);
		}
		
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		logger.debug("[DELETINGCNT] =======================================================");
		logger.debug("[DELETINGCNT] [REQUEST URL] {}", url);
		logger.debug("[DELETINGCNT] [REQUEST HEADER] Content-Type={} (Authorization masked)", headers.getContentType());
		logger.debug("[DELETINGCNT] [REQUEST BODY] {}", requestBody);
		logger.debug("[DELETINGCNT] =======================================================");
		
		HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity) this.restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
		logger.debug("[DELETE] Response: {}", responseEntity);
		if(responseEntity.getStatusCodeValue() != HttpStatus.SC_OK) {
			throw new IOException("API ERROR: " + responseEntity.getStatusCodeValue());
		}
		
		if(responseEntity.getBody().containsKey("count")) {
			count = (int) responseEntity.getBody().get("count");
		}
		logger.info("[DELETINGCNT] count: {}", count);
		
		return count;
	}

	@Override
	protected String requestDeleteByQuery(String indexName, String requestBody) throws IOException {
		String taskId = null;
		
		// URL
		String url =  this.apiBaseUrl + "/" + indexName + "/_delete_by_query?wait_for_completion=false";

		// HTTP HEADER 설정
		HttpHeaders headers = new HttpHeaders();
		if(this.apiAuth4Apikey != null) {
			headers.add("Authorization", "ApiKey " + this.apiAuth4Apikey);
		} else if(this.apiAuth4User != null) {
			String [] userInfo = this.apiAuth4User.split(":");
			headers.setBasicAuth(userInfo[0], userInfo[1]);
		}
		
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		// REQUEST BODY 설정

		logger.debug("[DELETE] =======================================================");
		logger.debug("[DELETE] [REQUEST URL] {}", url);
		logger.debug("[DELETE] [REQUEST HEADER] Content-Type={} (Authorization masked)", headers.getContentType());
		logger.debug("[DELETE] [REQUEST BODY] {}", requestBody);
		logger.debug("[DELETE] =======================================================");
		
		HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity) this.restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
		logger.debug("[DELETE] Response: {}", responseEntity);
		if(responseEntity.getStatusCodeValue() != HttpStatus.SC_OK) {
			throw new IOException("API ERROR: " + responseEntity.getStatusCodeValue());
		}
		
		if(responseEntity.getBody().containsKey("task")) {
			taskId = (String)responseEntity.getBody().get("task");
		}
		logger.info("[DELETE] taskId: {}", taskId);
		
		return taskId;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean requestTaskCompleted(String taskId) throws IOException, InterruptedException {
		boolean isCompleted = false;
		
		// URL
		String url =  this.apiBaseUrl + "/_tasks/" + taskId;

		// HTTP HEADER 설정
		HttpHeaders headers = new HttpHeaders();
		if(this.apiAuth4Apikey != null) {
			headers.add("Authorization", "ApiKey " + this.apiAuth4Apikey);
		} else if(this.apiAuth4User != null) {
			String [] userInfo = this.apiAuth4User.split(":");
			headers.setBasicAuth(userInfo[0], userInfo[1]);
		}
		
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		// REQUEST BODY 설정
		logger.debug("[TASK] =======================================================");
		logger.debug("[TASK] [REQUEST URL] {}", url);
		logger.debug("[TASK] [REQUEST HEADER] Content-Type={} (Authorization masked)", headers.getContentType());
		logger.debug("[TASK] =======================================================");
		
		HttpEntity<String> entity = new HttpEntity<String>(null, headers);
		ResponseEntity<Map<String, Object>> responseEntity = null;
		while(!isCompleted) {
			responseEntity = (ResponseEntity) this.restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
			logger.debug("[TASK] Response: {}", responseEntity);
			if(responseEntity.getStatusCodeValue() != HttpStatus.SC_OK) {
				throw new IOException("API ERROR: " + responseEntity.getStatusCodeValue());
			}
			
			isCompleted = (boolean) responseEntity.getBody().get("completed");
			logger.info("[TASK] task({}) is completed: {}", taskId, isCompleted);
			if(!isCompleted) {
				Thread.sleep(1000L);
			}
		}
		
		return isCompleted;
	}

}
