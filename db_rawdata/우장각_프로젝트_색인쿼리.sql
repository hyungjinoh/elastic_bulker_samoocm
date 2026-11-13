/* =====================================================================================
   우장각 프로젝트 색인쿼리 (Elasticsearch Bulk 색인용)
   -------------------------------------------------------------------------------------
   작성 근거
     1) v_ecm.sql           : METAECM 스키마 뷰 정의/코멘트 (Metanet 제공)
     2) UI_Filter 화면      : 검색 도구 / 프로젝트 필터(기본·상세) / 세부 필터(사업·시설·기술)
     3) w1~w7 (DRM 보호로 미확인) : 실제 로우데이터 매핑은 우장각 워크시트 레이아웃 스크린샷으로 대체

   매핑 신뢰도 표기
     ✅ 확정   : v_ecm.sql 코멘트의 "(화면 항목: ...)" 문구로 UI 라벨이 명시되어 1:1 확인됨
     ▲ 추정   : 코멘트/명칭으로 유력하나 화면 라벨 정확 표기가 없어 운영 DB 확인 권장
   ===================================================================================== */


/* =====================================================================================
   [1] 화면 항목 ↔ 컬럼 매핑 요약 (주석 — 실제 실행 구문 아님)
       2차 근거: 우장각 워크시트 레이아웃 실제 스크린샷(샘플데이터1~3 포함, 7장)
   -------------------------------------------------------------------------------------
   ■ 검색 도구 (문서 레벨, V_ECM_FILES 기준)
     키워드 포함/제외         : DOC_NAME, FILE_NAME, DOC_DESCRIPTION 등 풀텍스트 ✅
     작성자                   : DOC_CREATOR_NAME / DOC_CREATOR_ID ✅
     검색대상 파일명/폴더명/내용 : FILE_NAME / FOLDER_PATH / (내용=본문 OCR·텍스트, 별도 파이프라인) ✅
     기간(시작일~종료일)       : DOC_CREATED_DATE ✅
     파일형식                 : FILE_EXTENSION ✅
     폴더경로                 : FOLDER_PATH, FOLDER_ID ✅

   ■ 프로젝트 필터 - 기본
     프로젝트 번호            : PROJECTNO ✅
     등록명                   : PJTNAME + MISNAME 조합 표시 ✅ (워크시트에 "PJT_NAME, MIS_NAME"로 명기)
     계약명                   : CONTRACTNAME ✅
     준공명                   : MISNAME ✅
     보안등급                 : SECURITYLEVEL, SECURITYLEVEL_NM ✅
     종속 프로젝트 포함        : SUBPROJECTNO(종속프로젝트 번호), SUBMISNAME(종속프로젝트 명) ✅
     표준폴더1 / 구분타이틀    : SITEGUBUN1 / PROJECTGUBUNTITLE ✅
     표준폴더2 / 구분타이틀    : PROJECTGUBUN2 / PROJECTGUBUN2TITLE ✅
     현장구분                 : SITEGUBUN2 ✅

   ■ 프로젝트 필터 - 상세
     계약여부                 : ✅ 파생값 확정 — CONTRACT_DATE(계약일자, FW_PROJECT_INFO_LAYOUT에 USE_YN='Y'로
                                 실존) IS NOT NULL 여부로 'Y'/'N' 산출. 별도 Y/N 코드 컬럼은 없음
     우장각 진행상태           : PROGRESS / PROGRESS_NM ✅ (워크시트 "진행상황→PROGRESS", 샘플값 "진행")
     현장개설여부              : SITEOPENYN ✅
     업무구분                 : WORKGUBUN / WORKGUBUN_NM ✅
     수주형태                 : PARTICIPATIONINFO1(+ETC) ✅ (샘플 "기타, 수의계약")
     용역구분                 : SERVICEGUBUN / SERVICEGUBUN_NM ✅
     (용역형태 일반/상세)      : PARTICIPATIONINFO3(일반) / PARTICIPATIONINFO4(상세) ✅ — 워크시트 전용, UI_Filter 화면엔 별도 노출 안 됨
     공동이행방식              : ▲ 최유력후보 PARTICIPATIONINFO5_CO(단독/공동, CODE, "사업>참여정보>
                                 용역형태(상세)" 하위) — 정확한 라벨 문자열은 "공동이행방식"이 아니라
                                 "단독/공동"이지만 기능상 동일 분류로 판단됨. (PARTICIPATIONINFO2는
                                 "참여/당선"으로 다른 의미 확인되어 후보에서 제외)
     위치                     : REGIONLEVEL1_NM(용도지역) 또는 CITYBEFORE_NM(시/도, 워크시트 "광주광역시" 등) ✅
     용도시설 구분             : USEADD / USEADD_NM ✅
     주요자료                 : MATERIALTYPEA / MATERIALTYPEA_NM ✅
     참여자(내부)              : ✅ 그룹 확정 — DEPARTMENTPMNM(대표PM), GENERALMANAGERNM(단장) 등 이름 필드 묶음
                                 (단일 코드값 아님. 워크시트에 더 많은 하위 역할이 이어질 수 있음)
     참여자(외부)              : ✅ 그룹 확정 — CONSUMER(수요자), CONTRACTORDERER(계약발주자),
                                 ORIGINALORDERER(원발주자), ADMITTER(허가권자), INSPECTOR(감독자),
                                 CONSTRUCTIONPM(건설사업관리자), PROJECTOPERATOR(시행자), DESIGNER(설계자),
                                 BUILDER(시공자), CONSULTING(컨설팅), JOINTCONTRACTOR(공동수행사),
                                 EXTERNALETC(기타) — 12개 이름 필드 묶음 (단일 코드값 아님)

   ■ 세부 필터 - 사업
     사업분야                 : ▲ 유력 후보 ENTERPRISETYPE(사업유형) — FW_PROJECT_INFO_LAYOUT/
                                 V_ECM_PROJECT_DETAILS 전수조사에도 "사업분야"라는 라벨 자체가 없어
                                 이것이 사실상 최종 후보. 운영 화면에서 직접 대조 확인 권장
     (사업유형 하위) 시행방식  : DIVISIONBUSINESS2 ✅ (UI_Filter엔 없고 워크시트 전용)
     참여단계                 : PARTICIPATIONSTEP / _NM ✅
     건축행위                 : DIVISIONBUSINESS3 / _NM ✅ (샘플 "리모델링", "신축")
     사업비                   : TOTALEXPENSES(원) / TOTALEXPENSESP(평당) ✅
     설계가격                 : DESIGNEXPENSES(원) / DESIGNEXPENSESP(평당) ✅
     최초공사비               : INITIALCONSTRUCTIONEXPENSES(원) / _P(평당) ✅
     최종공사비               : FINALCONSTRUCTIONEXPENSES(원) / _P(평당) ✅
     공정                     : ✅ 단일 코드 아님 — "단계별일정" 그룹(설계전/기본설계/실시설계/구매조달/
                                 시공/시공후 단계의 착수일 DESIGNSTART_DATE 등·종료일·기간) +
                                 "CP(세부공정)" 그룹(가설·흙막이·터파기·기초·지하골조·지상골조·마감·
                                 준공청소시운전·기타) + "도서납품일" 그룹. 필터용 단일값이 필요하면
                                 아래 [4-A]/[4-B]/[4-C]에 공통으로 추가한 CASE 파생컬럼("currentProcessStage") 참고

   ■ 세부 필터 - 시설
     건축물의 용도             : USELEVEL1 / USELEVEL1_NM (+ETC) ✅
     면적                     : AREA(대표/분류코드, 필터용) ▲ — 워크시트엔 AREA 행이 안 보이고 대신
                                 SITEAREA/BUILDINGAREA/TOTALFLOORAREA(세부 실측값, ㎡·평)만 확인됨.
                                 필터 드롭다운이 범위选択이라면 AREA가 맞고, 실측값 기반이면 세부 컬럼 사용
     규모                     : SCALE(대표/분류코드, 필터용) ▲ — 마찬가지로 워크시트엔 GROUNDFLOORS/
                                 UNDERGROUNDFLOORS/MAXDEPTHEXCAVATION(세부 실측값)만 확인됨
     등급/인증                : CONFIRM(대표코드) ▲ + 실제 데이터는 GREENPC/MC(녹색건축),
                                 BUILDINGENERGYPC/MC(에너지효율), ZEROENERGYPC/MC(제로에너지) 등
                                 개별 인증 그룹에 값이 채워짐(워크시트로 확인) ✅
     설계조건                 : DESIGNCONDITION / _NM ✅

   ■ 세부 필터 - 기술
     구조                     : ▲ 추정 — 워크시트 섹션명은 "구조재료"(그룹), 하위 상세(FIELD_NAME)는
                                 캡처 범위 밖. STRUCTURALMETHODG(구조형식)/STRUCTURALMETHODC(구조재료)
                                 /STRUCTURALMETHODU(지하)/STRUCTURALMETHODS(강구조) 중 확인 필요
     재료기술                 : ▲ 추정 그룹 헤더. 토목/기계설비/전기통신/소방설비 하위 4개 컬럼 존재(아래)
     건축                     : BUILDINGMETHOD(건축공사) ▲ (UI는 "건축"만 표기, 라벨 축약 여부 확인)
     토목                     : MATERIALTECHCIVIL(재료기술 토목) / _NM ✅
     기계설비                 : MATERIALTECHMACHINE(재료기술 기계설비) / _NM ✅
     전기통신                 : MATERIALTECHELECTRIC(재료기술 전기통신) / _NM ✅
     소방설비                 : MATERIALTECHFIRE(재료기술 소방설비) / _NM ✅
     전문분야                 : PROFESSIONALMETHOD / _NM ✅
     (참고) 공종              : WORKINGTYPE / _NM ✅ — 워크시트 "공종>가설공사" 섹션, UI_Filter엔 미노출
   ===================================================================================== */


