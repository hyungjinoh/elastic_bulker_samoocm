package com.rayful.bulk.vo.df;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CustomClassVO {
	private CustomClassItemVO bulkerSerivce = null;
	private CustomClassItemVO etcReader = null;
	private CustomClassItemVO indexSerivce = null;
	private CustomClassItemVO etcProcessor = null;
	
	private CustomClassItemVO qaService = null;
	private CustomClassItemVO entityService = null;
	
	private CustomClassFileHelper fileHelper = null;
	private CustomClassItemVO bulkerCrypt = null;
	
	public CustomClassVO() {
		
	}
	
	public void setBulkerService(String id, String className, boolean use, int nThread, int nThreadCopy) {
		this.bulkerSerivce = new CustomClassItemVO(id, className, use, nThread, nThreadCopy);
	}

	public CustomClassItemVO getBulkerSerivce() {
		return bulkerSerivce;
	}
	
	public void setEtcReader(String id, String className, boolean use) {
		this.etcReader = new CustomClassItemVO(id, className, use);
	}

	public CustomClassItemVO getEtcReader() {
		return etcReader;
	}

	public void setIndexService(String id, String className, boolean use, boolean async) {
		this.indexSerivce = new CustomClassItemVO(id, className, use, async);
	}
	
	public CustomClassItemVO getIndexSerivce() {
		return indexSerivce;
	}
	
	public CustomClassItemVO getEtcProcessor() {
		return etcProcessor;
	}

	public void setEtcProcessor(String id, String className, boolean use) {
		this.etcProcessor = new CustomClassItemVO(id, className, use);;
	}

	public void setQaService(String id, String className, boolean use) {
		this.qaService = new CustomClassItemVO(id, className, use);
	}
	
	public CustomClassItemVO getQaService() {
		return qaService;
	}
	
	public void setEntityService(String id, String className, boolean use) {
		this.entityService = new CustomClassItemVO(id, className, use);
	}
	
	public CustomClassItemVO getEntityService() {
		return entityService;
	}
	
	public void setFileHelpr(String id, String className, boolean use, String remoteBasePath, String removeBasePath, String etcData, Map<String, String> ftpInfoMap) {
		FtpInfoVO ftpInfoVO = null;
		if(ftpInfoMap.size() > 0) {
			ftpInfoVO = new FtpInfoVO(
					ftpInfoMap.get("url")
					, ftpInfoMap.get("port")
					, ftpInfoMap.get("username")
					, ftpInfoMap.get("password"));
		}
		
		this.fileHelper = new CustomClassFileHelper(id, className, use, remoteBasePath, removeBasePath, etcData, ftpInfoVO);
	}
	
	public CustomClassFileHelper getFileHelper() {
		return fileHelper;
	}
	
	public void setBulkerCrypt(String id, String className, boolean use) {
		this.bulkerCrypt = new CustomClassItemVO(id, className, use);
	}
	
	public CustomClassItemVO getBulkerCrypt() {
		return bulkerCrypt;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
