#!/bin/bash

# ---------------------------------
# get path info
# ---------------------------------
SH_FILENAME=$(basename "$0" ".sh")
SH_BASEPATH=$(cd "$(dirname "$0")" && pwd)
PID_FILE=$SH_BASEPATH/$SH_FILENAME.pid

# ---------------------------------
# check pid file
# ---------------------------------
if [ -f "$PID_FILE" ]; then
	echo "$SH_FILENAME.sh program still running...."
	exit
else
	echo $$ > "$PID_FILE"
fi

EXEC_PATH=$(pwd $0)

# ---------------------------------
# Setup Environments
# ---------------------------------
source $EXEC_PATH/setupEnv.sh

# ---------------------------------
# Initial variables
# ---------------------------------
CLASSPATH=$BULKER_CLASSPATH
LIBPATH=$BULKER_LIB
EXEC_CLASSNAME=com.rayful.quartz.QuartzSchedule

# ---------------------------------
# Get CLASSPATH
# ---------------------------------
source $BULKER_BIN/setupJar.sh
CLASSPATH=$BULKER_CLASSPATH:$JARPATH

# ---------------------------------
# Start java application
# ---------------------------------
nohup $BULKER_JAVA_HOME/bin/java -cp $CLASSPATH $EXEC_CLASSNAME > /dev/null 2>&1 &

# ---------------------------------
#delete pid file
# ---------------------------------
rm -f "$PID_FILE"
