PID=`ps -ef | grep /home/schusr/kibana-7.10.0-linux-x86_64/bin/../node/bin/node | grep -v grep | awk '{print $2}'`
if [ "$PID" != "" ]; then
	echo "Kibana is running. (PID:$PID)"
	kill -9 $PID
	echo "Kibana was killed. (PID:$PID)"
	exit
fi
