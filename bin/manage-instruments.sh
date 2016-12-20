#!/bin/bash
#
#  Copyright: 2013 Regents of the University of Hawaii and the
#             School of Ocean and Earth Science and Technology
#    Purpose: A convenience script to start instrument drivers using the
#             SimpleTextSource class.
#
#   Authors: Christopher Jones
#
# $HeadURL$
# $LastChangedDate$
# $LastChangedBy$
# $LastChangedRevision$
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

if [[ -z ${BBL_HOME} ]]; then
BBL_HOME=".";                     # installed BBL software location
LOG_DIR="/var/log/bbl";           # log files location
fi
CLASSPATH="${BBL_HOME}/bbl-1.0.0-SNAPSHOT-jar-with-dependencies.jar";
if [[ -e /etc/bbl/log4j.properties ]]; then
    CLASSPATH="/etc/bbl:${CLASSPATH}";
fi
VERSION="1.0.0";                  # keep track of this script's version
instruments="";                   # the instruments list to be processed
operation="";                     # operation to perform, either start or stop
unset option;                     # command line options variable

# ensure the logging file is writable
if [[ ! -d $LOG_DIR ]]; then
    echo "WARN: The logging directory does not exist. Please issue:";
    echo "sudo mkdir -p $LOG_DIR";
    echo "sudo chown -R $USER $LOG_DIR";
    exit 1;
fi

# show usage
usage() {
    cat << EOF

Usage: $(basename ${0}) -o start|stop [-a] [-h] [-i instr1] [-i instr2] [-i instrN]

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
    instrumentName=${1};
    echo "Starting the source driver for ${instrumentName}.";
    # Run the instrument driver
    java -cp ${CLASSPATH} \
        edu.hawaii.soest.pacioos.text.TextSourceApp \
        ${BBL_HOME}/conf/${instrumentName}.xml 2>&1 &
    pid=$!;
    sleep 2;        
    echo ${pid} > ${BBL_HOME}/run/${instrumentName}.pid
}

# stop the source driver given a process id and instrument name
stop() {
    pidToKill=${1};
    instrumentName=${2};
    echo "Stopping the source driver for ${instrumentName}.";
    kill -15 ${pidToKill};
    sleep 1;
    rm ${BBL_HOME}/run/${instrumentName}.pid
}

# figure out how we were called
while getopts ":ahVi:o:" OPTION; do
    case ${OPTION} in
      "a") instruments=$(ls ${BBL_HOME}/conf);;
      "h") usage;;
      "i") instruments="${instruments} ${OPTARG}.xml";;
      "o") operation=${OPTARG};;
      "V") show_version;;
       \?) echo "ERROR:   Invalid option: -${OPTARG}";usage; exit 1;;
    esac
done

# ensure options are passed
if [ -z ${instruments} ]; then
    echo -e "\nWARN: Use the -a option to start or stop all instrument drivers, ";
    echo "or optionally use the -i option one or more times."
    usage;
fi
    
# validate the operation
if [ "${operation}" != "start" -a "${operation}" != "stop" -a "${OPTION}" != "V" ]; then
    echo "ERROR: The -o option value must be either start or stop.";
    usage;
fi

# if needed, make the directory to store running driver processes
if [ ! -d ${BBL_HOME}/run ]; then
    mkdir -p ${BBL_HOME}/run;
fi

# iterate through the list and perform the start or stop operation
for instrument in ${instruments}; do
    existingPid="";
    runningPid="";
    if [ -e ${BBL_HOME}/conf/${instrument} ]; then
        
        # Stop the running instrument driver (even on start if needed)
        if [ -e ${BBL_HOME}/run/${instrument%.xml}.pid ]; then
            existingPid=$(cat ${BBL_HOME}/run/${instrument%.xml}.pid);
        fi
        
        runningPid=$(ps -o pid ${existingPid} | grep -v PID);
        if [ ! -z "${existingPid}" -a ! -z "${runningPid}" ]; then
            stop ${existingPid} ${instrument%.xml};            
        fi
        
        # Conditionally start the instrument driver
        if [ "${operation}" == "start" ]; then
            start ${instrument%.xml};
        fi
              
    else
      echo "WARN: Couldn't find config file ${BBL_HOME}/conf/${instrument}. Skipping it.";
    fi
done

exit 0;















