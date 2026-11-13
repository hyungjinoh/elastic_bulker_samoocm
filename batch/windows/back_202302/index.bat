REM ---------------------------------
REM Index
REM ---------------------------------
SET INDEXURL="%ES_HTTP%://%ES_HOST%:%ES_PORT%/%ES_INDICES%/_bulk?pipeline=%ES_PIPELINE%"
SET DELETEURL="%ES_HTTP%://%ES_HOST%:%ES_PORT%/%ES_INDICES%/_bulk?pipeline=%ES_PIPELINE%"
cd %BULKER_RESULT%\%INDEX_NAME%

for /r %%I in (index_*.json) do (
	echo [INDEX ] %date% %time% %%I
	curl -s -k %ES_USER% -XPOST %INDEXURL% --ssl-no-revoke -H "Content-Type: application/json" --data-binary @%%~nxI > %%I.log
)

for /r %%I in (delete_*.json) do (
	echo [DELETE] %date% %time% %%I
	curl -s -k %ES_USER% -XPOST %DELETEURL% --ssl-no-revoke -H "Content-Type: application/json" --data-binary @%%~nxI > %%I.log
)

REM ---------------------------------
REM Result backup
REM ---------------------------------
mkdir %BULKER_RESULT_BACKUP%\%INDEX_NAME%\%CURTIME%
copy %BULKER_RESULT%\%INDEX_NAME%\* %BULKER_RESULT_BACKUP%\%INDEX_NAME%\%CURTIME%
del %BULKER_RESULT%\%INDEX_NAME%\* /Q

cd %BULKER_HOME%\bin