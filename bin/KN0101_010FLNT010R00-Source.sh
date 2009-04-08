#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/turbine-3.9.0.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

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
