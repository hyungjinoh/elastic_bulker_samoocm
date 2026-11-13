package com.rayful.bulk.vo.df;

public class QueryItemVO {
	private final String queryId;
	private final String sql;
	private final String connectionId;
	private final boolean cursorUse;
	private final boolean relayUse;
	
	public QueryItemVO(String queryId, String sql, String connectionId) {
		this(queryId, sql, connectionId, false, false);
	}
	
	public QueryItemVO(String queryId, String sql, String connectionId, boolean cursorUse, boolean relayUse) {
		if(queryId == null || queryId.trim().length() == 0) {
			throw new IllegalArgumentException("Must be not empty : queryId");
		}
		if(sql == null || sql.trim().length() == 0) {
			throw new IllegalArgumentException("Must be not empty : sql");
		}
		if(connectionId == null || connectionId.trim().length() == 0) {
			throw new IllegalArgumentException("Must be not empty : connectionId");
		}
		
		this.queryId = queryId;
		this.sql = sql;
		this.connectionId = connectionId;
		this.cursorUse = cursorUse;
		this.relayUse = relayUse;
	}
	
	public String getQueryId() {
		return queryId;
	}

	public String getSql() {
		return sql;
	}

	public String getConnectionId() {
		return connectionId;
	}
	
	public boolean isCursorUse() {
		return cursorUse;
	}	

	public boolean isRelayUse() {
		return relayUse;
	}

	@Override
	public String toString() {
		return "QueryItemVO [queryId=" + queryId + ", sql=" + sql + ", connectionId=" + connectionId + ", cursorUse="
				+ cursorUse + ", relayUse=" + relayUse + "]";
	}
}
