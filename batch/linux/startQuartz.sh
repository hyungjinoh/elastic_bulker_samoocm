#!/bin/bash

# ---------------------------------
# check User
# ---------------------------------
USERID=`whoami`
if [ "$USERID" != "schusr" ]; then
        echo "Can't execute. Exuecute user must be schusr. ($USERID)"
        exit
fi

# ---------------------------------
# check Duplicate running
# ---------------------------------
PID=`ps -ef | grep com.rayful.quartz.QuartzSchedule | grep bulker |  awk '{print $2}'`
if [ "$PID" != "" ]; then
        echo "Quartz is aleady running. (PID:$PID)"
        exit
fi

#EXEC_PATH=$(pwd $0)
EXEC_PATH=$(cd "$(dirname "$0")" && pwd)

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
LOG_NAME=scheduler

# ---------------------------------
# Get CLASSPATH
# ---------------------------------
source $BULKER_BIN/setupJar.sh
CLASSPATH=$BULKER_CLASSPATH:$JARPATH

# ---------------------------------
# Start java application
# ---------------------------------
nohup $BULKER_JAVA_HOME/bin/java -Dlogname=$LOG_NAME -cp $CLASSPATH $EXEC_CLASSNAME > /dev/null 2>&1 &

