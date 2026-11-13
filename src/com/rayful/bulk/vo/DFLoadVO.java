package com.rayful.bulk.vo;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.rayful.bulk.vo.df.ColumnVO;
import com.rayful.bulk.vo.df.ConnectInfoVO;
import com.rayful.bulk.vo.df.CustomClassVO;
import com.rayful.bulk.vo.df.DataSourceVO;
import com.rayful.bulk.vo.df.FieldVO;
import com.rayful.bulk.vo.df.NotesNsfListVO;
import com.rayful.bulk.vo.df.QueryVO;

public class DFLoadVO {
	private String dfName;
	private String indexName;
	private String resultFileName;
	private int maxRows;
	private boolean includeIndexname;
	private String sourceType = null;

	private final Map<String, ConnectInfoVO> connectInfoMap;
	public final QueryVO query;
	public final ColumnVO column;
	public final FieldVO field;
	public final CustomClassVO custom;
	public final NotesNsfListVO nsfList;
	public final DataSourceVO datasource;
	
	public DFLoadVO(String dfName) {
		this.dfName = dfName;
		
		this.connectInfoMap = new HashMap<String, ConnectInfoVO>();
		this.query = new QueryVO();
		this.column = new ColumnVO();
		this.field = new FieldVO();
		this.custom = new CustomClassVO();
		this.sourceType = null;
		this.nsfList = new NotesNsfListVO();
		this.datasource = new DataSourceVO();
	}
	
	public String getDfName() {
		return dfName;
	}
	
	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public ConnectInfoVO getConnectInfo(String connectionId) {
		return this.connectInfoMap.get(connectionId);
	}
	
	public void putConnectInfo (Map<String, String> connectInfoMap) {
		String connectionId = connectInfoMap.get("id");
		if(connectionId == null) {
			throw new NullPointerException("Must be id != null of connect_info tag in DF");
		}
		if(this.connectInfoMap.containsKey(connectionId)) {
			throw new IllegalArgumentException("Duplicated \"id\" attrubute of \"connect_info\" tag in DF");
		}
		
		ConnectInfoVO connectInfo = null;
		if("rdbms".equals(this.sourceType)) {
			connectInfo = new ConnectInfoVO (
					connectInfoMap.get("rdbms")
					, connectInfoMap.get("driverClass")
					, connectInfoMap.get("url")
					, connectInfoMap.get("username")
					, connectInfoMap.get("password"));
		} else if("notes".equals(this.sourceType)) {
			connectInfo = new ConnectInfoVO (
					this.sourceType
					, null
					, connectInfoMap.get("url")
					, connectInfoMap.get("username")
					, connectInfoMap.get("password"));
		} else {
			throw new IllegalArgumentException("Invalud sourcType valule in DFLoadVO");
		}

		this.connectInfoMap.put(connectionId, connectInfo);
	}

	public String getResultFileName() {
		return resultFileName;
	}

	public void setResultFileName(String resultFileName) {
		if(resultFileName == null) {
			throw new NullPointerException("Must be file_name != null of result_file tag in DF");
		}
		this.resultFileName = resultFileName;
	}

	public boolean isIncludeIndexname() {
		return includeIndexname;
	}

	public void setIncludeIndexname(boolean includeIndexname) {
		this.includeIndexname = includeIndexname;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public void setMaxRows(String maxRows) {
		if(maxRows == null) {
			throw new NullPointerException("Must be max_rows != null of result_file tag in DF");
		}
		try {
			this.maxRows = Integer.parseInt(maxRows);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("max_rows of result_file tag Must be number in DF");
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
