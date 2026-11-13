package com.rayful.elastic.batch.common.vo;

import java.sql.Date;

public class BatchLogVO {
	private String batchId;
	private String batchDt;
	private String batchStatus;
	private String batchHost;
	private Date startDt;
	private Date endDt;
	
	public String getBatchId() {
		return batchId;
	}
	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}
	public String getBatchDt() {
		return batchDt;
	}
	public void setBatchDt(String batchDt) {
		this.batchDt = batchDt;
	}
	public String getBatchStatus() {
		return batchStatus;
	}
	public void setBatchStatus(String batchStatus) {
		this.batchStatus = batchStatus;
	}
	public String getBatchHost() {
		return batchHost;
	}
	public void setBatchHost(String batchHost) {
		this.batchHost = batchHost;
	}
	public Date getStartDt() {
		return startDt;
	}
	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}
	public Date getEndDt() {
		return endDt;
	}
	public void setEndDt(Date endDt) {
		this.endDt = endDt;
	}
	
	@Override
	public String toString() {
		return "BatchLogVO [batchId=" + batchId + ", batchDt=" + batchDt + ", batchStatus=" + batchStatus
				+ ", batchHost=" + batchHost + ", startDt=" + startDt + ", endDt=" + endDt + "]";
	}
}
