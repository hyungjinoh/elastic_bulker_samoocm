# ---------------------------------
# Set lib
# ---------------------------------
JARPATH=$BULKER_LIB/bulker.jar
JARPATH=$JARPATH:$BULKER_LIB/commons-dbcp2-2.9.0.jar
JARPATH=$JARPATH:$BULKER_LIB/commons-lang3-3.12.0.jar
JARPATH=$JARPATH:$BULKER_LIB/commons-logging-1.2.jar
JARPATH=$JARPATH:$BULKER_LIB/commons-pool2-2.9.0.jar
JARPATH=$JARPATH:$BULKER_LIB/gson-2.8.6.jar
#   [SECURITY UPGRADE] replace above with: gson-2.11.0.jar

# JDK17로 올리고, TIKA 버전 변경하면 아래 2개의 jar 추가 필요
JARPATH=$JARPATH:$BULKER_LIB/httpclient-4.5.14.jar
JARPATH=$JARPATH:$BULKER_LIB/httpcore-4.4.16.jar
JARPATH=$JARPATH:$BULKER_LIB/httpmime-4.5.14.jar

JARPATH=$JARPATH:$BULKER_LIB/jsch-0.1.55.jar
#   [SECURITY UPGRADE] replace above with: jsch-2.28.4.jar (com.github.mwiede, drop-in, package com.jcraft.jsch)
JARPATH=$JARPATH:$BULKER_LIB/logback-classic-1.2.3.jar
#   [SECURITY UPGRADE] replace above with: logback-classic-1.2.13.jar
JARPATH=$JARPATH:$BULKER_LIB/logback-core-1.2.3.jar
#   [SECURITY UPGRADE] replace above with: logback-core-1.2.13.jar
JARPATH=$JARPATH:$BULKER_LIB/mybatis-3.4.4.jar
JARPATH=$JARPATH:$BULKER_LIB/mybatis-spring-1.3.0.jar
JARPATH=$JARPATH:$BULKER_LIB/opencsv-5.7.1.jar
JARPATH=$JARPATH:$BULKER_LIB/quartz-2.4.0-SNAPSHOT.jar
#   [SECURITY UPGRADE] replace above with: quartz-2.3.2.jar
JARPATH=$JARPATH:$BULKER_LIB/quartz-jobs-2.4.0-SNAPSHOT.jar
#   [SECURITY UPGRADE] replace above with: quartz-jobs-2.3.2.jar
JARPATH=$JARPATH:$BULKER_LIB/slf4j-api-1.7.25.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-aop-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-aop-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-beans-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-beans-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-context-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-context-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-context-support-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-context-support-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-core-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-core-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-expression-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-expression-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-jdbc-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-jdbc-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-tx-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-tx-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-web-5.3.8.jar
#   [SECURITY UPGRADE] replace above with: spring-web-5.3.39.jar
JARPATH=$JARPATH:$BULKER_LIB/spring-batch-infrastructure-4.3.10.jar
JARPATH=$JARPATH:$BULKER_LIB/jasypt-1.9.3.jar
JARPATH=$JARPATH:$BULKER_LIB/jasypt-spring4-1.9.3.jar

JARPATH=$JARPATH:$BULKER_LIB/tika/commons-io-2.20.0.jar
JARPATH=$JARPATH:$BULKER_LIB/tika/jsoup-1.21.2.jar
JARPATH=$JARPATH:$BULKER_LIB/tika/sqlite-jdbc-3.34.0.jar
#   [SECURITY UPGRADE] replace above with: sqlite-jdbc-3.45.3.0.jar
JARPATH=$JARPATH:$BULKER_LIB/tika/tika-app-3.2.1.jar

### mariadb jdbc
JARPATH=$JARPATH:$BULKER_LIB/mariadb-java-client-3.5.6.jar

### mysql jdbc
# JARPATH=$JARPATH:$BULKER_LIB/mysql-connector-j-9.4.0.jar

### mssql jdbc
# JARPATH=$JARPATH:$BULKER_LIB/mssql-jdbc-11.2.1.jre8.jar

### oracle jdbc
#JARPATH=$JARPATH:$BULKER_LIB/ojdbc8.jar

### Notes  jar...
#JARPATH=$JARPATH:$BULKER_LIB/corba-api-5.0.1.jar
#JARPATH=$JARPATH:$BULKER_LIB/Notes.jar
