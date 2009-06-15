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

# run the DavisWxXMLSink client, connecting to the to the RBNB server 
# @ bbl.ancl.hawaii.edu:3333, defining the source name to read from as
# 'KNWXXX_XXXDVP2XXXR00'.  The result exported XML file will be saved in
# '/usr/local/bbl/trunk/wxStatus.xml' every 5 seconds

java edu.hawaii.soest.kilonalu.dvp2.DavisWxXMLSink\
 -s bbl.ancl.hawaii.edu\
 -p 3333\
 -S KNWXXX_XXXDVP2XXXR00\
 -f /var/www/html/wx/wxStatus.xml
