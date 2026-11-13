/**
 * 
 */
package com.rayful.bulk.textfilter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @author choong10
 *
 */
public class NoEmbeddedExtractor implements EmbeddedDocumentExtractor {

	@Override
	public void parseEmbedded(InputStream arg0, ContentHandler arg1, Metadata arg2, boolean arg3)
			throws SAXException, IOException {
		 // 아무 작업도 하지 않음 → 임베디드 문서 파싱 무시
	}

	@Override
	public boolean shouldParseEmbedded(Metadata arg0) {
		// false를 리턴하면 Tika는 이 임베디드 문서를 파싱하지 않음
		return false;
	}

}
