#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

java edu.hawaii.soest.hioos.storx.StorXDispatcher\
 -s 10.0.0.100\
 -p 3333
