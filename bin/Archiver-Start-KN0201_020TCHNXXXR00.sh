#!/bin/bash
# set up the variables
sourceName="KN0201_020TCHNXXXR00";
sourceType="TChainSource";
sourceString="20 meter TChain";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
