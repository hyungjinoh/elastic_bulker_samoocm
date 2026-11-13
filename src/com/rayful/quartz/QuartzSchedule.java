package com.rayful.quartz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class QuartzSchedule {
	private static final Logger logger = LoggerFactory.getLogger(QuartzSchedule.class);
	
	public static void main(String[] args) {
		String [] configLocations = new String[] {
					"quartz-schedule-context.xml"
				};
		
		ApplicationContext appContext = new ClassPathXmlApplicationContext(configLocations);
		logger.info("### Scheduler is started");
		
		// kill로 프로세스 종료 시 종료 로그 기록.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				//Logger logger = LoggerFactory.getLogger(QuartzSchedule.class);
				logger.info("### Scheduler is shutdown");
			}
		});
	}
}
