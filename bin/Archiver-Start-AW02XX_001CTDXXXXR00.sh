#!/bin/bash
# set up the variables
sourceName="AW02XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter NS02 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
