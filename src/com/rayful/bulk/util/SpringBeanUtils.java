package com.rayful.bulk.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rayful.elastic.batch.common.service.BatchLogService;
import com.rayful.elastic.batch.common.utils.BatchLogUtils;

public class SpringBeanUtils {
	private static final Logger logger = LoggerFactory.getLogger(SpringBeanUtils.class);
	private SpringBeanUtils() {}

	private static ApplicationContext springContext = null;
	private static boolean IS_INIT = false;
	
	public static void init(Properties prop) throws Exception {
		
		if ("yes".equalsIgnoreCase((String) prop.get("BULKER.ADMIN.LOG.USE"))) {
			String[] configLocations = new String[]{
		            "admin/spring/spring-context-admin.xml",
		            "admin/spring/mybatis-db-pool.xml"
		    };
	
			springContext = new ClassPathXmlApplicationContext(configLocations);
		}
		
		IS_INIT = true;
	}
	
	public static BatchLogService newInstanceBatchLogService(String dfName, String indexMode, boolean isWriteBatchLog, String dupCheck)  {
		if(!IS_INIT) {
			throw new FactoryBeanNotInitializedException("Use this method after call init() mehtod.");
		}
		
		BatchLogService service = null;
		if(springContext != null ) {
			if(isWriteBatchLog || !"none".equals(dupCheck) ) {
				service = (BatchLogService) springContext.getBean("batchLogService");
				service.init(BatchLogUtils.getBatchId(dfName, indexMode));
			} else {
				logger.info("No write admin batch log table(DB)");
			}
		} else {
			logger.info("No Spring context");
		}
		
		return service;
	}

}
