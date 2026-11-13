package com.rayful.bulk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rayful.bulk.attach.IAttachFileHelper;
import com.rayful.bulk.encryption.IBulkerCrypt;
import com.rayful.bulk.service.IBulkerNotesService;
import com.rayful.bulk.service.IBulkerService;
import com.rayful.bulk.service.IEtcProcessor;
import com.rayful.bulk.service.IEtcReader;
import com.rayful.bulk.service.IIndexService;
import com.rayful.bulk.service.maru42.Qa42Service;
import com.rayful.bulk.service.maru42.entity.EntityExtractorService;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.CustomClassItemVO;

/**
 * 벌커 수행에 필요한 공통기능을 구현한 유틸릴티 클래스
 */
public class BulkerUtils {
	private static final Logger logger = LoggerFactory.getLogger(BulkerUtils.class);
	private static final int RESTTEMPAT_READ_TIMOUT=60000;
	private static final int RESTTEMPAT_CONN_TIMOUT=3000;
	private static final int RESTTEMPAT_POOL_MAXTOTAL=30;
	private static final int RESTTEMPAT_POOL_MAXPERROUTE=10;
	public static final String LOG_DATA_LINE 		= "---------------------------------------------------------------";
	public static final String LOG_SUMMARY_LINE 	= "===============================================================================";
	
	
	private static RestTemplate REST_TMPLATE = null;
	private static RestTemplate REST_SSL_TMPLATE = null;
//	private static final Gson GSON =  new GsonBuilder().disableHtmlEscaping().create();
//	private static final Gson GSONS24 =  new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	// 변경 후
	private static final Gson GSON =  new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
	// 변경 후
	private static final Gson GSONS24 =  new GsonBuilder().disableHtmlEscaping().serializeNulls().setPrettyPrinting().create();
	
	/**
	 * 벌커 실행파라미터 유효성 체크
	 * @param args 실팽 파라미터
	 * @return 파라미터 정보 Map
	 */
	public static Map<String, Object> validParams(String[] args, Properties prop) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String paramKey = null;
		String paramVal = null;
		
