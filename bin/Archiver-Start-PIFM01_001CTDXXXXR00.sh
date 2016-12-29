#!/bin/bash
# set up the variables
sourceName="PIFM01_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter PIFM CTD";

# start the instrument archiver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";