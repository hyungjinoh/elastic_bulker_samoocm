package com.rayful.bulk.vo.df;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class QueryVO {
	public static final String QUERY_KEQ = "key_query";
	public static final String QUERY_KEY_INC= "key_inc_query";
	public static final String QUERY_UPD_QUEUE = "update_queue_query";
	public static final String QUERY_DATA = "data_query";
	public static final String QUERY_FILE = "file_query";
	
	private final Map<String, QueryItemVO> baseQueryMap;
	private final Map<String, QueryItemVO> etcQueryMap;
	
	public QueryVO() {
		this.baseQueryMap = new HashMap<String, QueryItemVO>();
		this.etcQueryMap = new HashMap<String, QueryItemVO>();
	}
	
	public void putQuery(String queryId, String sql, String connectionId) {
		QueryItemVO sqlQueryVO = new QueryItemVO(queryId, sql, connectionId);
		if(this.baseQueryMap.containsKey(queryId)) {
			throw new IllegalArgumentException("Duplicated \"" + queryId + "\" tag of \"query_info \" in DF" + queryId);
		}
		
		this.baseQueryMap.put(queryId, sqlQueryVO);
	}
	
	public void putQuery(String queryId, String sql, String connectionId, boolean cursorUse, boolean relayUse) {
		QueryItemVO sqlQueryVO = new QueryItemVO(queryId, sql, connectionId, cursorUse, relayUse);
		if(this.baseQueryMap.containsKey(queryId)) {
			throw new IllegalArgumentException("Duplicated \"" + queryId + "\" tag of \"query_info \" in DF" + queryId);
		}
		
		this.baseQueryMap.put(queryId, sqlQueryVO);
	}
	
	public void putEtcQuery(String queryId, String sql, String connectionId) {
		QueryItemVO sqlQueryVO = new QueryItemVO(queryId, sql, connectionId);
		if(this.etcQueryMap.containsKey(queryId)) {
			throw new IllegalArgumentException("Duplicated \"id\" attribute of \"etc_query\" in DF: id=" + queryId);
		}

		this.etcQueryMap.put(queryId, sqlQueryVO);
	}
	
	public QueryItemVO getKeyQuery() {
		return this.baseQueryMap.get(QUERY_KEQ);
	}
	
	public QueryItemVO getKeyIncQuery() {
		return this.baseQueryMap.get(QUERY_KEY_INC);
	}
	
	public QueryItemVO getUpdateQueueQuery() {
		return this.baseQueryMap.get(QUERY_UPD_QUEUE);
	}
	
	public QueryItemVO getDataQuery() {
		return this.baseQueryMap.get(QUERY_DATA);
	}
	
	public QueryItemVO getFileQuery() {
		return this.baseQueryMap.get(QUERY_FILE);
	}
	
	public QueryItemVO getEtcQuery(String etcQueryId) {
		return this.etcQueryMap.get(etcQueryId); 
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
