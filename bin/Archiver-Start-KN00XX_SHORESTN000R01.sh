#!/bin/bash
# set up the variables
sourceName="KN00XX_SHORESTN000R01";
sourceType="ShoreStationADAM";
sourceString="Shore Station ADAM 300V Line";

# start the instrument driver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
