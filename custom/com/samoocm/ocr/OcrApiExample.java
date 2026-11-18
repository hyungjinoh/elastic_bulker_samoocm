package com.samoocm.ocr;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

public class OcrApiExample {

    public static void main(String[] args) throws Exception {

        // 1. 파일 읽기 및 Base64 인코딩
        byte[] fileBytes = Files.readAllBytes(
//                Path.of("sample.pdf")
                Path.of("C:\\workspace\\source_code\\elastic_bulker\\db_rawdata\\test_doc\\Pin-OCR Bundle API 설명서 v1.03.pdf")

        );

        String base64File = Base64.getEncoder()
                .encodeToString(fileBytes);

        // 2. JSON 요청 생성
        String jsonBody = """
                {
                    "DocID": "document_001",
                    "ReqFile": "%s",
                    "IncludePageImage": false
                }
                """.formatted(base64File);

        // 3. HTTP Client 생성
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 4. POST 요청 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.0.1/v1/ocr_json"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 5. 요청 및 응답 확인
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("HTTP Status: " + response.statusCode());
        System.out.println("Response Body:");
        System.out.println(response.body());
    }
}