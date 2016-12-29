#!/bin/bash
# set up the variables
sourceName="PINM01_002CTDX008R00";
sourceType="CTDSource";
sourceString="1 meter PINM01 CTD";

# start the instrument archiver
cd ${REALTIME_DATA};
${REALTIME_DATA}/scripts/shell/$sourceName/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
