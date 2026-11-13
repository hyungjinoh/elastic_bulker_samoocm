package com.rayful.bulk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	/**
	 * 파일 경로를 구성하는 데 사용되는 이름 토큰(예: df 이름)의 허용 패턴.
	 * 영문/숫자/밑줄/하이픈/점만 허용한다.
	 */
	private static final java.util.regex.Pattern SAFE_PATH_TOKEN =
			java.util.regex.Pattern.compile("[A-Za-z0-9_.\\-]+");

	/**
	 * [행안부 SW 개발보안 가이드 준수 - 경로 조작 및 자원 삽입(CWE-22)]<br/>
	 * 파라미터로 받은 이름을 파일 경로 조립에 사용하기 전에 검증한다.
	 * 화이트리스트 문자만 허용하고, 상위 경로 이동(..)과 경로 구분자, NULL 문자를 차단한다.
	 *
	 * @param name  검증 대상 이름
	 * @param label 예외 메시지에 사용할 항목명
	 * @return 검증을 통과한 이름(원본 그대로)
	 * @throws IllegalArgumentException 경로 조작 가능성이 있는 값일 경우
	 */
	public static String requireSafePathToken(String name, String label) {
		if (name == null || name.isEmpty()
				|| name.contains("..")
				|| name.indexOf('/') >= 0
				|| name.indexOf('\\') >= 0
				|| name.indexOf('\0') >= 0
				|| !SAFE_PATH_TOKEN.matcher(name).matches()) {
			throw new IllegalArgumentException("Invalid " + label + " (path traversal blocked): " + name);
		}
		return name;
	}
	
	/**
	 * String 타입의 현재 시간
	 * @return String yyyyMMddHHmmss
	 */
	public static String getNow() {
		String result = null;
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.S");
		result = sdf.format(date);
		
		return result;
	}
	
	/**
	 * String 타입의 현재 시간
	 * @return String yyyy-MM-dd HH:mm:ss
	 */
	public static String getNow2() {
		String result = null;
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		result = sdf.format(date);
		
		return result;
	}
	
	public static String getNow(String format) {
		String result = null;
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		result = sdf.format(date);
		
		return result;
	}
	
	/**
	 * Bulker 실행 시작 시간
	 * @param timeZone
	 * @return String yyyy-MM-dd HH:mm:ss
	 */
	public static String getExecuteTime(String timeZone) {
		String result = null;
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		if(timeZone != null) {
			sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
		}
		
		result = sdf.format(date);
		
		return result;
	}
	
	/**
	 * 프로그램 수행 시간
	 * @param ms
	 * @return
	 */
	public static String getMsToTime(long ms) {
		String result = String.format("%02d:%02d:%02d", 
				TimeUnit.MILLISECONDS.toHours(ms)
				, TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ms))
				, TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms))
			);
		
		return result;
	}
	
	/**
	 * 첨부파일 다운로드명
	 * @param fileName
	 * @return
	 */
	public static String getDownFileName(String fileName, String fileId) {
		String result = null;
		String ext = "";
		
		if(fileName != null) {
			if(fileName.lastIndexOf(".") > -1) {
				ext = fileName.substring(fileName.lastIndexOf(".") + 1);
			}
			
			result = fileId + "." + ext;
		}
		
		return result;
	}
	
	/**
	 * 파일 확장자 조회
	 * @param fileName
	 * @return
	 */
