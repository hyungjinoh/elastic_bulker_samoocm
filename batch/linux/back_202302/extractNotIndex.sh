# ---------------------------------
# Execute reIndex
# ---------------------------------
CLASSPATH=$BULKER_CLASSPATH:$JARPATH

EXEC_CLASSNAME=com.rayful.bulk.exec.ExtractNotIndex
$BULKER_JAVA_HOME/bin/java -Xms2048m -Xmx2048m -cp $CLASSPATH $EXEC_CLASSNAME $INDEX_NAME
