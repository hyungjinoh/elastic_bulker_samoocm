package com.rayful.bulk.encryption;

import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.vo.DFLoadVO;

/**
 * Jasypt 라이브러리를 사용하여 DB 계정/패스워드 등 중요정보를 암/복호화 하는 클래스 (jasypt-1.9.3.jar)
 * <p/>
 * [행정안전부 SW 개발보안 가이드 준수 사항]
 * - 취약한 암호화 알고리즘(MD5/DES) 사용 금지(CWE-327): 강력한 PBE 알고리즘
 *   "PBEWITHHMACSHA256ANDAES_256" + 무작위 IV(RandomIvGenerator) 사용<br/>
 * - 하드코딩된 암호화 키 사용 금지(CWE-321): 기반 패스워드를 소스에 하드코딩하지 않고
 *   환경변수(BULKER_CRYPT_PASSWORD)로 외부에서 주입받는다.<br/>
 * <p/>
 * 암호화된 스트링은 DF 혹은 설정 파일 내에서 다음과 같은 형식으로 정의: ENC(암호화된 스트링)
 */
public class BulkerBaseCrypt implements IBulkerCrypt {
	private static final Logger logger = LoggerFactory.getLogger(BulkerBaseCrypt.class);

	/** 기반 패스워드를 주입받는 환경변수명 */
	private static final String ENV_CRYPT_PASSWORD = "BULKER_CRYPT_PASSWORD";
	/** 강력한 PBE 알고리즘(AES-256) */
	private static final String CRYPT_ALGORITHM = "PBEWITHHMACSHA256ANDAES_256";

	private final StandardPBEStringEncryptor encryptor;

	public BulkerBaseCrypt(DFLoadVO dfVO, Properties prop) {
		String password = resolvePassword();

		this.encryptor = new StandardPBEStringEncryptor();
		this.encryptor.setAlgorithm(CRYPT_ALGORITHM);      // 강력한 PBE(AES-256) 알고리즘
		this.encryptor.setIvGenerator(new RandomIvGenerator()); // 암호화 시마다 무작위 IV 사용
		this.encryptor.setPassword(password);              // 환경변수로 주입된 기반 패스워드
	}

	/**
	 * 기반 패스워드를 환경변수(우선) 또는 JVM 시스템 프로퍼티에서 조회한다.
	 * 값이 없으면 예외를 던져 하드코딩/기본값 사용을 원천 차단한다.
	 */
	private static String resolvePassword() {
		String password = System.getenv(ENV_CRYPT_PASSWORD);
		if (password == null || password.trim().isEmpty()) {
			password = System.getProperty(ENV_CRYPT_PASSWORD);
		}

		if (password == null || password.trim().isEmpty()) {
			logger.error("Encryption base password is not set. Set environment variable [{}].", ENV_CRYPT_PASSWORD);
			throw new IllegalStateException(
					"Encryption base password is not configured. Set environment variable ["
							+ ENV_CRYPT_PASSWORD + "].");
		}
		return password;
	}

	@Override
	public String encode(String str) {
		return this.encryptor.encrypt(str);
	}

	@Override
	public String decode(String str) {
		return this.encryptor.decrypt(str);
	}
}
