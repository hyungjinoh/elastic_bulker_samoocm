package com.rayful.bulk.textfilter;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ExcelSAXParser {
	private static final Logger logger = LoggerFactory.getLogger(ExcelSAXParser.class);
	
    public static void main(String[] args) throws Exception {
        String sourcePath = "C:\\Users\\choong10\\Documents\\GS 인증\\Ray.Search\\포스코인터내셔널 ECM 이관관련 문의사항.xlsx";
        String destPath = sourcePath + ".txt";
        extractXlsxText(new File(sourcePath), new File(destPath));
    }
    
    public static String detectExcelFormat(File file) throws IOException {
    	try (FileInputStream fis = new FileInputStream(file)) {
    	    InputStream magic = FileMagic.prepareToCheckMagic(fis);
    	    FileMagic fileMagic = FileMagic.valueOf(magic);

    	    if (fileMagic == FileMagic.OLE2) {
    	        return "xls";
    	    } else if (fileMagic == FileMagic.OOXML) {
    	        return "xlsx";
    	    } else {
    	        return "unknown";
    	    }
    	}
    }
    
   public static void extractXlsxText(File inputFile, File outputFile) throws IOException {
	   try (OPCPackage pkg = OPCPackage.open(inputFile, PackageAccess.READ);
			BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
           XSSFReader reader = new XSSFReader(pkg);
           SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();
           XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
           
           // [행안부 SW 개발보안 가이드 - XXE(CWE-611)] 외부 문서(XLSX) 파싱 시 외부개체 차단
           SAXParserFactory factory = com.rayful.bulk.util.SecureXmlUtils.newSecureSAXParserFactory();
           XMLReader parser = null;
           
           SheetHandler handler = null;
           String sheetTitle = null;
           int writeCharSize = 0;
           while (sheets.hasNext()) {
               try (InputStream sheet = sheets.next()) {
                   InputSource sheetSource = new InputSource(sheet);
                   
                   sheetTitle = "\n======== " + sheets.getSheetName() + " =======\n";
                   writer.append(sheetTitle);
                   writeCharSize += sheetTitle.length();
                   
                   handler = new SheetHandler(sst, writer, TikaExtractText.CHAR_LIMIT_ISZE - writeCharSize);
                   parser = factory.newSAXParser().getXMLReader();
                   parser.setContentHandler(handler);
                   parser.parse(sheetSource);
                   
                   writeCharSize += handler.getWriteByteSize();
                   if(writeCharSize >= TikaExtractText.CHAR_LIMIT_ISZE) {
                	   logger.warn("[XSX] extract text reach max limit.({}) - {}", writeCharSize, inputFile.getName());
                	   break;
                   }
               } catch (SAXException e) {
            	   throw new IOException("(XLSX) SAX Parse", e);
				} catch (ParserConfigurationException e) {
					new IOException("(XLSX) Parse Config", e);
				}
           }
           
           writer.flush();
       } catch (InvalidFormatException e) {
			throw new IOException("(XLSX) Invalid Format", e);
       } catch (OpenXML4JException e) {
    	   throw new IOException("(XLSX) OpenXML4", e);
       }
   }
    /**
     * SheetHandler: SAX 이벤트 핸들러 정의
     */
    private static class SheetHandler extends DefaultHandler {
        private final SharedStringsTable sst;
        private final BufferedWriter writer;
        private final int charLimitSize;
        
        private StringBuilder cellValue;
        private boolean nextIsString;
        private int prevRowNo = -1;
        private int writeByteSize = 0;

        public SheetHandler(SharedStringsTable sst, BufferedWriter writer, int charLimitSize) {
            this.sst = sst;
            this.writer = writer;
            this.charLimitSize = charLimitSize;
            
            this.cellValue = new StringBuilder();
        }
        
        public int getWriteByteSize() {
        	return this.writeByteSize;
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) {
        	if(this.writeByteSize < charLimitSize) {
	        	// 셀 시작
	        	if ("c".equals(name)) { 
	        		String cellRef = attributes.getValue("r");
	        		if (cellRef != null) {
		                int curRowNo = Integer.parseInt(cellRef.replaceAll("[^0-9]", ""));
		                
		        		if (this.prevRowNo != curRowNo) {
		        			if(this.prevRowNo > -1) {
		        				try {
									writer.newLine();
									writeByteSize += 1;
								} catch (IOException e) {
									throw new RuntimeException("Fail to parse xlsx - write new line.", e);
								}
		        			}
		        			
		        			this.prevRowNo = curRowNo;
		        		}
	        		}
	        		
	                String cellType = attributes.getValue("t");
	                this.nextIsString = "s".equals(cellType); // shared string인지 확인
	            }
	            this.cellValue.setLength(0);
        	}
        }

        @Override
        public void endElement(String uri, String localName, String name) {
        	if(this.writeByteSize < charLimitSize) {
	        	// 쉘 종료
	            if (this.nextIsString && "v".equals(name)) {
	                int idx = Integer.parseInt(this.cellValue.toString());
	                this.cellValue.setLength(0);  // 기존 내용 초기화
	                
	                this.cellValue.append(sst.getItemAt(idx).getString());
	                //ystem.out.print(sst.getItemAt(idx).getString());
	                this.nextIsString = false;
	            }
	
	            if ("v".equals(name)) {
	            	try {
						writer.append(this.cellValue.toString());
						writer.append("\t");
						writeByteSize += this.cellValue.length() + 1;
					} catch (IOException e) {
						throw new RuntimeException("Fail to parse xlsx - write cell value.", e);
					}
	            }
        	}
        }

        @Override
        public void characters(char[] ch, int start, int length) {
        	if(this.writeByteSize < charLimitSize) {
        		this.cellValue.append(new String(ch, start, length));
        	}
        }
    }
}