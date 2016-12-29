#!/bin/bash
# set up the variables
sourceName="AW02XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter NS02 CTD";

# start the instrument driver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
