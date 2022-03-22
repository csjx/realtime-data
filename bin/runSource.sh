#!/bin/bash
export REALTIME_DATA=/usr/local/realtime-data;
export CLASSPATH=$REALTIME_DATA/realtime-data-1.4.4-jar-with-dependencies.jar;
$REALTIME_DATA/build/classes/:\
$REALTIME_DATA/lib/rbnb.jar:\
$REALTIME_DATA/lib/commons-codec-1.3.jar:\
$REALTIME_DATA/lib/commons-cli-1.0.jar:\
$REALTIME_DATA/lib/commons-logging-1.0.4.jar:\
$REALTIME_DATA/lib/log4j-1.2.8.jar:\
$REALTIME_DATA/lib/log4j2.properties

# run the ADCPSource driver, connecting to the ADCP @ 192.168.1.101:2104
# and to the RBNB server @ 192.168.1.103:3333, defining the source name as
# 'KN0101_010ADCP010R00' and the data channel as 'BinaryPD0EnsembleData'.  The 
# client is also requesting a cache size of 10000 frames, and an archive size
# of 100000 frames.  In this case, each frame is 1 ensemble transmitted by the
# ADCP instrument, which equates to about 512 bytes to 1024 bytes of 
# data per frame.
java edu.hawaii.soest.kilonalu.adcp.ADCPSource\
 -H 192.168.1.101\
 -P 2104\
 -S KN0101_010ADCP010R00 -C BinaryPD0EnsembleData\
 -s localhost\
 -p 3334\
 -z 10000\
 -Z 100000
