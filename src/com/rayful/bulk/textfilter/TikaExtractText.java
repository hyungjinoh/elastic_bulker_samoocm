package com.rayful.bulk.textfilter;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ThreadSafe
public class TikaExtractText {
	private static final Logger logger = LoggerFactory.getLogger(TikaExtractText.class);
	public static final int CHAR_LIMIT_ISZE = 500000;
   // private static final String INPUT_DIR = "D:\\hwp";
    //private static final String OUTPUT_DIR = "D:\\hwp\\tika";
    //private static final Tika tika;
	
	@GuardedBy("ThreadLocal")
	private static final ThreadLocal<AutoDetectParser> parserThreadLocal =
		    ThreadLocal.withInitial(AutoDetectParser::new);

	public static void main (String[] args) throws Exception {
		String filePath = "C:\\Users\\choong10\\Documents\\GS 인증\\Ray.Search\\포스코인터내셔널 ECM 이관관련 문의사항.xlsx";
		
		File inputFile= new File(filePath);
		File outputFile = new File(filePath + ".txt");
		extractText(inputFile, outputFile, "xlsx");
	}
    /*
    public static void main(String[] args) {
        File inputFolder = new File(INPUT_DIR);
        File[] files = inputFolder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("변환할 파일이 없습니다: " + INPUT_DIR);
            return;
        }

        new File(OUTPUT_DIR).mkdirs();
        List<String> results = new ArrayList<>();

        for (File file : files) {
            if (!file.isFile()) continue;
            String outputFilePath = OUTPUT_DIR + "\\" + file.getName() + "_tika.txt";
            long startTime = System.nanoTime();
            
            boolean success = file.getName().endsWith(".hwpx") ? extractHwpXText(file, new File(outputFilePath))
                            : extractDocText(file, new File(outputFilePath));
            
            long endTime = System.nanoTime();
            double elapsedTime = (endTime - startTime) / 1_000_000_000.0;
            results.add(String.format("%s, %s, %s, %.3f초", file.getName(), outputFilePath, success ? "성공" : "실패", elapsedTime));
        }

        System.out.println("파일명, 출력경로, 상태, 소요 시간");
        results.forEach(System.out::println);
    }
*/
    
	//2025-09-06 정충열 POI라이브러리의 IOUtils 클래스의 바이트 배열의 크기를 100M 메모리 제한을 변경 : 600 M => 300M
	static {
		// [주의] 반드시 Tika 객체 생성 전에 설정
		// Apache POI의 IOUtils 클래스에서 사용하는 기본 최대 바이트 배열 크기 100M
		// 에러나는 문서가 많아 이 값의 크기를 조정
		org.apache.poi.util.IOUtils.setByteArrayMaxOverride(1024 * 1024 * 300);
	}

    
    public static void extractText(File inputFile, File outputFile, String fileExt) throws IOException, TikaException, SAXException{
    	if ("hwpx".equalsIgnoreCase(fileExt)) {
    		extractHwpXText(inputFile, outputFile);
    	} else if ("xlsx".equalsIgnoreCase(fileExt)) {
    		// 실제 파일 헤더를 확인해서 포맷을 체크
    		String detectForamt = ExcelSAXParser.detectExcelFormat(inputFile);
    		if("xlsx".equalsIgnoreCase(detectForamt)) {
    			// XLSX용 저용 파서 사용
    			ExcelSAXParser.extractXlsxText(inputFile, outputFile);
    		} else {
    			extractDocText(inputFile, outputFile);
    		}
    	} else {
    		extractDocText(inputFile, outputFile);
    	}
    }
    

    /**
     * 일반 문서에서 Tika를 이용해 텍스트를 추출하는 메소드
     * @throws IOException 
     * @throws TikaException 
     * @throws SAXException 
     */
    private static boolean extractDocText(File inputFile, File outputFile) throws IOException, TikaException, SAXException {
        try (InputStream stream = Files.newInputStream(inputFile.toPath());
        	BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            
        	//---------------------- 기존  로직 ----------------------
        	//writer.write(cleanUpText(tika.parseToString(stream)));
        	//----------------------------------------------------
        	
        	// 기본 파일 내용 추출 문서 수 제한은 : 100,000 자 => 5,000,000 => 500,000
			ContentHandler handler = new BodyContentHandler(CHAR_LIMIT_ISZE);
			Metadata metadata = new Metadata();
			ParseContext cotext = new ParseContext();
			
			// 아래 설정이 수행되면 암축파일 포맷의 파일도 색인이 안됨 : zip, tar...
			//cotext.set(EmbeddedDocumentExtractor.class, new NoEmbeddedExtractor());
			AutoDetectParser parser = parserThreadLocal.get(); // 변경된 부분
			try {
				parser.parse(stream, handler, metadata, cotext);
			} catch(WriteLimitReachedException wlre) {
				logger.warn(wlre.getMessage());
			} 
			
        	writer.write(cleanUpText(handler.toString()));
        	
            return true;
        } 
    }

    /**
     * HWPX 파일에서 텍스트를 추출하는 메소드
     * @throws IOException 
     */
    private static void extractHwpXText(File hwpXFile, File outputFile) throws IOException {
        try (ZipFile hwpZipFile = new ZipFile(hwpXFile);
        	BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            
            List<ZipEntry> xmlEntries = hwpZipFile.stream()
                    .filter(entry -> entry.getName().startsWith("Contents/") && entry.getName().endsWith(".xml"))
                    .sorted(Comparator.comparing(ZipEntry::getName))  // 정렬 추가
                    .collect(Collectors.toList());

            for (ZipEntry entry : xmlEntries) {
            	String extracted = extractTextFromXmlZipEntry(hwpZipFile, entry);
                if (extracted != null && !extracted.isBlank()) {
                    writer.write(cleanUpText(extracted));
                    writer.newLine(); // 줄 바꿈
                }
            }
        } 
    }

    /**
     * 텍스트를 정리하는 메소드 (공백 및 개행 문자 정리)
     */
	private static String cleanUpText(String text) {
		return text.replaceAll("(?m)^[ \t]+|[ \t]+$", "")
		       .replaceAll("\\s{2,}", " ")
		       .replaceAll("(\r?\n){2,}", "\n")
		       .trim();
	}

    /**
     * ZipEntry에서 XML을 파싱하여 <hp:t> 태그 내용만 추출하는 메소드
     */
    private static String extractTextFromXmlZipEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            String xmlContent = new String(bytes, StandardCharsets.UTF_8);

            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());
            Elements textElements = doc.select("hp|t");

            StringBuilder textBuilder = new StringBuilder();
            for (Element element : textElements) {
                textBuilder.append(element.text()).append("\n");
            }

            return textBuilder.toString().trim();
        }
    }
}