#!/bin/bash
# set up the variables
sourceName="KNWXXX_XXXDVP2XXXR00";
sourceType="DavisWxXMLSink";
sourceString="JABSOM Davis Weather Station";

# start the instrument driver
${REALTIME_DATA}/$sourceName-ExportXML.sh >> /var/log/realtime-data/$sourceName-Export.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceName $sourceString XML exporter\n";
