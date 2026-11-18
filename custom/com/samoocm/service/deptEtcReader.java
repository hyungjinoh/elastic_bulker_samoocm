package com.samoocm.service;

import com.rayful.bulk.service.BaseEtcReader;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;


/**
 * ecm_projects.xml 전용 EtcReader.
 * <p/>
 * etc_query(participants_query)로 프로젝트 참여자 목록을 조회하여,
 * 컬럼별로 "병렬 리스트(user_id_list[i] ↔ user_name_list[i] ↔ ... 는 같은 참여자)"를 구성해 dataMap에 채운다.
 * <p/>
 * 색인 일관성 보장 규칙:
 *  1) 참여자가 0명이든, 쿼리 자체가 실패하든, PARTICIPANT_FIELDS 에 정의된 ES 필드는
 *     반드시 dataMap에 키가 존재한다 (값이 없으면 null, 있으면 List).
 *  2) 한 참여자 행(row) 내에서 특정 컬럼 값이 null이어도 해당 위치에 null을 그대로 채워서
 *     리스트 길이(참여자 수)와 인덱스 정합성을 깨지 않는다.
 */
public class deptEtcReader extends BaseEtcReader {
    private static final Logger logger = LoggerFactory.getLogger(deptEtcReader.class);

    private static final String ETC_QUERY_ID = "folderPath_Query";

    public deptEtcReader(DFLoadVO dfVO, Properties prop) throws Exception {
        super(dfVO, prop);
    }

    @Override
    public void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO) {


        try {

                String folderPath = (String) dataMap.get("folderPath");
                dataMap.put("folderPath", toDisplayFolderPath(folderPath));

                String fileContent = (String) dataMap.get("fileContent");
//                fileContent="\n\n";
                dataMap.put("fileContent", cleanupFileContent(fileContent));
                etcLogVO.increaseSuccessCount();

        } catch (Exception e) {
            logger.error("[new_projectsEtcReader] folderPath_Query 처리 실패. " , e);
            etcLogVO.increaseFailCount("folderPath_Query failed." + ", message=" + e.getMessage());
        }
    }

    // 폴더경로를 > 형식으로 구분하여 처리함
    public String toDisplayFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return folderPath;
        }
        return folderPath.replace("/", ">");
    }

    // 폴더경로를 > 형식으로 구분하여 처리함
    public String cleanupFileContent(String fcnt) {
        if (fcnt == null || fcnt.isEmpty()) {
            return fcnt;
        }

        if(fcnt.equals("\n\n")) {
            return "";
        }
        return fcnt.replace("\\n", " ");
    }
}