# 오픈소스 고지 (THIRD-PARTY NOTICES)

본 소프트웨어(`elastic_bulker`, RAY.Search.Bulker)는 아래 오픈소스 소프트웨어를 포함/사용합니다.
각 구성요소는 해당 라이선스 조건에 따라 배포됩니다. 라이선스 전문은 각 프로젝트의
배포물 또는 공식 저장소에서 확인할 수 있습니다.

> 본 프로젝트는 Ant 기반이며, 의존 라이브러리는 `lib/` 및 `lib/tika/` 에 개별 JAR 형태로 포함됩니다.
> 아래 버전은 배포물(`lib/`) 실측 기준입니다.

## Apache License 2.0
- Spring Framework 5.3.8 구성요소 (spring-core, -beans, -context, -context-support, -aop, -expression, -jdbc, -tx, -web)
- Spring Batch Infrastructure 4.3.10
- Apache Commons — commons-dbcp2 2.9.0, commons-pool2 2.9.0, commons-lang3 3.12.0, commons-logging 1.2, commons-io 2.20.0
- Apache HttpComponents — httpclient 4.5.14, httpcore 4.4.16, httpmime 4.5.14
- MyBatis 3.4.4, MyBatis-Spring 1.3.0
- Jasypt 1.9.3 및 jasypt-spring4 1.9.3
- Quartz Scheduler 2.4.0, quartz-jobs 2.4.0
- OpenCSV 5.7.1
- Google Gson 2.8.6
- Apache Tika (tika-app) 3.2.1
- Xerial SQLite JDBC 3.34.0

  라이선스 전문: https://www.apache.org/licenses/LICENSE-2.0

## MIT License
- SLF4J API 1.7.25
- jsoup 1.21.2
- Microsoft JDBC Driver for SQL Server (mssql-jdbc) 11.2.1

  라이선스 전문: https://opensource.org/licenses/MIT

## BSD License (3-Clause / Revised BSD)
- JSch (com.jcraft:jsch) 0.1.55 — SFTP 통신용

  라이선스/저장소: http://www.jcraft.com/jsch/

## Eclipse Public License 1.0 (선택) — 이중 라이선스 항목
- **Logback** (ch.qos.logback:logback-classic, logback-core) 1.2.3
  - Logback 은 EPL-1.0 또는 LGPL-2.1 이중 라이선스로 배포됩니다.
  - **본 배포물은 EPL-1.0 조건을 선택하여 사용합니다.**
  - 라이선스 전문: https://www.eclipse.org/legal/epl-v10.html

## GNU Lesser General Public License, version 2.1 (LGPL-2.1-or-later)
- **MariaDB Connector/J** (org.mariadb.jdbc:mariadb-java-client) 3.5.6
  - JDBC 드라이버로서 **동적 링크**되며, `lib/` 내 **독립 JAR** 형태로 포함되어 사용자가 **교체**할 수 있습니다. 라이브러리 소스는 **수정되지 않았습니다.**
  - 저장소: https://github.com/mariadb-corporation/mariadb-connector-j
- **CORBA API** (corba-api) 5.0.1 — OW2 Consortium
  - JDK 11+ 에서 제거된 CORBA API 를 보완하기 위한 라이브러리로, **동적 링크**되며 **독립 JAR** 로 포함되고 소스는 **수정되지 않았습니다.**
  - 라이선스 전문: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

## 독점(비오픈소스) 구성요소 — 고객사/공급사 라이선스 하에 사용
- **Oracle JDBC Driver** (ojdbc8) — Oracle Corporation 독점. Oracle 라이선스(OTN/OFUTC) 조건에 따름. Oracle 접속이 필요 없는 경우 제거 가능.
- **IBM/HCL Notes** (Notes.jar) — IBM/HCL 독점. Lotus Notes/Domino 연동 시 고객사 Notes 라이선스 범위 내에서 사용.
---

### LGPL 준수 안내
LGPL-2.1 대상 구성요소(MariaDB Connector/J, CORBA API, 그리고 LGPL 선택 시 Logback)는
소스코드를 수정하지 않고 동적 링크로만 사용되며, 별도 JAR 로 포함되어 교체가 가능합니다.
해당 라이브러리 소스코드 사본이 필요한 경우 위 공식 저장소 링크에서 입수할 수 있습니다.

### Apache Tika 관련 안내
`tika-app` 은 다수의 파서/의존 라이브러리를 포함하는 통합(uber) JAR 로, 내부 포함 구성요소는
각각의 라이선스(대부분 Apache-2.0, 일부 BSD/MIT 등)를 따릅니다. 상세 목록은 Tika 배포물의
`NOTICE`/`LICENSE` 파일을 참조하십시오.

### 제외(라이선스 준수 조치) 안내
아래 구성요소는 라이선스 준수를 위해 배포물에서 **제외**되었습니다.
- MySQL Connector/J (GPLv2) — 미사용, 제거
- jai-imageio-jpeg2000 (JJ2000, 비표준 필드제한 라이선스) — 미사용, 제거

*이 파일은 검토 시점 기준이며, 라이브러리 버전 변경 시 목록·버전을 갱신하십시오.*
