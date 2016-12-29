#!/bin/bash
# set up the variables
sourceName="KN0101_010TCHNXXXR00";
sourceType="TChainSource";
sourceString="10 meter TChain";

# start the instrument driver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
