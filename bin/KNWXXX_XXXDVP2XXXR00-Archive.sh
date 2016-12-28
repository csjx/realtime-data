#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

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
 -c DecimalASCIISampleData\
 -k KNWXXX_XXXDVP2XXXR00_DecimalASCIISampleData_FileArchiver\
 -I hourly\
 -d /data/raw/kilonalu/KNWXXX_XXXDVP2XXXR00/DecimalASCIISampleData
