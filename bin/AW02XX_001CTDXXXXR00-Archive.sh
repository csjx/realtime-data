#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 127.0.0.1:3333, reading from the source named 'AW02XX_001CTDXXXXR00' and the 
# data channel 'DecimalASCIISampleData'.  The client is also setting the sink 
# name to be 'AW02XX_001CTDXXXXR00_DecimalASCIIData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/AW02XX_001CTDXXXXR00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s 192.168.103.50\
 -p 3333\
 -n AW02XX_001CTDXXXXR00\
 -c DecimalASCIISampleData\
 -I hourly\
 -k AW02XX_001CTDXXXXR00_DecimalASCIIData_FileArchiver\
 -d /data/raw/alawai/AW02XX_001CTDXXXXR00/DecimalASCIISampleData
