# ---------------------------------
# Set date
# ---------------------------------
CURDATE=`date +%Y%m%d`
CURTIME=`date +%H%M%S.%S`

# ---------------------------------
# Set path
# ---------------------------------
BULKER_JAVA_HOME=/man/maru/elasticsearch-7.13.1/jdk
BULKER_HOME=/man/maru/bulker
BULKER_BIN=$BULKER_HOME/bin
BULKER_CONFIG=$BULKER_HOME/config
BULKER_LIB=$BULKER_HOME/lib
BULKER_CLASSPATH=$BULKER_HOME:$BULKER_CONFIG
BULKER_RESULT=$BULKER_HOME/result
BULKER_RESULT_BACKUP=$BULKER_HOME/result_backup/$CURDATE

# ---------------------------------
# Set elastic
# ---------------------------------
ES_HTTP=https
ES_USER="-u elastic:@fpdlvnf"
ES_APIKEY="Authorization: xxxxx"
