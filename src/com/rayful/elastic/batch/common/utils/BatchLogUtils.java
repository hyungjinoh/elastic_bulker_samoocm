package com.rayful.elastic.batch.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchLogUtils {
	private static final Logger logger = LoggerFactory.getLogger(BatchLogUtils.class);
	
	private BatchLogUtils() {}
	
	public static String getBatchDt() {
		LocalDateTime ldt = LocalDateTime.now();
		String formattedDate = ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		
		logger.debug("formattedDate: {}", formattedDate);
		return formattedDate;
	}
	
	public static String getBatchId(String dfName, String indexMode) {
		return "BULKER_" + dfName + "_" + indexMode;
	}
	
	public static String getBatchBaseDtKwd(String baseDt, String dupCheck)  {
		if(baseDt == null ) {
			throw new NullPointerException("baseDt == null");
		}
		
		if(baseDt.length() != 14) {
			throw new IllegalArgumentException("Expect length == 14" );
		}
		
		int lastPosition;
		if("month".equals(dupCheck)) {
			lastPosition = 6;
		} else if("day".equals(dupCheck)) {
			lastPosition = 8;
		} else if("hour".equals(dupCheck)) {
			lastPosition = 10;
		} else if("minute".equals(dupCheck)) {
			lastPosition = 12;
		} else  {
			throw new IllegalArgumentException("Invalid dupCheck value: " + dupCheck);
		}
		
		return baseDt.substring(0, lastPosition);
	}
	
	public static String getHost() {
	    String host = null;
	    try {
	        host = InetAddress.getLocalHost().getHostAddress();
	    } catch (UnknownHostException e) {
	        host = "";
	    }
	   return host; 
	}
	

}
