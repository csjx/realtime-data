#!/bin/bash
# set up the variables
sourceName="PIAS01_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter PIAS CTD";

# start the instrument archiver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";