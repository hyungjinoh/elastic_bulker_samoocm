package com.rayful.bulk.vo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@ThreadSafe
public class DataLogVO {
	private final Map<String, Object> key;
	private final int total;
	private final int current;
	private final String indexAction;
	
	@GuardedBy("AtomicReference")
	private final AtomicReference<String> errorMessage = new AtomicReference<>(null);
	
	@GuardedBy("CountLogVO.class")
	public final CountLogVO fileLog;
	@GuardedBy("CountLogVO.class")
	public final CountLogVO etcLog;
	
	public DataLogVO(Map<String, Object> key, int total, int current, String indexAction) {
		this.key = key;
		this.total = total;
		this.current = current;
		this.indexAction = indexAction;
		
		this.fileLog = new CountLogVO();
		this.etcLog = new CountLogVO();
	}
	
	public Map<String, Object> getKey() {
		return key;
	}
	
	public int getTotal() {
		return total;
	}

	public int getCurrent() {
		return current;
	}

	public String getIndexAction() {
		return indexAction;
	}

	public String getErrorMessage() {
		return errorMessage.get();
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage.set(errorMessage);
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
	}
	
}
