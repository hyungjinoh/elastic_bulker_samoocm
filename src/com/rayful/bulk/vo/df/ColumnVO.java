package com.rayful.bulk.vo.df;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ColumnVO {
	public static final String ROLE_KEY= "key";
	public static final String ROLE_BODY= "body";
	public static final String ROLE_INDEX_ACTION= "index_action";
	public static final String ROLE_INDEX_TIME= "index_time";
	public static final String ROLE_INDEX_PATTERN= "index_pattern";
	public static final String ROLE_NSF_NAME= "nsf_name";

	
	public static final String ROLE_FILE_PATH= "file_path";
	public static final String ROLE_FILE_NAME= "file_name";
	public static final String ROLE_FILE_SIZE= "file_size";
	public static final String QUERY_DATA= "data_query";
	public static final String QUERY_FILE= "file_query";
	public static final String QUERY_NONE= "none";

	
	private final Map<String, ColumnItemVO> columnMap;
	
	private ColumnItemVO keyColumn = null;
	private ColumnItemVO bodyColumn = null;
	private ColumnItemVO indexActionColumn = null;
	private ColumnItemVO indexTimeColumn = null;
	private ColumnItemVO indexPatternColumn = null;
	private ColumnItemVO nsfNameColumn = null;
	private ColumnItemVO fileKeyColumn = null;
	private ColumnItemVO filePathColumn = null;
	private ColumnItemVO fileNameColumn = null;
	private ColumnItemVO fileSizeColumn = null;
	
	public ColumnVO() {
		this.columnMap = new HashMap<String, ColumnItemVO>();
	}
	
	public void putColumnItem(String name, String role, boolean output, boolean removeHtml, boolean removeSpace, boolean encodeHash, boolean decodeBase64, String separator, String renameTo, String copyTo, String query) {
		if(this.columnMap.containsKey(name)) {
			throw new IllegalArgumentException("Duplicated column's name value - \"name\" attrubute of \"db_column_info > column\" tag in DF: name=" + name);
		}
		
		ColumnItemVO columnItemVO =  new ColumnItemVO(name, output, removeHtml, removeSpace, encodeHash, decodeBase64, separator, renameTo, copyTo, query);
		
		if(role != null && role.trim().length() >= 0) {
			if(ROLE_KEY.equals(role)) {
				this.keyColumn = columnItemVO;
			} else if(ROLE_BODY.equals(role)) {
				this.bodyColumn = columnItemVO;
			} else if(ROLE_INDEX_ACTION.equals(role)) {
				this.indexActionColumn = columnItemVO;
			} else if(ROLE_INDEX_TIME.equals(role)) {
				this.indexTimeColumn = columnItemVO;
			} else if(ROLE_INDEX_PATTERN.equals(role)) {
				this.indexPatternColumn = columnItemVO;
			} else if(ROLE_NSF_NAME.equals(role)) {
				this.nsfNameColumn = columnItemVO;
			} else if(ROLE_FILE_PATH.equals(role)) {
				this.filePathColumn = columnItemVO;
			} else if(ROLE_FILE_NAME.equals(role)) {
				this.fileNameColumn = columnItemVO;
			} else if(ROLE_FILE_SIZE.equals(role)) {
				this.fileSizeColumn = columnItemVO;
			} else {
				throw new IllegalArgumentException("Invalid column's role value - \"role\" attrubute of \"db_column_info > column\" tag in DF: role=" + role);
			}
		}
		
		this.columnMap.put(name, columnItemVO);
	}

	public String getKeyColumnName() {
		return this.keyColumn == null ? null: this.keyColumn.getName();
	}
	
	public String getBodyColumnName() {
		return this.bodyColumn == null ? null: this.bodyColumn.getName();
	}
	
	public String getIndexActionColumnName() {
		return this.indexActionColumn == null ? null: this.indexActionColumn.getName();
	}
	
	public String getIndexTimeColumnName() {
		return this.indexTimeColumn == null ? null: this.indexTimeColumn.getName();
	}

	public String getIndexPatternColumnName() {
		return this.indexPatternColumn == null ? null: this.indexPatternColumn.getName();
	}

	public String getNsfNameColumnName() {
		return this.nsfNameColumn == null ? null: this.nsfNameColumn.getName();
	}
	
	public String getFileKeyColumnName() {
		return this.fileKeyColumn == null ? null: this.fileKeyColumn.getName();
	}
	
	public String getFilePathColumnName() {
		return this.filePathColumn == null ? null: this.filePathColumn.getName();
	}
	
	public String getFileNameColumnName() {
		return this.fileNameColumn == null ? null: this.fileNameColumn.getName();
	}
	
	public String getFileSizeColumnName() {
		return this.fileSizeColumn == null ? null: this.fileSizeColumn.getName();
	}

	public List<String> getNoOutputColumnNameList(String query) {
		if(!QUERY_DATA.equalsIgnoreCase(query) && !QUERY_FILE.equalsIgnoreCase(query)) {
			throw new IllegalArgumentException("Expect: \"data_query\" or \"file_query\"");
		} 
		
		List<String> list = new ArrayList<String>();
		for(ColumnItemVO columnItemVO : this.columnMap.values()) {
			if(columnItemVO.getQuery().equalsIgnoreCase(query)) {
				if(!columnItemVO.isOutput()) {
					list.add(columnItemVO.getName());
				}
			}
		}
		return list;
	}
	
	public List<ColumnItemVO> getSeparatorColumnList() {
		List<ColumnItemVO> list = new ArrayList<ColumnItemVO>();
		for(ColumnItemVO columnItemVO : this.columnMap.values()) {
			if(columnItemVO.getSeparator() != null) {
				list.add(columnItemVO);
			}
		}
		return list;
	}
	
	public List<ColumnItemVO> getColumnList () {
		List<ColumnItemVO> list = new ArrayList<ColumnItemVO>();
		for(ColumnItemVO columnItemVO : this.columnMap.values()) {
			list.add(columnItemVO);
		}
		return list;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
