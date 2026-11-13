package com.rayful.elastic.batch.common.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.rayful.elastic.batch.common.vo.BatchLogVO;

@Mapper
public interface IBatchLogDao {
	
	BatchLogVO selectBatchLog(
			@Param("batchId") String batchId
			, @Param("batchDt") String batchDt);

	int insertBatchInitLog(
			@Param("batchId") String batchId
			, @Param("batchDt") String batchDt
			, @Param("batchHost") String batchHost);
	
	int updateBatchStatus(
			@Param("batchId") String batchId
			, @Param("batchDt") String batchDt
			, @Param("batchStatus") String batchStatus
	);
	
}
