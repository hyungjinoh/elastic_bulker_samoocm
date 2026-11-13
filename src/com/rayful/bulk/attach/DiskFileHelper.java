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

import com.google.errorprone.annotations.ThreadSafe;
import com.rayful.bulk.vo.DFLoadVO;

/**
 * 로컬 경로에 존재하는 첨부파일들의 복사 및 필터 처리 관련 클래스의 공통 함수를 구현한 클래스
 * <p/>
 * 다운로드(복사) 부분을 구현 <br/>
 * 필터처리는 베이스 클래스 기능을 상속 <br/>
 */
@ThreadSafe
public class DiskFileHelper extends BaseFileHelper {
	private static final Logger logger = LoggerFactory.getLogger(DiskFileHelper.class);
	
	protected final Semaphore semaphore;
	public DiskFileHelper(DFLoadVO dfVO, Properties prop) throws IOException {
		super(dfVO, prop);
		
		int nThread = this.dfVO.custom.getBulkerSerivce() == null ? 1: this.dfVO.custom.getBulkerSerivce().getnThreadCopy();
		logger.info("Limit number of Copy Thread: {}", nThread);
		this.semaphore = new Semaphore(nThread, true); 
	}

	@Override
	public void init() throws IOException {}

	@Override
	public File download(String remotePath, String downFileName) throws IOException {
		
		Path dest = null;
		try {
			this.semaphore.acquire();
			
			Path source = Paths.get(this.getRemoteFullPath(remotePath));
			Path destFolder = Paths.get(this.downloadPath.toString(), Thread.currentThread().getName());
			if(!Files.exists(destFolder)) {
				Files.createDirectory(destFolder);
			}
			// [행정안전부 SW 개발보안 가이드 - 경로 조작 및 자원 삽입(CWE-22)]
			// 다운로드 파일명이 다운로드 폴더를 벗어나지 못하도록 검증한다.
			dest = com.rayful.bulk.util.SecurityUtils.resolveSafely(destFolder, downFileName);
			logger.debug("downloading: source={}, dest={}", source, dest);
			
			Files.copy(source, dest);
		} catch (InterruptedException e) {
			throw new IOException("Terminate abnoramly. - semaphore.acquire()");
		} finally {
			this.semaphore.release();
		}
		
		return dest.toFile();
	}

	@Override
	public void destroy() {}
}