//	public static String getFileExt(String fileName) {
//		String result = null;
//		
//		if(fileName != null && fileName.lastIndexOf(".") > 0) {
//			result = fileName.substring(fileName.lastIndexOf(".") + 1);
//		}
//		
//		return result;
//	}
	public static String getFileExt(String fileName) {
		if(fileName == null) {
			return null;
		}
		
		if(fileName.lastIndexOf(".") > 0) {
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		}
		
		return "unknown";
	}

	public static String getFileNameWithoutExt(String fileName) {
		String result = fileName;
		
		if(fileName != null && fileName.lastIndexOf(".") > 0) {
			result = fileName.substring(0, fileName.lastIndexOf("."));
		}
		
		return result;
	}
	
	public static String getFileName(String filePath) {
		String fileName = filePath;
		
		if(filePath != null) {
			if(filePath.lastIndexOf("\\") > 0) {
				fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
			} else if(filePath.lastIndexOf("/") > 0) {
				fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
			}
		}
		
		return fileName;
	}
	
	/**
	 * 파일을 읽어 바이너리로 리턴
	 * @param filepath
	 * @return
	 */
	public static byte[] getFileBinary(String filepath) {
		if (filepath == null) {
			return new byte[0];
		}
		try {
			// Files.readAllBytes: 자원 자동 해제 + 전체 바이트 읽기 보장(부분 read 버그 방지)
			return Files.readAllBytes(Paths.get(filepath));
		} catch (IOException e) {
			LOGGER.error("Failed to read file binary: {}", filepath, e);
			return new byte[0];
		}
	}
	
	public static String getFileText(String filePath, String lineSeparator) throws IOException {
		StringBuilder builder = new StringBuilder();
		String line = null;
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath), Charset.forName("utf-8"))) {
			while((line = br.readLine()) != null) {
				builder.append(line).append(lineSeparator);
			}
		}
		
		return builder.toString();
	}
	
	public static void getFileText(String filePath, String lineSeparator, StringBuilder builder) throws IOException {
		String line = null;
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath), Charset.forName("utf-8"))) {
			while((line = br.readLine()) != null) {
				builder.append(line).append(lineSeparator);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getMapFromJsonFile(File filePath) throws IOException {
		Map<String,Object> result =  (Map<String,Object>) new ObjectMapper().readValue(filePath, HashMap.class);
		return result;
	}
	
    	public static void moveDirectory(Path source, Path target) throws IOException{
    		if(!Files.isDirectory(source)) {
    			throw new IllegalArgumentException("Not directory: " + source);
    		} else if(!Files.exists(source)) {
    			throw new FileNotFoundException("Source is not exists: " + source);
    		}
		
    		if(!Files.exists(target.getParent())) {
    			throw new FileNotFoundException("The Parent of target is not exists: " + target.getParent());
    		}
		
    		Files.walkFileTree(source, new FileVisitor<Path>() {

    			@Override
    			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    				if(dir.equals(source)) {
    					if(!Files.exists(target)) {
    						Files.createDirectory(target);
    					}
    				} else {
    					Path destDir = target.resolve(source.relativize(dir));
    					if(!Files.exists(destDir)) {
    						Files.createDirectory(destDir);
    					}
    				}
    				return FileVisitResult.CONTINUE;
    			}

    			@Override
    			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    				Path destFile = target.resolve(source.relativize(file));
    				Files.move(file, destFile, StandardCopyOption.REPLACE_EXISTING);
				
    				return FileVisitResult.CONTINUE;
    			}

    			@Override
    			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    				return FileVisitResult.TERMINATE;
    			}

    			@Override
    			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    				if(isEmptyDirectory(dir)) {
    					Files.delete(dir);
    				}
    				return FileVisitResult.CONTINUE;
    			}
    		});
    	}
	
    /**
     * 지정한 디렉터리 아래의 모든 파일과 하위 디렉터리를 삭제한다.
     * 단, parentPath 자신은 삭제하지 않는다.
     *
     * @param parentPath 삭제 대상의 부모 디렉터리
     * @throws IOException 삭제 중 오류 발생 시
     */
    public static void deleteAllChildren(Path parentPath) throws IOException {
        if (parentPath == null) {
            throw new IllegalArgumentException("parentPath is null");
        }

        if (!Files.exists(parentPath)) {
            return;
        }

        if (!Files.isDirectory(parentPath)) {
            throw new IllegalArgumentException("Not directory: " + parentPath);
        }

        try (java.util.stream.Stream<Path> walk = Files.walk(parentPath)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .filter(path -> !path.equals(parentPath))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }

    	public static boolean isEmptyDirectory(Path dirPath) throws IOException{
    		boolean isEmptyDirectory = true;
		
    		if(Files.exists(dirPath)) {
    			if(!Files.isDirectory(dirPath)) {
    				throw new IllegalArgumentException("Not directory: " + dirPath);
    			}
			
    			try(DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
    				isEmptyDirectory = !stream.iterator().hasNext();
    			}
    		}
		
    		return isEmptyDirectory;
    	}
	
	/**
	 * Object > String 형변환
	 * @return
	 */
	public static String objToString(Object objStr) {
		String result = null;
		
		if(objStr == null) {
			result = null;
		} else if(objStr instanceof String) {
			result = (String) objStr;
		} else if (objStr instanceof Integer
				|| objStr instanceof Long
				|| objStr instanceof Double
				|| objStr instanceof Float
				) {
			result = String.valueOf(objStr);
		} else {
			result = objStr.toString();
		}
		
		return result;
	}
	
	public static long objToLong(Object objStr) {
		Long result = null;
		
		if(objStr == null) {
			result = null;
		} else if(objStr instanceof String) {
			result = Long.valueOf((String)objStr);
		} else if (objStr instanceof Integer
				|| objStr instanceof Long
				|| objStr instanceof Double
				|| objStr instanceof Float
				) {
			result = (Long) objStr;
			
		} else if(objStr instanceof BigInteger) {
			BigInteger bigInteger = new BigInteger(String.valueOf(objStr));
			result = bigInteger.longValueExact();
		} else {
			

			result = Long.valueOf((String)objStr);
		}
		
		return result;
	}
	
	/**
	 * HTML 테그 제거
	 * @return
	 */
	public static String getRemoveTag(String str) {
		String result = null;
		
		if(str != null) {
			result = str.replaceAll("<(/)?([a-zA-Z1-9]*)(\\s[a-zA-Z1-9]*=[^>]*)?(\\s)*(/)?>", "");
			result = result.replace("<!DOCTYPE html>", "");
			result = result.replace("\n", "");
			
			while (result.indexOf("  ") > -1) {
				result = result.replace("  ", " ");
			}
		}
		
		return result;
	}
	
	/**
	 * long 타입을 시간으로 변경
	 * @param ms
	 * @return
	 */
	public static String getLongToTime(long ms) {
		String result = null;
		long hours = (ms / 1000) / 60 / 60 % 24;
		long minutes = (ms / 1000) / 60 % 60;
		long seconds = (ms / 1000) % 60;
		
		if(hours + minutes + seconds == 0) {
			result = ms + "(ms)";
		
		} else {
			result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		}
		
		return result;
	}
	
	public static String toFormatString(Date date) {
		SimpleDateFormat oSdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		String sDate = oSdf.format( date );
		return sDate;
	}

	/**
	 * Html 테그를 제거한다.
	 * </p>
	 * @param text 태그를 포함한 텍스트  
	 * @return 테그를 제거한 텍스트
	*/
	
	public static String removeHtmlTag(String text) {
		if(text == null) return null;

		// HTML 엔터티(예:&nbsp; &lt 등)를 일반 문자로 변환
		String result = HtmlUtils.htmlUnescape(text);

		// 1. <script> 태그와 그 내용 제거
		result = result.replaceAll("(?is)<script.*?>.*?</script>", " ");

		// 2. <style> 태그와 그 내용 제거
		result = result.replaceAll("(?is)<style.*?>.*?</style>", " ");

		// 3. 모든 HTML태그 제거
		result = result.replaceAll("<[^>]+>", " ");

		// 4. CSS 클래스 선택자 형태의 문자열 제거
		result = result.replaceAll("(\\.[a-zA-Z0-9_-]+\\s*)+", " ");

		// 5. CSS 블록 ( {...} ) 형태 제거
		result = result.replaceAll("[a-zA-Z0-9.#_-]+\\s*\\{[^}]*\\}", " ");

		// 6. 연속된 공백을 하나의 공백으로 통합
		result = result.replaceAll("\\s+", " ");

		return result.trim();
	}

	/**
	 * 불필요한 공백(스페이스, 탭, 개행)문자를 제거한다.
	 * </p>
	 * @param text 텍스트
	 * @return 불필요한 공백(스페이스, 탭, 개행)문자를 제거한 텍스트
	 */
	public static String removeSpace(String text) {
		String result = null;
		if(text != null) {
			result = text.replaceAll("\\s", " ").replaceAll("\\s{2,}", " ");
		}
		
		return result;
	}
	
	public static boolean isImageExt(String fileName) {
	    String ext = Utils.getFileExt(fileName).toLowerCase();

	    return ext.equals("png")
	        || ext.equals("jpg")
	        || ext.equals("jpeg")
	        || ext.equals("gif")
	        || ext.equals("bmp")
	        || ext.equals("tif")
	        || ext.equals("tiff")
	        || ext.equals("webp");
	}
}
