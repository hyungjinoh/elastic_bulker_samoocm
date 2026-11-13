package com.rayful.bulk.attach;

import java.io.File;
import java.io.IOException;

/**
 * 첨부 파일의 다운로드 및 필터 처리 관련 메서드를 정의한 인터페이스
 */
public interface IAttachFileHelper {
	/**
	 * 다운로드 및 필터처리 전 전처리 필요시 이 메서드 내에서 처리한다.
	 * 
	 * @throws IOException
	 */
	void init() throws IOException;
	
	/**
	 * 원격지 첨부파일을 로컬경로로 복사해 온다. 
	 * <p/>
	 * [구현 시 ### 주의사항 ### ] <br/>
	 *  1. BaseFileHelper.getRemoteFullPath(remotePath)를 호출하여 리모트 파일의 실제경로를 먼저 알아낸 후 물리파일을 접근해야한다.<br/>
	 *  2. BaseFileHelper.dowonloadPath + localFileName 의 조합한 로컬 경로에 복사한 파일을 저장 해야한다. <br/>
	 * 
	 * @param remotePath 물리파일 경로(DB 조회 물리파일 경로)
	 * @param localFileName 로컬에 저장할 파일명
	 * @return 다운로드 파일 객체
	 * @throws IOException
	 */
	File download(String remotePath, String localFileName) throws IOException;
	
	/**
	 * 종료 전 후처리 필요시 이 메서드 내에서 처리한다.
	 */
	void destroy();
	
	/**
	 * 첨부파일로 부터 텍스트를 추출하여 "첨부파일명.txt" 파일에 저장한다.
	 * 
	 * @param downloadFile 텍스트 추출할 첨부파일
	 * @param fileExt 파일 확장자, 물리파일의 확장자가 아니라 논리파일(화면 출력 파일명)의 확장자를 명시해야함.
	 * @return 텍스트 추출 파일 객체
	 * @throws Exception
	 */
	@Deprecated
	File filterText(File downloadFile, String fileExt) throws Exception;
	
	/**
	 * 첨부파일로 부터 텍스트를 추출하여 "첨부파일명.txt" 파일에 저장한다.
	 * 
	 * @param downloadFile 텍스트 추출할 첨부파일
	 * @param fileExt 파일 확장자, 물리파일의 확장자가 아니라 논리파일(화면 출력 파일명)의 확장자를 명시해야함.
	 * @param filteredFile 텍스트 추출한 결과를 저장하는 파일
	 * @throws Exception
	 */
	void filterText(File downloadFile, String fileExt, File filteredFile) throws Exception;
	
	/**
	 * 인코딩된 파일을 복호화한다.
	 * 
	 * @param sourceFile 인코딩된 파일
	 * @return 복호화된 파일
	 * @throws IOException
	 * @Since 2023.08.07
	 */
	File decodeDrm(File sourceFile)  throws IOException;

}
	
