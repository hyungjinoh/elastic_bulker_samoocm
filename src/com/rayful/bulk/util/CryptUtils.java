package com.rayful.bulk.util;

import java.util.Properties;

import com.rayful.bulk.encryption.BulkerBaseCrypt;
import com.rayful.bulk.encryption.IBulkerCrypt;
import com.rayful.bulk.vo.DFLoadVO;

public class CryptUtils {
	private static IBulkerCrypt INSTANCE = null;

	/** ENC(...) 값 복호화 전용 인스턴스 (환경변수 BULKER_CRYPT_PASSWORD 기반, Crypt CLI와 동일) */
	private static IBulkerCrypt ENC_RESOLVER = null;

	public static synchronized IBulkerCrypt getInstance(DFLoadVO dfVO, Properties prop) throws Exception{
		if (INSTANCE == null) {
			INSTANCE = BulkerUtils.newInstanceCryptClass(dfVO, prop);
		}

		return INSTANCE;
	}

	/**
	 * 설정값이 ENC(...) 형태이면 복호화하여 평문을 리턴하고, 아니면 원문을 그대로 리턴한다.
	 * <p/>
	 * 메인 config.properties 는 Jasypt 프로퍼티 후처리를 거치지 않으므로, ES API Key 등
	 * 중요정보를 ENC(...) 로 설정한 경우 이 메서드로 복호화해야 한다.
	 * 복호화는 Crypt CLI 와 동일한 BulkerBaseCrypt(환경변수 BULKER_CRYPT_PASSWORD) 를 사용한다.
	 *
	 * @param value 설정값(ENC(...) 또는 평문)
	 * @return 복호화된 평문(또는 원문)
	 */
	public static String resolve(String value) {
		if (value == null) {
			return null;
		}
		String v = value.trim();
		if (v.startsWith("ENC(") && v.endsWith(")")) {
			String enc = v.substring(4, v.length() - 1).trim();
			return getEncResolver().decode(enc);
		}
		return value;
	}

	private static synchronized IBulkerCrypt getEncResolver() {
		if (ENC_RESOLVER == null) {
			// 환경변수 BULKER_CRYPT_PASSWORD 미설정 시 생성자에서 명확한 예외 발생
			ENC_RESOLVER = new BulkerBaseCrypt(null, null);
		}
		return ENC_RESOLVER;
	}
}
