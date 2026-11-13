package com.rayful.bulk.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayful.bulk.attach.IAttachFileHelper;
import com.rayful.bulk.dao.NotesDAO;
import com.rayful.bulk.util.BulkerUtils;
import com.rayful.bulk.util.CodecUtil;
import com.rayful.bulk.util.Utils;
import com.rayful.bulk.vo.BulkerLogVO;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.DataLogVO;
import com.rayful.bulk.vo.df.ColumnItemVO;
import com.rayful.bulk.vo.df.ColumnVO;
import com.rayful.bulk.vo.df.NotesNsfItemVO;
import com.rayful.bulk.vo.df.QueryItemVO;

import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.MIMEEntity;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.Session;

public abstract class BaseBulkerNotesService implements IBulkerNotesService{
	private static final Logger logger = LoggerFactory.getLogger(BaseBulkerNotesService.class);
	public static final String KEYINC_QUERY_LASTINDEXDATE = "#data_source@last_doc_indexdate#";
	public static final String FILE_SEPERATOR = "\n\n";
	
	protected final NotesDAO notesDAO;
	
	protected final Properties prop;
	protected final DFLoadVO dfVO;
	
	protected final Path resultReadyPath;
	
	protected final IEtcReader etcReader;
	protected final IAttachFileHelper fileHelper;
	
	protected Map<String, String> allowExtMap = null;
	protected final boolean isExecDrmDecode;
	
	protected int keyReadCount = -1;
	protected DocumentCollection docCollection = null; 
	
	protected final int maxRows;
	protected final boolean printDetail;
	protected final int printInterval;
	
	public BaseBulkerNotesService(DFLoadVO dfVO, Properties prop) throws Exception {
		this.dfVO = dfVO;
		this.prop = prop;
		this.notesDAO = NotesDAO.getInstance(dfVO, prop);
		
		String resultBasePath = this.prop.getProperty("BULKER.PATH.RESULT");
		if(resultBasePath == null) {
			logger.error("\"BULKER.PATH.RESULT\" setting is not found.");
			throw new IllegalArgumentException("\"BULKER.PATH.RESULT\" setting is not found.");
		} else if (!Files.exists(Paths.get(resultBasePath))) {
			logger.error("Result base path does not exist: {}", resultBasePath);
			throw new FileNotFoundException("Result base path does not exist");
		}
		this.resultReadyPath = Paths.get(resultBasePath, "ready");
		
		String allowExt = this.prop.getProperty("BULKER.ALLOWED.FILEFORMAT");
		if(allowExt != null) {
			this.allowExtMap = new HashMap<String, String>();
			String [] arrExt = allowExt.toUpperCase().split(",");
			for(String ext : arrExt) {
				this.allowExtMap.put(ext, null);
			}
		}
		
		// 복호화 수행여부
		if("1".equals(this.prop.getProperty("BULKER.DRM.DEOCDE.EXEC"))) {
			isExecDrmDecode = true;
		} else {
			isExecDrmDecode = false;
		}
		
		// 2024.09.01 max rows 설정 추가
		this.maxRows = (Integer) this.prop.get("__RUNTIME.MAX_ROWS");
		
		// 2024.08.23 print detail 설정 추가
		this.printDetail = (Boolean) this.prop.get("__RUNTIME.PRINT_DETAIL");
		this.printInterval = (Integer) this.prop.get("__RUNTIME.PRINT_INTERVAL");
		
		this.etcReader = BulkerUtils.newInstanceEtcClass(dfVO, prop);
		this.fileHelper = BulkerUtils.newInstanceFileHelperClass(dfVO, prop);
	}
	
	@Override
	public void init() throws IOException {
		if(this.fileHelper != null) {
			this.fileHelper.init();
		}
	}
	
