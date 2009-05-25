#!/bin/bash
# set up the variables
sourceName="KN0101_010FLNT010R00";
sourceType="FLNTUSource";
sourceString="10 meter WetLabs FLNTU";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
