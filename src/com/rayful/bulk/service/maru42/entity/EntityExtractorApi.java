package com.rayful.bulk.service.maru42.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.rayful.bulk.util.BulkerUtils;

public class EntityExtractorApi {
	private static final Logger logger = LoggerFactory.getLogger(EntityExtractorApi.class);
	
	private final String apiUrl;
	private final String tagType;
	private final String companyModule;
	private final String model;
	private final Boolean analyze;
	
	private final RestTemplate restTemplate;
	protected final Gson gson;
	
	public EntityExtractorApi(Properties prop) {
		this.apiUrl = prop.getProperty("QA42.API.EXTRACT_ENTITY.URL");
		if(this.apiUrl == null || this.apiUrl.trim().length() == 0) {
			logger.error("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.URL");
			throw new IllegalArgumentException("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.URL");
		}
		this.tagType = prop.getProperty("QA42.API.EXTRACT_ENTITY.TAG_TYPE");
		if(this.tagType == null || this.tagType.trim().length() == 0) {
			logger.error("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.TAG_TYPE");
			throw new IllegalArgumentException("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.TAG_TYPE");
		}
		this.companyModule = prop.getProperty("QA42.API.EXTRACT_ENTITY.COMPANY_MODULE");
		if(this.companyModule == null || this.companyModule.trim().length() == 0) {
			logger.error("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.COMPANY_MODULE");
			throw new IllegalArgumentException("[ENTITY] Not found setting ... QA42.API.EXTRACT_ENTITY.COMPANY_MODULE");
		}
		String model = prop.getProperty("QA42.API.EXTRACT_ENTITY.MODEL");
		if(model == null || model.trim().length() == 0) {
			this.model = null;
		} else {
			this.model = model;
		}
		String analyze = prop.getProperty("QA42.API.EXTRACT_ENTITY.ANALYZE");
		if(analyze == null || analyze.trim().length() == 0) {
			this.analyze = null;
		} else {
			this.analyze = Boolean.valueOf(analyze.equalsIgnoreCase("yes"));
		}
		
		this.restTemplate = BulkerUtils.getInstanceRestTemplate();
		this.gson = BulkerUtils.getGsonS24();
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getEntity(String content) throws IOException {
		List<String> resultList = new ArrayList<String>();
		
		if(content != null && content.length() > 0) {
			// URL
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(this.apiUrl);
			String url = builder.build().toUriString();
			
			// HTTP HEADER 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			
			// REQUEST BODY 설정
			Map<String, Object> bodyMap = new HashMap<String, Object>();
			bodyMap.put("tag_type", this.tagType);  // DNER or MNER or MDNER 
			bodyMap.put("requests", new String[] {content});
			bodyMap.put("company_module", this.companyModule); // 반드시 설정해야 함.
			if(this.model != null) {
				bodyMap.put("model", this.model);
			}
			if(this.analyze != null) {
				bodyMap.put("analyze", this.analyze);
			}
			
			String requestBody = this.gson.toJson(bodyMap);
			
			if(logger.isDebugEnabled()) {
				logger.debug("[ENTITY] =======================================================");
				logger.debug("[ENTITY] [REQUEST URL] {}", url);
				logger.debug("[ENTITY] [REQUEST HEADER] {}", headers);
				logger.debug("[ENTITY] [REQUEST BODY] {}", requestBody);
				logger.debug("[ENTITY] [REQUEST BODYSIZE] {}", requestBody.length());
				logger.debug("[ENTITY] =======================================================");
			}
			
			HttpEntity<String> entity = new HttpEntity<String> (requestBody, headers);
			Map<String, Object> responseMap = this.restTemplate.postForObject(url, entity, Map.class);
			
			logger.trace("[ENTITY] [RESPONSE] {}", responseMap);
			
			String responseCode = (String) responseMap.get("response_code");
			if("C200".equals(responseCode)) {
				Map<String, String> dupCheckMap = new HashMap<String, String>();
				List<Map<String, Object>> predictList = (List<Map<String, Object>>) responseMap.get("predictions");
				List<String> tagTypeList = null;
				List<String> tokenList = null;
				String tag4Index = null;
				String tagType = null;
				String token = null;
				
				for(Map<String, Object> tagMap : predictList) {
					tagTypeList = (List<String>) tagMap.get("tags");
					tokenList = (List<String>) tagMap.get("token");
					for(int index = 0; index < tagTypeList.size(); index++) {
						tag4Index = null;
						tagType = tagTypeList.get(index);
						token = tokenList.get(index).trim();
						
						logger.trace("[ENTITY] entity: {}, {}", tagType, token);
						if(token.length() > 0) {
							if(tagType.startsWith("LOC")) {
								tag4Index = "place|" + token;
							} else if(tagType.startsWith("PER")) {
								tag4Index = "person|" + token;
							} else if(tagType.startsWith("ORG")) {
								tag4Index = "gov|" + token;
							} else if(tagType.startsWith("ETC")) {
								tag4Index = "etc|" + token;
							} else {
								logger.trace("[ENTITY] --> pass entity (No support tag type): {}, {}", tagType, token);
							}
						} else {
							logger.trace("[ENTITY] --> pass entity (Empty token): {}, {}", tagType, token);
						}
						
						if(tag4Index != null) {
							if(!dupCheckMap.containsKey(tokenList.get(index).toLowerCase())) {
								dupCheckMap.put(tokenList.get(index).toLowerCase(), null);
								resultList.add(tag4Index);
							}
						}
					}
				}
				
			} else {
				logger.warn("[ENTITY] Can't extract entity: {}", responseMap);
			}
			
		} else {
			logger.warn("[ENTITY] Content is empty");
		}
		
		logger.debug("[ENTITY] Count of extracted entity: {}", resultList.size());
		logger.trace("[ENTITY] extracted entity: {}", resultList);
		
		return resultList;
	}
}
