#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the FLNTUSource driver, connecting to the FLNTU @ 92.168.100.136:2102
# and to the RBNB server @ 192.168.100.60:3333, defining the source name as
# 'KN0101_010FLNT010R00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 31536000 frames.
java edu.hawaii.soest.kilonalu.flntu.FLNTUSource\
 -H 192.168.100.136\
 -P 2102\
 -S KN0101_010FLNT010R00\
 -C DecimalASCIISampleData\
 -s 192.168.100.60\
 -p 3333\
 -z 50000\
 -Z 31536000
