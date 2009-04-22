#!/bin/bash

# set up the variables
sourceName="AW01XX_002CTDXXXXR00";
sourceType="CTDSource";
sourceString="2 meter NS01 CTD";

# find any pertinent processes
processDetails=$(ps -ef | grep $sourceType | grep $sourceName);
processCount=$(echo $processDetails | grep -ve "^$" | wc -l);

# kill the driver process if it exists
if [ $processCount == 1 ]
then
  kill -15 `ps -ef | grep $sourceType | grep $sourceName | tr -s ' ' ' ' | cut -d" " -f2`;
  sleep 1;
  echo "Stopped the $sourceType driver connected to the $sourceString.";
else
  echo "There are no running $sourceType drivers to stop.";
fi
