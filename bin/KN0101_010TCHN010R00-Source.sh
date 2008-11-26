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

# run the TChainSource driver, connecting to the CTD @ 92.168.100.136:2104
# and to the RBNB server @ 192.168.100.60:3333, defining the source name as
# 'KN0101_010TCHN010R00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 375000 frames, and an archive size
# of 7884000 frames.  In this case, each frame is 1 sample transmitted by the
# TChain instrument (20/minute), which equates to about 81 bytes of data per frame,
# With this ring buffer request, this source use approximately 30MB of RAM, and 
# 638MB of disk storage (in a 1 year period).
java edu.hawaii.soest.kilonalu.tchain.TChainSource\
 -H 192.168.100.136\
 -P 2104\
 -S KN0101_010TCHN010R00\
 -C DecimalASCIISampleData\
 -s 192.168.100.60\
 -p 3333\
 -z 375000\
 -Z 7884000