/* =====================================================================================
   [2] ACL(권한) 해석 공통 CTE
   -------------------------------------------------------------------------------------
   권한 모델 요약 (v_ecm.sql 코멘트 기준)
     - 문서/폴더/프로젝트는 각각 ACL_ID 하나를 가진다
       (V_ECM_FILES.DOC_ACL_ID, V_ECM_FOLDERS.ACL_ID, V_ECM_PROJECTS.ACL_ID)
     - ACL_ID → V_ECM_ACL_READERS 에서 PRINCIPAL_TYPE/PRINCIPAL_ID 별 CAN_BROWSE/CAN_READ 를 얻는다
         USER          : PRINCIPAL_ID = 사용자 ID 그 자체
         DEPT          : PRINCIPAL_ID = 부서 ID → V_ECM_USER_GROUPS 로 전개
         PROJECT_GROUP : PRINCIPAL_ID = 프로젝트 그룹 ID → V_ECM_PROJECT_GROUP_USERS 로 전개
         ALIAS         : PRINCIPAL_ID = WORLD(전체) 등 별칭 → 전체 사용자/별칭 그룹으로 전개
         ROLE          : PRINCIPAL_ID = 역할 ID → FW_USER_ROLE_LINKS 로 전개
         OWNER         : 문서를 올린 사람 1명 (문서별로 달라짐 → 문서 조인 시 별도 처리)
         OGROUP        : 문서를 올린 사람의 부서원 전체 (문서별로 달라짐 → 문서 조인 시 별도 처리)
     - V_ECM_USER_PRINCIPALS 는 "사용자 → 해당하는 모든 PRINCIPAL_TYPE/ID" 역매핑 뷰로,
       색인 시점이 아니라 [6] 검색 시점에 로그인 사용자 1명의 태그 집합을 구할 때 쓴다.
     - 공유(V_ECM_SHARES)는 ACL과 별도로 추가 열람자를 부여하므로 반드시 합쳐서 봐야 한다.

   본 CTE는 OWNER/OGROUP 플래그만 판단한다(둘 다 사용자 전개 없이 ACL_ID 단위 존재 여부만
   필요). 그 외 일반 태그(USER/DEPT/PROJECT_GROUP/ALIAS/ROLE)는 메인 SELECT에서 ACL_ID로
   바로 필터링해 가져온다(아래 [4-A]/[4-B]/[4-C] 참고). 아래 사항은 더 이상 유효하지 않으니 반영 금지:
   "V_ECM_ACL_READERS 와 V_ECM_USER_PRINCIPALS 를 조인해 사용자 목록을 전개" — 이는
   ORA-01489(문자열 길이 초과)의 원인이 된 구버전 설계이며 현재는 사용하지 않는다.
   ===================================================================================== */
/* -------------------------------------------------------------------------------------
   설계 변경 이력: ACL을 "구체적 USER_ID 목록"으로 펼치는 대신 "권한 태그(TYPE:ID)" 그대로 저장
   -------------------------------------------------------------------------------------
   최초 버전은 ACL_ID 당 열람 가능한 USER_ID를 전부 전개해 '|' 문자열로 저장했으나,
   ALIAS:WORLD(전체 사용자)처럼 대상 인원이 수천 명인 권한 라인을 만나면 LISTAGG 결과가
   Oracle 문자열 길이 한계를 넘겨 ORA-01489가 발생한다. (근본 원인: 색인 시점에 이미
   대규모 사용자 집합을 문서마다 복제 저장하려 한 설계 자체의 문제, 글자수 조정으로는 해결 불가)

   해결: 사용자로 펼치지 않고 ACL/공유의 "원본 대상(PRINCIPAL_TYPE:PRINCIPAL_ID)" 태그만
   그대로 저장한다. 문서 하나의 ACL이 가리키는 대상은 보통 몇 개뿐이라(사용자 수와 무관하게)
   절대 폭발하지 않는다. 대신 "로그인한 사용자가 어떤 태그를 갖는지"는 검색 시점에 그 한
   사용자에 대해서만 가볍게 계산해([6] 참고), ES에서 문서의 태그 배열과 사용자의 태그
   배열이 하나라도 겹치는지(terms 쿼리)로 열람 가능 여부를 판정한다.
   ------------------------------------------------------------------------------------- */
-- (주의) 아래 ACL_OWNER_FLAG/ACL_OGROUP_FLAG 정의는 설명용 원형이며, 실제로는
-- [4-A]/[4-B]/[4-C] 각 쿼리 본문 맨 앞에서 독립적으로 다시 선언해 사용한다
-- (CTE는 자신이 속한 SQL 문 안에서만 유효하기 때문).


/* =====================================================================================
   [3] 공유(SHARE) 해석 — 문서/폴더 단위 개별 공유자를 "태그"로 그대로 저장
   -------------------------------------------------------------------------------------
   AUTHORITY 비트 중 2(읽기)가 포함된 경우만 "읽기 가능"으로 간주한다.
   SHARE_TYPE(1 사용자/0 부서) + SHARED_IS_ALIAS(1이면 별칭) 조합으로 PRINCIPAL_TAG를
   만든다 (사용자 전개 없음 — 공유 대상 수는 원래도 적어 폭발 위험이 없지만, ACL과
   형식을 통일해 검색 시점 비교 로직을 하나로 둔다). V_ECM_SHARES.TARGET_TYPE은
   DOCUMENT/FOLDER 뿐이라 프로젝트 자체([4-A])에는 적용되지 않는다.
   이 SHARE_RESOLVED도 설명용 원형이며, 실제로는 [4-B]/[4-C] 본문에서 다시 선언한다.
   ===================================================================================== */

/* =====================================================================================
   색인 쿼리를 "통합검색 > 프로젝트" 하위 3가지 선택지에 맞춰 3종류로 나눈다.
     [4-A] 프로젝트 선택 → 프로젝트 1건당 1 row (워크시트 전체 + 프로젝트 정보, 파일/폴더 조인 없음)
     [4-B] 파일 선택     → 파일(첨부문서) 1건당 1 row (파일 정보 + 연관 프로젝트 필터 컬럼)
     [4-C] 폴더 선택     → 폴더 1건당 1 row (폴더 정보 + 연관 프로젝트 필터 컬럼)
   세 쿼리는 서로 독립된 SQL 문이라 WITH절의 ACL_OWNER_FLAG/ACL_OGROUP_FLAG/SHARE_RESOLVED를
   각자 다시 선언한다(CTE는 그 문장 안에서만 유효하므로 공유 불가).
   ===================================================================================== */


/* =====================================================================================
   [4-A] 프로젝트 레벨 색인 — "통합검색 > 프로젝트 > 프로젝트" 선택 시 사용
   -------------------------------------------------------------------------------------
   프로젝트 1건당 1 row. 파일/폴더와 조인하지 않으므로 같은 프로젝트가 중복되지 않는다.
   ACL은 프로젝트 자체 ACL_ID(P.ACL_ID)로 판단한다. 공유(SHARE)는 V_ECM_SHARES.TARGET_TYPE이
   DOCUMENT/FOLDER 뿐이라 프로젝트 자체에는 적용되지 않으므로 공유 태그는 포함하지 않는다
   (프로젝트 루트 폴더 공유 여부는 [4-C] 폴더 색인 쪽에 반영된다).
   ===================================================================================== */
