package com.rayful.bulk.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 시큐어코딩(행정안전부 SW 개발보안 가이드) 준수를 위한 공통 보안 유틸리티.
 * <p/>
 * - 경로 조작(Path Traversal, CWE-22) 방지: 파일명이 기준 디렉터리를 벗어나지 않도록 검증<br/>
 * - 중요정보 노출(CWE-532) 방지: 로그 출력 시 자격증명/토큰 마스킹<br/>
 */
public final class SecurityUtils {

	private SecurityUtils() {}

	/**
	 * 기준 디렉터리(baseDir) 하위로만 파일 경로를 안전하게 결합한다.
	 * <p/>
	 * 입력 파일명에 "../" 등 상위 경로 이동 문자가 포함되어 기준 디렉터리를 벗어나는 경우
	 * IOException을 던져 경로 조작 공격을 차단한다. (CWE-22)
	 *
	 * @param baseDir  기준 디렉터리
	 * @param fileName 결합할 파일명(외부/DB 입력)
	 * @return 검증된 안전한 절대 경로
	 * @throws IOException 경로가 기준 디렉터리를 벗어나거나 파일명이 유효하지 않은 경우
	 */
	public static Path resolveSafely(Path baseDir, String fileName) throws IOException {
		if (baseDir == null) {
			throw new IllegalArgumentException("baseDir is null");
		}
		if (fileName == null || fileName.trim().isEmpty()) {
			throw new IOException("Invalid file name: empty");
		}

		// 경로 구분자가 포함된 파일명은 허용하지 않는다(디렉터리 이동 차단).
		if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
			throw new IOException("Invalid file name (path traversal detected): " + fileName);
		}

		Path base = baseDir.toAbsolutePath().normalize();
		Path resolved = base.resolve(fileName).normalize();

		if (!resolved.startsWith(base)) {
			throw new IOException("Path traversal detected. base=" + base + ", target=" + resolved);
		}

		return resolved;
	}

	/**
	 * 로그 등에 노출되면 안 되는 자격증명/토큰 값을 마스킹한다. (CWE-532)
	 *
	 * @param secret 원문 값
	 * @return 마스킹된 값(앞 2자리만 노출, 나머지는 *)
	 */
	public static String mask(String secret) {
		if (secret == null || secret.isEmpty()) {
			return "(none)";
		}
		if (secret.length() <= 4) {
			return "****";
		}
		return secret.substring(0, 2) + "****";
	}
}
