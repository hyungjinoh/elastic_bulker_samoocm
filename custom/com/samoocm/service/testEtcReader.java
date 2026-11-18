package com.samoocm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.rayful.bulk.service.BaseEtcReader;
import com.rayful.bulk.vo.CountLogVO;
import com.rayful.bulk.vo.DFLoadVO;
import com.rayful.bulk.vo.df.QueryItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class testEtcReader extends BaseEtcReader {
    private static final Logger logger = LoggerFactory.getLogger(testEtcReader.class);

    private static final String ETC_QUERY_ID = "participants_query";

    /**
     * participants_query의 SELECT 별칭(ES 색인 필드명)과,
     * ResultSet(Map) 에서 그 값을 꺼낼 때 쓸 키를 1:1로 매핑한다.
     * (JdbcDAO.selectQueryList 가 리턴하는 Map의 key는 쿼리의 컬럼 별칭과 동일)
     * <p/>
     * idx_projects 인덱스 매핑이 "dynamic": "strict" 이므로, 여기 나열된 필드는
     * 반드시 매핑에도 동일한 이름으로 먼저 정의돼 있어야 한다 (account, group_title, position,
     * task, man_hour, authority, sort_order 7개 필드를 매핑에 추가 완료한 상태를 전제로 함).
     * 필드를 추가/삭제할 땐 매핑과 이 배열을 항상 같이 맞춰야 한다.
     */
    private static final String[] ES_FIELDS = {
            "user_id",
            "user_name",
            "account",
            "group_title",
            "position",
            "task",
            "man_hour",
            "authority",
            "sort_order"
    };

    public testEtcReader(DFLoadVO dfVO, Properties prop) throws Exception {
        super(dfVO, prop);
    }

    @Override
    public void executeEtc(Map<String, Object> keyMap, Map<String, Object> dataMap, String indexMode, CountLogVO etcLogVO) {
        // 1) 선제 처리: 어떤 경우든 필드 자체는 반드시 존재하도록 전부 null로 초기화
        for (String field : ES_FIELDS) {
            dataMap.put(field, null);
        }

        try {
            QueryItemVO queryItemVO = dfVO.query.getEtcQuery(ETC_QUERY_ID);
            if (queryItemVO == null) {
                throw new NullPointerException("Can't find etc_query [" + ETC_QUERY_ID + "]");
            }

            List<Map<String, Object>> rows =
                    this.jdbcDAO.selectQueryList(dfVO, queryItemVO.getSql(), keyMap, queryItemVO.getConnectionId());

            // 2) 참여자가 0명이면 null 유지 그대로 두고 성공 처리
            if (rows == null || rows.isEmpty()) {
                etcLogVO.increaseSuccessCount();
                return;
            }

            // 3) 필드별 병렬 리스트 생성 (리스트 길이 = 참여자 수, 각 인덱스는 동일 참여자를 가리킴)
            Map<String, List<Object>> fieldListMap = new HashMap<>();
            for (String field : ES_FIELDS) {
                fieldListMap.put(field, new ArrayList<>(rows.size()));
            }

            for (Map<String, Object> row : rows) {
                for (String field : ES_FIELDS) {
                    // row.get(field) 가 null이어도(해당 참여자의 특정 컬럼 값이 null)
                    // 그대로 null을 add하여 인덱스 정합성을 유지한다.
                    fieldListMap.get(field).add(row.get(field));
                }
            }

            // 4) 선제 세팅해둔 null을 실제 리스트로 덮어쓴다.
            for (String field : ES_FIELDS) {
                dataMap.put(field, fieldListMap.get(field));
            }

            etcLogVO.increaseSuccessCount();

        } catch (Exception e) {
            // 실패해도 PARTICIPANT_FIELDS는 이미 null로 채워진 상태이므로 색인 자체(필드 존재)는 깨지지 않는다.
            String docid = String.valueOf(keyMap.get("docid"));
            logger.error("[projectsEtcReader] participants_query 처리 실패. docid=" + docid, e);
            etcLogVO.increaseFailCount("participants_query failed. docid=" + docid + ", message=" + e.getMessage());
        }
    }
}