WITH ACL_OWNER_FLAG AS (
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OWNER' AND CAN_READ = 1
),
ACL_OGROUP_FLAG AS (
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OGROUP' AND CAN_READ = 1
)
SELECT
    -- ── 프로젝트 식별/메타 ───────────────────────────────────────────
    P.PROJECT_ID                          AS "projectId",  -- 프로젝트 ID
    P.PROJECTNO                           AS "projectNo",  -- 프로젝트 번호
    P.PJTNAME                             AS "pjtName",  -- 등록명(국문) 구성요소1
    P.MISNAME                             AS "completionNm",  -- 준공명(등록명 구성요소2)
    P.CONTRACTNAME                        AS "contractName",  -- 계약명
    P.NAME                                AS "projectName",  -- 프로젝트명
    P.SUBPROJECTNO                        AS "subProjectNo",  -- 종속 프로젝트 번호
    P.SUBMISNAME                          AS "subMisName",  -- 종속 프로젝트명
    P.CREATOR_ID                          AS "creatorId",  -- 등록자 ID
    P.CREATED_DATE                        AS "createdDate",  -- 등록일
    P.PROJECTSTATUS                       AS "projectStatus",       -- 프로젝트 상태
    P.SAVE_STATUS                         AS "saveStatus",          -- C 확정 / T 임시저장
    P.TRANSFERSTATUS                      AS "transferStatusCd",    -- 이관여부(코드)
    P.TRANSFERSTATUS_NM                   AS "transferStatusNm",  -- 이관여부(명)
    P.SECURITYLEVEL                       AS "projectSecurityLevel",  -- 프로젝트 보안등급(코드)
    P.SECURITYLEVEL_NM                    AS "projectSecurityLevelNm",  -- 프로젝트 보안등급(명)
    P.SITEGUBUN1                          AS "stdFolder1Cd",     -- 표준폴더1
    P.PROJECTGUBUNTITLE                   AS "stdFolder1Title",  -- 표준폴더1 구분타이틀
    P.SITEGUBUN2                          AS "siteGubunCd",      -- 현장구분
    P.PROJECTGUBUN2                       AS "stdFolder2Cd",     -- 표준폴더2
    P.PROJECTGUBUN2TITLE                  AS "stdFolder2Title",  -- 표준폴더2 구분타이틀

    -- ── 프로젝트 필터 : 상세 (확정) ───────────────────────────────────
    P.SITEOPENYN                          AS "siteOpenYn",       -- 현장개설여부
    P.WORKGUBUN                           AS "workGubun",        -- 업무구분(코드)
    P.WORKGUBUN_NM                        AS "workGubunNm",      -- 업무구분(명)
    P.PARTICIPATIONINFO1                  AS "orderTypeCd",      -- 수주형태
    P.PARTICIPATIONINFO1_NM               AS "orderTypeNm",  -- 수주형태(명)
    P.SERVICEGUBUN                        AS "serviceGubun",     -- 용역구분(코드)
    P.SERVICEGUBUN_NM                     AS "serviceGubunNm",   -- 용역구분(명)
    P.PROGRESS                            AS "wjgProgressCd",    -- 우장각 진행상태(코드)
    P.PROGRESS_NM                         AS "wjgProgressNm",    -- 우장각 진행상태(명)
    P.REGIONLEVEL1_NM                     AS "locationNm",       -- 위치(예: 세종특별자치시)
    P.USEADD                              AS "facilityUseCd",    -- 용도시설 구분(코드)
    P.USEADD_NM                           AS "facilityUseNm",  -- 용도시설 구분(명)
    P.MATERIALTYPEA                       AS "keyMaterialCd",    -- 주요자료(코드)
    P.MATERIALTYPEA_NM                    AS "keyMaterialNm",  -- 주요자료(명)

    -- 참여자(내부)
    P.DEPARTMENTPMNM                      AS "participantInPm",      -- 대표PM
    P.GENERALMANAGERNM                    AS "participantInLeader",  -- 단장

    -- 참여자(외부)
    P.CONSUMER                            AS "participantExConsumer",  -- 참여자(외부) 수요자
    P.CONTRACTORDERER                     AS "participantExOrderer",  -- 참여자(외부) 계약발주자
    P.ORIGINALORDERER                     AS "participantExOrgOrderer",  -- 참여자(외부) 원발주자
    P.ADMITTER                            AS "participantExAdmitter",  -- 참여자(외부) 허가권자
    P.INSPECTOR                           AS "participantExInspector",  -- 참여자(외부) 감독자
    P.CONSTRUCTIONPM                      AS "participantExCm",  -- 참여자(외부) 건설사업관리자
    P.PROJECTOPERATOR                     AS "participantExOperator",  -- 참여자(외부) 시행자
    P.DESIGNER                            AS "participantExDesigner",  -- 참여자(외부) 설계자
    P.BUILDER                             AS "participantExBuilder",  -- 참여자(외부) 시공자
    P.CONSULTING                          AS "participantExConsulting",  -- 참여자(외부) 컨설팅
    P.JOINTCONTRACTOR                     AS "participantExJoint",  -- 참여자(외부) 공동수행사
    P.EXTERNALETC                         AS "participantExEtc",  -- 참여자(외부) 기타

    P.CONTRACT_DATE                       AS "contractDate",  -- 계약일자(원본)
    CASE WHEN P.CONTRACT_DATE IS NOT NULL THEN 'Y' ELSE 'N' END
                                           AS "contractYn",  -- 계약여부(파생, 계약일자 존재 여부)
    P.PARTICIPATIONINFO5_CO               AS "jointPerformCd",  -- 공동이행방식 최유력후보(단독/공동, 코드)
    P.PARTICIPATIONINFO5_CO_NM            AS "jointPerformNm",  -- 공동이행방식(명)

    -- ── 세부 필터 : 사업 ─────────────────────────────────────────────
    P.ENTERPRISETYPE                      AS "businessFieldCand",  -- 사업분야 유력후보(사업유형)
    P.DIVISIONBUSINESS2                   AS "bizMethodCd",  -- 시행방식(코드, 사업유형 하위)
    P.DIVISIONBUSINESS2_NM                AS "bizMethodNm",  -- 시행방식(명)
    P.PARTICIPATIONSTEP                   AS "participationStep",  -- 참여단계(코드)
    P.PARTICIPATIONSTEP_NM                AS "participationStepNm",  -- 참여단계(명)
    P.DIVISIONBUSINESS3                   AS "buildActCd",  -- 건축행위(코드)
    P.DIVISIONBUSINESS3_NM                AS "buildActNm",  -- 건축행위(명)
    P.TOTALEXPENSES                       AS "businessCost",  -- 사업비(원)
    P.TOTALEXPENSESP                      AS "businessCostP",  -- 사업비(평당)
    P.DESIGNEXPENSES                      AS "designPrice",  -- 설계가격(원)
    P.DESIGNEXPENSESP                     AS "designPriceP",  -- 설계가격(평당)
    P.INITIALCONSTRUCTIONEXPENSES         AS "initConstructCost",  -- 최초공사비(원)
    P.FINALCONSTRUCTIONEXPENSES           AS "finalConstructCost",  -- 최종공사비(원)

    P.DESIGNSTART_DATE                    AS "designStartDate",  -- 설계전 단계 착수일
    P.DESIGNEND_DATE                      AS "designEndDate",  -- 설계전 단계 종료일
    P.BASICDESIGNSTART_DATE               AS "basicDesignStartDate",  -- 기본설계 단계 착수일
    P.BASICDESIGNEND_DATE                 AS "basicDesignEndDate",  -- 기본설계 단계 종료일
    P.WORKINGDESIGNSTART_DATE             AS "workingDesignStartDate",  -- 실시설계 단계 착수일
    P.WORKINGDESIGNEND_DATE               AS "workingDesignEndDate",  -- 실시설계 단계 종료일
    P.PURCHASESTART_DATE                  AS "purchaseStartDate",  -- 구매조달 단계 착수일
    P.PURCHASEEND_DATE                    AS "purchaseEndDate",  -- 구매조달 단계 종료일
    P.CONSTRUCTSTART_DATE                 AS "constructStartDate",  -- 시공 단계 착수일
    P.CONSTRUCTEND_DATE                   AS "constructEndDate",  -- 시공 단계 종료일
    P.AFTERCONSTRUCTSTART_DATE            AS "afterConstructStartDate",  -- 시공후 단계 착수일
    P.AFTERCONSTRUCTEND_DATE              AS "afterConstructEndDate",  -- 시공후 단계 종료일

    -- ── 세부 필터 : 시설 ─────────────────────────────────────────────
    P.USELEVEL1                           AS "buildingUseCd",  -- 건축물의 용도(코드)
    P.USELEVEL1_NM                        AS "buildingUseNm",  -- 건축물의 용도(명)
    P.AREA                                AS "totalArea",  -- 면적(필터용 분류코드)
    P.TOTALFLOORAREA                      AS "totalFloorAreaM2",  -- 연면적(㎡, 실측값)
    P.SITEAREA                            AS "siteAreaM2",  -- 대지면적(㎡)
    P.BUILDINGAREA                        AS "buildingAreaM2",  -- 건축면적(㎡)
    P.SCALE                                AS "buildingScale",  -- 규모(필터용 분류코드)
    P.GROUNDFLOORS                        AS "groundFloors",  -- 지상 층수(실측값)
    P.UNDERGROUNDFLOORS                   AS "undergroundFloors",  -- 지하 층수
    P.CONFIRM                             AS "certGrade",  -- 등급/인증(대표코드)
    P.GREENPC                             AS "greenPc",  -- 녹색건축인증 예비인증
    P.GREENMC                             AS "greenMc",  -- 녹색건축인증 본인증
    P.BUILDINGENERGYPC                    AS "buildingEnergyPc",  -- 건축물에너지효율등급 예비인증
    P.BUILDINGENERGYMC                    AS "buildingEnergyMc",  -- 건축물에너지효율등급 본인증
    P.ZEROENERGYPC                        AS "zeroEnergyPc",  -- 제로에너지 건축물인증 예비인증
    P.ZEROENERGYMC                        AS "zeroEnergyMc",  -- 제로에너지 건축물인증 본인증
    P.DESIGNCONDITION                     AS "designConditionCd",  -- 설계조건(코드)
    P.DESIGNCONDITION_NM                  AS "designConditionNm",  -- 설계조건(명)

    -- ── 세부 필터 : 기술 ─────────────────────────────────────────────
    NULL                                   AS "structureType",  -- 구조(TODO: 확인 필요)
    NULL                                   AS "materialTech",  -- 재료기술(TODO: 확인 필요)
    P.BUILDINGMETHOD                       AS "buildMethodCd",  -- 건축(코드, 라벨 "건축공사"와 상이 확인 권장)
    P.BUILDINGMETHOD_NM                    AS "buildMethodNm",  -- 건축(명)
    P.MATERIALTECHCIVIL                    AS "civilTechCd",  -- 토목(재료기술, 코드)
    P.MATERIALTECHCIVIL_NM                 AS "civilTechNm",  -- 토목(명)
    P.MATERIALTECHMACHINE                  AS "machineTechCd",  -- 기계설비(재료기술, 코드)
    P.MATERIALTECHMACHINE_NM               AS "machineTechNm",  -- 기계설비(명)
    P.MATERIALTECHELECTRIC                 AS "electricTechCd",  -- 전기통신(재료기술, 코드)
    P.MATERIALTECHELECTRIC_NM              AS "electricTechNm",  -- 전기통신(명)
    P.MATERIALTECHFIRE                     AS "fireTechCd",  -- 소방설비(재료기술, 코드)
    P.MATERIALTECHFIRE_NM                  AS "fireTechNm",  -- 소방설비(명)
    P.PROFESSIONALMETHOD                   AS "specialtyCd",  -- 전문분야(코드)
    P.PROFESSIONALMETHOD_NM                AS "specialtyNm",  -- 전문분야(명)

    -- ── "공정" 파생 현재단계 ─────────────────────────────────────────
    CASE
        WHEN SYSDATE BETWEEN P.DESIGNSTART_DATE AND P.DESIGNEND_DATE THEN '설계전단계'
        WHEN SYSDATE BETWEEN P.BASICDESIGNSTART_DATE AND P.BASICDESIGNEND_DATE THEN '기본설계단계'
        WHEN SYSDATE BETWEEN P.WORKINGDESIGNSTART_DATE AND P.WORKINGDESIGNEND_DATE THEN '실시설계단계'
        WHEN SYSDATE BETWEEN P.PURCHASESTART_DATE AND P.PURCHASEEND_DATE THEN '구매조달단계'
        WHEN SYSDATE BETWEEN P.CONSTRUCTSTART_DATE AND P.CONSTRUCTEND_DATE THEN '시공단계'
        WHEN SYSDATE BETWEEN P.AFTERCONSTRUCTSTART_DATE AND P.AFTERCONSTRUCTEND_DATE THEN '시공후단계'
        ELSE NULL
    END                                      AS "currentProcessStage",  -- 공정 파생 현재단계

    -- ── ACL: 프로젝트 자체 ACL_ID 기준 권한 태그 ─────────────────────
    P.ACL_ID                               AS "projectAclId",  -- 프로젝트 ACL ID
    (
        (SELECT LISTAGG(R.PRINCIPAL_TYPE || ':' || R.PRINCIPAL_ID, '|')
                WITHIN GROUP (ORDER BY R.PRINCIPAL_TYPE, R.PRINCIPAL_ID)
         FROM METAECM.V_ECM_ACL_READERS R
         WHERE R.ACL_ID = P.ACL_ID
           AND R.CAN_READ = 1
           AND R.PRINCIPAL_TYPE NOT IN ('OWNER', 'OGROUP'))
        ||
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OWNER_FLAG O WHERE O.ACL_ID = P.ACL_ID)
              THEN '|USER:' || TO_CHAR(P.CREATOR_ID) END)
        ||
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OGROUP_FLAG G WHERE G.ACL_ID = P.ACL_ID)
              THEN '|' || (SELECT LISTAGG('DEPT:' || UG.GROUP_ID, '|')
                                  WITHIN GROUP (ORDER BY UG.GROUP_ID)
                           FROM METAECM.V_ECM_USER_GROUPS UG
                           WHERE UG.USER_ID = P.CREATOR_ID)
         END)
    )                                        AS "aclPrincipalTags"  -- ACL 권한 태그 목록('|' 구분)
