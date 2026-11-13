package com.rayful.bulk.service;

import java.io.File;

import com.rayful.bulk.vo.BulkerLogVO;

/**
 * 색인 요청을 담당하는 클래스(IndexService)의 필요한 함수를 정의한 인터페이스
 */
public interface IIndexService {
	
	void requestSingleBulkIndex(File bulkFile, String dfName, String indexName, BulkerLogVO bulkerLogVO) throws Exception;
	/**
	 * dfName과 관련 벌크파일 목록들을 알아내어 각각 색인요청을 수행한다.
	 * <p/>
	 * 특정 벌크파일의 벌크 색인 요청 후 응답 결과를 벌크파일과 동일한 이름의 log 파일로 저장한다. <br/>
	 * 응답결과가 성공이면 설정에 따라 관련 벌크파일과 로그파일을 삭제하거나 "_success" 폴더로 이동한다. <br/>
	 * 응답결과가 실패이면 설정에 따라 관련 벌크파일과 로그파일을 "_fail"폴더로 이동한다.
	 * 
	 * @param dfName DF명
	 * @param indexName 인덱스명
	 * @param bulkerLogVO 벌커 로깅 객체
	 * @throws Exception
	 */
	void requestBulkIndex(String dfName, String indexName, BulkerLogVO bulkerLogVO) throws Exception;
	
	boolean requestDeleteNotIndexed(String indexName)  throws Exception;
	
	public void waitForComplete() throws InterruptedException;
}
