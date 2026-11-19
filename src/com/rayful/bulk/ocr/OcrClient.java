package com.rayful.bulk.ocr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Pin-OCR(Bundle) OCR API 연동 클라이언트.
 * <p/>
 * 이미지/스캔 문서(png, jpg, tiff, pdf 등)를 Base64로 인코딩하여 OCR 서버에 POST하고,
 * 응답(JSON)에서 페이지별 인식 텍스트를 페이지 순서대로 이어붙여 돌려준다.
 * <p/>
 * 설정(config.properties):
 * <ul>
 *   <li>BULKER.OCR.URL            : OCR 서버 베이스 URL. 예) http://10.100.20.51:8180
 *                                   (엔드포인트 "/v1/ocr_json" 은 자동으로 붙는다. 미설정/빈값이면 OCR 비활성화)</li>
 *   <li>BULKER.OCR.ALLOWED_EXT    : OCR 처리 대상 확장자(콤마 구분). 예) png,jpg,jpeg,tif,tiff,pdf</li>
 *   <li>BULKER.OCR.INCLUDE_PAGE_IMAGE : 개별 페이지 이미지 회신 여부(yes/1/true). 색인엔 불필요하므로 기본 no</li>
 *   <li>BULKER.OCR.TIMEOUT.CONNECT    : 연결 타임아웃(초). 기본 10</li>
 *   <li>BULKER.OCR.TIMEOUT.READ       : 응답(소켓) 타임아웃(초). 기본 300</li>
 *   <li>BULKER.OCR.MAXCONN            : HTTP 커넥션 풀 최대 수. 기본 20</li>
 * </ul>
 *
 * 스레드 세이프(Apache PoolingHttpClientConnectionManager 기반). 멀티스레드 첨부 처리에서 공유 사용 가능.
 *
 * @since 2026.11.18
 */