FROM METAECM.V_ECM_PROJECTS P
WHERE P.SAVE_STATUS = 'C'   -- 임시저장(T, 개설신청 중간저장) 제외, 확정 프로젝트만 색인(필요 시 조정)
;


/* =====================================================================================
   [4-B] 파일(문서) 레벨 색인 — "통합검색 > 프로젝트 > 파일" 선택 시 사용
   -------------------------------------------------------------------------------------
   ES Bulk 적재 스크립트(elastic_bulker)에서 이 결과를 읽어
     - acl_principal_tags / share_principal_tags 는 '|' 구분 문자열 → 배열(split, 빈 요소 제거)로
       변환해 색인 (예: ["USER:1001", "DEPT:30", "ALIAS:WORLD"]). 사용자 ID로 펼치지 않은
       "원본 권한 대상 태그"이므로 ACL_ID당 태그 수가 항상 적어 안전하다.
     - 검색 시점에 ES "terms" 쿼리로 로그인 사용자의 태그 집합([6] 참고)과 교집합이
       있는지 확인하는 방식으로 열람 권한을 판정한다 (색인 시점엔 사용자 전개를 하지 않음)
     - _id 는 DOC_SERIES_ID||'_'||FILE_ID 조합 권장(버전 갱신 시 동일 문서 덮어쓰기 방지용 키 설계는
       운영 정책에 맞게 조정)
   ===================================================================================== */
/* 컬럼 표기 규칙: 모든 결과 컬럼은 큰따옴표로 감싼 camelCase 별칭을 명시한다.
   Oracle은 따옴표 없는 식별자를 자동 대문자로 바꾸므로, camelCase를 유지하려면
   반드시 "alias"처럼 큰따옴표가 있어야 한다. (ETL/JSON 변환 시 그대로 필드명이 된다) */