	@Override
	public void destroy() {
		if(this.fileHelper != null) {
			this.fileHelper.destroy();
		}
		
		if(this.notesDAO != null) {
			this.notesDAO.destory();
		}
		
		if(this.docCollection != null) { 
			try { this.docCollection.recycle(); } catch(NotesException e) { /* recycle 실패 무시(자원정리) */ }
		}
	}
	
	@Override
	public int getDocCount () throws NotesException {
		return this.docCollection != null ? this.docCollection.getCount() : 0;
	}
	
	@Override
	public boolean getKeyData(String indexMode, NotesNsfItemVO nsfItem) {
		boolean isSuccess = true;
		String sql = null;
		try {
			QueryItemVO queryItem = null;
			if("all".equalsIgnoreCase(indexMode)) {
				queryItem = this.dfVO.query.getKeyQuery();
				
				if(queryItem == null) {
					logger.error("key_query is null");
					throw new NullPointerException("key_query is null");
				}
				
				sql = queryItem.getSql();
			
			} else {
				queryItem = this.dfVO.query.getKeyIncQuery();
				
				if(queryItem == null) {
					logger.error("key_inc_query is null");
					throw new NullPointerException("key_inc_query is null");
				}
				
				sql = queryItem.getSql();
				sql = sql.replace(KEYINC_QUERY_LASTINDEXDATE, "'" + this.dfVO.datasource.getLastIndexDate() + "'");
			}
			
			if(this.docCollection != null) {
				try { this.docCollection.recycle(); } catch(NotesException e) { /* recycle 실패 무시(자원정리) */ }
				this.docCollection = null;
			}
			
			this.keyReadCount = 0;
			this.docCollection = this.notesDAO.selectQueryList(this.dfVO, sql, null, nsfItem.getConnectionId(), nsfItem.getName());
		} catch (NotesException e) {
			isSuccess = false;
			logger.error("Notes error -> {} : {}", e.id, e.getMessage(), e);
		} catch (Exception e) {
			isSuccess = false;
			logger.error(e.getMessage(), e);
		} 
		
		return isSuccess;
	}
	
	@Override
	public List<Document> nextKeyData() throws NotesException {
		List<Document> keyDataList = new ArrayList<>();
		
		int count = 0;
		Document currentDoc = null;
		
		if(this.keyReadCount < 0) {
			throw new IllegalStateException("Must call getKeyData() first.");
		} else if(this.keyReadCount == 0) {
			currentDoc = this.docCollection.getFirstDocument();
			if(currentDoc != null) {
				keyDataList.add(currentDoc);
				count ++;
				this.keyReadCount ++;
			} else {
				count = -1;
			}
		}
		
		while(count >= 0 && (currentDoc = this.docCollection.getNextDocument()) != null) {
			if(count >= dfVO.getMaxRows()) {
				break;
			}
			
			if(this.maxRows > 0 && this.keyReadCount >= this.maxRows) {
				break;
			}
			
			keyDataList.add(currentDoc);
			count ++;
			this.keyReadCount ++;
		}
		
		if(keyDataList.size() == 0) {
			if(this.docCollection != null) {
				try { this.docCollection.recycle(); } catch(NotesException e) { /* recycle 실패 무시(자원정리) */ }
				this.docCollection = null;
			}
		}
		
		return keyDataList.size() > 0 ? keyDataList : null;
	}
	