		if(args.length > 0) {
			for (int i = 0; i < args.length - 1; i += 2) {
				paramKey = args[i].toLowerCase().replace("-", "");
				paramVal = args[i + 1].toLowerCase();
				paramMap.put(paramKey, paramVal);
			}
			
			if(!paramMap.containsKey("dfname")) {
				logger.error("-dfname \"DEFINITION_FILE_NAME\" parameter is required");
				System.out.println("Invalid parameter:");
				System.out.println("\t-dfname \"DEFINITION_FILE_NAME\" parameter is required");
				System.exit(1);
			}
			
			String indexMode = (String)paramMap.get("indexmode");
			if(indexMode == null) {
				paramMap.put("indexmode", prop.get("BULKER.DEFAULT.INDEXMODE"));
				if(!"all".equals(indexMode) && "inc".equals(indexMode) &&"send".equals(indexMode)) {
					logger.error("indexmode is invalid: {}",  indexMode);
					System.out.println("Invalid parameter:");
					System.out.println("\tindexmode is invalid: " + indexMode);
					System.exit(1);
				}
			}
			
			// 2024.09.02 정충열 - parameter (maxrows) 추가 
			String maxrows = (String)paramMap.get("maxrows");
			if(paramMap.containsKey("maxrows")) {
				try {
					paramMap.put("maxrows", Integer.parseInt(maxrows));
				} catch(Exception e) {
					logger.error("maxrows must be number format: {}", maxrows);
					System.exit(1);
				}
			} else {
				paramMap.put("maxrows", -1); // Default 값
			}
			
			// 2024.08.26 정충열 - parameter (printdetail) 추가 
			if(paramMap.containsKey("printdetail")) {
				paramMap.put("printdetail", "yes".equals((String)paramMap.get("printdetail")));
			} else {
				paramMap.put("printdetail", true); // Default 값
			}
			
			// 2024.08.26 정충열 - parameter (printinterval) 추가 
			if(paramMap.containsKey("printinterval")) {
				try {
					paramMap.put("printinterval", Integer.parseInt((String)paramMap.get("printinterval")));
				} catch(Exception e) {
					logger.warn("Parameter - printinterval is ignored. Because not number format.");
					paramMap.put("printinterval", 100); // Default 값
				}
			} else {
				paramMap.put("printinterval", 100); // Default 값
			}
			
			// 2025.07.10 정충열 - parameter (batchlog) 추가 
			if(paramMap.containsKey("batchlog")) {
				paramMap.put("batchlog", "yes".equals((String)paramMap.get("batchlog")));
			} else {
				paramMap.put("batchlog", false); // Default 값
			}
			
			// 2025.07.10 정충열 - parameter (dupcheck) 추가 
			if(paramMap.containsKey("dupcheck")) {
				String dupCheck = (String)paramMap.get("dupcheck");
				
				if(!"month".equals(dupCheck) 
						&& !"day".equals(dupCheck)
						&& !"hour".equals(dupCheck)
						&& !"minute".equals(dupCheck)
						&& !"none".equals(dupCheck)) {
					logger.error("dupcheck is invalid: {}",  dupCheck);
					System.out.println("Invalid parameter:");
					System.out.println("\tdupcheck is invalid: " + dupCheck);
					System.exit(1);
				} else {
					paramMap.put("dupcheck", dupCheck);
				}
			} else {
				paramMap.put("dupcheck", "none"); // Default 값
			}
			
		} else {
			System.out.println("printUsage:");
			System.out.println("\tjava com.rayful.bulk.exec.Bulker -indexmode INDEX_MODE -dfname DEFINITION_FILE_NAME");
			System.out.println("\t\t[-maxrows number]");
			System.out.println("\t\t[-printdetail yes/no] [-printinterval number]"); 
			System.out.println("\t\t[-batchlog yes/no]"); 
			System.out.println("\t\t[-dupcheck month/day/hour/minute]"); 
			
			System.out.println("\tOptions:");
			
			System.out.println("\t\t-indexmode INDEX_MODE all/inc/send");
			System.out.println("\t\t\tall: Collect all data");
			System.out.println("\t\t\tinc: Collect modified data after last indexed");
			System.out.println("\t\t\tsend: Do not collect dat but only send indexing reqeust for bulk file on result folder");
			
			System.out.println("\t\t-dfname DEFINITION_FILE_NAME");
			System.out.println("\t\t\tDEFINITION_FILE_NAME value must be xml file name without extension");
			
			System.out.println("\t\t-maxrows number");
			System.out.println("\t\t\tDefine number for bulkering data to test (TEST MODE)");
			
			System.out.println("\t\t-printdetail yes/no");
			System.out.println("\t\t\tyes: Print detail log for bulkering (default value)");
			System.out.println("\t\t\tno: Print short log for bulkering");
			
			System.out.println("\t\t-printinterval number");
			System.out.println("\t\t\tDefine interval(number) of printing log for bulkering");
			System.out.println("\t\t\tdefault value: 100");
			
			System.out.println("\t\t-batchlog yes/no");
			System.out.println("\t\t\tyes: Write to admin's batch log table(DB)");
			System.out.println("\t\t\tno: Don't write to admin's batch log table(DB) (default value)");
			
			System.out.println("\t\t-dupcheck month/day/hour/minute/none");
			System.out.println("\t\t\t...Check Duplicate bulker's excecution(DB)");
			System.out.println("\t\t\tmonth: Check Duplicate bulker's excecution by month ");
			System.out.println("\t\t\tday: Check Duplicate bulker's excecution by day ");
			System.out.println("\t\t\thour: Check Duplicate bulker's excecution by hour ");
			System.out.println("\t\t\tminute: Check Duplicate bulker's excecution by minute ");
			System.out.println("\t\t\tnone: Don't check Duplicate bulker's excecution (default value)");
			
			System.exit(1);
		}
		
