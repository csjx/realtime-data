#!/bin/bash
# set up the variables
sourceName="PIGM01_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter PIGM CTD";

# start the instrument archiver
cd ${REALTIME_DATA};
${REALTIME_DATA}/scripts/shell/$sourceName/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";