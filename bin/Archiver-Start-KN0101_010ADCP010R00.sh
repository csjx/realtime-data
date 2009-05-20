#!/bin/bash
# set up the variables
sourceName="KN0101_010ADCP010R00";
sourceType="ADCPSource";
sourceString="10 meter 1200 kHz ADCP";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
