#!/bin/bash

# set up the variables
sourceName="KN0101_010ADCP010R00";
sourceType="ADCPSource";
sourceString="10 meter 1200 kHz ADCP";

# find any pertinent processes
processDetails=$(ps -ef | grep $sourceType | grep sourceName);
processCount=$(echo $processDetails | wc -l);

# kill the driver process if it exists
if [ $processCount == 1 ]
then
  kill -15 `ps -ef | grep $sourceType | grep $sourceName | tr -s ' ' ' ' | cut -d" " -f2`;
  sleep 1;
  echo "Stopped the $sourceType driver connected to the $sourceString.";
else
  echo "There are no running $sourceType drivers to stop.";
fi
