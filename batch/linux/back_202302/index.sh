# ---------------------------------
# Index
# ---------------------------------
INDEXURL=$ES_HTTP://$ES_HOST:$ES_PORT/$ES_INDICES/_bulk?pipeline=$ES_PIPELINE
DELETEURL=$ES_HTTP://$ES_HOST:$ES_PORT/$ES_INDICES/_bulk

for jsonFileName in `ls $BULKER_RESULT/$INDEX_NAME`; do
	if [[ $jsonFileName == 'index_'* ]]; then
		echo '[INDEX ]' $(date "+%Y-%m-%d %H:%M:%S,") $jsonFileName
		curl -s -k -XPOST $INDEXURL $ES_USER -H "Content-Type: application/json" --data-binary @$BULKER_RESULT/$INDEX_NAME/$jsonFileName &> $BULKER_RESULT/$INDEX_NAME/$jsonFileName.log
	fi
done

for jsonFileName in `ls $BULKER_RESULT/$INDEX_NAME`; do
	if [[ $jsonFileName == 'delete_'* ]]; then
		echo '[DELETE]' $(date "+%Y-%m-%d %H:%M:%S,") $jsonFileName
		curl -s -k -XPOST $DELETEURL $ES_USER -H "Content-Type: application/json" --data-binary @$BULKER_RESULT/$INDEX_NAME/$jsonFileName &> $BULKER_RESULT/$INDEX_NAME/$jsonFileName.log
	fi
done
