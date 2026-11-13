#!/bin/bash

PID=`ps -ef | grep com.rayful.quartz.QuartzSchedule | grep bulker | awk '{print $2}'`

if [ "$PID" == "" ]; then
        echo "Quartz is not running."
else
        kill $PID
        echo "Quartz is stoppted."
fi
