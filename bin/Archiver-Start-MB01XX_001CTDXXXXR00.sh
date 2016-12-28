#!/bin/bash
# set up the variables
sourceName="MB01XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter NS10 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
