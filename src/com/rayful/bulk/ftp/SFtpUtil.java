package com.rayful.bulk.ftp;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.ResourceUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Deprecated
public class SFtpUtil {
	
	public SFtpUtil() {
		
	}
	
	private static String knownHostName = "known_hosts";
	private static Session session = null;
	private static ChannelSftp channelSftp = null;
	
	/**
	 * SFTP 접속
	 * known_hosts 생성 방법 : ssh-keyscan -H -t rsa REMOTE_HOSTNAME >> known_hosts
	 * known_hosts 파일 경로 : /resoruce/known_hosts
	 * (StrictHostKeyChecking을 no로 세팅 할 경우 호스트 정보를 검사하지 않음)
	 * @return
	 * @throws JSchException
	 * @throws IOException 
	 */
	public static void connect(Map<String, Object> ftpInfoMap) throws JSchException, IOException {
		String remoteHost = (String) ftpInfoMap.get("url");
		int port = Integer.parseInt((String) ftpInfoMap.get("port"));
		String userName = (String) ftpInfoMap.get("username");;
		String password = (String) ftpInfoMap.get("password");
		
		JSch jsch = new JSch();
		session = jsch.getSession(userName, remoteHost, port);
		String knowHostPath = getKnownHostPath();
		
		if(knowHostPath != null) {
			jsch.setKnownHosts(knowHostPath);
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "yes");
			session.setConfig(config);
		
		} else {
			// [행정안전부 SW 개발보안 가이드 - 부적절한 인증서 유효성 검증(CWE-295)]
			// known_hosts 미설정 시 호스트키 검증을 생략하지 않고 연결을 실패시킨다.
			throw new IOException("SFTP known_hosts is not configured. Register the host key (known_hosts).");
		}
		
		session.setPassword(password);
		session.connect();
		channelSftp = (ChannelSftp) session.openChannel("sftp");
	}
	
	/**
	 * SFTP 접속 종료
	 */
	public static void disconnect() {
		if(channelSftp != null) {
			channelSftp.exit();
		}
		
		if(session != null) {
			session.disconnect();
		}
	}
	
	/**
	 * 파일 다운로드
	 * @throws JSchException
	 * @throws SftpException
	 * @throws IOException
	 */
	public static void download(String remoteFileFullPath, String downloadFilePath) 
			throws JSchException, SftpException {
		
		if(remoteFileFullPath != null && downloadFilePath != null) {
			if(!channelSftp.isConnected()) {
				channelSftp.connect();
			}
			
			channelSftp.get(remoteFileFullPath, downloadFilePath);
		}
	}
	
	/**
	 * known_hosts 절대 경로 리턴
	 * @return
	 */
	public static String getKnownHostPath() {
		String result = null;
		
		try {
			File file = ResourceUtils.getFile("classpath:" + knownHostName);
			result =  file.getAbsolutePath();
			
		} catch (Exception e) {
			// known_hosts 파일이 없을 수 있음(정상 흐름).
			result = null;
		}
		
		return result;
	}
}
