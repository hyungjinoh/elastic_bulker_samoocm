# ---------------------------------
# Execute bulker
# ---------------------------------
CLASSPATH=$BULKER_CLASSPATH:$JARPATH

if [ "$MEMORY" == "" ]; then
        if [ "$INDEX_MODE" == "all" ]; then
                MEMORY=2048m
        else
                MEMORY=1024m
        fi
fi

if [ "$LOG_NAME" == "" ]; then
        LOG_NAME=$DF_NAME.$INDEX_MODE.`date +'%H%M%S'`
fi

ETC_OPTIONS=""
if [ "$MAX_ROWS" != "" ] && [ $MAX_ROWS -gt 0 ]; then
	ETC_OPTIONS="$ETC_OPTIONS -maxrows $MAX_ROWS";
fi
if [ "$PRINT_DETAIL" != "" ]; then
	ETC_OPTIONS="$ETC_OPTIONS -printdetail $PRINT_DETAIL";
fi
if [ "$PRINT_INTERVAL" != "" ]; then
	ETC_OPTIONS="$ETC_OPTIONS -printinterval $PRINT_INTERVAL";
fi

JAVAOPTIONS="$JAVAOPTIONS -XX:+UseG1GC "

if [ $USE_INDEX == 'Y' ]; then
        EXEC_CLASSNAME=com.rayful.bulk.exec.BulkerNotes
        $BULKER_JAVA_HOME/bin/java $JAVAOPTIONS -Xms$MEMORY -Xmx$MEMORY -Dlogname=$LOG_NAME -cp $CLASSPATH $EXEC_CLASSNAME -indexmode $INDEX_MODE -dfname $DF_NAME $ETC_OPTIONS
fi

if [ $USE_DELETE == 'Y' ]; then
        EXEC_DEL_CLASSNAME=com.rayful.bulk.exec.BulkerDelete
        $BULKER_JAVA_HOME/bin/java $JAVAOPTIONS -Xms$MEMORY -Xmx$MEMORY -Dlogname="$LOG_NAME"_delete -cp $CLASSPATH $EXEC_DEL_CLASSNAME -indexmode $INDEX_MODE -dfname $DF_NAME $ETC_OPTIONS
fi

# Softech 전용
#if [ $USE_ENTITY == 'Y' ] && [ "$INDEX_MODE" == "all" ]; then
#	EXEC_ENTITY_CLASSNAME=com.rayful.bulk.exec.BulkerEntity
#	$BULKER_JAVA_HOME/bin/java $JAVAOPTIONS -Xms$MEMORY -Xmx$MEMORY -Dlogname="$DF_NAME"_entity -cp $CLASSPATH $EXEC_ENTITY_CLASSNAME -indexmode $INDEX_MODE -dfname $DF_NAME $ETC_OPTIONS
#fi
