#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the SeabirdCTDSource driver, connecting to the CTD @ 68.25.65.111:5111
# and to the RBNB server @ realtime.pacioos.hawaii.edu:3333, defining the source name as
# 'AW01XX_002CTDXXXXR00' and the data channel as 'BinaryPD0EnsembleData'.  The 
# client is also requesting a cache size of 750000 frames, and an archive size
# of 7884000 frames.  In this case, each frame is 1 sample transmitted by the
# CTD instrument (15/minute), which equates to about 69 bytes of data per frame,
# depending on the CTD configuration. With this ring buffer request, this source
# use approximately 25MB of RAM, and 543MB of disk storage (in a 1 year period).
# -H 68.25.65.111\
java edu.hawaii.soest.kilonalu.ctd.CTDSource\
 -H 68.25.35.242\
 -P 5111\
 -S AW01XX_002CTDXXXXR00\
 -C DecimalASCIISampleData\
 -t socket\
 -s 192.168.103.50\
 -p 3333\
 -z 50000\
 -Z 31536000
