package com.rayful.bulk.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.rayful.bulk.vo.BulkerLogVO;

/**
 * 색인 데이터 수집 관련 데이터 조회 및 첨부파일 처리 클래스(BulkerService)의 필요한 함수를 정의한 인터페이스
 */
public interface IBulkerService {
	/**
	 * BulkerService의 전처리를 처리를 수행한다.
	 * @throws IOException 
	 */
	void init() throws IOException ;
	
	/**
	 *  BulkerService의 후처리를 처리를 수행한다.
	 */
	void destroy();
	
	/**
	 * 색인 대상 전체 문서를 조회한다.
	 * @return 색인 대상 전체 문서 수
	 */
	int getDocCount ();
	
	/**
	 * 색인대상(키) 쿼리를 수행
	 * 
	 * @param indexMode 전체(all) or 증분(inc)
	 * @return 조회 목록 결과
	 */
	void getKeyData(String indexMode);

	
	/**
	 * 색인대상(키) 쿼리를 수행 (커서 사용하여 부분  fetch)
	 * ### lguplus 프로젝트에서 추가됨
	 * 
	 * @return 조회 목록 결과 (fetchSize 만큼), 결과가 없으면 null
	 * @throws Exception
	 */
	List<Map<String, Object>> nextKeyData() throws Exception;
	
	/**
	 * keyDataList에 해당하는 색인데이터를(데이터 쿼리, 파일 쿼리, etcReader 모두 수행) 수집
	 * 
	 * @param keyDataList 키 정보 서브 목록(전체 중 일부)
	 * @param indexMode 전체(all) or 증분(inc)
	 * @param bulkerLogVO 벌커 로깅 객체
	 * @param keyNo 전체 키정보 목록 내 위치
	 * @return 키목록에 해당하는 색인데이터 목록
	 */
	List<Map<String, Object>> getData(List<Map<String, Object>> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo);
	
	List<Map<String, Object>> getDataAsync(List<Map<String, Object>> keyDataList, String indexMode,
			BulkerLogVO bulkerLogVO, int keyNo);
	
	/**
	 * 조회된 색인데이터(dataList)를 덤프파일의 형식으로 재구성 한다.
	 * 
	 * @param dataList 색인 데이터 목록
	 * @param indexMode 전체(all) or 증분(inc)
	 * @return 덤프파일 형식의 색인 데이터
	 */
	List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode);
	
	/**
	 * 조회된 색인 삭제 데이터(dataList)를 덤프파일의 형식으로 재구성 한다.
	 * 
	 * @param dataList 색인 삭제 데이터 목록
	 * @return 덤프파일 형식의 색인 삭제 데이터
	 */
	List<Object> getDeleteResultData(List<Map<String, Object>> dataList);
	
	/**
	 * 덤프파일 형식으로 컨버팅한 데이터(resultDataList)를 덤프파일(json)로 저장한다.
	 * 
	 * @param resultDataList 덤프파일 형식으로 컨버팅한 색인 데이터
	 * @param indexType 색인(index) or 삭제(delete)
	 * @param bulkerLogVO 벌커 로깅 객체
	 * @return json 형식의 벌크 File 객게
	 * @throws Exception
	 */
	@Deprecated
	File makeJson(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws Exception;
	
	/**
	 * 덤프파일 형식으로 컨버팅한 데이터(resultDataList)를 150M 크기의 덤프파일(json)들에 저장한다.
	 * <p>
	 * 덤프파일의 크기가 150M를 초과하면 추가 파일을 생성해서 저정한다. 
	 * 
	 * @param resultDataList 덤프파일 형식으로 컨버팅한 색인 데이터
	 * @param indexType 색인(index) or 삭제(delete)
	 * @param bulkerLogVO 벌커 로깅 객체
	 * @return json 형식의 벌크 File 목록 객게
	 * @throws Exception
	 * @since 2025.10.03
	 * @author choong10
	 */
	List<File> makeJsonLimtSize(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws Exception;
	
	/**
	 * 입력 파라미티터 keyDataList에 해당하는 큐 데이터의 상태를 처리완료로 업데이트한다.
	 * <p/>
	 * indexMode가 증분(inc)일 경우만 큐 데이터의 상태 업데이트를 수행한다.
	 * 
	 * @param keyDataList 키 정보 서브 목록(전체 중 일부)
	 * @param indexMode 전체(all) or 증분(inc)
	 * @return true: 모든 업데이트 성공, false: 실패
	 */
	boolean updateQueueData(List<Map<String, Object>> keyDataList, String indexMode);

}
