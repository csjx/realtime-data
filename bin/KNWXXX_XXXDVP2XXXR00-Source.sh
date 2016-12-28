#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the DavisWxSource driver, connecting to the Wx station @ 168.105.160.135:2101
# and to the RBNB server @ realtime.pacioos.hawaii.edu:3333, defining the source name as
# 'KNWXXX_XXXDVP2XXXR00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 31536000 frames.  The driver can currently only be run from realtime.pacioos.hawaii.edu
# since the portserver it's connecting to filters requests from other IPs.
java edu.hawaii.soest.kilonalu.dvp2.DavisWxSource\
 -H 168.105.160.135\
 -P 2101\
 -S KNWXXX_XXXDVP2XXXR00\
 -C DecimalASCIISampleData\
 -s 192.168.103.50\
 -p 3333\
 -z 50000\
 -Z 31536000