	@Override
	public List<Map<String, Object>> getData(List<Document> keyDataList, String indexMode, BulkerLogVO bulkerLogVO, int keyNo) throws NotesException {
		List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
		
		Map<String, Object> keyMap = null;
		Map<String, Object> dataMap = null;
		List<Map<String, Object>> fileDataList = null;
		DataLogVO dataLogVO = null;
		int idx = keyNo;
		String indexAction = "I";
		String startTime = null;
		String keyValue = null;
		
		List<ColumnItemVO> columnList = this.dfVO.column.getColumnList();
		
		for (Document doc : keyDataList) {
			keyMap = new HashMap<String, Object>();
			try {
					// Notes Document 객체로부터 데이터를 읽어온다. 
					dataMap = getDocument(doc, columnList);
					
					// 전체 키쿼리, 증분 키쿼리 결과가 key Column 존재여부 체크 (ROLE: key)
					//if(dataMap.containsKey(this.dfVO.column.getKeyColumnName())) {
					//	throw new NullPointerException("Key Query (inc) must contain ROLE='key' column - " + this.dfVO.column.getKeyColumnName());
					//}
					// 전체 키쿼리, 증분 키쿼리 결과가 IndexPatern Column 존재여부 체크 (ROLE: index_pattern)
					if(this.dfVO.isIncludeIndexname()) { 
						if(!dataMap.containsKey(this.dfVO.column.getIndexPatternColumnName())) {
							throw new NullPointerException("Key Query (inc) must contain ROLE='index_pattern' column - " + this.dfVO.column.getIndexPatternColumnName());
						}
					}
					
					// ROLE: key
					keyValue = Utils.objToString(dataMap.get(this.dfVO.column.getKeyColumnName()));
					if( keyValue == null || keyValue.trim().length() == 0) {
						throw new NullPointerException("key column value must be not empty");
					}
					keyMap.put(this.dfVO.column.getKeyColumnName(), keyValue);
					dataLogVO = new DataLogVO(keyMap, keyDataList.size(), idx, indexAction);
					
					// ROLE: index_action
					dataMap.put(this.dfVO.column.getIndexActionColumnName(), indexAction);
					
					// ROLE: nsf_name 
					if(this.dfVO.column.getNsfNameColumnName() != null) {
						dataMap.put(this.dfVO.column.getNsfNameColumnName(), this.notesDAO.getCurrentDbName());
					}
					
					// ROLE: index_time
					if(this.dfVO.column.getIndexTimeColumnName() != null) {
						startTime = this.prop.getProperty("__RUNTIME.START_ITME");
						if(startTime != null) {
							dataMap.put(this.dfVO.column.getIndexTimeColumnName(), startTime.replace(" ","T"));
						} else {
							dataMap.put(this.dfVO.column.getIndexTimeColumnName(), null);
						}
					}
					
					// DataQuery 결과를 컨버팅한다. (커스텀 로직 반영 가능)
					this.convertDataQueryReult(dataMap);
					
					// FileQuerey 실행
					if((this.dfVO.field.isIndexFileInfo() || this.dfVO.field.isIndexFileData())
							&& "I".equals(indexAction)) {
						fileDataList = this.getFileData(doc);
						
						if(fileDataList != null && fileDataList.size() > 0) {
							// es_file_info 의 값이 Y 일 경우
							if(this.dfVO.field.isIndexFileInfo()) {
								dataMap.put(this.dfVO.field.getFileInfoFieldName(), fileDataList);
							}
							
							// es_file_data 의 값이 Y 일 경우 (파일 내용 추출)
							//if(this.dfVO.field.isIndexFileData() && this.fileHelper != null) {
							//	this.mergeFileContentData(fileDataList, dataMap, dataLogVO.fileLog);
							//}
						}
					}
					
					// EtcQuery 실행
					if(this.etcReader != null) {
						this.etcReader.executeEtc(keyMap, dataMap, indexMode, dataLogVO.etcLog);
					}
					
					// 데이터를 결과 목록에 담는다.
					dataList.add(dataMap);
					bulkerLogVO.query.increaseSuccessCount();
			} catch (NotesException e) {
				logger.error(e.getMessage(), e);
				dataLogVO.setErrorMessage(e.toString());
				bulkerLogVO.query.increaseFailCount(e.toString());
				bulkerLogVO.nsf.setCompletFail(e.getMessage());
				throw e;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				dataLogVO.setErrorMessage(e.toString());
				bulkerLogVO.query.increaseFailCount(e.toString());
			} finally {
				if (doc != null) try { doc.recycle(); } catch(NotesException ne) { /* recycle 실패 무시(자원정리) */ }
			}
			
			if(!this.printDetail) {
				if(idx % this.printInterval == 0) {
					this.printLog(bulkerLogVO.query.getTotalCount(), idx);
				}
			} else {
				this.printDetailLog(dataLogVO, bulkerLogVO.query.getTotalCount(), idx, indexAction);
			}
			
			idx ++;
		} // for
		
		if(!this.printDetail) {
			if((idx-1) % this.printInterval != 0) {
				this.printLog(bulkerLogVO.query.getTotalCount(), idx-1);
			}
		}
		
		return dataList;
	}
	
