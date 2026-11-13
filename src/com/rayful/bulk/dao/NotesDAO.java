package com.rayful.bulk.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.encryption.IBulkerCrypt;
import com.rayful.bulk.util.CryptUtils;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.ConnectInfoVO;

import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.domino.Database;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;

/**
 * 노츠 접속 및 쿼리 수행을 담당하는 클래스
 */
public class NotesDAO {
	private static final Logger logger = LoggerFactory.getLogger(NotesDAO.class);
	private static NotesDAO INSTANCE = null;
	
	public static synchronized NotesDAO getInstance(DFLoadVO dfVO, Properties prop) throws Exception {
		if(INSTANCE == null) {
			INSTANCE = new NotesDAO(dfVO, prop);
		}
		
		return INSTANCE;
	}
	
	private final Map<String, Session> cachedSession;
	private Session lastAccessSession;
	private Database currentDb = null;
	private String currentDbName = null;
	private final IBulkerCrypt bulkerCrypt;
	
	private NotesDAO(DFLoadVO dfVO, Properties prop) throws Exception {
		this.cachedSession = new HashMap<String, Session>();
		this.bulkerCrypt = CryptUtils.getInstance(dfVO, prop);
	}
	
	public Session getCurrentSession() {
		return this.lastAccessSession;
	}
	
	
	/**
	 * queryType에 해당하는 쿼리를 수행하고 조회 목록 결과를 리턴
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param params 쿼리 파리미터
	 * @param connectId 커낵션 아디디
	 * @return 조회 목록
	 * @throws NotesException 
	 */
	public DocumentCollection selectQueryList(DFLoadVO dfVO, String sql, Map<String, Object> params, String connectId, String nsfName) throws NotesException {
		DocumentCollection docCollection = null;
		
		if(sql != null) {
			
			// 노츠 세션  획득
			Session session = this.cachedSession.get(connectId);
			if(session == null) {
				BasicDataSource dataSource = (BasicDataSource) this.getDataSource(dfVO, connectId);
				session = NotesFactory.createSession(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword() );
				this.cachedSession.put(connectId, session);
			}
			
			this.lastAccessSession = session;
			
			Database db = null;
			logger.debug("nsfName={}", nsfName);
			
			if(this.currentDb != null) {
				try {this.currentDb.recycle(); } catch(NotesException ne) { /* recycle 실패 무시(자원정리) */ }
			}
			
			// 노츠 디비(nsf파일) 접속
			db = session.getDatabase("", nsfName);
			if(db == null || db.isOpen() == false) {
				throw new NotesException(9999, "Can't open database - nsf: " + nsfName);
			} else {
				this.currentDbName = nsfName;
				this.currentDb = db;
			}
		
			// 쿼리 수행
			docCollection = db.search(sql, null, 0);
		}

		return docCollection;
	}
	
	public String getCurrentDbName() {
		return this.currentDbName;
	}
	
	/**
	 * 노츠 세션 리소스를 모두 해제한다.
	 */
	public void destory() {
		if(this.currentDb != null) {
			try {this.currentDb.recycle(); } catch(NotesException ne) { /* recycle 실패 무시(자원정리) */ }
		}
		
		for(String key: this.cachedSession.keySet()) {
			try { this.cachedSession.get(key).recycle(); } catch (NotesException e) { /* recycle 실패 무시(자원정리) */ }
		}
	}
	
		
	/**
	 * DF에 정의된 커넥션 정보를 DB Datasource 객체로 로드한다.
	 * 
	 * @param dfVO DF 정보
	 * @param connectId 커넥션 아이디
	 * @return connectId애 해당하는 Datasource 객체
	 */
	private DataSource getDataSource(DFLoadVO dfVO, String connectId) {
		BasicDataSource dataSource = new BasicDataSource();
		
		ConnectInfoVO connectInfo = dfVO.getConnectInfo(connectId);
		if(connectInfo == null) {
			throw new NullPointerException("Does not exist DataSource. [" + connectId + "]");
		}		
		
		if(this.bulkerCrypt == null) {
			dataSource.setDriverClassName(connectInfo.getDriverClass());
			dataSource.setUrl(connectInfo.getUrl());
			dataSource.setUsername(connectInfo.getUsername());
			dataSource.setPassword(connectInfo.getPassword());
		} else {
			dataSource.setDriverClassName(this.bulkerCrypt.decode(connectInfo.getDriverClass()));
			dataSource.setUrl(this.bulkerCrypt.decode(connectInfo.getUrl()));
			dataSource.setUsername(this.bulkerCrypt.decode(connectInfo.getUsername()));
			dataSource.setPassword(this.bulkerCrypt.decode(connectInfo.getPassword()));
		}
		
		return dataSource;
	}
}
