@echo off

REM ---------------------------------
REM Set elastic
REM ---------------------------------
SET ES_HOST=192.168.221.137
SET ES_PORT=9200
SET ES_PIPELINE=attachments
SET ES_INDICES=manual

REM ---------------------------------
REM Set bulker
REM ---------------------------------
SET INDEX_MODE=all
SET INDEX_NAME=manual

REM ---------------------------------
REM Set index
REM ---------------------------------
SET USE_INDEX=Y
SET USE_DELETE=Y

REM ---------------------------------
REM Execute bat
REM ---------------------------------

for /f %%a in (%~f0) do (
    set /a numLines=!numLines!+1
    echo line !numLines!: %%a
)




CALL setupEnv.bat
CALL setupJar.bat
CALL bulker.bat
CALL index.bat

@endlocal
