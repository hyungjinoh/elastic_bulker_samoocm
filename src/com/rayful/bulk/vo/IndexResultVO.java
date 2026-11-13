package com.rayful.bulk.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@ThreadSafe
public class IndexResultVO {
	@GuardedBy("AtomicInteger")
	AtomicInteger createCount;
	@GuardedBy("AtomicInteger")
	AtomicInteger updateCount;
	@GuardedBy("AtomicInteger")
	AtomicInteger deleteCount;
	@GuardedBy("AtomicInteger")
	AtomicInteger partialCount;
	
	@GuardedBy("AtomicInteger")
	AtomicInteger etcCount;
	@GuardedBy("Collections.synchronizedList")
	List<String> etcResult;
	@GuardedBy("AtomicInteger")
	AtomicInteger errorCount;
	@GuardedBy("Collections.synchronizedList")
	List<String> errorReason;
	
	public IndexResultVO() {
		this.createCount = new AtomicInteger(0);
		this.updateCount = new AtomicInteger(0);
		this.deleteCount = new AtomicInteger(0);
		this.partialCount = new AtomicInteger(0);

		this.etcCount = new AtomicInteger(0);
		this.etcResult = Collections.synchronizedList(new ArrayList<String>());
		this.errorCount = new AtomicInteger(0);
		this.errorReason = Collections.synchronizedList(new ArrayList<String>());
	}

	public int getTotalCount() {
		return createCount.get() + updateCount.get() + deleteCount.get() + partialCount.get() + etcCount.get() + errorCount.get() ;
	}
	
	public int getCreateCount() {
		return createCount.get();
	}

	public int getUpdateCount() {
		return updateCount.get();
	}

	public int getDeleteCount() {
		return deleteCount.get();
	}

	public int getPartialCount() {
		return partialCount.get();
	}

	public int getErrorCount() {
		return errorCount.get();
	}

	public List<String> getErrorReason() {
		return errorReason;
	}

	public int getEtcCount() {
		return etcCount.get();
	}

	public List<String> getEtcResult() {
		return etcResult;
	}

	public void increaseCreateCount() {
		this.createCount.incrementAndGet();
	}
	
	public void increaseUpdateCount() {
		this.updateCount.incrementAndGet();
	}
	
	public void increaseDeleteCount() {
		this.deleteCount.incrementAndGet();
	}
	
	public void increasePartialCount() {
		this.partialCount.incrementAndGet();
	}
	
	public void increaseErrorCount(String reason) {
		this.errorCount.incrementAndGet();
		this.errorReason.add(reason);
	}
	
	public void increaseEtcCount(String result) {
		this.etcCount.incrementAndGet();
		this.etcResult.add(result);
	}

	@Override
	public String toString() {
		return "IndexResultVO [createCount=" + createCount + ", updateCount=" + updateCount + ", deleteCount="
				+ deleteCount + ", partialCount=" + partialCount + ", etcCount=" + etcCount + ", etcResult=" + etcResult
				+ ", errorCount=" + errorCount + ", errorReason=" + errorReason + "]";
	}
}
