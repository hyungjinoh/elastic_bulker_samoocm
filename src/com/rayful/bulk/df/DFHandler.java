package com.rayful.bulk.df;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.vo.DFLoadVO;

public class DFHandler extends DefaultHandler {
	private static final Logger logger = LoggerFactory.getLogger(DFHandler.class);
	private String parentStartElement = null;
	private Attributes attribute = null;
	private StringBuffer currentValue = new StringBuffer();
	private Map<String, String> connectInfoMap = null; // DataSource 접속정보
	private Map<String, String> ftpInfoMap = null;
	private Map<String, String> fileHelperMap = null;
	DFLoadVO loadVO;
	
	public DFHandler(String dfName) {
		loadVO = new DFLoadVO(dfName);
	}
	
	/**
	 * 결과 리턴
	 * @return
	 */
	public DFLoadVO getResult() {
		return loadVO;
	}
	
	/**
	 * xml 시작
	 */
	@Override
	public void startDocument() {
		
	}
	
	/**
	 * xml 종료
	 */
	@Override
	public void endDocument() {
		
	}
	
	/**
	 * Element 시작
	 */
	@Override
	public void startElement(String url, String localName, String qName, Attributes att) {
		// reset tag value
		currentValue.setLength(0);
		this.attribute = null;
		
		if("target".equalsIgnoreCase(qName)) {
			loadVO.setIndexName(att.getValue("index_name"));
		} else if("result_file".equalsIgnoreCase(qName)) {
			loadVO.setResultFileName(att.getValue("file_name"));
			loadVO.setMaxRows(att.getValue("max_rows"));
			if(BulkerUtils.isUseIndexNamePattern(loadVO.getIndexName())) {
				loadVO.setIncludeIndexname(true);
			} else {
				loadVO.setIncludeIndexname(false);
			}
		} else if("source".equalsIgnoreCase(qName)) {
			String sourceType = att.getValue("type");
			if(sourceType == null) {
				sourceType = "rdbms";
			} else if (!"rdbms".equals(sourceType) && !"notes".equals(sourceType)) {
				throw new IllegalArgumentException("Invalid value: source - type attribute");
			}
			loadVO.setSourceType(sourceType);
			
		} else if("connect_info".equalsIgnoreCase(qName)) {
			this.parentStartElement = "connect_info";
			String dataSourceId = null;
			this.connectInfoMap = new HashMap<String, String>();
			if(att.getLength() > 0 && (att.getValue("id") != null || att.getValue("id").length() >  0)) {
				dataSourceId = att.getValue("id");
			} else {
				dataSourceId = "default";
			}
			this.connectInfoMap.put("id", dataSourceId);
			
		} else if("nsf_list".equalsIgnoreCase(qName)) {
			this.parentStartElement = "nsf_list";
			if(att.getValue("file_path") != null) {
				this.loadVO.nsfList.setListFile(att.getValue("file_path"));
			}
		} else if("nsf".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("bulker_service".equalsIgnoreCase(qName)) {
			String use = "yes"; // 속성 참조없이 Y로 설정한다. 
			int nThread = -1;
			int nThreadCopy = -1;
			if(att.getValue("nthread") == null) {
				nThread = 1;
			} else {
				try {
					nThread = Integer.parseInt(att.getValue("nthread"));
				} catch (NumberFormatException e) {
					logger.warn("nthread attr is not number format. then ignored.");
					nThread = 1;
				}
			}
			
			if(att.getValue("nthread_copy") == null) {
				nThreadCopy = nThread;
			} else {
				try {
					nThreadCopy = Integer.parseInt(att.getValue("nthread_copy"));
				} catch (NumberFormatException e) {
					logger.warn("nthread_copy attr is not number format. then ignored.");
					nThreadCopy = nThread;
				}
			}
			
			this.loadVO.custom.setBulkerService(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use)
					, nThread
					, nThreadCopy);
			
		} else if("qa_service".equalsIgnoreCase(qName)) {
			String use = att.getValue("use");
			if(use == null) use = "yes";
			
			this.loadVO.custom.setQaService(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use));
		} else if("index_service".equalsIgnoreCase(qName)) {
			String use = att.getValue("use");
			if(use == null) use = "yes";
			
			String async = att.getValue("async");
			if(async == null) async = "no";
			
			this.loadVO.custom.setIndexService(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use)
					, "yes".equalsIgnoreCase(async));
		} else if("entity_service".equalsIgnoreCase(qName)) {
			String use = att.getValue("use");
			if(use == null) use = "yes";
			
			this.loadVO.custom.setEntityService(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use));
		} else if("etc_reader".equalsIgnoreCase(qName)) {
			String use = att.getValue("use");
			if(use == null) use = "yes";
			
			this.loadVO.custom.setEtcReader(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use));
		} else if("etc_processor".equalsIgnoreCase(qName)) {
			String use = "yes"; // 속성 참조없이 Y로 설정한다. 
			
			this.loadVO.custom.setEtcProcessor(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use));
		} else if("file_helper".equalsIgnoreCase(qName)) {
			this.parentStartElement = "file_helper";
			this.fileHelperMap = new HashedMap<String, String>();
			this.fileHelperMap.put("use", att.getValue("use"));
			this.fileHelperMap.put("class_name", att.getValue("class_name"));
			this.fileHelperMap.put("remote_basepath", att.getValue("remote_basepath"));
			this.fileHelperMap.put("remove_basepath", att.getValue("remove_basepath"));
			this.fileHelperMap.put("etc_data", att.getValue("etc_data"));
			this.ftpInfoMap = new HashedMap<String, String>();
		} else if("bulker_crypt".equalsIgnoreCase(qName)) {
			String use = att.getValue("use");
			if(use == null) use = "yes";
			
			this.loadVO.custom.setBulkerCrypt(
					qName
					, att.getValue("class_name")
					, "yes".equalsIgnoreCase(use));
		} else if("column".equalsIgnoreCase(qName)) {
			String output = att.getValue("output");
			String removeHtml = att.getValue("removeHtml");
			String removeSpace = att.getValue("removeSpace");
			String encodeHash = att.getValue("encodeHash");
			String decodeBase64 = att.getValue("decodeBase64");
			if(output == null) output = "yes";
			if(removeHtml == null) removeHtml = "no";
			if(removeSpace == null) removeSpace = "no";
			if(encodeHash == null) encodeHash = "no";
			
			this.loadVO.column.putColumnItem(
					att.getValue("name")
					, att.getValue("role")
					, "yes".equalsIgnoreCase(output)
					, "yes".equalsIgnoreCase(removeHtml)
					, "yes".equalsIgnoreCase(removeSpace)
					, "yes".equalsIgnoreCase(encodeHash)
					, "yes".equalsIgnoreCase(decodeBase64)
					, att.getValue("separator")
					, att.getValue("rename_to")
					, att.getValue("copy_to")
					, att.getValue("query")
					);
		} else if("field".equalsIgnoreCase(qName)) {
			String index = att.getValue("index");
			if(index == null) index = "yes";
			
			this.loadVO.field.putFieldItem(
					att.getValue("name")
					, att.getValue("role")
					, "yes".equalsIgnoreCase(index));
		} else if("data_source".equalsIgnoreCase(qName)) {
			String deleteNotIndexed = att.getValue("delete_not_indexed");
			String printDetail = att.getValue("print_detail");
			if(deleteNotIndexed == null) deleteNotIndexed = "no";
			if(printDetail == null) printDetail = "no";
			
			loadVO.datasource.setLastIndexDate(att.getValue("last_doc_indexdate"));
			loadVO.datasource.setDeleteNotIndexed("yes".equalsIgnoreCase(deleteNotIndexed));
		} else if("key_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("key_inc_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("update_queue_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("data_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("file_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		} else if("etc_query".equalsIgnoreCase(qName)) {
			this.attribute = new AttributesImpl(att);
		}
	}
	
	/**
	 * Element 종료
	 */
	@Override
	public void endElement(String url, String localName, String qName) {
		
		if("connect_info".equalsIgnoreCase(qName)) {
			this.loadVO.putConnectInfo(this.connectInfoMap);
			this.connectInfoMap = null;
			this.parentStartElement = null;
		} else if("nsf_list".equalsIgnoreCase(qName)) {
			this.parentStartElement = null;
		} else if("key_query".equalsIgnoreCase(qName)) {
			String cursorUse = this.attribute.getValue("cursor_use");
			String relayUse = this.attribute.getValue("relay_use");
			String dataSource = "notes".equals(this.loadVO.getSourceType())? "notes": this.attribute.getValue("dataSource");
			this.loadVO.query.putQuery(qName, this.currentValue.toString(), dataSource, "yes".equals(cursorUse), "yes".equals(relayUse));
		} else if("key_inc_query".equalsIgnoreCase(qName)) {
			String cursorUse = this.attribute.getValue("cursor_use");
			String dataSource = "notes".equals(this.loadVO.getSourceType())? "notes": this.attribute.getValue("dataSource");
			this.loadVO.query.putQuery(qName, this.currentValue.toString(), dataSource, "yes".equals(cursorUse), false);
		} else if("update_queue_query".equalsIgnoreCase(qName)) {
			this.loadVO.query.putQuery(qName, this.currentValue.toString(), this.attribute.getValue("dataSource"));
		} else if("data_query".equalsIgnoreCase(qName)) {
			this.loadVO.query.putQuery(qName, this.currentValue.toString(), this.attribute.getValue("dataSource"));
		} else if("file_query".equalsIgnoreCase(qName)) {
			this.loadVO.query.putQuery(qName, this.currentValue.toString(), this.attribute.getValue("dataSource"));
		} else if ("etc_query".equalsIgnoreCase(qName)) {
			this.loadVO.query.putEtcQuery(this.attribute.getValue("id"), this.currentValue.toString(), this.attribute.getValue("dataSource"));
		} else if ("file_helper".equalsIgnoreCase(qName)) {
			String use = this.fileHelperMap.get("use");
			if(use == null) use = "yes";
			
			this.loadVO.custom.setFileHelpr(
					qName
					, this.fileHelperMap.get("class_name")
					, "yes".equalsIgnoreCase(use)
					, this.fileHelperMap.get("remote_basepath")
					, this.fileHelperMap.get("remove_basepath")
					, this.fileHelperMap.get("etc_data")
					, this.ftpInfoMap );
			
			this.fileHelperMap = null;
			this.ftpInfoMap = null;
			this.parentStartElement = null;
		} else {
			if("connect_info".equalsIgnoreCase(this.parentStartElement)) {
				if( "rdbms".equalsIgnoreCase(qName) 
						|| "driverClass".equalsIgnoreCase(qName)
						|| "url".equalsIgnoreCase(qName)
						|| "username".equalsIgnoreCase(qName)
						|| "password".equalsIgnoreCase(qName)){
					this.connectInfoMap.put(qName, this.currentValue.toString());
				}
			} else if("nsf_list".equalsIgnoreCase(this.parentStartElement)) {
				if("nsf".equalsIgnoreCase(qName)) {
					this.loadVO.nsfList.add(this.attribute.getValue("dataSource"), this.currentValue.toString());
				}
			} else if("file_helper".equalsIgnoreCase(this.parentStartElement)) {
				if("url".equalsIgnoreCase(qName)
						|| "port".equalsIgnoreCase(qName)
						|| "username".equalsIgnoreCase(qName)
						|| "password".equalsIgnoreCase(qName)) {
					this.ftpInfoMap.put(qName, this.currentValue.toString());
				}
			} 
		}
	}
	
	/**
	 * Element의 값
	 * 인식된 문자의 각 세그먼트에 대해서 호출
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		currentValue.append(ch, start, length);
	}

}