	@Override
	abstract public List<Object> getResultData(List<Map<String, Object>> dataList, String indexMode);
	
	@Override
	abstract public File makeJson(List<Object> resultDataList, String indexType, BulkerLogVO bulkerLogVO) throws IOException;
	
	protected Map<String, Object> getDocument(Document doc, List<ColumnItemVO> columnList) throws NotesException {
		Map<String, Object> dataMap = new HashMap<> ();
		int iColumnType = 0;
		Item item = null;
		String readValue = null;
		
		for(ColumnItemVO columnItem : columnList) {
			logger.debug("column name: {}", columnItem.getName());
			
			// query="none" 설정 시 Document 객체로 부터 값을 읽지 않게한다.
			if(ColumnVO.QUERY_NONE.equals(columnItem.getQuery()) ) {
				logger.debug("pass... column name: {}", columnItem.getName());
				continue;
			}
			
			if(doc.hasItem(columnItem.getName())) {
				item = doc.getFirstItem( columnItem.getName() );
				iColumnType = item.getType();
				
				if ( iColumnType == Item.DATETIMES ) {
					//날짜TYPE
					DateTime dominoDateTime = item.getDateTimeValue();
					if ( dominoDateTime != null ) {
						Date date = dominoDateTime.toJavaDate();
						readValue = Utils.toFormatString( date );
					} else {
						readValue = null;
					}
				} else if  ( iColumnType == Item.RICHTEXT ) { 
					// RICHTEXT TYPE
					RichTextItem richTextItem = (RichTextItem)item;
					readValue = richTextItem.getFormattedText(false,0,0);
				} else if ( iColumnType == Item.MIME_PART ) {
					MIMEEntity mime = doc.getMIMEEntity();
					if ( mime != null ) {
						readValue = mime.getContentAsText();
						MIMEEntity child = mime.getFirstChildEntity();
						while (child != null) {
							mime = child;
							child = mime.getFirstChildEntity();
						}
						
						if (mime.getContentType().equals("text") &&
								mime.getContentSubType().equals("html")) {
							// 1727 : MIMEEntity.ENC_BASE64
							if (mime.getEncoding() == MIMEEntity.ENC_BASE64) {
								mime.decodeContent();
							}
							readValue = mime.getContentAsText();
							logger.debug("mime text: {}", mime.getContentAsText() );
						} else {
							logger.debug("no mime text");
						}
					}
				} else {
					readValue = item.getText();	
				}
				
				dataMap.put(columnItem.getName(), readValue);
			} else {
				logger.debug("Invalid column name: {}", columnItem.getName());
			}
		}
		
		return dataMap;
	}
	
