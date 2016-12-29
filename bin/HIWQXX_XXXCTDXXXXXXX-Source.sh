#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

java edu.hawaii.soest.hioos.storx.StorXDispatcher\
 -s realtime.pacioos.hawaii.edu\
 -p 3333
