#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/RXTXcomm.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/turbine-3.9.0.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

# run the SeabirdCTDSource driver, connecting to the CTD the /dev/ttyUSB0 serial port
# and to the RBNB server @ bbl.ancl.hawaii.edu:3333, defining the source name as
# 'PIMI01_001CTDX001R00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 126000 frames, and an archive size
# of 7776000 frames.  In this case, each frame is 1 sample transmitted by the
# CTD instrument (15/minute), which equates to about 65 bytes of data per frame,
# depending on the CTD configuration. With this ring buffer request, this source
# use approximately 2MB of RAM, and 126MB of disk storage (in a 90 day period).
java edu.hawaii.soest.kilonalu.ctd.CTDSource\
 -t serial\
 -o xml\
 -c /dev/ttyUSB0\
 -S PIMI01_001CTDX001R00\
 -C DecimalASCIISampleData\
 -s bbl.ancl.hawaii.edu\
 -p 3333\
 -z 126000\
 -Z 7776000
