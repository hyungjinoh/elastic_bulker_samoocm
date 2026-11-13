package com.rayful.bulk.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.rayful.bulk.encryption.IBulkerCrypt;
import com.rayful.bulk.util.CryptUtils;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.ConnectInfoVO;

/**
 * DB 접속 및 쿼리 수행을 담당하는 클래스
 */
@ThreadSafe
public class JdbcDAO {
	private static JdbcDAO INSTANCE = null;
	
	public static synchronized JdbcDAO getInstance(DFLoadVO dfVO, Properties prop) throws Exception {
		if(INSTANCE == null) {
			INSTANCE = new JdbcDAO(dfVO, prop);
		}
		
		return INSTANCE;
	}
	
	@GuardedBy("ConcurrentHashMap")
	private final ConcurrentHashMap<String, NamedParameterJdbcTemplate> cachedTemplate;
	private final IBulkerCrypt bulkerCrypt;
	
	private JdbcDAO(DFLoadVO dfVO, Properties prop) throws Exception {
		this.cachedTemplate = new ConcurrentHashMap<String, NamedParameterJdbcTemplate>();
		this.bulkerCrypt = CryptUtils.getInstance(dfVO, prop);
	}
	
	/**
	 * queryType에 해당하는 key 쿼리를 수행하고, 목록 결과를 리턴
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param connectId 커낵션 아디디
	 * @return 조회 목록
	 */
	public List<Map<String, Object>> selectKeyQuery(DFLoadVO dfVO, String sql, String connectId) {
		List<Map<String, Object>> resultList = null;
		
		if(sql != null) {
			NamedParameterJdbcTemplate jdbcTemplate = null;
			
			jdbcTemplate = this.cachedTemplate.computeIfAbsent(connectId, key -> {
				DataSource dataSource = this.getDataSource(dfVO, connectId);
				return new NamedParameterJdbcTemplate(dataSource);
			});
						
			resultList = jdbcTemplate.queryForList(sql, Collections.emptyMap());
		}
		
		return resultList;
	}

	/**
	 * 서버 커서를 이용하여 db 데이터를 일정 개수 씩 조회하기 위한 Readr 객체를 생성한 후 리턴
	 * ### lguplus 프로젝트에서 추가됨
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param connectId 커낵션 아디디
	 * @return Reader 객체
	 */
	public JdbcCursorItemReader<Map<String, Object>> getItemReader (DFLoadVO dfVO, String sql, String connectId) {
		DataSource dataSource = this.getDataSource(dfVO, connectId);
		String url = ((BasicDataSource)dataSource).getUrl();
		int fetchSize;
		if(url.contains("mariadb")) {
			fetchSize = 0;
		} else {
			fetchSize = dfVO.getMaxRows();
		}
		
		return new JdbcCursorItemReaderBuilder<Map<String,Object>>()
				.dataSource(dataSource)
				.name("keyDataItemReader")
				.fetchSize(fetchSize)
				.verifyCursorPosition(false)  // 중요함.
				.queryTimeout(3600)
				.sql(sql)
				.rowMapper(new RowMapper<Map<String, Object>>(){
					@Override
					public Map<String, Object> mapRow(ResultSet rs, int rownum) throws SQLException {
						ResultSetMetaData metaData = rs.getMetaData();
						int columnCount = metaData.getColumnCount();
						String columnName = null;
						Map<String, Object> dataMap = new HashMap<> ();
						for(int index=1; index <= columnCount; index ++) {
							columnName = metaData.getColumnLabel(index);
							if(columnName == null || columnName.trim().length() == 0) {
								columnName = metaData.getColumnName(index);
							}
							dataMap.put(columnName, rs.getObject(index));
						}
						
						return dataMap;
					}
				}).build();
	}

	
	/**
	 * queryType에 해당하는 쿼리를 수행하고 단일 건의 결과를 리턴
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param keyMap 쿼리 파리미터
	 * @param connectId 커낵션 아디디
	 * @return 조회 데이터
	 */
	public Map<String, Object> selecyQueryMap(DFLoadVO dfVO, String sql, Map<String, Object> keyMap, String connectId) {
		Map<String, Object> resultMap = null;
		
		if(sql != null) {
			NamedParameterJdbcTemplate jdbcTemplate = null;
			
			jdbcTemplate = this.cachedTemplate.computeIfAbsent(connectId, key -> {
				DataSource dataSource = this.getDataSource(dfVO, connectId);
				return new NamedParameterJdbcTemplate(dataSource);
			});
			
			resultMap = jdbcTemplate.queryForMap(sql, keyMap);
		}
		
		return resultMap;
	}
	
