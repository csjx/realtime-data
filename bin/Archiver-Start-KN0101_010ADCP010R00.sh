#!/bin/bash
# set up the variables
sourceName="KN0101_010ADCP010R00";
sourceType="ADCPSource";
sourceString="10 meter 1200 kHz ADCP";

# start the instrument driver
cd ;
/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
