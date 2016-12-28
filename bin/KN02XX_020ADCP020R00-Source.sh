#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the ADCPSource driver, connecting to the ADCP @ 192.168.100.139:2102
# and to the RBNB server @ 192.168.100.60:3333, defining the source name as
# 'KN02XX_020ADCP020R00' and the data channel as 'BinaryPD0EnsembleData'.  The 
# client is also requesting a cache size of 50000 frames, and an archive size
# of 7884000 frames.  In this case, each frame is 1 ensemble transmitted by the
# ADCP instrument, which equates to about 1024 bytes of data per frame,
# depending on the ADCP configuration. With this ring buffer request, this source
# use approximately 50MB od RAM, and 32GB of disk storage (in a 1 year period).
java edu.hawaii.soest.kilonalu.adcp.ADCPSource\
 -H 192.168.100.139\
 -P 2102\
 -S KN02XX_020ADCP020R00 -C BinaryPD0EnsembleData\
 -s 192.168.100.60\
 -p 3333\
 -z 50000\
 -Z 31536000
