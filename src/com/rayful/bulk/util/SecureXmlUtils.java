package com.rayful.bulk.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

/**
 * XML 파서 보안 설정 유틸리티.
 * <p/>
 * [행정안전부 SW 개발보안 가이드 준수 - XML 외부개체 주입(XXE, CWE-611)]<br/>
 * 외부 개체(DTD/외부 엔티티) 해석을 차단한 SAX 파서 팩토리를 생성한다.
 * DTD 선언 자체를 금지(disallow-doctype-decl=true)하는 것을 1차 방어로,
 * 외부 일반/파라미터 엔티티 및 외부 DTD 로딩 비활성화를 2차 방어로 적용한다.
 * 설정 실패 시 안전하지 않은 파서를 반환하지 않고 예외를 던진다(fail-closed).
 */
public final class SecureXmlUtils {

	private static final String FEATURE_DISALLOW_DOCTYPE =
			"http://apache.org/xml/features/disallow-doctype-decl";
	private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
			"http://xml.org/sax/features/external-general-entities";
	private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
			"http://xml.org/sax/features/external-parameter-entities";
	private static final String FEATURE_LOAD_EXTERNAL_DTD =
			"http://apache.org/xml/features/nonvalidating/load-external-dtd";

	private SecureXmlUtils() {
	}

	/**
	 * XXE 방어가 적용된 SAXParserFactory 를 생성해서 리턴한다.
	 * (namespaceAware 는 호출부 기존 동작 유지를 위해 기본값 그대로 둔다.)
	 *
	 * @return XXE 방어가 적용된 SAXParserFactory
	 */
	public static SAXParserFactory newSecureSAXParserFactory() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);
			factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
			factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
			factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException | SAXException e) {
			throw new IllegalStateException("Failed to harden SAXParserFactory against XXE", e);
		}
		factory.setXIncludeAware(false);
		factory.setValidating(false);
		return factory;
	}
}