	/**
	 * 파일정보 조회 쿼리를 수행한다.
	 * 
	 * @param dfVO DF 정보
	 * @param keyMap 쿼리 파라미터
	 * @return 파일정보 목록
	 * @throws NotesException 
	 */
	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getFileData(Document doc) throws NotesException {
		List<Map<String, Object>> fileDataList = new ArrayList<>();
		Map<String, Object> fileDataMap = null;
		
		Vector<Object> attachNames = null;
		String attachName = null;
		String attachExt = null;
		
		Session session = this.notesDAO.getCurrentSession();
		if(session == null) {
			throw new NullPointerException("Can't find current session.");
		}
		
		// 첨부명  정보를 조회한다.
		attachNames = session.evaluate( "@AttachmentNames", doc );
		logger.debug("@AttachmentNames={}", attachNames);
		if ( attachNames != null ) {
			for ( int j=0; j < attachNames.size(); j++ ) {
				attachName = attachNames.elementAt(j).toString();
				if ( attachName == null || attachName.trim().length() == 0) {
					continue;
				}
				attachExt = Utils.getFileExt(attachName);
				if(attachExt != null) {
					attachExt = attachExt.toLowerCase();
				}
				
				fileDataMap = new HashMap<>();
				fileDataMap.put("attach_nm", attachName);
				fileDataMap.put("attach_ext", attachExt);
				fileDataList.add(fileDataMap);
			}
		}
		
		return fileDataList;
	}
	
