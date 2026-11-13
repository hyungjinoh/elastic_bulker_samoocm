package com.wooribank.service;

import com.rayful.bulk.service.BaseEtcReader;
import com.rayful.bulk.service.BaseEtcReader;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.QueryItemVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MenusEtcReader extends BaseEtcReader {
    private static final Logger logger = LoggerFactory.getLogger(MenusEtcReader.class);
    private static final int REQUIRED_GT_COUNT = 3; // 허용할 ‘>’ 개수

    public MenusEtcReader(DFLoadVO dfVO, Properties prop) throws Exception {
        super(dfVO, prop);
    }

    @Override
    public void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO) {
        String full_title=String.valueOf(dataMap.get("title"));
        int gtCount= StringUtils.countMatches(full_title, ">");

        //제목필드의 ‘>’값 3개 이상인경우만 처리
        if(gtCount != REQUIRED_GT_COUNT) {
            dataMap.clear();
            etcLogVO.increaseWarnCount("title '>' count : " + gtCount + ", expected : " + REQUIRED_GT_COUNT + "), skip key =" + keyMap);
            logger.warn("Skip index. 3레벨까지 정보만 색인처리합니다. title '>' count={}, key={}", gtCount, keyMap );
            return;
        }

        String real_title = "";
//재산관리시스템>업무용부동산>현황 및 공통>소유부동산현황

        String full_midPath=String.valueOf(dataMap.get("menuIdPath"));
        String real_midPath = "";

        if(full_title.startsWith("재산관리시스템")) {
            real_title=full_title.replaceFirst("재산관리시스템 > ", "");
                    dataMap.put("title", real_title);

            //대중소 (1,2,3레벨) 메뉴명 분리
            String[] mNm_levels=real_title.split(">");
            String lv1_mNm = mNm_levels[0].trim();
            String lv2_mNm = mNm_levels[1].trim();
            String lv3_mNm = mNm_levels[2].trim();

            dataMap.put("lv1_mmNm", (lv1_mNm!=null && !lv1_mNm.isEmpty()) ? lv1_mNm : "");
            dataMap.put("lv2_mmNm", (lv2_mNm!=null && !lv2_mNm.isEmpty()) ? lv2_mNm : "");
            dataMap.put("lv3_mmNm", (lv3_mNm!=null && !lv3_mNm.isEmpty()) ? lv3_mNm : "");
        }

        if(full_midPath.startsWith("MNU0000000000000000")) {
            real_midPath=full_midPath.replaceFirst("MNU0000000000000000 > ", "");
            dataMap.put("menuIdPath", real_midPath);

            // 대중소(1,2,3레벨) 메뉴Id 분리
            String[] mId_levels = real_midPath.split(">");
            String lv1_mId = mId_levels[0].trim();
            String lv2_mId = mId_levels[1].trim();
            String lv3_mId = mId_levels[2].trim();

            dataMap.put("lv1_mId", (lv1_mId !=null && !lv1_mId.isEmpty()) ? lv1_mId : "");
            dataMap.put("lv1_mId", (lv2_mId !=null && !lv2_mId.isEmpty()) ? lv2_mId : "");
            dataMap.put("lv1_mId", (lv3_mId !=null && !lv3_mId.isEmpty()) ? lv3_mId : "");
        }
    }
}