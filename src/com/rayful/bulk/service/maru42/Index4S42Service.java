package com.rayful.bulk.service.maru42;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import com.rayful.bulk.service.BaseIndexService;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * Search42에 색인 요청을 담당하는 클래스(IndexService)의 필요한 함수를 모두 구현한 클래스
 */
public class Index4S42Service extends BaseIndexService{
	private static final Logger logger = LoggerFactory.getLogger(Index4S42Service.class);

	private String apiIndexBulkUrl = null;
	private String apiApikey = null;
	private String apiApplicationId = null;
	
	/**
	 * 생성자 
	 * 
	 * @param prop 설정파일 정보
	 * @throws Exception RestTemplateSsl 생성 실패 시
	 * @throws IllegalArgumentException 설정이 존재하지 않을 때
	 */
	public Index4S42Service(DFLoadVO dfVO, Properties prop) throws Exception {
		super(dfVO, prop);
		
		this.apiIndexBulkUrl = prop.getProperty("SEARCH42.API.BULK.URL");
		this.apiApikey = com.rayful.bulk.util.CryptUtils.resolve(prop.getProperty("SEARCH42.API.APIKEY"));
		this.apiApplicationId = prop.getProperty("SEARCH42.API.APPLICATION_ID");
		
		if(this.apiIndexBulkUrl == null || this.apiIndexBulkUrl.length() == 0) {
			logger.error("[INDEX] Not found setting ... SEARCH42.API.BULK.URL");
			throw new IllegalArgumentException("[INDEX] Not found setting ... SEARCH42.API.BULK.URL");
		}
		if(this.apiApikey == null || this.apiApikey.length() == 0) {
			logger.error("[INDEX] Not found setting ... SEARCH42.API.APIKEY");
			throw new IllegalArgumentException("[INDEX] Not found setting ... SEARCH42.API.APIKEY");	
		}
		if(this.apiApplicationId == null || this.apiApplicationId.length() == 0) {
			logger.error("[INDEX] Not found setting ... SEARCH42.API.APPLICATION_ID");
			throw new IllegalArgumentException("[INDEX] Not found setting ... SEARCH42.API.APPLICATION_ID");	
		}
		
		// 부모클래스의 멤버 변수 restTemplate을 설정 
		if(this.apiIndexBulkUrl.startsWith("https")) {
			this.restTemplate = BulkerUtils.getInstanceRestTemplateSsl(prop);
		} else {
			this.restTemplate = BulkerUtils.getInstanceRestTemplate();
		}
	}
	
	@Override
	protected boolean requestIndex(Path bulkFile, Path logFile, String indexName, BulkerLogVO bulkerLogVO) throws IOException {
		boolean isSuccess = false;
		
		// URL
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("APPLICATION_ID", this.apiApplicationId);
		paramsMap.put("INDEX_NAME", indexName);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(this.apiIndexBulkUrl);
		String url = builder.buildAndExpand(paramsMap).toUriString();

		// HTTP HEADER 설정
		HttpHeaders headers = new HttpHeaders();
		headers.add("Search42-X-Api-Key", this.apiApikey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		
		// REQUEST BODY 설정
		String requestBody = Utils.getFileText(bulkFile.toString(), "\r\n");
		
		logger.debug("[INDEX] =======================================================");
		logger.debug("[INDEX] [REQUEST URL] {}", url);
		logger.debug("[INDEX] [REQUEST HEADER] Content-Type={} (Api-Key masked)", headers.getContentType());
		logger.debug("[INDEX] [REQUEST BODY SIZE] {}", requestBody.length());
		logger.debug("[INDEX] =======================================================");
		logger.info("[INDEX] Request Index: bulkFile ({}/{})", bulkFile.getParent().getFileName(), bulkFile.getFileName());
		
		HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = this.restTemplate.postForObject(url, entity, Map.class);
		try(BufferedWriter bw = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8)) {
			bw.write(responseMap.toString());
			bw.flush();
		}
		
		logger.debug("[INDEX] Response: {}", responseMap);
		
		if(responseMap.containsKey("success")) {
			isSuccess = (Boolean) responseMap.get("success");
		}
		logger.info("[INDEX]  -> Response - isSuccess: {}", isSuccess);
		
		return isSuccess;
	}

	@Override
	protected int requestDeletingCount(String indexName, String requestBody) throws IOException {
		throw new IOException("Not implement");
	}

	@Override
	protected String requestDeleteByQuery(String indexName, String requestBody) throws IOException {
		throw new IOException("Not implement");
	}

	@Override
	protected boolean requestTaskCompleted(String taskId) throws IOException, InterruptedException {
		throw new IOException("Not implement");
	}
}
