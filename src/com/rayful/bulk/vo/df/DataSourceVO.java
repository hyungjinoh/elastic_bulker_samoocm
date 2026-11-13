package com.rayful.bulk.vo.df;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DataSourceVO {
	private boolean deleteNotIndexed = false;
	private String lastIndexDate;
	
	public DataSourceVO() {
		this.deleteNotIndexed = false;
		this.lastIndexDate = null;
	}
	
	public boolean isDeleteNotIndexed() {
		return deleteNotIndexed;
	}

	public void setDeleteNotIndexed(boolean deleteNotIndexed) {
		this.deleteNotIndexed = deleteNotIndexed;
	}
	
	public String getLastIndexDate() {
		return lastIndexDate;
	}

	public void setLastIndexDate(String lastIndexDate) {
		this.lastIndexDate = lastIndexDate;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
