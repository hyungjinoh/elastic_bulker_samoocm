package com.rayful.bulk.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 색인 데이터의 인코딩/해시 처리를 담당하는 유틸리티.
 * <p/>
 * [행정안전부 SW 개발보안 가이드 준수 사항]
 * - 취약한 해시 알고리즘(MD5/SHA-1) 사용 금지(CWE-327): SHA-256 사용<br/>
 * - 16진수 변환 시 0 패딩 누락 버그 수정(정확한 해시값 생성 보장)<br/>
 */
public class CodecUtil {
	private static final Logger logger = LoggerFactory.getLogger(CodecUtil.class);

	/** 안전한 해시 알고리즘 */
	private static final String HASH_ALGORITHM = "SHA-256";

	private CodecUtil() {}

	/**
	 * 입력 문자열을 SHA-256 으로 해시하여 16진수 문자열로 리턴한다.
	 *
	 * @param message 원문
	 * @return 해시 16진수 문자열, 실패 시 빈 문자열
	 */
	public static String encodeHash(String message) {
		if (message == null) {
			return "";
		}

		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));

			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				// %02x : 항상 2자리로 0 패딩 (기존 toHexString 패딩 누락 버그 수정)
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();

		} catch (NoSuchAlgorithmException e) {
			logger.error("Hash algorithm not available: {}", HASH_ALGORITHM, e);
			return "";
		}
	}

	/**
	 * Base64 로 인코딩된 문자열을 디코딩한다. Base64 형식이 아니면 원문을 그대로 리턴한다.
	 *
	 * @param message Base64 인코딩 문자열
	 * @return 디코딩된 문자열
	 */
	public static String decodeBase64(String message) {
		if (message == null) {
			return null;
		}

		try {
			return new String(Base64.getDecoder().decode(message), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			logger.warn("Not base64 encoded data. Returning original value.");
			return message;
		}
	}
}
