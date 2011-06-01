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
# @ 127.0.0.1:3333, reading from the source named 'MB01XX_001CTDXXXXR00' and the 
# data channel 'DecimalASCIISampleData'.  The client is also setting the sink 
# name to be 'MB01XX_001CTDXXXXR00_DecimalASCIIData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/MB01XX_001CTDXXXXR00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s 192.168.103.50\
 -p 3333\
 -n MB01XX_001CTDXXXXR00\
 -c DecimalASCIISampleData\
 -I hourly\
 -k MB01XX_001CTDXXXXR00_DecimalASCIIData_FileArchiver\
 -d /data/raw/alawai/MB01XX_001CTDXXXXR00/DecimalASCIISampleData
