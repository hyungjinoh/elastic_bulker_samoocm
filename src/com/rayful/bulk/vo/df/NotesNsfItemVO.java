package com.rayful.bulk.vo.df;

public class NotesNsfItemVO {
	private final String connectionId;
	private final String name;
	
	public NotesNsfItemVO(String connectionId, String name) {
		if(connectionId == null || connectionId.trim().length() == 0) {
			throw new IllegalArgumentException("Must be not empty : connectionId");
		}
		if(name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("Must be not empty : name");
		}
		
		this.connectionId = connectionId;
		this.name = name;
	}
	
	public String getConnectionId() {
		return connectionId;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "NotesNsfItemVO [connectionId=" + connectionId + ", name=" + name + "]";
	}
}
