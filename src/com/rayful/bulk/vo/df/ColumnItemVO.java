package com.rayful.bulk.vo.df;

import java.util.Arrays;

public class ColumnItemVO {
	private final String name;
	private final boolean output;
	private final boolean removeHtml;
	private final boolean removeSpace;
	private final boolean encodeHash;
	private final boolean decodeBase64;
	private final String separator;
	private final String renameTo;
	private final String [] copyTo;
	private final String query;
	
	public ColumnItemVO(String name, boolean output, boolean removeHtml, boolean removeSpace, boolean encodeHash, boolean decodeBase64, String separator, String renameTo, String copyTo, String query) {
		if(name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired \"name\" attrubute of \"column\" tag in DF");
		}
		
		this.name = name;
		this.output = output;
		this.separator = separator;
		this.removeHtml = removeHtml;
		this.removeSpace = removeSpace;
		this.renameTo = renameTo;
		this.encodeHash = encodeHash;
		this.decodeBase64 = decodeBase64;
		
		
		if(copyTo == null || copyTo.trim().isEmpty()) {
			this.copyTo = null;
		} else {
			String [] columns = copyTo.split(",");
			for (int index=0; index <columns.length; index ++) {
				columns[index] = columns[index].trim();
			}
			
			this.copyTo = columns;
		}
		
		if(query == null || query.trim().isEmpty()) {
			this.query = "data_query";	// 기본 값
		} else if(!ColumnVO.QUERY_DATA.equalsIgnoreCase(query) 
				&& !ColumnVO.QUERY_FILE.equalsIgnoreCase(query)
				&& !ColumnVO.QUERY_NONE.equalsIgnoreCase(query)) {
			throw new IllegalArgumentException("\"query\" attrubute of \"column\" value is expected \"data_query\" or \"file_query\" or \"none\"");
		} else {
			this.query = query;
		}
		
	}

	public String getName() {
		return name;
	}

	public boolean isOutput() {
		return output;
	}

	public boolean isRemoveHtml() {
		return removeHtml;
	}

	public boolean isRemoveSpace() {
		return removeSpace;
	}

	public boolean isEncodeHash() {
		return encodeHash;
	}

	public boolean isDecodeBase64() {
		return decodeBase64;
	}

	public String getSeparator() {
		return separator;
	}
	
	public String getRenameTo() {
		return renameTo;
	}

	public String[] getCopyTo() {
		return copyTo;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public String toString() {
		return "ColumnItemVO [name=" + name + ", output=" + output + ", removeHtml=" + removeHtml + ", removeSpace="
				+ removeSpace + ", encodeHash=" + encodeHash + ", decodeBase64=" + decodeBase64 + ", separator="
				+ separator + ", renameTo=" + renameTo + ", copyTo=" + Arrays.toString(copyTo) + ", query=" + query
				+ "]";
	}
}
