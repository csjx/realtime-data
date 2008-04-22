#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/turbine-3.9.0.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 127.0.0.1:3333, reading from the source named 'KN0101_010ADCP010R00' and the 
# data channel 'BinaryPD0EnsembleData'.  The client is also setting the sink 
# name to be 'KN0101_0101ADCP010R00_BinaryPD0EnsembleData_FileArchiver', and is
# setting the archive directory to be '
# '/data1/rbnb/KN0101_0101ADCP010R00/BinaryPD0EnsembleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s 127.0.0.1\
 -p 3333\
 -n KN0101_010ADCP010R00\
 -c BinaryPD0EnsembleData\
 -k KN0101_010ADCP010R00_BinaryPD0EnsembleData_FileArchiver\
 -d /data1/kilonalu/KN0101_010ADCP010R00/BinaryPD0EnsembleData
