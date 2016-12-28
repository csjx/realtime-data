#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the SeabirdCTDSource driver, connecting to the CTD @ 68.25.168.134:5113
# and to the RBNB server @ realtime.pacioos.hawaii.edu:3333, defining the source name as
# 'WK01XX_001CTDXXXXR00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 31536000 frames.  In this case, each frame is 1 sample transmitted by the
# CTD instrument (15/minute), which equates to about 69 bytes of data per frame,
# depending on the CTD configuration. With this ring buffer request, this source
# use approximately 3.5MB of RAM, and 2.175GB of disk storage (in a 1 year period).
java edu.hawaii.soest.kilonalu.ctd.CTDSource\
 -H 68.25.168.134\
 -P 5113\
 -S WK01XX_001CTDXXXXR00\
 -C DecimalASCIISampleData\
 -t socket\
 -s realtime.pacioos.hawaii.edu\
 -p 3333\
 -z 50000\
 -Z 31536000
