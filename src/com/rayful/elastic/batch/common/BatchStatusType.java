package com.rayful.elastic.batch.common;

public enum BatchStatusType {
	SUCCESS("S")
	, FAIL("F")
	, PROCESSING("P");
	
	private final String name;
	
	private BatchStatusType(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
}
