REM ---------------------------------
REM Execute bulker
REM ---------------------------------
SET CLASSPATH=%BULKER_CLASSPATH%;%JARPATH%

IF "%USE_INDEX%" == "Y" (
	SET EXEC_CLASSNAME=com.rayful.bulk.exec.Bulker
	"%JAVA_HOME%\bin\java.exe" -Xms2048m -Xmx2048m -cp %CLASSPATH% %EXEC_CLASSNAME% -indexmode %INDEX_MODE% %INDEX_NAME%
)

IF "%USE_DELETE%" == "Y" (
	SET EXEC_DEL_CLASSNAME=com.rayful.bulk.exec.BulkerDelete
	"%JAVA_HOME%\bin\java.exe" -Xms2048m -Xmx2048m -cp %CLASSPATH% %EXEC_DEL_CLASSNAME% -indexmode %INDEX_MODE% %INDEX_NAME%
)
