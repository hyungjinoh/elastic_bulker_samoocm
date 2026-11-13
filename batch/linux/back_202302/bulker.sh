# ---------------------------------
# Execute bulker
# ---------------------------------
CLASSPATH=$BULKER_CLASSPATH:$JARPATH

if [[ $USE_INDEX == 'Y' ]]; then
	EXEC_CLASSNAME=com.rayful.bulk.exec.Bulker
	$BULKER_JAVA_HOME/bin/java -Xms2048m -Xmx2048m -cp $CLASSPATH $EXEC_CLASSNAME -indexmode $INDEX_MODE $INDEX_NAME
fi

if [[ $USE_DELETE == 'Y' ]]; then
	EXEC_DEL_CLASSNAME=com.rayful.bulk.exec.BulkerDelete
	$BULKER_JAVA_HOME/bin/java -Xms2048m -Xmx2048m -cp $CLASSPATH $EXEC_DEL_CLASSNAME -indexmode $INDEX_MODE $INDEX_NAME
fi
