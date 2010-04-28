#!/bin/bash
# set up the variables
sourceName="PIAS01_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter PIAS CTD";
bblHome="/usr/local/bbl/trunk";

# start the instrument archiver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";