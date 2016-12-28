#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the DavisWxXMLSink client, connecting to the to the RBNB server 
# @ realtime.pacioos.hawaii.edu:3333, defining the source name to read from as
# 'KNWXXX_XXXDVP2XXXR00'.  The result exported XML file will be saved in
# '/var/www/html/wx/wxStatus.xml' every 5 seconds

java edu.hawaii.soest.kilonalu.dvp2.DavisWxXMLSink\
 -s realtime.pacioos.hawaii.edu\
 -p 3333\
 -S KNWXXX_XXXDVP2XXXR00\
 -f /var/www/html/wx/wxStatus.xml
