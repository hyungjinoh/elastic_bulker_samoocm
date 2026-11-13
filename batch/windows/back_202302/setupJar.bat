REM ---------------------------------
REM Set lib
REM ---------------------------------

if "%BULKER_LIB%" == "" (
	echo "BULKER_LIB is not setted."
	goto END_BATCH
)

SET JARPATH=%BULKER_LIB%\bulker.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\commons-dbcp2-2.8.0.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\commons-lang3-3.12.0.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\commons-logging-1.2.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\commons-pool2-2.9.0.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\gson-2.8.6.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\logback-classic-1.2.3.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\logback-core-1.2.3.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\mariadb-java-client-1.6.0.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\mybatis-3.4.4.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\mybatis-spring-1.3.0.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\ojdbc8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\slf4j-api-1.7.25.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-aop-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-beans-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-context-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-core-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-expression-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-jdbc-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\spring-tx-5.3.8.jar
SET JARPATH=%JARPATH%;%BULKER_LIB%\tika.jar

:END_BATCH