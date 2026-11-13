package com.rayful.bulk.service;

import java.util.Properties;

import com.rayful.bulk.vo.DFLoadVO;

public abstract class BaseEtcProcessor implements IEtcProcessor {
	protected final DFLoadVO dfVO;
	protected final Properties prop;
	
	public BaseEtcProcessor(DFLoadVO dfVO, Properties prop){
		this.dfVO = dfVO;
		this.prop = prop;
	}

	@Override
	abstract public void preProcess();

	@Override
	abstract public void postProcess();
}
