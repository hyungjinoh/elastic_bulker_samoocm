package com.rayful.bulk.exec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.encryption.IBulkerCrypt;
import com.rayful.bulk.vo.DFLoadVO;

public class Crypt {
	private static final Logger logger = LoggerFactory.getLogger(Crypt.class);
	
	private final IBulkerCrypt bulekrCrypt;
	public Crypt(String className) throws Exception {
		String fullClassName = "com.rayful.bulk.encryption." + className;
		
		try {
			@SuppressWarnings("unchecked")
			Class<IBulkerCrypt> newClass = (Class<IBulkerCrypt>) Class.forName(fullClassName);
			Constructor<IBulkerCrypt> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
			this.bulekrCrypt = construct.newInstance(null, null);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			logger.error("Loading Crypt class is failed.", e);
			throw new Exception("Loading Crypt class(" + fullClassName + ") is failed: " + e.getMessage());
		}
	}
	
	public void exec(String mode, String source) {
		if("decode".equals(mode)) {
			System.out.println("decode str=[" + bulekrCrypt.decode(source) + "]");
		} else {
			System.out.println("encode str=[" + bulekrCrypt.encode(source) + "]");
		}
	}
	
	public static Map<String, String> parseParams(String[] args) {
		Map<String, String> paramMap = new HashMap<String, String>();
		
		if(args.length == 0) {
			System.out.println("require source string to ecode");
			System.exit(1);
		} else if(args.length > 3) {
			System.out.println("too many parameter");
			System.exit(1);
		}
		
		paramMap.put("source", args[0]);
		String paramKey = null;
		String paramVal = null;
		
		for (int i = 1; i < args.length - 1; i += 2) {
			paramKey = args[i].toLowerCase().replace("-", "");
			paramVal = args[i + 1].toLowerCase();
			paramMap.put(paramKey, paramVal);
		}
		
		if(!paramMap.containsKey("class")) {
			// default value
			paramMap.put("class", "BulkerBaseCrypt");
		}
		if(!paramMap.containsKey("mode")) {
			// default value
			paramMap.put("mode", "encode");
		}
		
		return paramMap;
	}
	
	public static void main(String[] args) {
		Map<String, String> paramMap = parseParams(args);
		try {
			Crypt crypt = new Crypt(paramMap.get("class"));
			crypt.exec(paramMap.get("mode"), paramMap.get("source"));
			
		} catch(Exception e) {
			logger.error("Crypt execution failed.", e);
			System.exit(1);
		}
	}

}
