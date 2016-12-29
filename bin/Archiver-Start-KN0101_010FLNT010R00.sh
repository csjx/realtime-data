#!/bin/bash
# set up the variables
sourceName="KN0101_010FLNT010R00";
sourceType="FLNTUSource";
sourceString="10 meter WetLabs FLNTU";

# start the instrument driver
${REALTIME_DATA}/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
