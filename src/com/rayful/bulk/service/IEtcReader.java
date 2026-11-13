package com.rayful.bulk.service;

import java.util.Map;

import com.rayful.bulk.vo.CountLogVO;

/**
 * 추가 데이터 조회 및 커스텀 로직 수행을 담당하는 커스텀 클래스(EtcReader)의 필요한 함수를 정의한 인터페이스
 */
public interface IEtcReader {
	/**
	 * 추가 데이터 조회 및 커스텀 처리 로직을 수행 후 결과를 색인 데이터(dataMap)에 직접 반영한다.
	 * <p/>
	 * 추가 쿼리 실행 예제:
	 * <pre>
	 * 	QueryItemVO queryItemVO = dfVO.query.getEtcQuery( "추가쿼리 ID 값" );
	 *	if(queryItemVO != null) {
	 *		throw NullPointerException("Can't find etc_query");
	 *	}
	 *
	 *	List<Map<String, Object>> selectList = null
	 *	selectList = this.jdbcDAO.selectQueryList(dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());
	 * </pre>
	 * 
	 * 로그 기록:
	 * <pre>
	 *	etcLogVO 객체에 성공/실패에 관련 로그를 기록하여 벌커 로그 출력에 포함되도록 한다.
	 *	   - etcLogVO.increaseSuccessCount():  성공 카운트 증가
	 *	   - etcLogVO.increaseWarnCount(String warnMessage): 워닝 카우트 증가 및 워닝 메시지 저장
	 *	   - etcLogVO.increaseFailCount(String failMessage): 실패 카운트 증가 및 실패 메시지 저장
	 * </pre>
	 * 
	 * @param keyMap 키 쿼리 결과 데이터
	 * @param dataMap 키 쿼리에 해당하는 색인 데이터
	 * @param indexMode 전체(all) or 증분(inc)
	 * @param etcLogVO 로깅 객체
	 */
	void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO);
}
