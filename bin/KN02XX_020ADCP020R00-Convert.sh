#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

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
