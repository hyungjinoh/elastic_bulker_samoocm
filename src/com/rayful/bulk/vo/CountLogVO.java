package com.rayful.bulk.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@ThreadSafe
public class CountLogVO {
	@GuardedBy("AtomicInteger")
	private final AtomicInteger totalCount;
	@GuardedBy("AtomicInteger")
	private final AtomicInteger successCount;
	@GuardedBy("AtomicInteger")
	private final AtomicInteger warnCount;
	@GuardedBy("AtomicInteger")
	private final AtomicInteger failCount;
	@GuardedBy("Collections.synchronizedList")
	private final List<String> warnMessage;
	@GuardedBy("Collections.synchronizedList")
	private final List<String> failMessage;
	
	public CountLogVO() {
		this.totalCount = new AtomicInteger(-1);
		this.successCount = new AtomicInteger(0);
		this.warnCount = new AtomicInteger(0);
		this.failCount = new AtomicInteger(0);
		
		this.warnMessage = Collections.synchronizedList(new ArrayList<String>());
		this.failMessage = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public int getTotalCount() {
		int count = 0;
		if (totalCount.get() < 0) {
			count = successCount.get() + warnCount.get() + failCount.get();
		} else {
			count = totalCount.get();
		}
		return count;
	}
	
	public int getSuccessCount() {
		return successCount.get();
	}

	public int getWarnCount() {
		return warnCount.get();
	}

	public int getFailCount() {
		return failCount.get();
	}
	
	
	public String getWarnMessage() {
		String message = "";
		StringBuilder builder = new StringBuilder();
		for(int index=0; index < this.warnMessage.size(); index++) {
			if (index > 0) 
					builder.append(", ");
			builder.append(this.warnMessage.get(index));
		}
		
		if(builder.length() > 0) {
			message = ", Message : " + builder.toString();
		}

		return message;
	}
	
	public String getFailMessage() {
		String message = "";
		StringBuilder builder = new StringBuilder();
		for(int index=0; index < this.failMessage.size(); index++) {
			if (index > 0) 
					builder.append(", ");
			builder.append(this.failMessage.get(index));
		}
		
		if(builder.length() > 0) {
			message = ", Message : " + builder.toString();
		}

		return message;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount.compareAndSet(-1, totalCount);
	}

	public void increaseSuccessCount() {
		this.successCount.incrementAndGet();
	}
	
	public void increaseWarnCount(String warnMessage) {
		this.warnCount.incrementAndGet();
		this.warnMessage.add(warnMessage);
	}
	
	public void increaseFailCount(String failMessage) {
		this.failCount.incrementAndGet();
		this.failMessage.add(failMessage);
	}

	@Override
	public String toString() {
		return "CountLogVO [totalCount=" + totalCount + ", successCount=" + successCount + ", warnCount=" + warnCount
				+ ", failCount=" + failCount + ", warnMessage=" + warnMessage + ", failMessage=" + failMessage + "]";
	}
}
