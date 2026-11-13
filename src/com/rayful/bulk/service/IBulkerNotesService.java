package com.rayful.bulk.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.df.NotesNsfItemVO;

import lotus.domino.Document;
import lotus.domino.NotesException;

public interface IBulkerNotesService {

	void init() throws IOException ;
	
	void destroy();
	
	int getDocCount () throws NotesException;
	
	boolean getKeyData(String indexMode, NotesNsfItemVO nsfItem);
	
	List<Document> nextKeyData() throws NotesException;
	
	List<Map<String, Object>> getData(List<Document> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo) throws NotesException;
	
	List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode);

	File makeJson(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws Exception;
}
