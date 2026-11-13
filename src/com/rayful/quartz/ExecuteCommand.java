package com.rayful.quartz;

import java.lang.ProcessBuilder.Redirect;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;


public class ExecuteCommand  extends QuartzJobBean {
	private static final Logger logger = LoggerFactory.getLogger(ExecuteCommand.class);
	
	@Override
	protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException {
		
		try {
			logger.info("///////////////////////////////////////////////////////////");
			logger.info("Start " +  ctx.getJobDetail().getKey().getName() + " job");
			
			String arg = (String) ctx.getMergedJobDataMap().get("arg");
			
			if(arg == null || arg.trim().length() == 0) {
				throw new IllegalArgumentException("Invalid parameter");
			}
			
			// [행정안전부 SW 개발보안 가이드 - 운영체제 명령어 삽입(CWE-78)]
			// 셸을 경유하지 않도록 ProcessBuilder(인자 배열)를 사용하며,
			// 명령 문자열에 셸 메타문자/개행/널문자가 포함되면 실행을 차단한다.
			if(arg.matches(".*[;&|`$><\\n\\r\\u0000].*")) {
				throw new IllegalArgumentException("Illegal characters in command argument.");
			}
			
			ProcessBuilder processBuilder = new ProcessBuilder(arg.split(" "));
			processBuilder.redirectOutput(Redirect.INHERIT);
			processBuilder.redirectError(Redirect.INHERIT);
			processBuilder.start();
			
		} catch (Exception e) {
			logger.error("Execute failed....", e);
		} finally {
			logger.info("End " + ctx.getJobDetail().getKey().getName() + " job");
			logger.info("///////////////////////////////////////////////////////////");
		}
	}

}
