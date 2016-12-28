#!/bin/bash
# set up the variables
sourceName="KN0101_010SBEX010R00";
sourceType="SBE37Source";
sourceString="10 meter Seabird 37 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
