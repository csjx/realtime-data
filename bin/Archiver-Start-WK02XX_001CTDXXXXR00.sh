#!/bin/bash
# set up the variables
sourceName="WK02XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter Waikiki NS04 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
${REALTIME_DATA}/scripts/shell/$sourceName/scripts/shell/$sourceName-Archive.sh >> /var/log/realtime-data/$sourceName-Archive.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString file archiver\n";
