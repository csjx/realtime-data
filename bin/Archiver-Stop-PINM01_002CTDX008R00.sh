#!/bin/bash

# set up the variables
sourceName="PINM01_002CTDX008R00";
sinkType="FileArchiverSink";
sourceString="1 meter PINM01 CTD";

# find any pertinent processes
processDetails=$(ps -ef | grep $sinkType | grep $sourceName);
processCount=$(echo $processDetails | grep -ve "^$" | wc -l);

# kill the archiver process if it exists
if [ $processCount == 1 ]
then
  kill -15 `ps -ef | grep $sinkType | grep $sourceName | tr -s ' ' ' ' | cut -d" " -f2`;
  sleep 1;
  echo "Stopped the $sinkType driver connected to the $sourceString.";
else
  echo "There are no running $sinkType archivers to stop.";
fi
