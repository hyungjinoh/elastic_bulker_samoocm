package com.rayful.bulk.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.NotesNsfItemVO;

public class SampleEtcProcessor extends BaseEtcProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SampleEtcProcessor.class);
	
	public SampleEtcProcessor(DFLoadVO dfVO, Properties prop) {
		super(dfVO, prop);
	}
	
	@Override
	public void preProcess() {
		logger.info("Called pre process");
		setNsfList();
	}
	
	@Override
	public void postProcess() {
	}
	
	public void setNsfList() {
		File listFile = this.dfVO.nsfList.getListFile();
		if(listFile == null ) {
			logger.warn("Not defined nsf_list file in DF.");
		}
		
		try {
			// READ FILE TO LIST
			Map<String, Object> fileMap = Utils.getMapFromJsonFile(listFile);
			@SuppressWarnings("unchecked")
			List<Map<String, String>> nsfList = (List<Map<String, String>>) fileMap.get("nsf_list");
			if(nsfList == null) {
				throw new NullPointerException("nsf_list not eixts.");
			}
			
			// Get New nsf name
			LocalDate now = LocalDate.now();
			int year = now.getYear();
			int quarter = (int)Math.ceil( (double)now.getMonthValue() / 3);
			String newNsfName = String.format("egate/global/eap1/aprv/repository/doc/repository%d%d.nsf", year, quarter);
			logger.info("newNsfName: {}", newNsfName);
			
			// Check exists - New nsf name 
			boolean isFind = false;
			String dataSource = null;
			String nsfName = null;
			for(Map<String, String> nsfMap : nsfList) {
				dataSource = nsfMap.get("dataSource");
				nsfName = nsfMap.get("name");
				if(newNsfName.equals(nsfName)) {
					isFind = true;
					logger.info("newNsfName: {} is aleady exists.", newNsfName);
					break;
				}
			}
			
			// WIRTE LIST TO FILE
			if(!isFind) {
				Map<String, String> map = new HashMap<>();
				map.put("dataSource", dataSource);
				map.put("name", newNsfName);
				nsfList.add(map);
				logger.info("Added newNsfName: {} ", newNsfName);
				
				Gson gson = BulkerUtils.getGsonS24();
				try (BufferedWriter bw = new BufferedWriter(new FileWriterWithEncoding(listFile, "UTF-8"))) {
					bw.write(gson.toJson(fileMap));
				}
			}
			
			logger.info("Success to execute pre process");
		} catch (Exception e) {
			logger.error("Fail to execute pre process", e);
		}
		
	}
}