		return paramMap;
	}
	
	/**
	 * lock 파일을 객체를 생성해서 리턴한다.
	 * @param dfName DF 정보
	 * @param indexType 색인 타입 (index / delete)
	 * @param prop 설정정보 파일
	 * @return lock 파일
	 */
	public static File getLockFile(String dfName, String indexType, Properties prop) {
		String lockPath = prop.getProperty("BULKER.PATH.LOCK");
		String lockName = null;
		
		if("index".equalsIgnoreCase(indexType)) {
			lockName = dfName + ".lck";
		} else if("update".equalsIgnoreCase(indexType)) {
			lockName = dfName + "_update.lck";
		} else {
			lockName = dfName + "_delete.lck";
		}
		
		File file = new File(lockPath, lockName);
		
		return file;
	}
	
	/**
	 * Lock 파일 존재 여부 확인 및 생성
	 * <p/>
	 * lock 파일이 존재 하지 않으면 lock 파일을 생성하고 false를 리턴한다.
	 * @param file lock 파일
	 * @return Lock 파일 존재할 경우 true, Lock 파일 존재하지 않을 경우 false + lock 파일 생성
	 */
	public static boolean hasLockFile(File file) {
		boolean isResult = false;

		if(file.exists()) {
			isResult = true;
		
		} else {
			FileOutputStream fos = null;
			
			try {
				if(!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				
				fos = new FileOutputStream(file);
				fos.write(1);
			
			} catch (FileNotFoundException e1) {
				logger.error(e1.toString(), e1);
			
			} catch (IOException e2) {
				logger.error(e2.toString(), e2);
			
			} finally {
				if(fos != null) { try { fos.close(); } catch (IOException e3) { logger.error(e3.getMessage(), e3); } }
			}
		}
		
		return isResult;
	}
	
	/**
	 * Lock 파일 삭제
	 * @param file lock 파일
	 */
	public static void deleteLockFile(File file) {
		if(file.exists()) {
			file.delete();
		}
	}
	
	/**
	 * IEtcReader를 implements 클래스 인스트턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 etcReader 클랙스의 인스턴스
	 * @throws Exception
	 */
	public static IEtcReader newInstanceEtcClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IEtcReader instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getEtcReader();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IEtcReader> newClass = (Class<IEtcReader>) Class.forName(customCalssItem.getClassName());
				Constructor<IEtcReader> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading etcReader class is failed.", e);
				throw new Exception("New etcReader class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load EtcReader class: {}", customCalssItem.getClassName());
		}
		
		return instance;
	}
	
	/**
	 * IBulkerService를 implements한 클래스 인스트턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 IBulkerService 클랙스의 인스턴스, 명시하지 않으면 "com.rayful.bulk.service.es.BulkerService" 인스턴스
	 * @throws Exception
	 */
	public static IBulkerService newInstanceBulkerServiceClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IBulkerService instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getBulkerSerivce();
		String className = null;
		
		if(customCalssItem == null) {
			className = "com.rayful.bulk.service.es.BulkerService";
		} else {
			className = customCalssItem.getClassName();
		}
	
		try {
			@SuppressWarnings("unchecked")
			Class<IBulkerService> newClass = (Class<IBulkerService>) Class.forName(className);
			Constructor<IBulkerService> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
			instance = (IBulkerService) construct.newInstance(dfVO, prop);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			logger.error("Loading bulkerService class is failed.", e);
			throw new Exception("Loading bulkerService class(" + className + ") is failed: " + e.getMessage());
		}
		
		logger.info("- Load BulkerService class: {}", className);
		
		return instance;
	}
	
	public static IBulkerNotesService newInstanceBulkerNotesServiceClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IBulkerNotesService instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getBulkerSerivce();
		String className = null;
		
		if(customCalssItem == null) {
			className = "com.rayful.bulk.service.es.BulkerNotesService";
		} else {
			className = customCalssItem.getClassName();
		}
	
		try {
			@SuppressWarnings("unchecked")
			Class<IBulkerNotesService> newClass = (Class<IBulkerNotesService>) Class.forName(className);
			Constructor<IBulkerNotesService> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
			instance = (IBulkerNotesService) construct.newInstance(dfVO, prop);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
				| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
			logger.error("Loading bulkerNotesService class is failed.", e);
			throw new Exception("Loading bulkerNotesService class(" + className + ") is failed: " + e.getMessage());
		}
		
		logger.info("- Load bulkerNotesService class: {}", className);
		
		return instance;
	}
	
	/**
	 * Qa42Service 클래스 인스턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 Qa42Service 클랙스의 인스턴스
	 * @throws Exception
	 */
	public static Qa42Service newInstanceQa42ServiceClass(DFLoadVO dfVO, Properties prop) throws Exception {
		Qa42Service instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getQaService();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<Qa42Service> newClass = (Class<Qa42Service>) Class.forName(customCalssItem.getClassName());
				Constructor<Qa42Service> construct = newClass.getConstructor(Properties.class);
				instance = construct.newInstance(prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading Qa42Service class is failed.", e);
				throw new Exception("Loading Qa42Service class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load Qa42Service class: {}", customCalssItem.getClassName());
		}
		
		return instance;
	}
	
	/**
	 * IIndexService를 implements한 클래스 인스턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 IIndexService 클랙스의 인스턴스
	 * @throws Exception
	 */
	public static IIndexService newInstanceIndexServiceClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IIndexService instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getIndexSerivce();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IIndexService> newClass = (Class<IIndexService>) Class.forName(customCalssItem.getClassName());
				Constructor<IIndexService> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading IndexService class is failed.", e);
				throw new Exception("Loading IndexService class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load IndexService class: {}", customCalssItem.getClassName());
		}
		
		return instance;
	}
	
	/**
	 * EntityExtractorService 클래스 인스턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 EntityExtractorService 클랙스의 인스턴스
	 * @throws Exception
	 */
	public static EntityExtractorService newInstanceEntityServiceClass(DFLoadVO dfVO, Properties prop) throws Exception {
		EntityExtractorService instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getEntityService();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<EntityExtractorService> newClass = (Class<EntityExtractorService>) Class.forName(customCalssItem.getClassName());
				Constructor<EntityExtractorService> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading EntityService class is failed.", e);
				throw new Exception("Loading EntityService class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load EntityService class: {}", customCalssItem.getClassName());
		}
		
		return instance;
	}
	
	/**
	 * IAttachFileHelper를 implements한 클래스 인스턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 IAttachFileHelper 클랙스의 인스턴스, 명시하지 않으면 "com.rayful.bulk.attach.DiskFileHelper" 인스턴스
	 * @throws Exception
	 */
	public static IAttachFileHelper newInstanceFileHelperClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IAttachFileHelper instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getFileHelper();
		String className = null;
		
		if(customCalssItem == null) {
			className = "com.rayful.bulk.attach.DiskFileHelper";
		} else {
			if(customCalssItem.isUse()) {
				className = customCalssItem.getClassName();
			}
		}

		if(className != null && dfVO.field.isIndexFileData()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IAttachFileHelper> newClass = (Class<IAttachFileHelper>) Class.forName(className);
				Constructor<IAttachFileHelper> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading FileHelper class is failed.", e);
				throw new Exception("Loading FileHelper class(" + className + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load FileHelper class: {}", className);
		}
		
		return instance;
	}
		
	/**
	 * IBulkerCrypt를 implements한 클래스(암호화 클래스) 인스턴스를 생성한다.
	 * @param dfVO df 정보
	 * @param prop 설정파일 정보
	 * @return df에 명시한 IBulkerCrypt를 클랙스의 인스턴스
	 * @throws Exception
	 */
	public static IBulkerCrypt newInstanceCryptClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IBulkerCrypt instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getBulkerCrypt();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IBulkerCrypt> newClass = (Class<IBulkerCrypt>) Class.forName(customCalssItem.getClassName());
				Constructor<IBulkerCrypt> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading Crypt class is failed.", e);
				throw new Exception("Loading Crypt class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load Crypt class: {}", customCalssItem.getClassName());
		}
		return instance;
	}
	
	
	public static IEtcProcessor newInstancEtcProcessorClass(DFLoadVO dfVO, Properties prop) throws Exception {
		IEtcProcessor instance = null;
		CustomClassItemVO customCalssItem = dfVO.custom.getEtcProcessor();
		
		if(customCalssItem != null && customCalssItem.isUse()) {
			try {
				@SuppressWarnings("unchecked")
				Class<IEtcProcessor> newClass = (Class<IEtcProcessor>) Class.forName(customCalssItem.getClassName());
				Constructor<IEtcProcessor> construct = newClass.getConstructor(DFLoadVO.class, Properties.class);
				instance = construct.newInstance(dfVO, prop);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
				logger.error("Loading EtcProcessor class is failed.", e);
				throw new Exception("Loading EtcProcessor class(" + customCalssItem.getClassName() + ") is failed: " + e.getMessage());
			}
			
			logger.info("- Load EtcProcessor class: {}", customCalssItem.getClassName());
		}
	
		return instance;
	}
	
	/**
	 * RestTemplate 클래스의 인스턴스를 생성애서 리턴한다.
	 * @return RestTemplate 클래스의 인스턴스
	 */
	public static RestTemplate getInstanceRestTemplate(){
		if( REST_TMPLATE == null) {
			HttpClient httpClient = HttpClientBuilder.create()
					.setMaxConnTotal(RESTTEMPAT_POOL_MAXTOTAL)    //최대 오픈되는 커넥션 수, 연결을 유지할 최대 숫자
					.setMaxConnPerRoute(RESTTEMPAT_POOL_MAXPERROUTE)   //IP, 포트 1쌍에 대해 수행할 커넥션 수, 특정 경로당 최대 숫자
					.build();
	
			HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
							factory.setReadTimeout(RESTTEMPAT_READ_TIMOUT);        
							factory.setConnectTimeout(RESTTEMPAT_CONN_TIMOUT);     
							factory.setHttpClient(httpClient);
			
			RestTemplate restTemplate = new RestTemplate(factory);
			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
					logger.error("RestTemplate --- ERROR statusCode={}", response.getStatusCode());
				}
			});
			
			REST_TMPLATE = restTemplate;
		}
		
		return REST_TMPLATE;
	}
	
	/**
	 * HTTPS(SSL) 용 RestTemplate 인스턴스를 생성해서 리턴한다.
	 * <p/>
	 * [행정안전부 SW 개발보안 가이드 준수 - 부적절한 인증서 유효성 검증(CWE-295)]<br/>
	 * 기존의 "모든 인증서 무조건 신뢰(trust-all)" + 호스트명 검증 비활성화(NoopHostnameVerifier)
	 * 코드를 제거하고, 다음 3단계 전략으로 인증서 검증을 수행한다.
	 * <ol>
	 *   <li>[TrustStore] ES.SSL.TRUSTSTORE.PATH 설정 시: 지정한 신뢰저장소(CA/자체서명 인증서)로 검증</li>
	 *   <li>[명시적 opt-in] ES.SSL.TRUST_SELF_SIGNED=true 설정 시: 자체서명 인증서만 신뢰
	 *       (폐쇄망 한정, 보안경고 로그 출력). 호스트명 검증은 기본 수행하며,
	 *       ES.SSL.DISABLE_HOSTNAME_VERIFY=true 로 명시할 때만 예외적으로 비활성화</li>
	 *   <li>[JVM 기본] 그 외: JVM 기본 신뢰저장소 + 정상 호스트명 검증(안전한 기본값)</li>
	 * </ol>
	 *
	 * @param prop 설정 정보
	 * @return SSL 용 RestTemplate 인스턴스
	 * @throws Exception SSL 컨텍스트 구성 실패 시
	 */
	public static RestTemplate getInstanceRestTemplateSsl(Properties prop) throws Exception {
		if (REST_SSL_TMPLATE == null) {
			String truststorePath = prop == null ? null : prop.getProperty("ES.SSL.TRUSTSTORE.PATH");
			boolean trustAll = prop != null && "true".equalsIgnoreCase(prop.getProperty("ES.SSL.TRUST_ALL"));          // 추가
			boolean trustSelfSigned = prop != null && "true".equalsIgnoreCase(prop.getProperty("ES.SSL.TRUST_SELF_SIGNED"));
			boolean disableHostnameVerify = prop != null && "true".equalsIgnoreCase(prop.getProperty("ES.SSL.DISABLE_HOSTNAME_VERIFY"));

			SSLContext sslContext;
			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();

			try {
				if (truststorePath != null && truststorePath.trim().length() > 0) {
					// (1) TrustStore 브랜치: 지정한 신뢰저장소로 인증서 검증
					char[] truststorePassword = null;
					String pw = prop.getProperty("ES.SSL.TRUSTSTORE.PASSWORD");
					if (pw != null && pw.length() > 0) {
						truststorePassword = pw.toCharArray();
					}

					KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
					try (FileInputStream fis = new FileInputStream(new File(truststorePath))) {
						trustStore.load(fis, truststorePassword);
					}

					sslContext = org.apache.http.ssl.SSLContexts.custom()
									.loadTrustMaterial(trustStore, null)
									.build();
					logger.info("[SSL] Certificate validation with truststore: {}", truststorePath);

				} else if (trustAll) {
					// (0) 명시적 opt-in 브랜치: 모든 인증서 무조건 신뢰 (인증서 검증 자체를 생략)
					logger.warn("[SSL][SECURITY] TRUST_ALL is enabled. All certificates are accepted WITHOUT validation. "
							+ "Use only for local/dev. Never use on an open network in production.");
					sslContext = org.apache.http.ssl.SSLContexts.custom()
							.loadTrustMaterial(null, (chain, authType) -> true)
							.build();

					if (disableHostnameVerify) {
						logger.warn("[SSL][SECURITY] Hostname verification is DISABLED by ES.SSL.DISABLE_HOSTNAME_VERIFY=true.");
						hostnameVerifier = NoopHostnameVerifier.INSTANCE;
					}

				} else if (trustSelfSigned) {
					// (2) 명시적 opt-in 브랜치: 자체서명 인증서만 신뢰(폐쇄망 한정)
					logger.warn("[SSL][SECURITY] TRUST_SELF_SIGNED is enabled. Only allowed in an isolated/closed network.");
					sslContext = org.apache.http.ssl.SSLContexts.custom()
									.loadTrustMaterial(null, new TrustSelfSignedStrategy())
									.build();

					if (disableHostnameVerify) {
						logger.warn("[SSL][SECURITY] Hostname verification is DISABLED by ES.SSL.DISABLE_HOSTNAME_VERIFY=true.");
						hostnameVerifier = NoopHostnameVerifier.INSTANCE;
					}

				} else {
					// (3) JVM 기본 브랜치: 기본 신뢰저장소 + 정상 호스트명 검증(안전한 기본값)
					sslContext = SSLContext.getDefault();
					logger.info("[SSL] Certificate validation with JVM default truststore.");
				}
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
				logger.error("Failed to initialize SSL context for RestTemplate.", e);
				throw new Exception("RestTemplateSsl init failed: " + e.getMessage());
			}

			SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			CloseableHttpClient httpClient = HttpClients.custom()
												.setSSLSocketFactory(sslFactory)
												.setMaxConnTotal(RESTTEMPAT_POOL_MAXTOTAL)
												.setMaxConnPerRoute(RESTTEMPAT_POOL_MAXPERROUTE)
												.build();

			HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
			factory.setReadTimeout(RESTTEMPAT_READ_TIMOUT);
			factory.setConnectTimeout(RESTTEMPAT_CONN_TIMOUT);
			factory.setHttpClient(httpClient);

			RestTemplate restTemplate = new RestTemplate(factory);
			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
					logger.error("RestTemplateSsl --- ERROR statusCode={}", response.getStatusCode());
				}
			});

			REST_SSL_TMPLATE = restTemplate;
		}

		return REST_SSL_TMPLATE;
	}
	
	/**
	 * Gson 객체를 생성해서 리턴한다.
	 * @return Gson 객체
	 */
	public static Gson getGson() {
		return GSON;
	}
	
	/**
	 * GsonS24 객체를 생성해서 리턴한다.
	 * @return GsonS24 객체
	 */
	public static Gson getGsonS24() {
		return GSONS24;
	}
	
	/**
	 * 벌커의 요약로그를 기록한다.
	 * <p/>
	 * 쿼리 요약 로그, 덤프 요약 로그, QA 덤프 요약 로그, 인덱스 요청 요약 로그를 기록한다.
	 * @param bulkerLogVO 벌커 로그 객체
	 */
	public static void printBulkerLog(BulkerLogVO bulkerLogVO) {
		CountLogVO logVO = null;

		logger.info("");
		logger.info(LOG_SUMMARY_LINE);
		
		// Notes일 경우  nsf name을 출력한다.
		if(bulkerLogVO.nsf != null) {
			logger.info("### NSF Name: {}", bulkerLogVO.nsf.getName());
			logger.info("# Completed: {}", bulkerLogVO.nsf.isCompleted());
			if(!bulkerLogVO.nsf.isCompleted()) {
				logger.error("# Reason: {}", bulkerLogVO.nsf.getDesc());
			}
		}
		
		// -----------------------------------------------
		// 쿼리 요약 로그 출력
		logVO = bulkerLogVO.query;
		if (logVO.getTotalCount() > 0) {
			logger.info(LOG_DATA_LINE);
			logger.info("# Data Query Summary #");
			logger.info("- [Key Query Count] {}", logVO.getTotalCount());
			logger.info("- [Data Process] Success: {}",logVO.getSuccessCount());
			if(logVO.getWarnCount() > 0) {
				logger.info("\tWarn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
			}
			if(logVO.getFailCount() > 0) {
				logger.info("\tFail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
			}
			//logger.info(LOG_DATA_LINE);
		}
		
		// -----------------------------------------------
		// 덤프 요약 로그 출력
		logVO = bulkerLogVO.dump;
		if (logVO.getTotalCount() > 0) {
			logger.info(LOG_DATA_LINE);
			logger.info("# Dump File Count Summary #");
			logger.info("- Success: {}",logVO.getSuccessCount());
			if(logVO.getWarnCount() > 0) {
				logger.info("- Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
			}
			if(logVO.getFailCount() > 0) {
				logger.info("- Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
			}
			//logger.info(LOG_DATA_LINE);
		}
		
		// -----------------------------------------------
		// QA 덤프 요약 로그 출력
		logVO = bulkerLogVO.qadump;
		if (logVO.getTotalCount() > 0) {
			logger.info(LOG_DATA_LINE);
			logger.info("# QA Dump File Count Summary #");
			logger.info("- Success: {}",logVO.getSuccessCount());
			if(logVO.getWarnCount() > 0) {
				logger.info("- Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
			}
			if(logVO.getFailCount() > 0) {
				logger.info("- Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
			}
			//logger.info(LOG_DATA_LINE);
		}
		
		// -----------------------------------------------
		// 인덱스 요청 요약 로그 출력
		logVO = bulkerLogVO.index;
		if (logVO.getTotalCount() > 0) {
			logger.info(LOG_DATA_LINE);
			logger.info("# Index Request Summary #");
			logger.info("- [Bulk File Count] {}", logVO.getTotalCount());
			logger.info("- [Index Request] Success: {}",logVO.getSuccessCount());
			if(logVO.getWarnCount() > 0) {
				logger.info("\tWarn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
			}
			if(logVO.getFailCount() > 0) {
				logger.info("\tFail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
			}
			//logger.info(LOG_DATA_LINE);
			
			logger.info(LOG_DATA_LINE);
			logger.info("# Index Response Summary #");
			logger.info("- [Engine Response] Total: {}", bulkerLogVO.indexResult.getTotalCount());
			logger.info("- [Engine Resposne] Created: {}", bulkerLogVO.indexResult.getCreateCount());
			logger.info("- [Engine Resposne] Updated: {}", bulkerLogVO.indexResult.getUpdateCount());
			logger.info("- [Engine Resposne] Deleted: {}", bulkerLogVO.indexResult.getDeleteCount());
			if(bulkerLogVO.indexResult.getPartialCount() > 0) {
				logger.warn("- [Engine Resposne] Partail: {}", bulkerLogVO.indexResult.getPartialCount());
			}
			if(bulkerLogVO.indexResult.getEtcCount() > 0) {
				logger.warn("- [Engine Resposne] Etc: {}", bulkerLogVO.indexResult.getEtcCount());
				logger.warn("    {}", bulkerLogVO.indexResult.getEtcResult());
			}
			if(bulkerLogVO.indexResult.getErrorCount() > 0) {
				logger.warn("- [Engine Resposne] Error: {}", bulkerLogVO.indexResult.getErrorCount());
				logger.warn("    {}", bulkerLogVO.indexResult.getErrorReason());
			}
		}
		logger.info(LOG_SUMMARY_LINE);
		
	}
	
	public static String getSuccessDatetimeFolderName(Properties prop) {
		String result = null;
		String startTime = prop.getProperty("__RUNTIME.START_ITME");
		if (startTime == null) {
			throw new NullPointerException("Can't find \"__RUNTIME.START_ITME\"");
		}
		
		 DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	     LocalDateTime locatTime = LocalDateTime.parse(startTime, sourceFormatter);
	     
	     DateTimeFormatter destFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	     result = locatTime.format(destFormatter);
	     
	     return result;
	}
	
	public static boolean isUseIndexNamePattern (String indexName) {
		 return indexName.contains("#index_pattern#");
	}
	
	public static String applyIndexNamePattern(String indexName, String applyValue) {
		if(applyValue == null) {
			throw new NullPointerException("applyValue must be not null");
		}
		
		return indexName.replace("#index_pattern#", applyValue);
	}
	
	public static String getJsonFileName(String indexType, String fileBaseName) {
		String fileName = null;
		if("index".equalsIgnoreCase(indexType)) {
			fileName = "index_" + fileBaseName + "_" + Utils.getNow() + ".json";
		} else if("update".equalsIgnoreCase(indexType)) {
			fileName = "update_" + fileBaseName + "_" + Utils.getNow() + ".json";
		} else {
			fileName = "delete_" + fileBaseName + "_" + Utils.getNow() + ".json";
		}
		
		return fileName;
	}
}
