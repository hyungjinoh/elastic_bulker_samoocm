-- =================================================================================================
-- METAECM.FW_PROJECT_INFO 전체 컬럼 조회 쿼리 (컬럼별 주석 포함)
-- 원본 COMMENT ON COLUMN 정의를 그대로 인라인 주석으로 반영
-- =================================================================================================
SELECT
    PROJECT_INFO_ID,               -- 프로젝트 정보 ID
    BUSINESSINFO,                  -- 사업
    PROJECTNO,                     -- 프로젝트 번호
    PJTNAME,                       -- 프로젝트명
    MISNAME,                       -- 등록명(국문)
    MISNAMEENG,                    -- 등록명(영문)
    CONTRACTNAME,                  -- 계약명(국문)
    CONTRACTNAMEENG,               -- 계약명(영문)
    NAME,                          -- 파일명
    NAMEENG,                       -- 준공명(영문)
    SUBPROJECTNO,                  -- 종속프로젝트 일련번호
    SUBMISNAME,                    -- 종속프로젝트 구분타이틀
    SECURITYLEVEL,                 -- 보안등급
    PROJECTGUBUN,                  -- 표준폴더
    "POSITION",                    -- 위치
    COUNTRY,                       -- 국내
    CBEFORECOMPLETED,              -- 국내 준공전
    CITYBEFORE,                    -- 국내 준공전 시/도
    TOWNBEFORE,                    -- 국내 준공전 시/군/구
    DETAILEDADDRESSBEFORE,         -- 국내 준공전 세부주소
    CAFTERCOMPLETED,               -- 국내 준공후
    CITYAFTER,                     -- 국내 준공후 시/도
    TOWNAFTER,                     -- 국내 준공후 시/군/구
    DETAILEDADDRESSAFTER,          -- 국내 준공후 세부주소
    OVERSEAS,                      -- 해외
    OBEFORECOMPLETED,              -- 해외 준공전
    NATIONBEFORE,                  -- 해외 준공전 국가
    NATIONDETAILEDADDRESSBEFORE,   -- 해외 준공전 세부주소
    OAFTERCOMPLETED,               -- 해외 준공후
    NATIONAFTER,                   -- 해외 준공후 국가
    NATIONDETAILEDADDRESSAFTER,    -- 해외 준공후 세부주소
    ENTERPRISETYPE,                -- 사업유형
    DIVISIONBUSINESS1,             -- 사업주체
    DIVISIONBUSINESS2,             -- 시행방식
    DIVISIONBUSINESS2ETC,          -- 시행방식 기타
    DIVISIONBUSINESS3,             -- 건축행위
    DIVISIONBUSINESS3ETC,          -- 건축행위 기타
    BUSINESSEXPENSES,              -- 사업비
    COMMONE,                       -- 공통
    TOTALEXPENSES,                 -- 사업비(￦)
    TOTALEXPENSESP,                -- 사업비(평당)
    TOTALEXPENSESC,                -- 사업비-해외
    DESIGNEXPENSES,                -- 설계가격(￦)
    DESIGNEXPENSESP,               -- 설계가격(평당)
    DESIGNEXPENSESC,               -- 설계가격-해외
    INITIALCONSTRUCTIONEXPENSES,   -- 공사비(최초(￦))
    INITIALCONSTRUCTIONEXPENSESP,  -- 공사비(최초)(평당)
    INITIALCONSTRUCTIONEXPENSESC,  -- 공사비(최초)-해외
    FINALCONSTRUCTIONEXPENSES,     -- 공사비(최종(￦))
    FINALCONSTRUCTIONEXPENSESP,    -- 공사비(최종)(평당)
    FINALCONSTRUCTIONEXPENSESC,    -- 공사비(최종)-해외
    COSTPRICE,                     -- 원가
    DESIGNPRICE,                   -- 설계가격
    DCONSTRUCTION,                 -- 설계가격 건축
    DMACHINE,                      -- 설계가격 기계
    DELECTRIC,                     -- 설계가격 전기
    DCOMMUNICATION,                -- 설계가격 통신
    DFIREMACHINE,                  -- 설계가격 소방(기계)
    DFIREELECTRIC,                 -- 설계가격 소방(전기)
    DCIVIL,                        -- 설계가격 토목
    DLANDSCAPING,                  -- 설계가격 조경
    COSTPRICEETC,                  -- 기타
    CONSTRUCTIONPRICE,             -- 공사비(최초)
    CCONSTRUCTION,                 -- 공사비(최초) 건축
    CMACHINE,                      -- 공사비(최초) 기계
    CELECTRIC,                     -- 공사비(최초) 전기
    CCOMMUNICATION,                -- 공사비(최초) 통신
    CFIREMACHINE,                  -- 공사비(최초) 소방(기계)
    CFIREELECTRIC,                 -- 공사비(최초) 소방(전기)
    CCIVIL,                        -- 공사비(최초) 토목
    CLANDSCAPING,                  -- 공사비(최초) 조경
    CONSTRUCTIONPRICEETC,          -- 기타
    COMPLETIONPRICE,               -- 공사비(최종)
    PCONSTRUCTION,                 -- 공사비(최종) 건축
    PMACHINE,                      -- 공사비(최종) 기계
    PELECTRIC,                     -- 공사비(최종) 전기
    PCOMMUNICATION,                -- 공사비(최종) 통신
    PFIREMACHINE,                  -- 공사비(최종) 소방(기계)
    PFIREELECTRIC,                 -- 공사비(최종) 소방(전기)
    PCIVIL,                        -- 공사비(최종) 토목
    PLANDSCAPING,                  -- 공사비(최종) 조경
    COMPLETIONPRICEETC,            -- 기타
    PROCESS,                       -- 허용 PROCESS 정보
    COMMON,                        -- 공통
    PROJECTCREATED_DATE,           -- 등록일자
    LASTTRANSFERD_DATE,            -- 이관일자
    CONTRACT_DATE,                 -- 계약일자
    PROJECTAPPROVAL_DATE,          -- 사업계획승인일자
    BUILDINGPERMIT_DATE,           -- 건축허가일자
    CONSTRUCTION_DATE,             -- 착공일자
    COMPLETIONAPPROVAL_DATE,       -- 사용승인일자
    PROGRESS,                      -- 진행상황
    STARTDATE,                     -- 착수일
    ENDDATE,                       -- 종료일
    SSTEP,                         -- 단계
    DESIGNSTART_DATE,              -- 설계전 단계(착수일)
    DESIGNEND_DATE,                -- 설계전 단계(종료일)
    DESIGNPERIOD,                  -- 설계전 단계 기간
    BASICDESIGNSTART_DATE,         -- 기본설계 단계(착수일)
    BASICDESIGNEND_DATE,           -- 기본설계 단계(종료일)
    BASICDESIGNPERIOD,             -- 기본설계 기간
    WORKINGDESIGNSTART_DATE,       -- 실시설계 단계(착수일)
    WORKINGDESIGNEND_DATE,         -- 실시설계 단계(종료일)
    WORKINGDESIGNPERIOD,           -- 실시설계 단계 기간
    PURCHASESTART_DATE,            -- 구매조달 단계(착수일)
    PURCHASEEND_DATE,              -- 구매조달 단계(종료일)
    PURCHASEPERIOD,                -- 구매조달 단계 기간
    CONSTRUCTSTART_DATE,           -- 시공 단계(착수일)
    CONSTRUCTEND_DATE,             -- 시공 단계(종료일)
    CONSTRUCTPERIOD,               -- 시공 기간
    AFTERCONSTRUCTSTART_DATE,      -- 시공후 단계(착수일)
    AFTERCONSTRUCTEND_DATE,        -- 시공후 단계(종료일)
    AFTERCONSTRUCTPERIOD,          -- 시공후 단계 기간
    SCP,                           -- CP
    INSTALLSTART_DATE,             -- 가설 및 공사 준비(착수일)
    INSTALLEND_DATE,               -- 가설 및 공사 준비(종료일)
    RETAININGSTART_DATE,           -- 흙막이공사(착수일)
    RETAININGEND_DATE,             -- 흙막이공사(종료일)
    DIGSTART_DATE,                 -- 터파기공사(착수일)
    DIGEND_DATE,                   -- 터파기공사(종료일)
    FOUNDATIONWORKSTART_DATE,      -- 기초공사(착수일)
    FOUNDATIONWORKEND_DATE,        -- 기초공사(종료일)
    UNDERGROUNDFRAMESTART_DATE,    -- 지하골조(착수일)
    UNDERGROUNDFRAMEEND_DATE,      -- 지하골조(종료일)
    GROUNDFRAMESTART_DATE,         -- 지상골조(착수일)
    GROUNDFRAMEEND_DATE,           -- 지상골조(종료일)
    FINISHCONSTRUCTSTART_DATE,     -- 골조완료 후 마감공사(착수일)
    FINISHCONSTRUCTEND_DATE,       -- 골조완료 후 마감공사(종료일)
    TESTSTART_DATE,                -- 준공청소 및 시운전(착수일)
    TESTEND_DATE,                  -- 준공청소 및 시운전(종료일)
    CPETCSTART_DATE,               -- 기타(착수일)
    CPETCEND_DATE,                 -- 기타(종료일)
    CPETC,                         -- 기타
    BOOKDELIVERY,                  -- 도서납품
    PLANDESIGNDRAWING_DATE,        -- 기획설계도서
    BASICDESIGNDRAWING_DATE,       -- 기본설계도서
    WORKINGDESIGNDRAWING_DATE,     -- 실시설계도서
    APPROVALDRAWING_DATE,          -- 인허가도서
    STARTCONSTRUCTDRAWING_DATE,    -- 착공도서
    COMPLETEDRAWING_DATE,          -- 준공도서
    DESIGNCHANGEDRAWING_DATE,      -- 설계변경도서
    PARTICIPATIONINFO,             -- 참여정보
    PARTICIPATIONINFO1,            -- 수주형태
    PARTICIPATIONINFO1ETC,         -- 수주형태 기타
    PARTICIPATIONINFO3,            -- 용역형태(일반)
    PARTICIPATIONINFO3ETC,         -- 용역형태(일반) 기타
    PARTICIPATIONINFO4,            -- 용역형태(상세)
    PARTICIPATIONINFO4_CM,         -- CM
    PARTICIPATIONINFO4_CMETC,      -- CM 기타
    PARTICIPATIONINFO4_DE,         -- 설계
    PARTICIPATIONINFO4_DEETC,      -- 설계 기타
    PARTICIPATIONINFO2,            -- 참여/당선
    PARTICIPATIONINFO5_CO,         -- 단독/공동
    PARTICIPATIONINFO5_CM,         -- 주관/부관
    PARTICIPANT,                   -- 참여자
    PARTICIPANTADD,                -- 수행자
    FACILITIESINFO,                -- 시설
    FACILITIESTYPE,                -- 시설유형
    FACILITIESTYPEETC,             -- 시설유형 기타
    BUILDINGUSE,                   -- 용도
    REGIONLEVEL1,                  -- 용도지역1
    REGIONLEVEL2,                  -- 용도지역2
    REGIONLEVEL3,                  -- 용도지역3
    REGIONLEVEL4,                  -- 용도지역4
    ZONELEVEL1,                    -- 용도지구1
    ZONELEVEL2,                    -- 용도지구2
    AREALEVEL1,                    -- 용도구역
    USEETCLEVEL1,                  -- 기타구역
    USELEVEL1,                     -- 건축물의 용도1
    USELEVEL2,                     -- 건축물의 용도2
    USELEVEL1ETC,                  -- 건축물의 용도1 기타
    USEADD,                        -- 건축물유형
    USEADDETC,                     -- 건축물유형 기타
    AREA,                          -- 면적
    SITEAREA,                      -- 대지면적(㎡)
    SITEAREAP,                     -- 대지면적(평)
    BUILDINGAREA,                  -- 건축면적(㎡)
    BUILDINGAREAP,                 -- 건축면적(평)
    TOTALFLOORAREA,                -- 연면적(㎡)
    TOTALFLOORAREAP,               -- 연면적(평)
    TOTALFLOORGROUNDAREA,          -- 연면적 지상(㎡)
    TOTALFLOORGROUNDAREAP,         -- 연면적 지상(평)
    TOTALFLOORUNDERGROUNDAREA,     -- 연면적 지하(㎡)
    TOTALFLOORUNDERGROUNDAREAP,    -- 연면적 지하(평)
    FLOORRATIOAREA,                -- 용적률산정용(㎡)
    FLOORRATIOAREAP,               -- 용적률산정용(평)
    BUSINESSAREA,                  -- 사업지면적(㎡)
    BUSINESSAREAP,                 -- 사업지면적(평)
    BUILDINGLANDRATIO,             -- 건폐율(%)
    FLOORAREARATIO,                -- 용적률(%)
    PARK,                          -- 주차
    PARKINGSPACE,                  -- 옥내외(대)
    INDOORPARKINGSPACE,            -- 옥내
    OUTDOORPARKINGSPACE,           -- 옥외
    LAWPARKINGSPACE,               -- 법정
    PLANPARKINGSPACE,              -- 계획주차대수
    MECHANICALPARKING,             -- 주차방식
    "SCALE",                       -- 규모
    GROUNDFLOORS,                  -- 지상층
    UNDERGROUNDFLOORS,             -- 지하층
    MAXDEPTHEXCAVATION,            -- 최대 굴착깊이(m)
    MAXHEIGHT,                     -- 최대 높이(m)
    ELEVATORCNT,                   -- 승강기(대)
    ELEVATOR,                      -- 승용
    EMERGENCYLIFT,                 -- 비상용
    BUILDINGCNT,                   -- 동(수)
    MAINBUILDINGNUM,               -- 주건축물
    SUBBUILDINGNUM,                -- 부속건축물
    UNIT,                          -- Unit(수)
    GENERATION,                    -- 세대
    ROOM,                          -- 실
    PARLOR,                        -- 객실
    SICKBED,                       -- 병상
    EVALUATION,                    -- 조사/평가
    EVALUATIONETC,                 -- 조사/평가 기타
    DELIBERATION,                  -- 심의/승인/허가
    DELIBERATIONETC,               -- 심의/승인/허가 기타
    CONFIRM,                       -- 등급/인증
    GREENC,                        -- 녹색건축인증
    GREENCERTIFICATION,            -- 녹색건축인증
    GREENPC,                       -- 녹색건축인증 예비인증
    GREENMC,                       -- 녹색건축인증 본인증
    BUILDINGENERGY,                -- 건축물에너지효율등급
    BUILDINGENERGYRATING,          -- 건축물에너지효율등급
    BUILDINGENERGYPC,              -- 건축물에너지효율등급 예비인증
    BUILDINGENERGYMC,              -- 건축물에너지효율등급 본인증
    ZEROENERGYC,                   -- 제로에너지 건축물 인증
    ZEROENERGYCERTIFICATION,       -- 제로에너지 건축물 인증
    ZEROENERGYPC,                  -- 제로에너지건축물인증 예비인증
    ZEROENERGYMC,                  -- 제로에너지건축물인증 본인증
    INCENTIVE,                     -- 완화/인센티브
    INCENTIVEK,                    -- 완화/인센티브
    INCENTIVEP,                    -- 완화/인센티브
    ENERGYCONSERVATIONPLAN,        -- 에너지절약계획서
    ENERGYCONSERVATIONPLANK,       -- 에너지절약계획서
    ENERGYCONSERVATIONPLANP,       -- 에너지절약계획서
    RENEWABLEENERGY,               -- 신재생에너지
    RENEWABLEENERGYK,              -- 신재생에너지
    RENEWABLEINSTALLATIONRATIO,    -- 신재생에너지_설치비율
    RENEWABLEENERGYP,              -- 신재생에너지
    RENEWABLEENERGYPETC,           -- 신재생에너지 기타
    BFCERTIFICATIONK,              -- BF 인증
    BFCERTIFICATION,               -- BF 인증
    CPTEDCERTIFICATIONK,           -- CPTED
    CPTEDCERTIFICATION,            -- CPTED
    IBCERTIFICATIONK,              -- 지능형 건축물
    IBCERTIFICATION,               -- 지능형 건축물
    BEMSCERTIFICATIONK,            -- BEMS
    BEMSCERTIFICATION,             -- BEMS
    LEEDCERTIFICATIONK,            -- LEED
    LEEDCERTIFICATION,             -- LEED
    HSINFOBUILDING,                -- 초고속정보통신건물 인증
    HSINFOBUILDINGK,               -- 초고속정보통신건물 인증
    HSINFOBUILDINGPC,              -- 초고속정보통신건물 인증 예비인증
    HSINFOBUILDINGMC,              -- 초고속정보통신건물 인증 본인증
    HOMENETWORKCERTIFICATION,      -- 홈네트워크건물 인증
    HOMENETWORKCERTIFICATIONK,     -- 홈네트워크건물 인증
    HOMENETWORKPC,                 -- 홈네트워크건물 인증 예비인증
    HOMENETWORKMC,                 -- 홈네트워크건물 인증 본인증
    LONGLIFECERTIFICATIONK,        -- 장수명주택 인증
    LONGLIFECERTIFICATION,         -- 장수명주택 인증
    CERTIFICATIONETC,              -- 인증 기타
    CERTIFICATIONETCK,             -- 기타
    CERTIFICATIONNAME,             -- 인증명
    CERTIFICATIONGRADE,            -- 등급
    PATENT,                        -- 신기술/특허
    DESIGNCONDITION,               -- 설계조건
    DESIGNCONDITIONETC,            -- 설계조건 기타
    SITECONDITION,                 -- 대지조건
    SITECONDITIONETC,              -- 대지조건 기타
    SOILCONDITION,                 -- 토질조건
    SOILCONDITIONETC,              -- 토질조건 기타
    ETCINFO,                       -- 기타정보
    AWARDHISTORY,                  -- 수상이력
    PRINTHISTORY,                  -- 출판이력
    MASTERPHOTO,                   -- 대표사진
    CGPHOTO,                       -- CG
    CONSTRUCTIONPHOTO,             -- 준공사진
    SKILLINFO,                     -- 기술
    "METHOD",                      -- 공법
    CONSTRUCTIONMETHOD,            -- 가설
    GROUNDMETHOD,                  -- 지반
    "STRUCTURE",                   -- 구조
    STRUCTURALMETHODG,             -- 지상
    STRUCTURALMETHODU,             -- 지하
    STRUCTURALMETHODC,             -- 콘크리트
    STRUCTURALMETHODS,             -- 강구조
    BUILDINGMETHOD,                -- 건축
    FACILITYMETHOD,                -- 설비
    PROFESSIONALMETHOD,            -- 전문분야
    PROFESSIONALMETHOD2,           -- 전문분야2
    MATEQUIP,                      -- 자재/장비
    MATERIALS,                     -- 자재
    EQUIPMENT,                     -- 장비
    WORKINFO,                      -- 업무/자료
    "WORK",                        -- 업무
    WORKINGTYPE,                   -- 공종
    WORKINGTYPEETC,                -- 공종 기타
    MATERIALTYPE,                  -- 유형
    MATERIALTYPEETC,               -- 유형 기타
    "SCOPE",                       -- 업무범위
    SCOPECM,                       -- CM
    SCOPECMC1,                     -- 공통업무
    SCOPECMC1ETC,                  -- 공통업무 기타
    SCOPECMC2,                     -- 설계전 단계
    SCOPECMC2ETC,                  -- 설계전 단계 기타
    SCOPECMC3,                     -- 기본설계 단계
    SCOPECMC3ETC,                  -- 기본설계 단계 기타
    SCOPECMC4,                     -- 실시설계 단계
    SCOPECMC4ETC,                  -- 실시설계 단계 기타
    SCOPECMC5,                     -- 구매조달 단계
    SCOPECMC5ETC,                  -- 구매조달 단계 기타
    SCOPECMC6,                     -- 시공단계
    SCOPECMC6ETC,                  -- 시공단계 기타
    SCOPECMC7,                     -- 시공후 단계
    SCOPECMC7ETC,                  -- 시공후 단계 기타
    SCOPECMD,                      -- 설계
    SCOPECMD1,                     -- 기획설계
    SCOPECMD1ETC,                  -- 기획설계 기타
    SCOPECMD2,                     -- 계획설계
    SCOPECMD2ETC,                  -- 계획설계 기타
    SCOPECMD3,                     -- 기본설계
    SCOPECMD3ETC,                  -- 기본설계 기타
    SCOPECMD4,                     -- 실시설계
    SCOPECMD4ETC,                  -- 실시설계 기타
    SCOPECMD5,                     -- 완성 후 업무
    SCOPECMD5ETC,                  -- 완성 후 업무 기타
    MATERIAL,                      -- 자료
    MATERIALTYPEA,                 -- 유형A
    MATERIALTYPEAETC,              -- 유형A 기타
    INNERPART,                     -- 내부
    DEPARTMENTPMID,                -- 대표PMID
    DEPARTMENTPMNM,                -- 대표PM
    GENERALMANAGERID,              -- 단장ID
    GENERALMANAGERNM,              -- 단장
    OUTERPART,                     -- 외부
    CONSUMER,                      -- 수요자
    CONTRACTORDERER,               -- 발주자
    ADMITTER,                      -- 허가권자
    INSPECTOR,                     -- 감독자
    CONSTRUCTIONPM,                -- 건설사업관리자
    PROJECTOPERATOR,               -- 시행자
    DESIGNER,                      -- 설계자
    BUILDER,                       -- 시공자
    CONSULTING,                    -- 컨설팅
    REGIONLEVEL1ETC,               -- 용도지역1 기타
    ZONELEVEL1ETC,                 -- 용도지구1 기타
    AREALEVEL1ETC,                 -- 용도구역 기타
    USEETCLEVEL1ETC,               -- 기타구역 기타
    DESIGNINFO,                    -- 설계 정보
    TRANSFERSTATUS,                -- 이관여부 코드
    SITEGUBUN1,                    -- 표준폴더 현장구분
    PROJECTGUBUN2,                 -- 표준폴더2
    SITEGUBUN2,                    -- 표준폴더2 현장구분
    PROJECTGUBUN2TITLE,            -- 표준폴더2 구분타이틀
    SITEOPENYN,                    -- 참여정보 현장개설여부
    ORDERDEPT,                     -- 참여정보 수주부서
    WORKGUBUN,                     -- 참여정보 업무구분
    SERVICEGUBUN,                  -- 참여정보 용역구분
    PARTICIPATIONSTEP,             -- 참여정보 참여단계
    SALESPMNM,                     -- 내부 영업PM
    SALESPANM,                     -- 내부 영업PA
    OPERATIONPANM,                 -- 내부 운영PA
    ORIGINALORDERER,               -- 외부 원발주자
    JOINTCONTRACTOR,               -- 외부 공동수행사
    EXTERNALETC,                   -- 외부 기타
    LOCATIONGUBUN,                 -- 위치 구분
    BASECURRENCY,                  -- 공통 기준통화
    PRETRANSFER_DATE,              -- 공통 사전이관일자
    PROJECTEND_DATE,               -- 공통 종료일자
    ZEROENERGYEVALMETHOD,          -- 본인증(등급) 평가방식
    BFCERTIFICATIONPC,             -- BF 인증 BF 예비인증(등급)
    IBCERTIFICATIONPC,             -- 지능형 건축물 지능형 건축물(예비등급)
    LEEDVERSION,                   -- LEED 버전
    ECOHOUSECERT,                  -- 장수명주택 인증 에너지 절약형 친환경주택
    HEALTHFRIENDLYTYPE,            -- 장수명주택 인증 건강친화형 주택 인증
    EDUSAFETYCERT,                 -- 교육시설안전인증 시설안전(등급) (A001 우수 / A002 최우수)
    ETCINFOGUBUN,                  -- 기타정보
    MATERIALTECHCIVIL,             -- 재료기술 토목
    MATERIALTECHMACHINE,           -- 재료기술 기계설비
    MATERIALTECHELECTRIC,          -- 재료기술 전기통신
    MATERIALTECHFIRE,              -- 재료기술 소방설비
    SMARTCONSTRUCTIONTECH,         -- 스마트건설 스마트건설기술
    PROJECTGUBUNTITLE,             -- 표준폴더1 구분타이틀
    EXCHANGERATE,                  -- 사업비 공통 환율 (기준통화 -> 원화)
    EXCHANGEDATE,                  -- 사업비 공통 환율 기준일
    PRETRANSFERYN1,                -- 표준폴더1 사전이관여부 (A001 사전이관)
    PRETRANSFERYN2,                -- 표준폴더2 사전이관여부 (A001 사전이관)
    EDUSAFETYINDOOR,               -- 교육시설안전인증 실내환경안전(등급) (A001 우수 / A002 최우수)
    EDUSAFETYOUTDOOR,              -- 교육시설안전인증 외부환경안전(등급) (A001 우수 / A002 최우수)
    OPERATIONPMNM,                 -- 운영PM
    SALESPMID,                     -- 영업PM 사번
    SALESPAID,                     -- 영업PA 사번
    OPERATIONPMID,                 -- 운영PM 사번
    OPERATIONPAID,                 -- 운영PA 사번
    DATACENTERCAPACITY,            -- 시설 규모 Unit - MW(데이터센터 수전용량)
    WITHDRAWPLAN_DATE1,            -- 표준폴더1 철수예정일 (관리자 전용)
    WITHDRAWPLAN_DATE2,            -- 표준폴더2 철수예정일 (관리자 전용)
    SMARTLOGISTICSK,               -- 스마트물류센터인증 여부 (A001 스마트물류센터인증)
    SMARTLOGISTICSPC,              -- 스마트물류센터인증 예비인증 등급 (A001~A005 1등급~5등급)
    SMARTLOGISTICSMC               -- 스마트물류센터인증 본인증 등급 (A001~A005 1등급~5등급)
FROM METAECM.FW_PROJECT_INFO;
