#!/bin/bash

# ---------------------------------
# Set elastic
# ---------------------------------
ES_HOST=localhost
ES_PORT=9200
ES_PIPELINE=manual_pipeline
ES_INDICES=manual

# ---------------------------------
# Set bulker
# ---------------------------------
INDEX_MODE=all
INDEX_NAME=manual

# ---------------------------------
# Set index
# ---------------------------------
USE_INDEX=Y
USE_DELETE=Y

# ---------------------------------
# Set env
# ---------------------------------
CURPATH=$(dirname $(realpath $0))
source $CURPATH/setupEnv.sh
source $CURPATH/setupJar.sh

# ---------------------------------
# non-indexed target index
# ---------------------------------
if test `find $BULKER_RESULT/$INDEX_NAME -type f | wc -l` -gt 0; then
    source $CURPATH/index.sh
    source $CURPATH/extractNotIndex.sh
    source $CURPATH/resultBackup.sh
fi

# ---------------------------------
# Execute bulker, index
# ---------------------------------
if test `find $BULKER_RESULT/$INDEX_NAME -type f | wc -l` -lt 1; then
	source $CURPATH/bulker.sh
	source $CURPATH/index.sh
	source $CURPATH/extractNotIndex.sh
	source $CURPATH/resultBackup.sh
fi
