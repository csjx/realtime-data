#!/bin/bash
# set up the variables
sourceName="WQKN01_XXXCTDX001R00";
sourceType="StorXSource";
sourceString="1 meter Kilo Nalu WQB CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the archiver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