WITH ACL_OWNER_FLAG AS (
    /* 이 ACL에 OWNER(문서등록자 본인) 읽기권한 라인이 있는지 */
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OWNER' AND CAN_READ = 1
),
ACL_OGROUP_FLAG AS (
    /* 이 ACL에 OGROUP(문서등록자의 부서원 전체) 읽기권한 라인이 있는지 */
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OGROUP' AND CAN_READ = 1
),
SHARE_RESOLVED AS (
    SELECT S.TARGET_TYPE, S.TARGET_ID, S.DOC_SERIES_ID,
           CASE
               WHEN S.SHARED_IS_ALIAS = 1 THEN 'ALIAS:' || S.SHARED_USER_ID
               WHEN S.SHARE_TYPE = 0     THEN 'DEPT:'  || S.SHARED_USER_ID
               ELSE 'USER:' || S.SHARED_USER_ID
           END AS PRINCIPAL_TAG
    FROM METAECM.V_ECM_SHARES S
    WHERE MOD(TRUNC(S.AUTHORITY / 2), 2) = 1   -- AUTHORITY 비트 2(읽기) 포함 여부
)
SELECT
    -- ── 문서 식별/본문 ──────────────────────────────────────────────
    F.DOC_SERIES_ID                       AS "docSeriesId",  -- 문서 시리즈 ID(버전 공통 키)
    F.FILE_ID                             AS "fileId",  -- 파일 ID
    F.DOC_ID                              AS "docId",  -- 문서 ID
    F.DOC_NAME                            AS "docName",  -- 문서명
    F.FILE_NAME                           AS "fileName",  -- 파일명
    F.FILE_EXTENSION                      AS "fileExtension",  -- 파일 확장자
    F.FILE_CONTENT_SIZE                   AS "fileContentSize",  -- 파일 용량
    F.DOC_DESCRIPTION                     AS "docDescription",  -- 문서 설명
    F.DOC_TYPE_NAME                       AS "docTypeName",  -- 문서 유형명
    F.DOC_STATUS                          AS "docStatus",  -- 문서 상태
    F.DOC_CODE                            AS "docCode",  -- 문서 코드
    F.DOC_STANDARD                        AS "docStandard",  -- 문서 표준(규격)

    -- ── 검색 도구: 작성자 / 기간 / 폴더경로 / 파일형식 ───────────────
    F.DOC_CREATOR_ID                      AS "docCreatorId",  -- 문서 작성자 ID
    F.DOC_CREATOR_NAME                    AS "docCreatorName",  -- 문서 작성자명
    F.DOC_CREATED_DATE                    AS "docCreatedDate",  -- 문서 등록일
    F.DOC_UPDATER_ID                      AS "docUpdaterId",  -- 문서 수정자 ID
    F.DOC_UPDATER_NAME                    AS "docUpdaterName",  -- 문서 수정자명
    F.DOC_UPDATED_DATE                    AS "docUpdatedDate",  -- 문서 수정일시
    F.FOLDER_ID                           AS "folderId",  -- 폴더 ID
    F.FOLDER_PATH                         AS "folderPath",  -- 폴더 경로
    F.MAP_NAME                            AS "areaName",         -- 전사문서/부서문서/프로젝트

    -- ── 프로젝트 필터 : 기본 ─────────────────────────────────────────
    P.PROJECT_ID                          AS "projectId",  -- 프로젝트 ID
    P.PROJECTNO                           AS "projectNo",        -- 프로젝트 번호
    P.PJTNAME                             AS "pjtName",          -- 등록명(국문) 구성요소1
    P.MISNAME                             AS "completionNm",     -- 준공명 (등록명 구성요소2)
    P.CONTRACTNAME                        AS "contractName",     -- 계약명
    P.NAME                                AS "projectName",  -- 프로젝트명
    P.SUBPROJECTNO                        AS "subProjectNo",  -- 종속 프로젝트 번호
    P.SUBMISNAME                          AS "subMisName",  -- 종속 프로젝트명
    P.SECURITYLEVEL                       AS "projectSecurityLevel",  -- 프로젝트 보안등급(코드)
    P.SECURITYLEVEL_NM                    AS "projectSecurityLevelNm",  -- 프로젝트 보안등급(명)
    P.SITEGUBUN1                          AS "stdFolder1Cd",     -- 표준폴더1
    P.PROJECTGUBUNTITLE                   AS "stdFolder1Title",  -- 표준폴더1 구분타이틀
    P.SITEGUBUN2                          AS "siteGubunCd",      -- 현장구분
    P.PROJECTGUBUN2                       AS "stdFolder2Cd",     -- 표준폴더2
    P.PROJECTGUBUN2TITLE                  AS "stdFolder2Title",  -- 표준폴더2 구분타이틀

    -- ── 프로젝트 필터 : 상세 (확정) ───────────────────────────────────
    P.SITEOPENYN                          AS "siteOpenYn",       -- 현장개설여부
    P.WORKGUBUN                           AS "workGubun",        -- 업무구분(코드)
    P.WORKGUBUN_NM                        AS "workGubunNm",      -- 업무구분(명)
    P.PARTICIPATIONINFO1                  AS "orderTypeCd",      -- 수주형태
    P.PARTICIPATIONINFO1_NM               AS "orderTypeNm",  -- 수주형태(명)
    P.SERVICEGUBUN                        AS "serviceGubun",     -- 용역구분(코드)
    P.SERVICEGUBUN_NM                     AS "serviceGubunNm",   -- 용역구분(명)
    P.PROGRESS                            AS "wjgProgressCd",    -- 우장각 진행상태(코드) ✅확정
    P.PROGRESS_NM                         AS "wjgProgressNm",    -- 우장각 진행상태(명)
    P.REGIONLEVEL1_NM                     AS "locationNm",       -- 위치(예: 세종특별자치시)
    P.USEADD                              AS "facilityUseCd",    -- 용도시설 구분(코드)
    P.USEADD_NM                           AS "facilityUseNm",  -- 용도시설 구분(명)
    P.MATERIALTYPEA                       AS "keyMaterialCd",    -- 주요자료(코드)
    P.MATERIALTYPEA_NM                    AS "keyMaterialNm",  -- 주요자료(명)

    -- 참여자(내부) — 그룹 확정: 이름 필드 묶음(단일 코드 아님)
    P.DEPARTMENTPMNM                      AS "participantInPm",      -- 대표PM
    P.GENERALMANAGERNM                    AS "participantInLeader",  -- 단장

    -- 참여자(외부) — 그룹 확정: 이름 필드 묶음(단일 코드 아님)
    P.CONSUMER                            AS "participantExConsumer",    -- 수요자
    P.CONTRACTORDERER                     AS "participantExOrderer",     -- 계약발주자
    P.ORIGINALORDERER                     AS "participantExOrgOrderer",  -- 원발주자
    P.ADMITTER                            AS "participantExAdmitter",    -- 허가권자
    P.INSPECTOR                           AS "participantExInspector",   -- 감독자
    P.CONSTRUCTIONPM                      AS "participantExCm",          -- 건설사업관리자
    P.PROJECTOPERATOR                     AS "participantExOperator",    -- 시행자
    P.DESIGNER                            AS "participantExDesigner",    -- 설계자
    P.BUILDER                             AS "participantExBuilder",     -- 시공자
    P.CONSULTING                          AS "participantExConsulting",  -- 컨설팅
    P.JOINTCONTRACTOR                     AS "participantExJoint",       -- 공동수행사
    P.EXTERNALETC                         AS "participantExEtc",         -- 기타

    -- ── 프로젝트 필터 : 상세 (2차 확인 완료) ──────────────────────────
    P.CONTRACT_DATE                       AS "contractDate",      -- 계약일자(원본)
    CASE WHEN P.CONTRACT_DATE IS NOT NULL THEN 'Y' ELSE 'N' END
                                           AS "contractYn",       -- ✅ 계약여부(파생) = 계약일자 존재 여부
    P.PARTICIPATIONINFO5_CO               AS "jointPerformCd",   -- ▲ 공동이행방식 최유력후보(단독/공동, CODE)
    P.PARTICIPATIONINFO5_CO_NM            AS "jointPerformNm",  -- 공동이행방식(명)

    -- ── 세부 필터 : 사업 ─────────────────────────────────────────────
    P.ENTERPRISETYPE                      AS "businessFieldCand",-- ▲ 사업분야 유력후보(사업유형)
    P.DIVISIONBUSINESS2                   AS "bizMethodCd",      -- 시행방식(사업유형 하위, 워크시트 전용)
    P.DIVISIONBUSINESS2_NM                AS "bizMethodNm",  -- 시행방식(명)
    P.PARTICIPATIONSTEP                   AS "participationStep",    -- 참여단계(코드)
    P.PARTICIPATIONSTEP_NM                AS "participationStepNm",  -- 참여단계(명)
    P.DIVISIONBUSINESS3                   AS "buildActCd",       -- 건축행위(코드)
    P.DIVISIONBUSINESS3_NM                AS "buildActNm",  -- 건축행위(명)
    P.TOTALEXPENSES                       AS "businessCost",     -- 사업비(원)
    P.TOTALEXPENSESP                      AS "businessCostP",    -- 사업비(평당)
    P.DESIGNEXPENSES                      AS "designPrice",      -- 설계가격(원)
    P.DESIGNEXPENSESP                     AS "designPriceP",     -- 설계가격(평당)
    P.INITIALCONSTRUCTIONEXPENSES         AS "initConstructCost",-- 최초공사비(원)
    P.FINALCONSTRUCTIONEXPENSES           AS "finalConstructCost",-- 최종공사비(원)

    -- 공정 — 단일 코드가 아니라 단계별 일정(날짜) 그룹. 필터용 대표값은 아래 파생컬럼 참고
    P.DESIGNSTART_DATE                    AS "designStartDate",          -- 설계전 단계
    P.DESIGNEND_DATE                      AS "designEndDate",  -- 설계전 단계 종료일
    P.BASICDESIGNSTART_DATE               AS "basicDesignStartDate",     -- 기본설계 단계
    P.BASICDESIGNEND_DATE                 AS "basicDesignEndDate",  -- 기본설계 단계 종료일
    P.WORKINGDESIGNSTART_DATE             AS "workingDesignStartDate",   -- 실시설계 단계
    P.WORKINGDESIGNEND_DATE               AS "workingDesignEndDate",  -- 실시설계 단계 종료일
    P.PURCHASESTART_DATE                  AS "purchaseStartDate",        -- 구매조달 단계
    P.PURCHASEEND_DATE                    AS "purchaseEndDate",  -- 구매조달 단계 종료일
    P.CONSTRUCTSTART_DATE                 AS "constructStartDate",       -- 시공 단계
    P.CONSTRUCTEND_DATE                   AS "constructEndDate",  -- 시공 단계 종료일
    P.AFTERCONSTRUCTSTART_DATE            AS "afterConstructStartDate",  -- 시공후 단계
    P.AFTERCONSTRUCTEND_DATE              AS "afterConstructEndDate",  -- 시공후 단계 종료일

    -- ── 세부 필터 : 시설 ─────────────────────────────────────────────
    P.USELEVEL1                           AS "buildingUseCd",    -- 건축물의 용도(코드)
    P.USELEVEL1_NM                        AS "buildingUseNm",  -- 건축물의 용도(명)
    P.AREA                                AS "totalArea",        -- 면적(▲ 필터용 분류코드 추정)
    P.TOTALFLOORAREA                      AS "totalFloorAreaM2", -- 연면적(㎡, 워크시트 확인된 실측값)
    P.SITEAREA                            AS "siteAreaM2",       -- 대지면적(㎡)
    P.BUILDINGAREA                        AS "buildingAreaM2",   -- 건축면적(㎡)
    P.SCALE                                AS "buildingScale",    -- 규모(▲ 필터용 분류코드 추정)
    P.GROUNDFLOORS                        AS "groundFloors",     -- 지상(층, 워크시트 확인된 실측값)
    P.UNDERGROUNDFLOORS                   AS "undergroundFloors",-- 지하(층)
    P.CONFIRM                             AS "certGrade",        -- 등급/인증(대표코드, ▲)
    P.GREENPC                             AS "greenPc",          -- 녹색건축인증 예비인증
    P.GREENMC                             AS "greenMc",          -- 녹색건축인증 본인증
    P.BUILDINGENERGYPC                    AS "buildingEnergyPc", -- 건축물에너지효율등급 예비인증
    P.BUILDINGENERGYMC                    AS "buildingEnergyMc", -- 건축물에너지효율등급 본인증
    P.ZEROENERGYPC                        AS "zeroEnergyPc",     -- 제로에너지 건축물인증 예비인증
    P.ZEROENERGYMC                        AS "zeroEnergyMc",     -- 제로에너지 건축물인증 본인증
    P.DESIGNCONDITION                     AS "designConditionCd",-- 설계조건(코드)
    P.DESIGNCONDITION_NM                  AS "designConditionNm",  -- 설계조건(명)

    -- ── 세부 필터 : 기술 ─────────────────────────────────────────────
    NULL                                   AS "structureType",   -- TODO: 구조 (STRUCTURALMETHODG 등 확인)
    NULL                                   AS "materialTech",    -- TODO: 재료기술(그룹 자체 선택값 존재 여부 확인)
    P.BUILDINGMETHOD                       AS "buildMethodCd",   -- 건축(▲ 라벨 "건축공사"와 상이, 확인 권장)
    P.BUILDINGMETHOD_NM                    AS "buildMethodNm",  -- 건축(명)
    P.MATERIALTECHCIVIL                    AS "civilTechCd",     -- 토목
    P.MATERIALTECHCIVIL_NM                 AS "civilTechNm",  -- 토목(명)
    P.MATERIALTECHMACHINE                  AS "machineTechCd",   -- 기계설비
    P.MATERIALTECHMACHINE_NM               AS "machineTechNm",  -- 기계설비(명)
    P.MATERIALTECHELECTRIC                 AS "electricTechCd",  -- 전기통신
    P.MATERIALTECHELECTRIC_NM              AS "electricTechNm",  -- 전기통신(명)
    P.MATERIALTECHFIRE                     AS "fireTechCd",      -- 소방설비
    P.MATERIALTECHFIRE_NM                  AS "fireTechNm",  -- 소방설비(명)
    P.PROFESSIONALMETHOD                   AS "specialtyCd",     -- 전문분야
    P.PROFESSIONALMETHOD_NM                AS "specialtyNm",  -- 전문분야(명)

    -- ── "공정" 필터용 파생 현재단계 (날짜 그룹 → 단일 코드화, 운영정책에 맞게 단계명/경계값 조정) ─
    CASE
        WHEN SYSDATE BETWEEN P.DESIGNSTART_DATE AND P.DESIGNEND_DATE THEN '설계전단계'
        WHEN SYSDATE BETWEEN P.BASICDESIGNSTART_DATE AND P.BASICDESIGNEND_DATE THEN '기본설계단계'
        WHEN SYSDATE BETWEEN P.WORKINGDESIGNSTART_DATE AND P.WORKINGDESIGNEND_DATE THEN '실시설계단계'
        WHEN SYSDATE BETWEEN P.PURCHASESTART_DATE AND P.PURCHASEEND_DATE THEN '구매조달단계'
        WHEN SYSDATE BETWEEN P.CONSTRUCTSTART_DATE AND P.CONSTRUCTEND_DATE THEN '시공단계'
        WHEN SYSDATE BETWEEN P.AFTERCONSTRUCTSTART_DATE AND P.AFTERCONSTRUCTEND_DATE THEN '시공후단계'
        ELSE NULL
    END                                      AS "currentProcessStage",  -- 공정 파생 현재단계

    -- ── 보안등급(문서에 실제 적용되는 값, 프로젝트값과 다를 수 있어 별도 보존) ─
    F.SECURITY_LEVEL                      AS "docSecurityLevel",  -- 문서 보안등급(코드)
    F.SECURITY_LEVEL_NM                   AS "docSecurityLevelNm",  -- 문서 보안등급(명)

    -- ── ACL: 문서 자체 ACL_ID 가 허용하는 "권한 태그" 목록 (사용자로 펼치지 않음) ──
    -- 태그 형식: 'USER:123', 'DEPT:30', 'PROJECT_GROUP:PJ_22164M_T1P', 'ALIAS:WORLD',
    --           'ROLE:LEADER' — V_ECM_ACL_READERS.PRINCIPAL_TYPE/ID 원본 그대로.
    -- OWNER→'USER:<등록자ID>', OGROUP→'DEPT:<등록자의 소속부서ID>' 로 치환(둘 다 문서별로
    -- 달라지므로 여기서 직접 계산). 어느 경우든 ACL_ID 하나당 태그 수는 보통 한 자릿수~
    -- 수십 개 수준이라 LISTAGG 길이 폭발 위험이 없다.
    F.DOC_ACL_ID                           AS "docAclId",  -- 문서 ACL ID
    (
        -- 1) 일반 태그(USER/DEPT/PROJECT_GROUP/ALIAS/ROLE) — 원본 그대로, 사용자 전개 없음
        (SELECT LISTAGG(R.PRINCIPAL_TYPE || ':' || R.PRINCIPAL_ID, '|')
                WITHIN GROUP (ORDER BY R.PRINCIPAL_TYPE, R.PRINCIPAL_ID)
         FROM METAECM.V_ECM_ACL_READERS R
         WHERE R.ACL_ID = F.DOC_ACL_ID
           AND R.CAN_READ = 1
           AND R.PRINCIPAL_TYPE NOT IN ('OWNER', 'OGROUP'))
        ||
        -- 2) OWNER 라인이 있으면 문서 등록자 본인 태그 추가
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OWNER_FLAG O WHERE O.ACL_ID = F.DOC_ACL_ID)
              THEN '|USER:' || TO_CHAR(F.DOC_CREATOR_ID) END)
        ||
        -- 3) OGROUP 라인이 있으면 등록자의 소속 부서(겸직 포함) 태그 추가 (부서원 전체로 안 펼침)
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OGROUP_FLAG G WHERE G.ACL_ID = F.DOC_ACL_ID)
              THEN '|' || (SELECT LISTAGG('DEPT:' || UG.GROUP_ID, '|')
                                  WITHIN GROUP (ORDER BY UG.GROUP_ID)
                           FROM METAECM.V_ECM_USER_GROUPS UG
                           WHERE UG.USER_ID = F.DOC_CREATOR_ID)
         END)
    )                                        AS "aclPrincipalTags",    -- '|' 구분 → ES 색인 시 배열 변환(빈 요소 제거)

    -- ── 개별 공유(SHARE) 로 추가 열람 가능한 "권한 태그" 목록 (문서/상위폴더 공유 모두 반영) ─
    (
        SELECT LISTAGG(SR.PRINCIPAL_TAG, '|') WITHIN GROUP (ORDER BY SR.PRINCIPAL_TAG)
        FROM SHARE_RESOLVED SR
        WHERE (SR.TARGET_TYPE = 'DOCUMENT' AND SR.DOC_SERIES_ID = F.DOC_SERIES_ID)
           OR (SR.TARGET_TYPE = 'FOLDER'   AND SR.TARGET_ID = F.FOLDER_ID)
    )                                        AS "sharePrincipalTags"  -- 공유 권한 태그 목록('|' 구분)

