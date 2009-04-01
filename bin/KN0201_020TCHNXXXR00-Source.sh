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

# run the TChainSource driver, connecting to the TChain @ 192.168.100.138:2101
# and to the RBNB server @ 192.168.100.60:3333, defining the source name as
# 'KN0201_020TCHN020R00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 31536000 frames.  In this case, each frame is 1 sample transmitted by the
# TChain instrument (20/minute), which equates to about 81 bytes of data per frame,
# With this ring buffer request, this source use approximately 4MB of RAM, and 
# 851MB of disk storage (in a 1 year period).
java edu.hawaii.soest.kilonalu.tchain.TChainSource\
 -H 192.168.100.138\
 -P 2101\
 -S KN0201_020TCHNXXXR00\
 -C DecimalASCIISampleData\
 -d "    "\
 -l "0D,0A"\
 -s 192.168.100.60\
 -p 3333\
 -z 50000\
 -Z 31536000