	/**
	 * queryType에 해당하는 쿼리를 수행하고 조회 목록 결과를 리턴
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param keyMap 쿼리 파리미터
	 * @param connectId 커낵션 아디디
	 * @return 조회 목록
	 */
	public List<Map<String, Object>> selectQueryList(DFLoadVO dfVO, String sql, Map<String, Object> keyMap, String connectId) {
		List<Map<String, Object>> resultList = null;
		
		if(sql != null) {
			NamedParameterJdbcTemplate jdbcTemplate = null;
			
			jdbcTemplate = this.cachedTemplate.computeIfAbsent(connectId, key -> {
				DataSource dataSource = this.getDataSource(dfVO, connectId);
				return new NamedParameterJdbcTemplate(dataSource);
			});
			
			resultList = jdbcTemplate.queryForList(sql, keyMap);
		}

		return resultList;
	}
	
	/**
	 * queryType에 해당하는 업데이트문 한 건을 수행
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param keyMap 쿼리 파리미터
	 * @param connectId 커낵션 아디디
	 * @return 영향받은 로우 수
	 */
	public int updateQuery(DFLoadVO dfVO, String sql, Map<String, Object> keyMap, String connectId) {
		int affectedRows = 0;
		
		if(sql != null) {
			NamedParameterJdbcTemplate jdbcTemplate = null;
			
			jdbcTemplate = this.cachedTemplate.computeIfAbsent(connectId, key -> {
				DataSource dataSource = this.getDataSource(dfVO, connectId);
				return new NamedParameterJdbcTemplate(dataSource);
			});
			
			affectedRows = jdbcTemplate.update(sql, keyMap);
		}
		
		return affectedRows;
	}
	
	/**
	 * queryType에 해당하는 배치 업데이트문을 수행
	 * 
	 * @param dfVO DF 정보
	 * @param sql 수행할 쿼리문
	 * @param keyList 배치 쿼리의 파라미터 목록
	 * @param connectId 커낵션 아디디
	 * @return
	 */
	public int batchUpdate(DFLoadVO dfVO, String sql, List<Map<String, Object>> keyList, String connectId) {
		int affectedRows = 0;
		
		if(sql != null) {
			NamedParameterJdbcTemplate jdbcTemplate = null;
			
			jdbcTemplate = this.cachedTemplate.computeIfAbsent(connectId, key -> {
				DataSource dataSource = this.getDataSource(dfVO, connectId);
				return new NamedParameterJdbcTemplate(dataSource);
			});
			
			SqlParameterSource [] batchParams = SqlParameterSourceUtils.createBatch(keyList.toArray());
			int [] arrAffectedRow = jdbcTemplate.batchUpdate(sql, batchParams);
			for(int affectedRow : arrAffectedRow) {
				affectedRows += affectedRow;
			}
		}
		
		return affectedRows;
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
		
		dataSource.setTestWhileIdle(true);
		//dataSource.setMinEvictableIdleTimeMillis(60000);
		dataSource.setTimeBetweenEvictionRunsMillis(60000);
		// Oracle은 FROM 절 없는 SELECT를 허용하지 않으므로 (ORA-00923) rdbms 별로 검증 쿼리를 분기한다.
		dataSource.setValidationQuery("oracle".equalsIgnoreCase(connectInfo.getRdbms()) ? "SELECT 1 FROM DUAL" : "SELECT 1");

		return dataSource;
	}
	
	/*
	private DataSource getDataSource(DFLoadVO dfVO, String connectId) {
		HikariConfig config = new HikariConfig();
		
		ConnectInfoVO connectInfo = dfVO.getConnectInfo(connectId);
		if(connectInfo == null) {
			throw new NullPointerException("Does not exist DataSource. [" + connectId + "]");
		}
		
		config.setDriverClassName(connectInfo.getDriverClass());
		config.setJdbcUrl(connectInfo.getUrl());
		config.setUsername(connectInfo.getUsername());
		config.setPassword(connectInfo.getPassword());
		config.setConnectionTimeout(2000L);
		
		return new HikariDataSource(config);
	}
	*/

}