public class OcrClient implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(OcrClient.class);

	/** OCR JSON API 엔드포인트 경로 */
	private static final String OCR_ENDPOINT_PATH = "/v1/ocr_json";
	private static final Gson GSON = new Gson();

	private final String endpointUrl;
	private final boolean includePageImage;
	private final CloseableHttpClient httpClient;

	public OcrClient(Properties prop) {
		String baseUrl = prop.getProperty("BULKER.OCR.URL");
		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			throw new IllegalArgumentException("\"BULKER.OCR.URL\" setting is not found.");
		}
		baseUrl = baseUrl.trim();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		// URL에 이미 엔드포인트 경로가 들어있으면 그대로, 아니면 붙인다.
		this.endpointUrl = baseUrl.endsWith(OCR_ENDPOINT_PATH) ? baseUrl : baseUrl + OCR_ENDPOINT_PATH;

		String includePageImageProp = prop.getProperty("BULKER.OCR.INCLUDE_PAGE_IMAGE");
		this.includePageImage = "yes".equalsIgnoreCase(includePageImageProp)
				|| "1".equals(includePageImageProp)
				|| "true".equalsIgnoreCase(includePageImageProp);

		int connectTimeoutSec = parseIntOrDefault(prop.getProperty("BULKER.OCR.TIMEOUT.CONNECT"), 10);
		int readTimeoutSec = parseIntOrDefault(prop.getProperty("BULKER.OCR.TIMEOUT.READ"), 300);
		int maxConn = parseIntOrDefault(prop.getProperty("BULKER.OCR.MAXCONN"), 20);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(connectTimeoutSec * 1000)
				.setConnectionRequestTimeout(connectTimeoutSec * 1000)
				.setSocketTimeout(readTimeoutSec * 1000)
				.build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(maxConn);
		cm.setDefaultMaxPerRoute(maxConn);

		this.httpClient = HttpClients.custom()
				.setConnectionManager(cm)
				.setDefaultRequestConfig(requestConfig)
				.build();

		logger.info("- OCR endpoint: {} (includePageImage={}, connectTimeout={}s, readTimeout={}s, maxConn={})",
				this.endpointUrl, this.includePageImage, connectTimeoutSec, readTimeoutSec, maxConn);
	}

	public String getEndpointUrl() {
		return this.endpointUrl;
	}

	/**
	 * 이미지/PDF 파일을 OCR API로 전송하고 인식된 전체 텍스트를 페이지 순서대로 이어붙여 리턴한다.
	 *
	 * @param imageFile OCR 대상 파일(복호화가 끝난 실제 이미지/문서 파일)
	 * @param docId     OCR 요청 식별자(응답에 echo 됨. 로그 추적용). null이면 파일명을 사용
	 * @return 인식된 전체 텍스트(페이지 사이는 개행으로 구분). 인식 결과가 없으면 빈 문자열
	 * @throws IOException 파일 읽기 실패, HTTP 오류, 응답 파싱 실패, OCR status != 200 인 경우
	 */
	public String extractText(File imageFile, String docId) throws IOException {
		if (imageFile == null || !imageFile.exists()) {
			throw new IOException("OCR source file not found: " + imageFile);
		}

		// 1. 파일 읽기 및 Base64 인코딩
		byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
		String base64File = Base64.getEncoder().encodeToString(fileBytes);

		// 2. JSON 요청 생성 (Gson으로 직렬화하여 특수문자/따옴표 이스케이프 안전 처리)
		JsonObject reqBody = new JsonObject();
		reqBody.addProperty("DocID", (docId == null || docId.isEmpty()) ? imageFile.getName() : docId);
		reqBody.addProperty("ReqFile", base64File);
		reqBody.addProperty("IncludePageImage", this.includePageImage);

		// 3. POST 요청
		HttpPost post = new HttpPost(this.endpointUrl);
		post.setHeader("Accept", "application/json");
		post.setEntity(new StringEntity(GSON.toJson(reqBody), ContentType.APPLICATION_JSON));

		try (CloseableHttpResponse response = this.httpClient.execute(post)) {
			int httpStatus = response.getStatusLine().getStatusCode();
			String responseBody = (response.getEntity() == null) ? ""
					: EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

			if (httpStatus != 200) {
				throw new IOException("OCR API HTTP error: status=" + httpStatus
						+ ", docId=" + docId + ", body=" + abbreviate(responseBody, 500));
			}
			return parseText(responseBody, docId);
		}
	}

	/**
	 * OCR 응답(JSON)에서 페이지별 text를 페이지 번호 순으로 이어붙인다.
	 */
	private String parseText(String responseBody, String docId) throws IOException {
		JsonObject root;
		try {
			root = GSON.fromJson(responseBody, JsonObject.class);
		} catch (Exception e) {
			throw new IOException("OCR API response parse error: docId=" + docId
					+ ", body=" + abbreviate(responseBody, 500), e);
		}
		if (root == null) {
			throw new IOException("OCR API empty response: docId=" + docId);
		}

		int status = (root.has("status") && !root.get("status").isJsonNull()) ? root.get("status").getAsInt() : -1;
		String message = (root.has("message") && !root.get("message").isJsonNull()) ? root.get("message").getAsString() : "";
		if (status != 200) {
			throw new IOException("OCR API result error: status=" + status + ", message=" + message + ", docId=" + docId);
		}

		if (!root.has("result") || root.get("result").isJsonNull()) {
			return "";
		}
		JsonObject result = root.getAsJsonObject("result");

		// 페이지 키("1","2"...)를 숫자 순으로 정렬
		List<String> pageKeys = new ArrayList<>(result.keySet());
		pageKeys.sort(Comparator.comparingInt(OcrClient::toPageNo));

		StringBuilder sb = new StringBuilder();
		for (String pageKey : pageKeys) {
			JsonElement pageEl = result.get(pageKey);
			if (pageEl == null || !pageEl.isJsonObject()) {
				continue;
			}
			JsonObject page = pageEl.getAsJsonObject();
			if (page.has("text") && !page.get("text").isJsonNull()) {
				String pageText = page.get("text").getAsString();
				if (pageText != null && !pageText.isEmpty()) {
					if (sb.length() > 0) {
						sb.append("\n");
					}
					sb.append(pageText);
				}
			}
		}
		return sb.toString();
	}

	private static int toPageNo(String key) {
		try {
			return Integer.parseInt(key.trim());
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE; // 숫자가 아닌 키는 뒤로
		}
	}

	private static int parseIntOrDefault(String value, int def) {
		if (value == null || value.trim().isEmpty()) {
			return def;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String abbreviate(String s, int max) {
		if (s == null) {
			return "";
		}
		return (s.length() <= max) ? s : s.substring(0, max) + "...(truncated)";
	}

	@Override
	public void close() {
		try {
			this.httpClient.close();
		} catch (IOException e) {
			logger.warn("Failed to close OCR http client", e);
		}
	}
}