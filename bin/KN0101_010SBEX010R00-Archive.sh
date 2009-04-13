#!/bin/bash
BBL_HOME=/Users/cjones/development/bbl/trunk;
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
# @ 127.0.0.1:3333, reading from the source named 'KN0101_010TCHN010R00' and the 
# data channel 'DecimalASCIISampleData'.  The client is also setting the sink 
# name to be 'KN0101_0101TCHN010R00_DecimalASCIISampleData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/KN0101_0101TCHN010R00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s bbl.ancl.hawaii.edu\
 -p 3333\
 -n KN0101_010SBEX010R00\
 -c DecimalASCIISampleData\
 -I hourly\
 -k KN0101_010SBEX010R00_DecimalASCIISampleData_FileArchiver\
 -d /Users/cjones/data/kilonalu/KN0101_010SBEX010R00/DecimalASCIISampleData
