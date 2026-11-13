package com.wooribank.service;

import com.rayful.bulk.service.BaseEtcReader;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FilesEtcReader extends BaseEtcReader {
    private static final Logger logger = LoggerFactory.getLogger(FilesEtcReader.class);
    private static final Set<String> IMG_EXTS = new HashSet<>(Arrays.asList("jpg","jpeg","png","gif","bmp","webp","tiff","svg"));

    public FilesEtcReader(DFLoadVO dfVO, Properties prop) throws Exception {
        super(dfVO, prop);
    }

    @Override
    public void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO) {
        String fileName=String.valueOf(dataMap.get("title"));
        String fileType = fileTypeCheck(fileName);
        dataMap.put("fileType", fileType);

    }

    //입력받은 파일타입에 따라 etcFile,imgFile,docFile값 리턴
    public String fileTypeCheck(String fileName){
        if (fileName == null || fileName.isEmpty()) {
            return "etcFile";
        }

        //확장자가 없거나 파일명이 ‘.’으로 시작해서 확장자 구분이 무의미한 경우 처리(.gitignore 등)
        int lastDotIndex = fileName.lastIndexOf(".");
        if(lastDotIndex==-1||lastDotIndex==fileName.length()-1||lastDotIndex==0) {
            return "etcFile";
        }

        String extension=fileName.substring(lastDotIndex+1).toLowerCase();

        if(IMG_EXTS.contains(extension)) {
            return "imgFile";
        } else {
            return "docFile";
        }
    }
}