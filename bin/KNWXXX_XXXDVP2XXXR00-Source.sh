#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk;
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/turbine-3.9.0.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

# run the DavisWxSource driver, connecting to the Wx station @ 168.105.160.135:2101
# and to the RBNB server @ bbl.ancl.hawaii.edu:3333, defining the source name as
# 'KNWXXX_XXXDVP2XXXR00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 31536000 frames.  The driver can currently only be run from bbl.ancl.hawaii.edu
# since the portserver it's connecting to filters requests from other IPs.
java edu.hawaii.soest.kilonalu.dvp2.DavisWxSource\
 -H 168.105.160.135\
 -P 2101\
 -S KNWXXX_XXXDVP2XXXR00\
 -C DecimalASCIISampleData\
 -s bbl.ancl.hawaii.edu\
 -p 3333\
 -z 50000\
 -Z 31536000
