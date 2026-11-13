package com.rayful.bulk.attach;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.errorprone.annotations.ThreadSafe;
import com.rayful.bulk.textfilter.TikaExtractText;
import com.rayful.bulk.util.ExecuteUtils;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.CustomClassFileHelper;


/**
 * 첨부 파일의 다운로드 및 필터 처리 관련 클래스의 공통 함수 중 함수를 일부 구현한 추상클래스
 */
@ThreadSafe
public abstract class BaseFileHelper implements IAttachFileHelper {
	private static final Logger logger = LoggerFactory.getLogger(BaseFileHelper.class);
	protected static final int RPATHTYPE_ABSOLUTE = 1;
	protected static final int RPATHTYPE_RELATIVE = 2;
	protected static final int RPATHTYPE_COMPOSITE = 3;
	protected static final int FILTERTYPE_TIKA = 1;
	protected static final int FILTERTYPE_EXTERANL = 2;
	
	protected final Properties prop;
	protected final DFLoadVO dfVO;
	
	protected final Path downloadPath;
	
	protected final int remotePathType;
	protected final String remoteBasePath;
	protected final String removeBasePath;
	protected final int filterType;
	protected final String filterExePath;
	protected final String filterExeOptions;
	//protected final TikaParser tikaParser;
	
	public BaseFileHelper(DFLoadVO dfVO, Properties prop) throws IOException {
		this.dfVO = dfVO;
		this.prop = prop;
		
		// 원격지 베이스 경로 
		CustomClassFileHelper customClassItemVO = dfVO.custom.getFileHelper();
		if(customClassItemVO != null) {
			this.remoteBasePath = customClassItemVO.getRemoteBasePath();
			this.removeBasePath = customClassItemVO.getRemoveBasePath();
		} else {
			this.remoteBasePath = null;
			this.removeBasePath = null;
		}
		
		if(this.remoteBasePath == null || this.remoteBasePath.trim().length() == 0) {
			this.remotePathType = RPATHTYPE_ABSOLUTE;
			logger.info("- Remote path type: Absolute");
		} else if(".".equals(this.remoteBasePath)) {
			this.remotePathType = RPATHTYPE_RELATIVE;
			logger.info("- Remote path type: Relative");
		} else {
			this.remotePathType = RPATHTYPE_COMPOSITE;
			logger.info("- Remote path type: Composite (base path + db path)");
		}
		
		// 로컬 다운로드 경로
		String downloadBasePath = this.prop.getProperty("BULKER.PATH.DOWNLOAD");
		if(downloadBasePath == null) {
			logger.error("\"BULKER.PATH.DOWNLOA\" setting is not found.");
			throw new IllegalArgumentException("\"BULKER.PATH.DOWNLOA\" setting is not found.");
		} else if (!Files.exists(Paths.get(downloadBasePath))) {
			logger.error("Download base path is not exist: {}", downloadBasePath);
			throw new FileNotFoundException("Download base path is not exist");
		}
		
		// 다운로드 경로 = 다운로드 베이스 경로 + "df 파일명(확장자 제외)"
		this.downloadPath = Paths.get(downloadBasePath, dfVO.getDfName());
		if(!Files.exists(downloadPath)) {
			Files.createDirectory(downloadPath);
		}
		
		// 파일 필터 
		this.filterExePath = this.prop.getProperty("BULKER.FILTER.EXE");
		this.filterExeOptions = this.prop.getProperty("BULKER.FILTER.OPTIONS");
		if(this.filterExePath == null || this.filterExePath.trim().length() == 0) {
			this.filterType = FILTERTYPE_TIKA;
			//this.tikaParser = new TikaParser(); // tika parser instance
			logger.info("- File Filter: TICKA");
		} else {
			this.filterType = FILTERTYPE_EXTERANL;
			//this.tikaParser = null;
			logger.info("- File Filter: {} (option:{})", filterExePath, filterExeOptions);
		}
	}
	
