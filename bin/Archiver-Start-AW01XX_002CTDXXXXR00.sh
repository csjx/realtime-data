#!/bin/bash
# set up the variables
sourceName="AW01XX_002CTDXXXXR00";
sourceType="CTDSource";
sourceString="2 meter NS01 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";