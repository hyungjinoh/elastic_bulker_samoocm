echo "elasticsearch is running. (PID:`cat es.pid`)"
kill `cat es.pid`
echo "elasticsearch has ended."
exit
