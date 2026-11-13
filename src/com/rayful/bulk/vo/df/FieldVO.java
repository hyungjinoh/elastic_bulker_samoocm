package com.rayful.bulk.vo.df;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FieldVO {
	public static final String ROLE_FILE_INFO= "file_info";
	public static final String ROLE_FILE_DATA= "file_data";
	public static final String ROLE_ENTITY= "entity";
	
	private final Map<String, FieldItemVO> fieldMap;
	
	private FieldItemVO fileInfoField = null;
	private FieldItemVO fileDataField = null;
	private FieldItemVO entityField = null;
	
	public FieldVO() {
		this.fieldMap = new HashMap<String, FieldItemVO>();
	}
	
	public void putFieldItem(String name, String role, boolean index) {
		if(this.fieldMap.containsKey(name)) {
			throw new IllegalArgumentException("Duplicated \"name\" attrubute of \"field\" tag in DF: name=" + name);
		}
		
		FieldItemVO fieldItemVO =  new FieldItemVO(name, index);
		
		if(ROLE_FILE_INFO.equals(role)) {
			this.fileInfoField = fieldItemVO;
		} else if(ROLE_FILE_DATA.equals(role)) {
			this.fileDataField = fieldItemVO;
		} else if(ROLE_ENTITY.equals(role)) {
			this.entityField = fieldItemVO;
		} else {
			throw new IllegalArgumentException("Invalid value - \"role\" attrubute of \"field\" tag in DF: role=" + role);
		}
		this.fieldMap.put(name, fieldItemVO);
	}
	
	public String getFileInfoFieldName() {
		return this.fileInfoField == null ? null: this.fileInfoField.getName();
	}
	
	public boolean isIndexFileInfo() {
		return this.fileInfoField == null ? false: this.fileInfoField.isIndex();
	}
	
	public String getFileDataFieldName() {
		return this.fileDataField == null ? null: this.fileDataField.getName();
	}
	
	public boolean isIndexFileData() {
		return this.fileDataField == null ? false: this.fileDataField.isIndex();
	}
	
	public String getEntityFieldName() {
		return this.entityField == null ? null: this.entityField.getName();
	}
	
	public boolean isIndexEntity() {
		return this.entityField == null ? false: this.entityField.isIndex();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
