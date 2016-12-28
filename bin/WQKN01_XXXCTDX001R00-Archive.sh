#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 192.168.103.50:3333, reading from the source named 'WQKN01_XXXCTDX001R00' and the 
# data channel 'DecimalASCIISampleData'.  The client is also setting the sink 
# name to be 'WQKN01_XXXCTDX001R00_DecimalASCIIData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/WQKN01_XXXCTDX001R00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s 192.168.103.50\
 -p 3333\
 -n WQKN01_XXXCTDX001R00\
 -c DecimalASCIISampleData\
 -I daily\
 -k WQKN01_XXXCTDX001R00_DecimalASCIIData_FileArchiver\
 -d /data/raw/kilonalu/WQKN01_XXXCTDX001R00/DecimalASCIISampleData
