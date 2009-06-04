#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk;
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
# @ 192.168.100.60:3333, reading from the source named 'KNWXXX_XXXDVP2XXXR00' and the 
# data channel 'BinarySampleData'.  The client is also setting the sink 
# name to be 'KNWXXX_XXXDVP2XXXR00_BinarySampleData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/KNWXXX_XXXDVP2XXXR00/BinarySampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink \
 -s 192.168.103.50\
 -p 3333\
 -n KNWXXX_XXXDVP2XXXR00\
 -c BinarySampleData\
 -k KNWXXX_XXXDVP2XXXR00_BinarySampleData_FileArchiver\
 -I hourly\
 -d /data/kilonalu/KNWXXX_XXXDVP2XXXR00/BinarySampleData
