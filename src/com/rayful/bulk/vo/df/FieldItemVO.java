package com.rayful.bulk.vo.df;

public class FieldItemVO {
	private final String name;
	private final boolean index;
	
	public FieldItemVO(String name, boolean index) {
		if(name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired \"name\" attrubute of \"field\" tag in DF");
		}
		
		this.name = name;
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public boolean isIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "FieldItemVO [name=" + name + ", index=" + index + "]";
	}
}
