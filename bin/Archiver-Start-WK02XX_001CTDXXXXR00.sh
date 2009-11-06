#!/bin/bash
# set up the variables
sourceName="WK02XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter Waikiki NS04 CTD";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";