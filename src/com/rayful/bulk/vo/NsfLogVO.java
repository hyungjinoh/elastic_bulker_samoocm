package com.rayful.bulk.vo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@ThreadSafe
public class NsfLogVO {
	private final String name;
	
	@GuardedBy("AtomicBoolean")
	private final AtomicBoolean completed  = new AtomicBoolean(false);
	@GuardedBy("AtomicReference")
	private final AtomicReference<String> desc = new AtomicReference<>(null);
	
	public NsfLogVO(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public boolean isCompleted() {
		return completed.get();
	}
	
	public String getDesc() {
		return desc.get();
	}

	public void setCompleted() {
		this.completed.set(true);
	}

	public void setCompletFail(String desc) {
		this.completed.set(false);
		this.desc.set(desc);
	}

	@Override
	public String toString() {
		return "NsfLogVO [name=" + name + ", completed=" + completed + ", desc=" + desc + "]";
	}
}