	/**
	 * 조회한 데이터를 변경처리한다.
	 * <p/>
	 * separator를 정의한 컬럼은 해당 구분자로 해서 배열 형식으로 변경한다. <br/>
	 * dataMap에 변경결과를 반영한다. <br/>
	 * 
	 * @param resultMap
	 */
	protected void convertDataQueryReult(Map<String, Object> dataMap) {
		List<ColumnItemVO> columnList = this.dfVO.column.getColumnList();
		String columnName = null;
		
		// separator
		String separator = null;
		String columnData = null;
		String [] splitData = null;
		List<String> listData = null; 
		// copy_to
		String [] copyColumns = null;
		// rename_to
		String renameColumn = null;
		
		for(ColumnItemVO columnItemVO : columnList) {
			columnName = columnItemVO.getName();
			
			///////// html tag 삭제 처리
			if(columnItemVO.isRemoveHtml()) {
				columnData = Utils.removeHtmlTag( String.valueOf(dataMap.get(columnName)) );
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// 불필요한 공잭 제거  처리
			if(columnItemVO.isRemoveSpace()) {
				columnData = Utils.removeSpace( String.valueOf(dataMap.get(columnName)) );
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// 해쉬함수를 적용한 값으로 대체
			if(columnItemVO.isEncodeHash()) {
				columnData = CodecUtil.encodeHash(String.valueOf(dataMap.get(columnName)));
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
			
			///////// BASE64 디코딩을 적용한 값으로 대체
			if(columnItemVO.isDecodeBase64()) {
				columnData = CodecUtil.decodeBase64(String.valueOf(dataMap.get(columnName)));
				// dataMap에  다시 저장한다.
				dataMap.put(columnName, columnData);
			}
				
			///////// split 처리 
			separator = columnItemVO.getSeparator();
			columnData = Utils.objToString( dataMap.get(columnName));
			if (separator != null && columnData != null) {
				listData = new ArrayList<> ();
				splitData = columnData.split(separator);
				
				// trim 처리
				for(int index = 0; index < splitData.length; index++) {
					if( splitData[index].trim().length() > 0) {
						listData.add(splitData[index].trim());
					}
				}
				
				// split 처리한 데이터를 다시 dataMap에 저장한다.
				dataMap.put(columnName, listData);
			}
			
			///////// copy_to 처리 
			copyColumns = columnItemVO.getCopyTo();
			if(copyColumns != null) {
				for(String copyToColumnName : copyColumns) {
					if(dataMap.containsKey(columnName)) {
						if(dataMap.containsKey(copyToColumnName)) {
							logger.debug("copyto ({} -> {}) is overwrited, copyto's column already exists.", columnName, copyToColumnName);
						}
						dataMap.put(copyToColumnName, dataMap.get(columnName));
					} else {
						logger.debug("copyto ({} -> {}) process is canceled. column's vaule = null", columnName, copyToColumnName);
					}
				}
			}
			
			///////// rename_to 처리 
			renameColumn = columnItemVO.getRenameTo();
			if(renameColumn != null) {
				if(!dataMap.containsKey(columnName)) {
					logger.debug("renameto ({} -> {}) process is canceled. source column({}) not exists.", columnName, renameColumn, columnName);
				} else if (dataMap.containsKey(renameColumn)) {
					logger.debug("renameto ({} -> {}) process is canceled. dest column({}) already exists.", columnName, renameColumn, renameColumn);
				} else {
					dataMap.put(renameColumn, dataMap.remove(columnName));
				}
			}
		}
	}
	
	/**
	 * fileList에 해당하는 파일을 다운(복사)받고 필터링하여 처리한 결과를 resultMap에 merge 한다.
	 * 
	 * @param fileDataList 파일정보 목록
	 * @param resultMap 색인 데이터 맵 (파일 처리 결과도 담음)
	 * @param dfVO df 정보
	 * @param fileLogVO 로깅 객체
	 * @throws IOException
	 */
	protected void mergeFileContentData(List<Map<String, Object>> fileDataList, Map<String, Object> resultMap, CountLogVO fileLogVO) throws IOException {
		StringBuilder sBuilder = null;
		
		String remoteFilePath = null;
		String showFileName = null;
		String downFileName = null;
		File downloadFile = null;
		File filteredFile = null;
		File sourceFile = null;
		
		boolean isAllowExt = false;
		
		if(fileDataList != null && fileDataList.size() > 0) {
			sBuilder = new StringBuilder();
			
			fileLogVO.setTotalCount(fileDataList.size());
			for (Map<String, Object> fileMap : fileDataList) {
				remoteFilePath = (String) fileMap.get(this.dfVO.column.getFilePathColumnName());
				if(remoteFilePath == null) {
					throw new NullPointerException("Can't read remoteFilePath: columnName=" + this.dfVO.column.getFilePathColumnName());
				}
				
				showFileName = (String) fileMap.get(this.dfVO.column.getFileNameColumnName()); // show 파일명
				if(showFileName == null) {
					throw new NullPointerException("Can't read showFileName: columnName=" + this.dfVO.column.getFileNameColumnName());
				}
				
				downFileName = Utils.getFileName(remoteFilePath); //물리 파일명
				
				isAllowExt = allowExt(showFileName); // 사용가능 확장자
				if(isAllowExt) {
					try {
						downloadFile = this.fileHelper.download(remoteFilePath, downFileName);
						if(downloadFile.exists()) {
							// 2023.08.07 정충열 : DRM 복호화를 수행한다.
							if(this.isExecDrmDecode) {
								sourceFile = this.fileHelper.decodeDrm(downloadFile);
							} else {
								sourceFile = downloadFile;
							}
							
							filteredFile = new File(sourceFile.getPath() + ".txt");
							
							// 파일로 부터 텍스트를 추출한다.
							this.fileHelper.filterText(sourceFile, Utils.getFileExt(showFileName), filteredFile);
							if(filteredFile.exists()) {
								logger.debug("Success to Extract text from file.");
								Utils.getFileText(filteredFile.getAbsolutePath(), "\n", sBuilder);
								sBuilder.append(FILE_SEPERATOR);
								fileLogVO.increaseSuccessCount();
							} else {
								logger.warn("Fail to Extract text from file [{}]", filteredFile.getPath());
								fileLogVO.increaseWarnCount("Fail to Extract text from file " + "[" + filteredFile.getPath() + "]");
							}
						} else {
							logger.warn("Can't find downloaded(copied) file [{}]", downloadFile.getPath());
							fileLogVO.increaseFailCount("Can't find downloaded(copied) file " + "[" + downloadFile.getPath() + "]");
						}
						
					} catch (Exception e) {
						logger.error("Occur exception: {} [{}]", e.toString(), remoteFilePath, e);
						fileLogVO.increaseFailCount(e.toString() + " [" + remoteFilePath + "]");
					} finally {
						if(sourceFile != null && sourceFile.exists()) try { sourceFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };
						if(downloadFile != null && downloadFile.exists()) try { downloadFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };
						if(filteredFile != null && filteredFile.exists()) try { filteredFile.delete(); } catch(Exception e) { /* 임시파일 삭제 실패 무시 */ };
					}
					
				} else {
					logger.warn("Unsupported file extension. [{}]", showFileName);
					fileLogVO.increaseWarnCount("Unsupported file extension. [" + showFileName + "]");
				}
			} // for
			
			if(sBuilder.length() > 0) {
				resultMap.put(this.dfVO.field.getFileDataFieldName(), sBuilder.toString());
			}
		}
	}
	
	/**
	 * 파일 필터링 처리 대상 확장자인지를 알아낸다.
	 * 
	 * @param fileName 파일명(논리 파일명)
	 * @return 대상확장자면 true, 아니면 false
	 */
	protected boolean allowExt(String fileName) {
		boolean isResult = false;
		if(this.allowExtMap == null) {
			// 설정되지 않았다면 무조건 대상 확장자로 간주
			isResult = true;
		} else {
			// 설정되지 되었더면 허용확장자 목록에 해당되는지 조사
			String fileExt = Utils.getFileExt(fileName);
			if(fileExt != null) {
				isResult = this.allowExtMap.containsKey(fileExt.toUpperCase());
			}
		}
		
		return isResult;
	}
	
	/**
	 * 색인수집 처리 로그를 간략히 출력한다.
	 * @param total
	 * @param idx
	 */
	protected void printLog(int total, int idx) {
		logger.info("{} / {} data is fetched.", idx, total);
	}
	
	/**
	 * 색인수집 처리 로그를 출력한다.
	 * 
	 * @param dataLogVO 로깅 객체
	 * @param idx 색인 키 데이터 카운트
	 * @param idx 색인 키 데이터 내 순번
	 * @param indexAction 색인 액션
	 */
	protected void printDetailLog(DataLogVO dataLogVO, int total, int idx, String indexAction) {
		String dataMsg = null;
		CountLogVO logVO = null;
		
		if(dataLogVO.getErrorMessage() == null) {
			dataMsg = "Success";
		} else {
			dataMsg = "Fail, Message : " + dataLogVO.getErrorMessage();
		}
		
		logger.info(BulkerUtils.LOG_DATA_LINE);
		logger.info("- [{}/{} - IndexAction: {}]  ", idx, total, indexAction);
		logger.info("- [key {}]  ", dataLogVO.getKey());
		logger.info("- [Data] {}", dataMsg);
		
		if(this.fileHelper != null) {
			// file 처리 로그 출력
			logVO = dataLogVO.fileLog;
			
			if(logVO.getTotalCount() > 0) {
				logger.info("- [File] Total: {}, Success: {}", logVO.getTotalCount(), logVO.getSuccessCount());
				if(logVO.getWarnCount() > 0) {
					logger.info("         Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
				}
				if(logVO.getFailCount() > 0) {
					logger.info("         Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
				}
			}
		}
		
		if(this.etcReader != null) {
			// etcReader 로그 출력
			logVO = dataLogVO.etcLog;
			
			if(logVO.getTotalCount() > 0) {
				logger.info("- [Etc ] Total: {}, Success: {}", logVO.getTotalCount(), logVO.getSuccessCount());
				if(logVO.getWarnCount() > 0) {
					logger.info("         Warn: {} {} ", logVO.getWarnCount(), logVO.getWarnMessage());
				}
				if(logVO.getFailCount() > 0) {
					logger.info("         Fail: {} {} ", logVO.getFailCount(), logVO.getFailMessage());
				}
			}
		}
		logger.info(BulkerUtils.LOG_DATA_LINE);
	}
}
