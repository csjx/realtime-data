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

# run the SeabirdCTDSource driver, connecting to the CTD @ 68.25.65.111:5111
# and to the RBNB server @ bbl.ancl.hawaii.edu:3333, defining the source name as
# 'AW01XX_002CTDXXXXR00' and the data channel as 'BinaryPD0EnsembleData'.  The 
# client is also requesting a cache size of 750000 frames, and an archive size
# of 7884000 frames.  In this case, each frame is 1 sample transmitted by the
# CTD instrument (15/minute), which equates to about 69 bytes of data per frame,
# depending on the CTD configuration. With this ring buffer request, this source
# use approximately 25MB of RAM, and 543MB of disk storage (in a 1 year period).
java edu.hawaii.soest.kilonalu.ctd.SBE37Source\
 -H 192.168.100.136\
 -P 2103\
 -S KN0101_010SBEX010R00\
 -C DecimalASCIISampleData\
 -s 192.168.100.60\
 -p 3333\
 -z 375000\
 -Z 7884000
