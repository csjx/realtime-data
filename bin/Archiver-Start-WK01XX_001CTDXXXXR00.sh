#!/bin/bash
# set up the variables
sourceName="WK01XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter Waikiki NS03 CTD";

# start the instrument driver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
