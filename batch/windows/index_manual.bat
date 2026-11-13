@echo off

REM ---------------------------------
REM Set bulker
REM ---------------------------------
REM INDEX_MODE : all/inc/send
SET INDEX_MODE=all
SET DF_NAME=manual

REM ---------------------------------
REM Set index
REM ---------------------------------
SET USE_INDEX=Y
SET USE_DELETE=Y

REM ---------------------------------
REM Execute bat
REM ---------------------------------

CALL setupEnv.bat
CALL setupJar.bat

CALL bulker.bat

@endlocal