	protected String getRemoteFullPath(String remotePath) {
		String remoteFullPath = null;
		String encRemotePath = null;
		
		if(this.removeBasePath != null && this.removeBasePath.trim().length() > 0) {
			if(remotePath.startsWith(this.removeBasePath)) {
				encRemotePath = remotePath.replaceFirst(this.removeBasePath, "");
			} else {
				encRemotePath = remotePath;
			}
		} else {
			encRemotePath = remotePath;
		}
		
		if(this.remotePathType == RPATHTYPE_ABSOLUTE) {
			remoteFullPath = encRemotePath;
		} else if (this.remotePathType == RPATHTYPE_RELATIVE) {
			if(encRemotePath.startsWith("/") || encRemotePath.startsWith("\\")) {
				remoteFullPath = encRemotePath.substring(1);
			} else {
				remoteFullPath = encRemotePath;
			}
		} else {
			remoteFullPath = Paths.get(this.remoteBasePath, encRemotePath).toString();
		}
		
		return remoteFullPath;
	}
	
	
	@Override
	abstract public void init() throws IOException;

	@Override
	abstract public File download(String remotePath, String downFileName) throws IOException;

	@Override
	abstract public void destroy();
	
	@Override
	public void filterText(File downloadFile, String fileExt, File filteredFile) throws Exception {
		if(this.filterType == FILTERTYPE_TIKA) {
			logger.debug("Try to Extract text from file by using tika.");
			// -------------------------------------------------------------------
			// RAY.Search.Bulker 2025.08 이전 버전 소스
			// extract text by using tika (result file name : original fileName + ".txt")
			//this.tikaParser.parseFile(downloadFile.getPath());
			// -------------------------------------------------------------------
			
			long fileSize = Files.size(downloadFile.toPath());
			if(fileSize > 0) {
				TikaExtractText.extractText(downloadFile, filteredFile, fileExt);
			} else {
				logger.warn("File size == 0, {}", downloadFile.getName());
			}
			
		} else {
			List<String> cmdList = new ArrayList<String>();
			logger.debug("Try to Extract text from file by using exteranl exe");
			// 1. exe
			cmdList.add(filterExePath);
			// 2. option
			logger.debug("filterExePath={}, filterExeOptions={}", filterExePath, filterExeOptions);
			if(filterExeOptions != null && filterExeOptions.length() > 0) {
				String [] cmdOptions = filterExeOptions.split(" ");
				for(String cmdOption : cmdOptions) {
					cmdList.add(cmdOption);
				}
			}
			//3. parameter
			cmdList.add(downloadFile.getPath());
			cmdList.add(filteredFile.getPath());
			
			//4. execute external filter.
			ExecuteUtils.executeCommand(cmdList);
		}
	}
	
	@Override
	@Deprecated
	public File filterText(File downloadFile, String fileExt) throws Exception {
		File filteredFile = new File(downloadFile.getPath() + ".txt");
		
		if(this.filterType == FILTERTYPE_TIKA) {
			logger.debug("Try to Extract text from file by using tika.");
			// -------------------------------------------------------------------
			// RAY.Search.Bulker 2025.08 이전 버전 소스
			// extract text by using tika (result file name : original fileName + ".txt")
			//this.tikaParser.parseFile(downloadFile.getPath());
			// -------------------------------------------------------------------
			
			long fileSize = Files.size(downloadFile.toPath());
			if(fileSize > 0) {
				TikaExtractText.extractText(downloadFile, filteredFile, fileExt);
			} else {
				logger.warn("File size == 0, {}", downloadFile.getName());
			}
			
		} else {
			List<String> cmdList = new ArrayList<String>();
			logger.debug("Try to Extract text from file by using exteranl exe");
			// 1. exe
			cmdList.add(filterExePath);
			// 2. option
			logger.debug("filterExePath={}, filterExeOptions={}", filterExePath, filterExeOptions);
			if(filterExeOptions != null && filterExeOptions.length() > 0) {
				String [] cmdOptions = filterExeOptions.split(" ");
				for(String cmdOption : cmdOptions) {
					cmdList.add(cmdOption);
				}
			}
			//3. parameter
			cmdList.add(downloadFile.getPath());
			cmdList.add(filteredFile.getPath());
			
			//4. execute external filter.
			ExecuteUtils.executeCommand(cmdList);
		}
		
		return filteredFile;
	}
	
	@Override
	public File decodeDrm(File sourceFile)  throws IOException{
		logger.debug("sourceFile={}", sourceFile);
		
		return sourceFile;
	}

}
