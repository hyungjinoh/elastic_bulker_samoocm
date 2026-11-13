package com.rayful.bulk.encryption;

/**
 * DF내  중요정보(예. DB 계정/패스워드 등)를 암복화하기 함수를 정의한 위한 인터페이스 
 * 
 */
public interface IBulkerCrypt {
	/**
	 * 입력받은 스트링(str)을 인코딩 하여 리턴한다.
	 * <p/>
	 * @param str 평문 스트링
	 * @return 인코딩 스트링
	 */
	public String encode(String str);
	
	/**
	 * 입력받은 스트링(str)을 디코딩 하여 리턴한다.
	 * <p/>
	 * @param str 인코딩 스트링
	 * @return 평문 스트링
	 */
	public String decode(String str);
}
