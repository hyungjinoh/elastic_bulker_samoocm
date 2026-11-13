package com.rayful.elastic.batch.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rayful.elastic.batch.common.BatchStatusType;
import com.rayful.elastic.batch.common.dao.IBatchLogDao;
import com.rayful.elastic.batch.common.utils.BatchLogUtils;
import com.rayful.elastic.batch.common.vo.BatchLogVO;


@Service
public class BatchLogService {
	private static final Logger logger = LoggerFactory.getLogger(BatchLogService.class);
	
	@Autowired
	private IBatchLogDao batchLogDao;
	
	private String batchId;
	private String batchDt;
	
	public void init(String batchId) {
		this.batchId = batchId;
		this.batchDt = BatchLogUtils.getBatchDt();
	}
	
	public boolean checkBatchProceed(String dupCheck) {
		boolean isProceed = false;
		BatchLogVO batchLogVO = null;
		
		String host = BatchLogUtils.getHost();
		if("none".equals(dupCheck)) {
			this.batchLogDao.insertBatchInitLog(this.batchId, this.batchDt, host);
			isProceed = true;
		} else {
			String batchDtKwd = BatchLogUtils.getBatchBaseDtKwd(this.batchDt, dupCheck);
			batchLogVO =  this.batchLogDao.selectBatchLog(this.batchId, batchDtKwd);
			
			if(batchLogVO == null) {
				this.batchLogDao.insertBatchInitLog(this.batchId, this.batchDt, host);
				isProceed = true;
			} else {
				if(BatchStatusType.FAIL.getName().equals(batchLogVO.getBatchStatus())) {
					this.batchLogDao.insertBatchInitLog(this.batchId, this.batchDt, host);
					isProceed = true;
				}
			}
		}
		
		return isProceed;
	}
	
	public void updateStatusToSuccess() {
		try {
			int effectedRow = this.batchLogDao.updateBatchStatus(this.batchId, this.batchDt, BatchStatusType.SUCCESS.getName());
			if(effectedRow <= 0) {
				throw new IllegalArgumentException("Can't update batch status to success (No update)");
			}
		} catch(Exception e) {
			logger.warn("Can't update batch status to success");
		}
	}
	
	public void updateStatusToFail() {
		try {
			int effectedRow = this.batchLogDao.updateBatchStatus(this.batchId, this.batchDt, BatchStatusType.FAIL.getName());
			if(effectedRow <= 0) {
				throw new IllegalArgumentException("Can't update batch status to fail (No update)");
			}
		} catch(Exception e) {
			logger.warn("Can't update batch status to fail");
		}
	}
}
