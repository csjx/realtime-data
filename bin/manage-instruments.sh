#!/bin/bash
#
#  Copyright: 2013 Regents of the University of Hawaii and the
#             School of Ocean and Earth Science and Technology
#    Purpose: A convenience script to start instrument drivers using the
#             SimpleTextSource class.
#
#   Authors: Christopher Jones
#
# $HeadURL:$
# $LastChangedDate:$
# $LastChangedBy:$
# $LastChangedRevision:$
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

BBL_HOME="/Users/cjones/pacioos"; # installed BBL software location
VERSION="1.0.0";                  # keep track of this script's version
instruments="";                   # the instruments list to be processed
operation="";                     # operation to perform, either start or stop
unset option;

# show usage
usage() {
    cat << EOF

usage: $(basename $0) -o start|stop [-a] [-h] [-i instr1] [-i instr2] [-i instrN]

Start or stop one or more instrument drivers by optionally providing the instrument id.

OPTIONS:

    -a  Start or stop all configured instruments
    -h  Show this message
    -i  The instrument id. Use -i multiple times for multiple instruments.
    -o  Indicate which operation to perform, start or stop.
    -V  Show the version (${VERSION})

EOF
exit 1;   
}

# show the version of this convenience script
show_version() {
    echo ${VERSION};
    exit 1;
}

# start the source driver given the instrument name
start() {
    instrumentName=$1;
    echo "Starting the source driver for ${instrumentName}.";
    # Run the instrument driver
    java -cp $BBL_HOME/bbl-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
        edu.hawaii.soest.pacioos.text.TextSourceApp \
        $BBL_HOME/conf/$instrumentName 2>&1 &
    pid=$!;
    sleep 2;        
    echo $pid > $BBL_HOME/run/${instrumentName}.pid
}

# stop the source driver given a process id and instrument name
stop() {
    pidToKill=$1;
    instrumentName=$2;
    echo "Stopping the source driver for ${instrumentName}.";
    kill -15 $pidToKill;
    sleep 1;
    rm $BBL_HOME/run/$instrumentName.pid
}

# figure out how we were called
while getopts ":ahi:o:" option
    do
        case "option" in
          "a") instruments=$(ls $BBL_HOME/conf);;
          "h") usage;;
          "i") instruments="${instruments} ${OPTARG}.xml";;
          "o") operation=$OPTARG;;
          \?)  echo "ERROR:   Invalid option: -$OPTARG";usage; exit 1;;
        esac
    done

# ensure options are passed
if [ -z $option ]; then
    usage;
fi
    
# validate the operation
if [ "$operation" != "start" -a "$operation" != "stop" ]; then
    echo "ERROR: The -o option value must be either start or stop.";
    usage;
fi

# if needed, make a directory to store running driver processes
if [ ! -d $BBL_HOME/run ]; then
    mkdir - $BBL_HOME/run;
fi

# iterate through the list and perform the start or stop operation
for instrument in $instruments; do
    echo $instrument;
    existingPid="";
    if [ -e $BBL_HOME/conf/$instrument ]; then
        echo "Starting ${instrument%.xml}";
        
        # Stop the running instrument driver (even on start if needed)
        existingPid=$(cat $BBL_HOME/run/${instrument%.xml}.pid);
        if [ ! -z "$existingPid" ]; then
            stop $existingPid ${instrument%.xml};            
        fi
        
        # Conditionally start the instrument driver
        if [ "$operation" == "start"]; then
            start ${instrument%.xml};
        fi
              
    else
      echo "WARN: Couldn't find config file $BBL_HOME/conf/$instrument. Skipping it.";
    fi
done

exit 0;















