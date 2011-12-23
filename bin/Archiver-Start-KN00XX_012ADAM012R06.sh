#!/bin/bash
# set up the variables
sourceName="KN00XX_012ADAM012R06";
sourceType="MiniNodeADAM";
sourceString="Mini-Node ADAM";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
