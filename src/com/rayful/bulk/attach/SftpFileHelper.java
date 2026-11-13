package com.rayful.bulk.attach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.CustomClassFileHelper;
import com.rayful.bulk.vo.df.FtpInfoVO;

/**
 * SFTP 서버에 접속해서 첨부파일들의 다운로드 및 필터 처리 관련 클래스의 공통 함수를 구현한 클래스
 * <p/>
 * 초기화(접속), 다운로드, 종료 부분을 구현 <br/>
 * 필터처리는 베이스 클래스 기능을 상속 <br/>
 */
public class SftpFileHelper extends BaseFileHelper{
	private static final Logger logger = LoggerFactory.getLogger(SftpFileHelper.class);
	private final String hostCheckFileName = "known_hosts";
	
	private final FtpInfoVO ftpInfo;
	private Session session = null;
	private ChannelSftp channelSftp = null;
	protected final Semaphore semaphore;
	
	public SftpFileHelper(DFLoadVO dfVO, Properties prop) throws IOException {
		super(dfVO, prop);
		
		CustomClassFileHelper customClassItemVO  = this.dfVO.custom.getFileHelper();
		this.ftpInfo = customClassItemVO.getFtpInfo();
		if(this.ftpInfo == null) {
			throw new NullPointerException("ftpInfo == null");
		}
		
		//int nThread = this.dfVO.custom.getBulkerSerivce() == null ? 1: this.dfVO.custom.getBulkerSerivce().getnThreadCopy();
		//session, channelSftp 를 여러 스레드에서 사용할 수 있도록 수정되지 않아서 접근 스레드 수를 1로 고정 설정
		int nThread = 1;
		logger.info("Limit number of Copy Thread: {}", nThread);
		this.semaphore = new Semaphore(nThread, true); 
	}

	private String getKnownHostPath() {
		String result = null;
		try {
			File file = ResourceUtils.getFile("classpath:" + this.hostCheckFileName);
			result =  file.getAbsolutePath();
			
		} catch (Exception e) {
			// known_hosts 파일이 없을 수 있음(정상 흐름). 상세는 debug 로그로만 기록.
			logger.debug("known_hosts not found on classpath: {}", e.getMessage());
		}
		
		return result;
	}
	
	@Override
	public void init() throws IOException {
		if(this.session != null || this.channelSftp != null) {
			throw new IllegalStateException("Aledary used.");
		}
		
		try {
			JSch jsch = new JSch();
			this.session = jsch.getSession(ftpInfo.getUsername(), ftpInfo.getUrl(), ftpInfo.getPort());
			String knowHostPath = getKnownHostPath();
			
			// [행정안전부 SW 개발보안 가이드 - 부적절한 인증서 유효성 검증(CWE-295)]
			// SFTP 호스트키 검증을 기본 활성화(StrictHostKeyChecking=yes)한다.
			// known_hosts 가 없고, 폐쇄망 등에서 부득이 검증을 생략해야 하는 경우에만
			// 설정(SFTP.DISABLE_HOST_KEY_CHECK=true)으로 명시적으로 opt-in 한다(보안경고 출력).
			if(knowHostPath != null) {
				jsch.setKnownHosts(knowHostPath);
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "yes");
				session.setConfig(config);
			} else {
				boolean disableHostKeyCheck = this.prop != null
						&& "true".equalsIgnoreCase(this.prop.getProperty("SFTP.DISABLE_HOST_KEY_CHECK"));
				if(disableHostKeyCheck) {
					logger.warn("[SFTP][SECURITY] Host key checking is DISABLED by SFTP.DISABLE_HOST_KEY_CHECK=true. Only allowed in an isolated/closed network.");
					Properties config = new Properties();
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);
				} else {
					throw new IOException("SFTP known_hosts is not configured. Register the host key (known_hosts) or set SFTP.DISABLE_HOST_KEY_CHECK=true for a closed network.");
				}
			}
			
			this.session.setPassword(ftpInfo.getPassword());
			this.session.connect();
			this.channelSftp = (ChannelSftp) session.openChannel("sftp");
		} catch(JSchException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public File download(String remotePath, String localFileName) throws IOException {
		Path dest = null;
		try {
			this.semaphore.acquire();
			
			if(remotePath == null) {
				throw new NullPointerException("remotePath == null");
			}
			
			if(localFileName == null) {
				throw new NullPointerException("localPath == null");
			}
			
			String source = this.getRemoteFullPath(remotePath);
			Path destFolder = Paths.get(this.downloadPath.toString(), Thread.currentThread().getName());
			if(!Files.exists(destFolder)) {
				Files.createDirectory(destFolder);
			}
			// [행정안전부 SW 개발보안 가이드 - 경로 조작 및 자원 삽입(CWE-22)]
			// 로컬 파일명이 다운로드 폴더를 벗어나지 못하도록 검증한다.
			dest = com.rayful.bulk.util.SecurityUtils.resolveSafely(destFolder, localFileName); // ".." 및 경로구분자 차단, 기준 디렉터리 이탈 검증
			logger.debug("downloading: source={}, dest={}", source, dest);
			
			try {
				if(!this.channelSftp.isConnected()) {
					this.channelSftp.connect();
				}
				
				this.channelSftp.get(source, dest.toString());
			} catch(SftpException | JSchException e) {
				throw new IOException(e.getMessage());
			}
		} catch (InterruptedException e) {
			throw new IOException("Terminate abnoramly. - semaphore.acquire()");
		} finally {
			this.semaphore.release();
		}
		
		return dest.toFile();
	}

	@Override
	public void destroy() {
		if(this.channelSftp != null) {
			this.channelSftp.exit();
			this.channelSftp = null;
		}
		
		if(this.session != null) {
			this.session.disconnect();
			this.session = null;
		}
	}
	
	protected String getRemoteFullPath(String remotePath) {
		String fullPath = super.getRemoteFullPath(remotePath);
		
		// ftp는 경로문자로 / 를 사용함.
		if (fullPath.contains("\\")) {
			fullPath = fullPath.replace("\\", "/");
		}
		
		return fullPath;
	}

}
