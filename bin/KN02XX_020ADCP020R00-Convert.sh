#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 127.0.0.1:3333, reading from the source named 'KN02XX_020ADCP020R00' and the 
# data channel 'BinaryPD0EnsembleData'.  The client is also setting the sink 
# name to be 'KN02XX_020ADCP020R00_BinaryPD0EnsembleData_Converter'.

java edu.hawaii.soest.kilonalu.utilities.TextOutputPlugin\
 -s 127.0.0.1\
 -p 3333\
 -n KN02XX_020ADCP020R00\
 -c BinaryPD0EnsembleData\
 -k KN02XX_020ADCP020R00_BinaryPD0EnsembleData_Converter
