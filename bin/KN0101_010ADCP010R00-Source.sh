#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

# run the ADCPSource driver, connecting to the ADCP @ 192.168.100.139:2102
# and to the RBNB server @ 192.168.100.60:3333, defining the source name as
# 'KN02XX_020ADCP020R00' and the data channel as 'BinaryPD0EnsembleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 3000000 frames.  In this case, each frame is 1 ensemble transmitted by the
# ADCP instrument, which equates to about 1024 bytes of # data per frame,
# depending on the ADCP configuration.
java edu.hawaii.soest.kilonalu.adcp.ADCPSource\
 -H 192.168.100.136\
 -P 2101\
 -S KN0101_010ADCP010R00 -C BinaryPD0EnsembleData\
 -s 192.168.100.60\
 -p 3333\
 -z 50000\
 -Z 31536000
