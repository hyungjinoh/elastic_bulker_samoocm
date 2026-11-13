# ---------------------------------
# Result backup
# ---------------------------------
if [ ! -d $BULKER_RESULT_BACKUP/$INDEX_NAME/$CURTIME ]; then
  mkdir -p $BULKER_RESULT_BACKUP/$INDEX_NAME/$CURTIME
fi

mv $BULKER_RESULT/$INDEX_NAME/*.log $BULKER_RESULT_BACKUP/$INDEX_NAME/$CURTIME

if test `find $BULKER_RESULT/$INDEX_NAME -type f | wc -l` -gt 0; then
	mv $BULKER_RESULT/$INDEX_NAME/*.json $BULKER_RESULT_BACKUP/$INDEX_NAME/$CURTIME
fi

# ---------------------------------
# remove .tmp
# ---------------------------------
cd $BULKER_RESULT/$INDEX_NAME
find . -type f -name "*.tmp" -exec rename .json.tmp .json {} \;
cd $BULKER_BIN
