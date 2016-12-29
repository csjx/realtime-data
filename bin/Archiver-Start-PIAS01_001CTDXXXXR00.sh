#!/bin/bash
# set up the variables
sourceName="PIAS01_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter PIAS CTD";

# start the instrument archiver
${REALTIME_DATA}/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";