package com.rayful.bulk.vo;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;

@ThreadSafe
public class BulkerLogVO {
	@GuardedBy("CountLogVO.class")
	public final CountLogVO query;
	@GuardedBy("CountLogVO.class")
	public final CountLogVO dump;
	@GuardedBy("CountLogVO.class")
	public final CountLogVO index;
	@GuardedBy("IndexResultVO.class")
	public final IndexResultVO indexResult;
	@GuardedBy("CountLogVO.class")
	public final CountLogVO qadump;
	@GuardedBy("CountLogVO.class")
	public final CountLogVO entity;
	@GuardedBy("NsfLogVO.class")
	public final NsfLogVO nsf;

	public BulkerLogVO() {
		this.query = new CountLogVO();
		this.dump = new CountLogVO();
		this.index = new CountLogVO();
		this.indexResult = new IndexResultVO();
		this.qadump = new CountLogVO();
		this.entity = new CountLogVO();
		this.nsf = null;
	}
	
	public BulkerLogVO(String nsfName) {
		this.query = new CountLogVO();
		this.dump = new CountLogVO();
		this.index = new CountLogVO();
		this.indexResult = new IndexResultVO();
		this.qadump = new CountLogVO();
		this.entity = new CountLogVO();
		this.nsf = new NsfLogVO(nsfName);
	}

}
