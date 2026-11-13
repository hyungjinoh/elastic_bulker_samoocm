#!/bin/bash

PID=`ps -ef | grep com.rayful.quartz.QuartzSchedule | grep bulker |  awk '{print $2}'`

if [ "$PID" == "" ]; then
        echo "Quartz is stopped."
else
        echo "Quartz is running. (PID:$PID)"
fi
