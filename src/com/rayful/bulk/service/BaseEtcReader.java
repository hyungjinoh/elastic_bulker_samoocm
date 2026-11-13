package com.rayful.bulk.service;

import java.util.Map;
import java.util.Properties;

import com.rayful.bulk.attach.IAttachFileHelper;
import com.rayful.bulk.dao.JdbcDAO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * EtcReder 클래스의 공통 함수 중 함수를 일부 구현한 추상클래스
 * <p/>
 * - 멤버변수 dfVO를 이용하여 DF정보를 확인할 수 있다. <br/>
 * - 멤버변수 jdbcDAO를 이용하여 기타 query를 수행할 수 있다. <br/>
 * <pre>
 * QueryItemVO queryItemVO = dfVO.query.getEtcQuery(queryName);
 * if(queryItemVO != null) {
 * 	XXXXXX = this.jdbcDAO.XXXXX(dfVO, queryItemVO.getSql(), parameterMap, queryItemVO.getConnectionId());
 * </pre>
 * 
 * 
 * - executeEtc 함수의 파라미터 CountLogVO etcLogVO 를 이용해서 로그를 기록한다. <br/>
 * <pre>
 * 	성공 : etcLogVO.etcLogVO.increaseSuccessCount();
 * 	워닝: etcLogVO.etcLogVO.increaseWarnCount("워닝 메시지");
 * 	실패 : etcLogVO.etcLogVO.increaseFailCount("실패 메시지");
 * </pre>
 * 
 * - 추가 쿼리 결과나 커스텀 로직의 결과를 색인에 반영하고자 한다면 executeEtc 함수의 파라미터 Map<String, Object> dataMap 에 직접 저장한다.
 * <pre>
 *  dataMap.put("색인필드명", "추가쿼리 데이터");
 * </pre>
 */
public abstract class BaseEtcReader implements IEtcReader {
	//private static final Logger logger = LoggerFactory.getLogger(BaseEtcReader.class);
	
	protected final DFLoadVO dfVO;
	protected final JdbcDAO jdbcDAO;
	protected final Properties prop;
	
	/**
	 * 2026.03.04 EtcReader 에서 첨부파일 추출 시 사용 
	 * 첨부파일 처리를 위한 Helper.
	 * BaseBulkerService에서 생성되어 Reader에 주입된다.
	 */
	protected IAttachFileHelper fileHelper;

	public void setFileHelper(IAttachFileHelper fileHelper) {
	    this.fileHelper = fileHelper;
	}
	
	public BaseEtcReader(DFLoadVO dfVO, Properties prop) throws Exception {
		this.dfVO = dfVO;
		this.prop = prop;
		this.jdbcDAO = JdbcDAO.getInstance(dfVO, prop);
	}
	
	@Override
	public abstract void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO);

}
