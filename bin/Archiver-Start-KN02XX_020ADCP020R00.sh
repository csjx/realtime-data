#!/bin/bash
# set up the variables
sourceName="KN02XX_020ADCP020R00";
sourceType="ADCPSource";
sourceString="20 meter 1200 kHz ADCP";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
