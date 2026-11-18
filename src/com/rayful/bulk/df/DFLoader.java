package com.rayful.bulk.df;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.ibatis.io.Resources;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.rayful.bulk.vo.DFLoadVO;

/**
 * 색인정보 xml 파일(DF)을 SAX 파서를 이용해서 파싱해서 정보를 DFLoadVO 클래스에 저장하는 클래스
 */
public class DFLoader {
	private static final Logger logger = LoggerFactory.getLogger(DFLoader.class);
	
	private final Properties prop;
	
	public DFLoader() {
		prop = new Properties();
		
		try {
			Reader reader = Resources.getResourceAsReader("config.properties");
			Reader versionReader = Resources.getResourceAsReader("version.properties");
			
			prop.load(reader);
			prop.load(versionReader);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * DF 파일 로드
	 * @param fileName
	 * @return
	 */
	public DFLoadVO load(String fileName, String indexMode, String indexType) throws Exception{
		// [행안부 SW 개발보안 가이드 - 경로 조작(CWE-22)] df 이름 검증
		com.rayful.bulk.util.Utils.requireSafePathToken(fileName, "df name");
		DFLoadVO vo = null;
		// [행안부 SW 개발보안 가이드 - XXE(CWE-611)] 외부개체 차단 SAX 파서 사용
		SAXParserFactory spf = com.rayful.bulk.util.SecureXmlUtils.newSecureSAXParserFactory();
		String filePath = prop.getProperty("BULKER.PATH.DF");
		String dfFileName = null;
		
		if("delete".equalsIgnoreCase(indexType)) {
			dfFileName = fileName + "_delete.xml";
		} else {
			dfFileName = fileName + ".xml";
		}
		
		File dfFile = new File(filePath, dfFileName);
		
		try {
			SAXParser sp = spf.newSAXParser();
			DFHandler handler = new DFHandler(fileName);
			sp.parse(dfFile, handler);
			vo = handler.getResult();
			
			// DF Validate
			if(vo.column.getKeyColumnName() == null) {
				throw new IllegalArgumentException("DF must be specify cloumn(ROLE=key)");
			}
			if(vo.column.getIndexActionColumnName() == null) {
				throw new IllegalArgumentException("DF must be specify cloumn(ROLE=index_action)");
			}
			
			if(vo.getIndexName() == null) {
				throw new IllegalArgumentException("DF must be specify attribute \"index_name\" of target");
			}
			
			if(vo.isIncludeIndexname()) {
				if(vo.column.getIndexPatternColumnName() == null) {
					throw new IllegalArgumentException("DF must be specify cloumn(ROLE=index_pattern). if use indexName pattern - #index_pattern#");
				}
			}

			if(vo.field.isIndexFileData()) {
				if(vo.column.getFilePathColumnName() == null || vo.column.getFilePathColumnName().trim().length() == 0) {
					throw new IllegalArgumentException("DF must be specify cloumn(ROLE=file_path)");
				}
			}
			
			logger.info("- Product Name : " + prop.getProperty("BULKER.PRODUCT"));
			logger.info("- S/W Name : " + prop.getProperty("BULKER.NAME"));
			logger.info("- S/W Version : " + prop.getProperty("BULKER.VERSION"));
			logger.info("- S/W Build : " + prop.getProperty("BULKER.BUILDTIME"));
			logger.info("- " + indexType + " name : " + fileName);
			logger.info("- " + indexType + " mode : " + indexMode);
			logger.info("- DF Load complete : " + dfFile.getAbsolutePath());
		
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.error("- DF Load fail");
			logger.error(e.getMessage(), e);
			throw e;
		}
		
		//logger.debug("DFLoadVO={}", vo);
		return vo;
	}
	
	/**
	 * DF 파일의 last_doc_indexdate 업데이트
	 * @param fileName
	 * @param indexDate
	 * @throws Exception
	 */
	public void updateIndexDate(String fileName, String indexDate, String indexType) throws Exception {
		// [행안부 SW 개발보안 가이드 - 경로 조작(CWE-22)] df 이름 검증
		com.rayful.bulk.util.Utils.requireSafePathToken(fileName, "df name");
		String filePath = prop.getProperty("BULKER.PATH.DF");
		String dfFileName = null;
		
		if("index".equalsIgnoreCase(indexType)) {
			dfFileName = fileName + ".xml";
		
		} else {
			dfFileName = fileName + "_delete.xml";
		}
		
		File dfFile = new File(filePath, dfFileName);
		
		try {
			Element element = searchElement(dfFile, "data_source");
			element.setAttribute("last_doc_indexdate", indexDate);
			XMLOutputter xmlOutput = new XMLOutputter();
			Format format = xmlOutput.getFormat();
			format.setEncoding("UTF-8");
			xmlOutput.output(element.getDocument(), new FileWriterWithEncoding(dfFile.getPath(), "UTF-8"));
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * xml의 element 속성 조회
	 * @param file
	 * @param paramName
	 * @return
	 */
	public Element searchElement(File file, String paramName) {
		Element resultElement = null;
		
		try {
			// [행안부 SW 개발보안 가이드 - XXE(CWE-611)] JDOM2 파서 외부개체 차단
			SAXBuilder builder = new SAXBuilder();
			builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
			builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			builder.setExpandEntities(false);
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			List<Element> elementList = root.getChildren();
			
			for (Element element : elementList) {
                List<Element> subElementList = element.getChildren();
                
                for (Element subElement : subElementList) {
                    if (paramName.equals(subElement.getName())) {
                        return subElement;
                    }
                }
            }
		
		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
		}
		
		return resultElement;
	}
}
