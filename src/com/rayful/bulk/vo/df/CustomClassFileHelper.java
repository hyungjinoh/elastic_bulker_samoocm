package com.rayful.bulk.vo.df;

/**
 * @author choong10
 *
 */
public class CustomClassFileHelper extends CustomClassItemVO {
	private final String remoteBasePath;
	private final String removeBasePath;
	private final String etcData;
	private final FtpInfoVO ftpInfo;

	public CustomClassFileHelper(String id, String className, boolean use, String remoteBasePath, String removeBasePath, String etcData, FtpInfoVO ftpInfo) {
		super(id, className, use);
		
		this.remoteBasePath = remoteBasePath;
		this.removeBasePath = removeBasePath;
		this.etcData = etcData;
		this.ftpInfo = ftpInfo;
	}

	public String getRemoteBasePath() {
		return remoteBasePath;
	}

	public String getRemoveBasePath() {
		return removeBasePath;
	}

	public String getEtcData() {
		return etcData;
	}

	public FtpInfoVO getFtpInfo() {
		return ftpInfo;
	}

	@Override
	public String toString() {
		return "CustomClassFileHelper [remoteBasePath=" + remoteBasePath + ", removeBasePath=" + removeBasePath
				+ ", etcData=" + etcData + ", ftpInfo=" + ftpInfo + "]";
	}
}
