REM ---------------------------------
REM Set date
REM ---------------------------------
SET CURDATE=%date:-=%
SET CURTIME=%time::=%

REM ---------------------------------
REM Set path
REM ---------------------------------
SET BULKER_JAVA_HOME=C:\Programs\openjdk-1.8.0.191
SET BULKER_HOME=C:\elastic\elastic_bulker
SET BULKER_BIN=%BULKER_HOME%\bin
SET BULKER_CONFIG=%BULKER_HOME%\config
SET BULKER_LIB=%BULKER_HOME%\lib
SET BULKER_CLASSPATH=%BULKER_HOME%;%BULKER_CONFIG%
SET BULKER_RESULT=%BULKER_HOME%\result
SET BULKER_RESULT_BACKUP=%BULKER_HOME%\result_backup\%CURDATE%

REM ---------------------------------
REM Set elastic
REM ---------------------------------
SET ES_HTTP=https
SET ES_USER=-u elastic:@fpdlvnf
SET ES_APIKEY="Authorization: xxxxx"
