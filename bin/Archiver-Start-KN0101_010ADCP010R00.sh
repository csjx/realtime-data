#!/bin/bash
# set up the variables
sourceName="KN0101_010ADCP010R00";
sourceType="ADCPSource";
sourceString="10 meter 1200 kHz ADCP";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

echo -e "\nStarted $sourceName $sourceString file archiver\n";