FROM METAECM.V_ECM_FILES F
LEFT JOIN METAECM.V_ECM_PROJECTS P ON P.PROJECT_ID = F.PROJECT_ID
WHERE F.DOC_IS_CURRENT = 1              -- 최신 버전만 색인(이전 버전 제외, 필요 시 제거)
  AND F.DOC_STATUS <> 'DELETED'         -- 삭제 문서 제외 (실제 코드값은 운영 DB 확인)
;


/* =====================================================================================
   [4-C] 폴더 레벨 색인 — "통합검색 > 프로젝트 > 폴더" 선택 시 사용
   -------------------------------------------------------------------------------------
   폴더 1건당 1 row. 폴더 자체 정보 + (폴더가 속한) 프로젝트의 필터 컬럼을 함께 담아
   [4-B]의 파일 쿼리와 동일한 패턴으로 "폴더 검색 + 프로젝트 조건 필터"를 지원한다.
   ACL은 폴더 자체 ACL_ID(D.ACL_ID)로 판단하고, 공유(SHARE)는 TARGET_TYPE='FOLDER',
   TARGET_ID=D.FOLDER_ID 기준으로 더해진다.
   ===================================================================================== */
WITH ACL_OWNER_FLAG AS (
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OWNER' AND CAN_READ = 1
),
ACL_OGROUP_FLAG AS (
    SELECT DISTINCT ACL_ID FROM METAECM.V_ECM_ACL_READERS
    WHERE PRINCIPAL_TYPE = 'OGROUP' AND CAN_READ = 1
),
SHARE_RESOLVED AS (
    SELECT S.TARGET_TYPE, S.TARGET_ID, S.DOC_SERIES_ID,
           CASE
               WHEN S.SHARED_IS_ALIAS = 1 THEN 'ALIAS:' || S.SHARED_USER_ID
               WHEN S.SHARE_TYPE = 0     THEN 'DEPT:'  || S.SHARED_USER_ID
               ELSE 'USER:' || S.SHARED_USER_ID
           END AS PRINCIPAL_TAG
    FROM METAECM.V_ECM_SHARES S
    WHERE MOD(TRUNC(S.AUTHORITY / 2), 2) = 1   -- AUTHORITY 비트 2(읽기) 포함 여부
)
SELECT
    -- ── 폴더 식별/본문 ──────────────────────────────────────────────
    D.FOLDER_ID                           AS "folderId",  -- 폴더 ID
    D.NAME                                AS "folderName",  -- 폴더명
    D.NAME_EN                             AS "folderNameEn",  -- 폴더명(영문)
    D.NAME_CN                             AS "folderNameCn",  -- 폴더명(중문)
    D.PARENT_ID                           AS "parentFolderId",  -- 상위 폴더 ID
    D."TYPE"                              AS "folderType",  -- 폴더 유형
    D.STATUS                              AS "folderStatus",  -- 폴더 상태
    D.DESCRIPTION                         AS "folderDescription",  -- 폴더 설명
    D.FOLDER_PATH                         AS "folderPath",  -- 폴더 경로
    D.MAP_NAME                            AS "areaName",         -- 전사문서/부서문서/프로젝트
    D.CREATOR_ID                          AS "creatorId",  -- 등록자 ID
    D.CREATOR_NAME                        AS "creatorName",  -- 등록자명
    D.CREATED_DATE                        AS "createdDate",  -- 등록일
    D.UPDATED_DATE                        AS "updatedDate",  -- 수정일시

    -- ── 프로젝트 필터 : 기본 ─────────────────────────────────────────
    P.PROJECT_ID                          AS "projectId",  -- 프로젝트 ID
    P.PROJECTNO                           AS "projectNo",  -- 프로젝트 번호
    P.PJTNAME                             AS "pjtName",  -- 등록명(국문) 구성요소1
    P.MISNAME                             AS "completionNm",  -- 준공명(등록명 구성요소2)
    P.CONTRACTNAME                        AS "contractName",  -- 계약명
    P.NAME                                AS "projectName",  -- 프로젝트명
    P.SUBPROJECTNO                        AS "subProjectNo",  -- 종속 프로젝트 번호
    P.SUBMISNAME                          AS "subMisName",  -- 종속 프로젝트명
    P.SECURITYLEVEL                       AS "projectSecurityLevel",  -- 프로젝트 보안등급(코드)
    P.SECURITYLEVEL_NM                    AS "projectSecurityLevelNm",  -- 프로젝트 보안등급(명)
    P.SITEGUBUN1                          AS "stdFolder1Cd",  -- 표준폴더1
    P.PROJECTGUBUNTITLE                   AS "stdFolder1Title",  -- 표준폴더1 구분타이틀
    P.SITEGUBUN2                          AS "siteGubunCd",  -- 현장구분
    P.PROJECTGUBUN2                       AS "stdFolder2Cd",  -- 표준폴더2
    P.PROJECTGUBUN2TITLE                  AS "stdFolder2Title",  -- 표준폴더2 구분타이틀

    -- ── 프로젝트 필터 : 상세 ─────────────────────────────────────────
    P.SITEOPENYN                          AS "siteOpenYn",  -- 현장개설여부
    P.WORKGUBUN                           AS "workGubun",  -- 업무구분(코드)
    P.WORKGUBUN_NM                        AS "workGubunNm",  -- 업무구분(명)
    P.PARTICIPATIONINFO1                  AS "orderTypeCd",  -- 수주형태(코드)
    P.PARTICIPATIONINFO1_NM               AS "orderTypeNm",  -- 수주형태(명)
    P.SERVICEGUBUN                        AS "serviceGubun",  -- 용역구분(코드)
    P.SERVICEGUBUN_NM                     AS "serviceGubunNm",  -- 용역구분(명)
    P.PROGRESS                            AS "wjgProgressCd",  -- 우장각 진행상태(코드)
    P.PROGRESS_NM                         AS "wjgProgressNm",  -- 우장각 진행상태(명)
    P.REGIONLEVEL1_NM                     AS "locationNm",  -- 위치(예: 세종특별자치시)
    P.USEADD                              AS "facilityUseCd",  -- 용도시설 구분(코드)
    P.USEADD_NM                           AS "facilityUseNm",  -- 용도시설 구분(명)
    P.MATERIALTYPEA                       AS "keyMaterialCd",  -- 주요자료(코드)
    P.MATERIALTYPEA_NM                    AS "keyMaterialNm",  -- 주요자료(명)

    P.DEPARTMENTPMNM                      AS "participantInPm",  -- 참여자(내부) 대표PM
    P.GENERALMANAGERNM                    AS "participantInLeader",  -- 참여자(내부) 단장
    P.CONSUMER                            AS "participantExConsumer",  -- 참여자(외부) 수요자
    P.CONTRACTORDERER                     AS "participantExOrderer",  -- 참여자(외부) 계약발주자
    P.ORIGINALORDERER                     AS "participantExOrgOrderer",  -- 참여자(외부) 원발주자
    P.ADMITTER                            AS "participantExAdmitter",  -- 참여자(외부) 허가권자
    P.INSPECTOR                           AS "participantExInspector",  -- 참여자(외부) 감독자
    P.CONSTRUCTIONPM                      AS "participantExCm",  -- 참여자(외부) 건설사업관리자
    P.PROJECTOPERATOR                     AS "participantExOperator",  -- 참여자(외부) 시행자
    P.DESIGNER                            AS "participantExDesigner",  -- 참여자(외부) 설계자
    P.BUILDER                             AS "participantExBuilder",  -- 참여자(외부) 시공자
    P.CONSULTING                          AS "participantExConsulting",  -- 참여자(외부) 컨설팅
    P.JOINTCONTRACTOR                     AS "participantExJoint",  -- 참여자(외부) 공동수행사
    P.EXTERNALETC                         AS "participantExEtc",  -- 참여자(외부) 기타

    P.CONTRACT_DATE                       AS "contractDate",  -- 계약일자(원본)
    CASE WHEN P.CONTRACT_DATE IS NOT NULL THEN 'Y' ELSE 'N' END
                                           AS "contractYn",  -- 계약여부(파생, 계약일자 존재 여부)
    P.PARTICIPATIONINFO5_CO               AS "jointPerformCd",  -- 공동이행방식 최유력후보(단독/공동, 코드)
    P.PARTICIPATIONINFO5_CO_NM            AS "jointPerformNm",  -- 공동이행방식(명)

    -- ── 세부 필터 : 사업 ─────────────────────────────────────────────
    P.ENTERPRISETYPE                      AS "businessFieldCand",  -- 사업분야 유력후보(사업유형)
    P.DIVISIONBUSINESS2                   AS "bizMethodCd",  -- 시행방식(코드, 사업유형 하위)
    P.DIVISIONBUSINESS2_NM                AS "bizMethodNm",  -- 시행방식(명)
    P.PARTICIPATIONSTEP                   AS "participationStep",  -- 참여단계(코드)
    P.PARTICIPATIONSTEP_NM                AS "participationStepNm",  -- 참여단계(명)
    P.DIVISIONBUSINESS3                   AS "buildActCd",  -- 건축행위(코드)
    P.DIVISIONBUSINESS3_NM                AS "buildActNm",  -- 건축행위(명)
    P.TOTALEXPENSES                       AS "businessCost",  -- 사업비(원)
    P.TOTALEXPENSESP                      AS "businessCostP",  -- 사업비(평당)
    P.DESIGNEXPENSES                      AS "designPrice",  -- 설계가격(원)
    P.DESIGNEXPENSESP                     AS "designPriceP",  -- 설계가격(평당)
    P.INITIALCONSTRUCTIONEXPENSES         AS "initConstructCost",  -- 최초공사비(원)
    P.FINALCONSTRUCTIONEXPENSES           AS "finalConstructCost",  -- 최종공사비(원)

    P.DESIGNSTART_DATE                    AS "designStartDate",  -- 설계전 단계 착수일
    P.DESIGNEND_DATE                      AS "designEndDate",  -- 설계전 단계 종료일
    P.BASICDESIGNSTART_DATE               AS "basicDesignStartDate",  -- 기본설계 단계 착수일
    P.BASICDESIGNEND_DATE                 AS "basicDesignEndDate",  -- 기본설계 단계 종료일
    P.WORKINGDESIGNSTART_DATE             AS "workingDesignStartDate",  -- 실시설계 단계 착수일
    P.WORKINGDESIGNEND_DATE               AS "workingDesignEndDate",  -- 실시설계 단계 종료일
    P.PURCHASESTART_DATE                  AS "purchaseStartDate",  -- 구매조달 단계 착수일
    P.PURCHASEEND_DATE                    AS "purchaseEndDate",  -- 구매조달 단계 종료일
    P.CONSTRUCTSTART_DATE                 AS "constructStartDate",  -- 시공 단계 착수일
    P.CONSTRUCTEND_DATE                   AS "constructEndDate",  -- 시공 단계 종료일
    P.AFTERCONSTRUCTSTART_DATE            AS "afterConstructStartDate",  -- 시공후 단계 착수일
    P.AFTERCONSTRUCTEND_DATE              AS "afterConstructEndDate",  -- 시공후 단계 종료일

    -- ── 세부 필터 : 시설 ─────────────────────────────────────────────
    P.USELEVEL1                           AS "buildingUseCd",  -- 건축물의 용도(코드)
    P.USELEVEL1_NM                        AS "buildingUseNm",  -- 건축물의 용도(명)
    P.AREA                                AS "totalArea",  -- 면적(필터용 분류코드)
    P.TOTALFLOORAREA                      AS "totalFloorAreaM2",  -- 연면적(㎡, 실측값)
    P.SITEAREA                            AS "siteAreaM2",  -- 대지면적(㎡)
    P.BUILDINGAREA                        AS "buildingAreaM2",  -- 건축면적(㎡)
    P.SCALE                                AS "buildingScale",  -- 규모(필터용 분류코드)
    P.GROUNDFLOORS                        AS "groundFloors",  -- 지상 층수(실측값)
    P.UNDERGROUNDFLOORS                   AS "undergroundFloors",  -- 지하 층수
    P.CONFIRM                             AS "certGrade",  -- 등급/인증(대표코드)
    P.GREENPC                             AS "greenPc",  -- 녹색건축인증 예비인증
    P.GREENMC                             AS "greenMc",  -- 녹색건축인증 본인증
    P.BUILDINGENERGYPC                    AS "buildingEnergyPc",  -- 건축물에너지효율등급 예비인증
    P.BUILDINGENERGYMC                    AS "buildingEnergyMc",  -- 건축물에너지효율등급 본인증
    P.ZEROENERGYPC                        AS "zeroEnergyPc",  -- 제로에너지 건축물인증 예비인증
    P.ZEROENERGYMC                        AS "zeroEnergyMc",  -- 제로에너지 건축물인증 본인증
    P.DESIGNCONDITION                     AS "designConditionCd",  -- 설계조건(코드)
    P.DESIGNCONDITION_NM                  AS "designConditionNm",  -- 설계조건(명)

    -- ── 세부 필터 : 기술 ─────────────────────────────────────────────
    NULL                                   AS "structureType",  -- 구조(TODO: 확인 필요)
    NULL                                   AS "materialTech",  -- 재료기술(TODO: 확인 필요)
    P.BUILDINGMETHOD                       AS "buildMethodCd",  -- 건축(코드, 라벨 "건축공사"와 상이 확인 권장)
    P.BUILDINGMETHOD_NM                    AS "buildMethodNm",  -- 건축(명)
    P.MATERIALTECHCIVIL                    AS "civilTechCd",  -- 토목(재료기술, 코드)
    P.MATERIALTECHCIVIL_NM                 AS "civilTechNm",  -- 토목(명)
    P.MATERIALTECHMACHINE                  AS "machineTechCd",  -- 기계설비(재료기술, 코드)
    P.MATERIALTECHMACHINE_NM               AS "machineTechNm",  -- 기계설비(명)
    P.MATERIALTECHELECTRIC                 AS "electricTechCd",  -- 전기통신(재료기술, 코드)
    P.MATERIALTECHELECTRIC_NM              AS "electricTechNm",  -- 전기통신(명)
    P.MATERIALTECHFIRE                     AS "fireTechCd",  -- 소방설비(재료기술, 코드)
    P.MATERIALTECHFIRE_NM                  AS "fireTechNm",  -- 소방설비(명)
    P.PROFESSIONALMETHOD                   AS "specialtyCd",  -- 전문분야(코드)
    P.PROFESSIONALMETHOD_NM                AS "specialtyNm",  -- 전문분야(명)

    -- ── "공정" 파생 현재단계 ─────────────────────────────────────────
    CASE
        WHEN SYSDATE BETWEEN P.DESIGNSTART_DATE AND P.DESIGNEND_DATE THEN '설계전단계'
        WHEN SYSDATE BETWEEN P.BASICDESIGNSTART_DATE AND P.BASICDESIGNEND_DATE THEN '기본설계단계'
        WHEN SYSDATE BETWEEN P.WORKINGDESIGNSTART_DATE AND P.WORKINGDESIGNEND_DATE THEN '실시설계단계'
        WHEN SYSDATE BETWEEN P.PURCHASESTART_DATE AND P.PURCHASEEND_DATE THEN '구매조달단계'
        WHEN SYSDATE BETWEEN P.CONSTRUCTSTART_DATE AND P.CONSTRUCTEND_DATE THEN '시공단계'
        WHEN SYSDATE BETWEEN P.AFTERCONSTRUCTSTART_DATE AND P.AFTERCONSTRUCTEND_DATE THEN '시공후단계'
        ELSE NULL
    END                                      AS "currentProcessStage",  -- 공정 파생 현재단계

    -- ── ACL: 폴더 자체 ACL_ID 기준 권한 태그 ─────────────────────────
    D.ACL_ID                                AS "folderAclId",  -- 폴더 ACL ID
    (
        (SELECT LISTAGG(R.PRINCIPAL_TYPE || ':' || R.PRINCIPAL_ID, '|')
                WITHIN GROUP (ORDER BY R.PRINCIPAL_TYPE, R.PRINCIPAL_ID)
         FROM METAECM.V_ECM_ACL_READERS R
         WHERE R.ACL_ID = D.ACL_ID
           AND R.CAN_READ = 1
           AND R.PRINCIPAL_TYPE NOT IN ('OWNER', 'OGROUP'))
        ||
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OWNER_FLAG O WHERE O.ACL_ID = D.ACL_ID)
              THEN '|USER:' || TO_CHAR(D.CREATOR_ID) END)
        ||
        (CASE WHEN EXISTS (SELECT 1 FROM ACL_OGROUP_FLAG G WHERE G.ACL_ID = D.ACL_ID)
              THEN '|' || (SELECT LISTAGG('DEPT:' || UG.GROUP_ID, '|')
                                  WITHIN GROUP (ORDER BY UG.GROUP_ID)
                           FROM METAECM.V_ECM_USER_GROUPS UG
                           WHERE UG.USER_ID = D.CREATOR_ID)
         END)
    )                                        AS "aclPrincipalTags",  -- ACL 권한 태그 목록('|' 구분)

    -- ── 개별 공유(SHARE) 로 추가 열람 가능한 "권한 태그" 목록 (폴더 공유만 해당) ─
    (
        SELECT LISTAGG(SR.PRINCIPAL_TAG, '|') WITHIN GROUP (ORDER BY SR.PRINCIPAL_TAG)
        FROM SHARE_RESOLVED SR
        WHERE SR.TARGET_TYPE = 'FOLDER' AND SR.TARGET_ID = D.FOLDER_ID
    )                                        AS "sharePrincipalTags"  -- 공유 권한 태그 목록('|' 구분)

