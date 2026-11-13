package com.rayful.bulk.util;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteUtils {
	private static final Logger logger = LoggerFactory.getLogger(ExecuteUtils.class);
	
	public static void executeCommand(List<String> command) {
		try {
			logger.debug("///////////////////////////////////////////////////////////");
			logger.debug("Start {} ", command.toString());
			
		
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.redirectOutput(Redirect.INHERIT);
			processBuilder.redirectError(Redirect.INHERIT);
			Process process = processBuilder.start();
			
			process.waitFor();
			
		} catch (InterruptedException e) {
			logger.error("Command execution was interrupted: {}", command, e);
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.error("Command execution failed: {}", command, e);

		} finally {
			logger.debug("End " + command.toString());
			logger.debug("///////////////////////////////////////////////////////////");
		}
	}
}
