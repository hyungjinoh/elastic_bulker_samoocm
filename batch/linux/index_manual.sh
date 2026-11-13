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
# Set bulker  
# ---------------------------------
# INDEX_MODE : all/inc/send
INDEX_MODE=all
DF_NAME=manual
#MAX_ROWS=-1
#PRINT_DETAIL=yes
#PRINT_INTERVAL=20

if [ "$1" != "" ]; then
	if [ "$1" == "all" ] || [ "$1" == "inc" ] || [ "$1" == "send" ]; then
		INDEX_MODE=$1
	else
		echo "Invalid parameter - indexmode: $1";
		exit;
	fi;
fi;

echo "INDEX_MODE=$INDEX_MODE"

if [ "$2" != "" ]; then
	if [ "$2" == "yes" ] || [ "$2" == "no" ]; then
		PRINT_DETAIL=$2
	else
		echo "Invalid parameter - printdetail: $2";
		exit;
	fi;
fi;
echo "PRINT_DETAIL=$PRINT_DETAIL"

# ---------------------------------
# Set index
# ---------------------------------
USE_INDEX=Y
USE_DELETE=N
#USE_ENTITY=Y

# ---------------------------------
# Set env
# ---------------------------------
CURPATH=$(dirname $(realpath $0))
source $CURPATH/setupEnv.sh
source $CURPATH/setupJar.sh

# ---------------------------------
# Execute bulker, index
# ---------------------------------
source $CURPATH/bulker.sh