FROM METAECM.V_ECM_FOLDERS D
LEFT JOIN METAECM.V_ECM_PROJECTS P ON P.PROJECT_ID = D.PROJECT_ID
WHERE D.STATUS <> 'DELETED'             -- 삭제 폴더 제외 (실제 코드값은 운영 DB 확인)
;


/* =====================================================================================
   [5] 증분 색인 대상 조회 (참고)
   -------------------------------------------------------------------------------------
   위 [4-A]/[4-B]/[4-C] 쿼리를 전체 재색인(Full)에 사용하고, 운영 중에는 아래로 변경분만
   추출한다. PROCESS_FLAG='N'(신규) 행만 가져와 TARGET_TYPE/TARGET_ID로
   [4-A]의 P.PROJECT_ID(TARGET_TYPE='PROJECT'), [4-B]의 F.FILE_ID(TARGET_TYPE='FILE'),
   [4-C]의 D.FOLDER_ID(TARGET_TYPE='FOLDER') 에 각각 WHERE 절로 연결해 해당 유형만
   다시 색인한다. 처리 후 PROCESS_FLAG 를 'C'로 갱신하는 것은 SAI(색인 엔진) 측 책임이며,
   본 조회는 읽기 전용이다.
   ===================================================================================== */
SELECT SEQ, ACTED_DATE, TARGET_TYPE, TARGET_ID, DOC_ID, PROJECT_ID, IUD_TYPE
FROM METAECM.V_ECM_SAI_INCREMENTS
WHERE PROCESS_FLAG = 'N'
ORDER BY SEQ;


