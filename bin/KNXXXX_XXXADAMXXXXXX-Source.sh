#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# Start the ADAM Dispatcher class, connect to the
# localhost computer on port 5168 to receive UDP
# packets from multiple ADAM Sources.  The 
# Dispatcher will read configuration information from
# the lib/sensor.properties.xml file, and will create
# RBNB Sources for each of the ADAM sensors listed.
# These sources will then connect to the RBNB
# DataTurbine at 192.168.100.60:3333, and will begin
# streaming ADAM data.
java edu.hawaii.soest.kilonalu.adam.AdamDispatcher\
 -H localhost\
 -P 5168\
 -S KN00XX_010ADAM010R01\
 -s 192.168.100.60\
 -p 3333
