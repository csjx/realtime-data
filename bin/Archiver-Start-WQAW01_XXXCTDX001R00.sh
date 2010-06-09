#!/bin/bash
# set up the variables
sourceName="WQAW01_XXXCTDX001R00";
sourceType="StorXSource";
sourceString="1 meter Alawai WQB CTD";
bblHome="/usr/local/bbl/trunk";

# start the instrument driver
cd $bblHome;
./bin/$sourceName-Archive.sh >> /var/log/rbnb/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the archiver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