/* =====================================================================================
   [6] 검색 시점(쿼리 타임) — 로그인 사용자 1명의 "권한 태그" 집합 계산
   -------------------------------------------------------------------------------------
   색인 시점엔 절대 사용자로 펼치지 않았으므로, 반대로 "이 사용자가 어떤 태그를 갖는지"는
   검색 API가 매 요청마다 딱 한 사용자(:USER_ID)에 대해서만 가볍게 계산한다. 결과는
   항상 적은 수(그 사람이 속한 부서·프로젝트그룹·역할 개수 정도)라 성능 부담이 없다.
   API 서버에서 이 쿼리를 미리 실행해 태그 배열을 만든 뒤, ES 쪽에 아래처럼 질의한다:

     GET /ecm_docs/_search
     {
       "query": { "bool": { "filter": {
         "terms": { "acl_principal_tags": ["USER:1001","DEPT:30","ALIAS:WORLD", ...] }
       }}}
       -- share_principal_tags 도 같은 배열로 OR(should) 조건을 추가해 합친다
     }
   ===================================================================================== */
SELECT 'USER:' || UP.PRINCIPAL_ID AS PRINCIPAL_TAG
FROM METAECM.V_ECM_USER_PRINCIPALS UP
WHERE UP.USER_ID = :USER_ID AND UP.PRINCIPAL_TYPE = 'USER'
UNION ALL
SELECT 'ALIAS:' || UP.PRINCIPAL_ID
FROM METAECM.V_ECM_USER_PRINCIPALS UP
WHERE UP.USER_ID = :USER_ID AND UP.PRINCIPAL_TYPE = 'ALIAS'
UNION ALL
SELECT 'DEPT:' || UP.PRINCIPAL_ID
FROM METAECM.V_ECM_USER_PRINCIPALS UP
WHERE UP.USER_ID = :USER_ID AND UP.PRINCIPAL_TYPE = 'DEPT'
UNION ALL
SELECT 'PROJECT_GROUP:' || UP.PRINCIPAL_ID
FROM METAECM.V_ECM_USER_PRINCIPALS UP
WHERE UP.USER_ID = :USER_ID AND UP.PRINCIPAL_TYPE = 'PROJECT_GROUP'
UNION ALL
SELECT 'ROLE:' || UP.PRINCIPAL_ID
FROM METAECM.V_ECM_USER_PRINCIPALS UP
WHERE UP.USER_ID = :USER_ID AND UP.PRINCIPAL_TYPE = 'ROLE';
-- (이 쿼리는 한 사용자 단위라 UNION 안에서 바깥 테이블을 참조하지 않으므로
--  ORA-00904/ORA-01489 둘 다와 무관하다. :USER_ID 는 바인드 변수)
