package com.rayful.bulk.vo.df;

import java.util.List;
import java.util.Map;

import com.rayful.bulk.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class NotesNsfListVO {
	private final List<NotesNsfItemVO> nsfList;
	private File listFile;
	
	public NotesNsfListVO()  {
		this.nsfList = new ArrayList<> ();
		this.listFile = null;
	}

	public void setListFile(String filePath) {
		this.listFile = new File(filePath);
	}
	
	public boolean hasListFile() {
		return this.listFile != null? true: false;
	}
	
	public File getListFile() {
		return this.listFile;
	}
	
	public void loadFile() throws IOException {
		 if(this.listFile == null) {
			throw new NullPointerException("Not support loadFile method: listFile == null"); 	
		 }
		 
		 if(!this.listFile.exists()) {
			throw new FileNotFoundException("Not exists nsf_list_file: " + this.listFile.getAbsolutePath() );
		 }
		 
			 Map<String, Object> fileMap = Utils.getMapFromJsonFile(this.listFile);

			 String dataSource;
			 String nsfName;
			 @SuppressWarnings("unchecked")
			 List<Map<String, String>> nsfList = (List<Map<String, String>>) fileMap.get("nsf_list");
			 for(Map<String, String> nsfMap : nsfList) {
				 dataSource = nsfMap.get("dataSource");
				 nsfName = nsfMap.get("name");
				 if(dataSource == null || nsfName == null) {
					 throw new NullPointerException("Must be not null: dataSource=" + dataSource + ", name=" + nsfName);
				 }
				 
				 this.nsfList.add(new NotesNsfItemVO(dataSource, nsfName));
			 }

	}
	
	public void add(String connectionId, String name) {
		if(this.listFile != null) {
			throw new NullPointerException("Not support add method: listFile != null"); 	
		}
		 
		NotesNsfItemVO nsf = new NotesNsfItemVO(connectionId, name);
		this.nsfList.add(nsf);
	}
	
	public List<NotesNsfItemVO> getList() {
		return this.nsfList;
	}
	
	public int size() {
		return this.nsfList.size();
	}
	
	@Override
	public String toString() {
		return "NotesNsfListVO [nsfList=" + nsfList + "]";
	}
}
