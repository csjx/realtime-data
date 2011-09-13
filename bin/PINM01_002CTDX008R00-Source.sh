#!/bin/bash
BBL_HOME=".";
export CLASSPATH=\
$BBL_HOME/lib/pacioos.jar:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties;

export SOURCE="PINM01_001CTDXXXXR00";

# run the FileSource driver, opening the data file at /data/spool/PINM01_001CTDXXXXR00.log
# and to the RBNB server @ bbl.ancl.hawaii.edu:3333, defining the source name as
# 'PINM01_001CTDXXXXR00' and the data channel as 'DecimalASCIISampleData'.  The 
# client is also requesting a cache size of 126000 frames, and an archive size
# of 7776000 frames.  In this case, each frame is 1 sample transmitted by the
# CTD instrument (15/minute), which equates to about 65 bytes of data per frame,
# depending on the CTD configuration. With this ring buffer request, this source
# use approximately 2MB of RAM, and 126MB of disk storage (in a 90 day period).

# The regular expression used to match data lines as opposed to other 
# instrument messages is:
# "# *.*, *.*, *.*, *.*, *.*, *.*, *\d{2} [A-Z][a-z][a-z] *\d{4} *\d{2}:\d{2}:\d{2}\s*"
#
# which corresponds to a data line, e.g.:
# " 29.2616,  5.73958,    1.666, 0.0681, 0.0803,  34.8810, 09 Jun 2010 09:59:21\r\n"
#
# Each section is explained below with single quotes around each section:
# '#'                - match a single pound sign
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *.*,'            - match any # of spaces followed by any # of characters, 
#                      followed by a comma
# ' *\d{2}'          - match any # of spaces followed by two digits
# ' [A-Z][a-z][a-z]' - match a space followed by an uppercase character,
#                      followed by two lowercase characters
# ' *\d{4}'          - match any # of spaces followed by four digits
# ' *\d{2}'          - match any # of spaces followed by two digits
# ':\d{2}'           - match a colon followed by two digits
# ':\d{2}'           - match a colon followed by two digits
# '\s*'              - match any number of whitespace characters (e.g. \r or \n)

java -cp $CLASSPATH edu.hawaii.soest.kilonalu.utilities.FileSource\
 -F "./log/input-file.log"\
 -e "# *.*, *.*, *.*, *.*, *.*, *.*, *\d{2} [A-Z][a-z][a-z] *\d{4} *\d{2}:\d{2}:\d{2}\s*"\
 -t UTC\
 -S $SOURCE\
 -C DecimalASCIISampleData\
 -s bbl.ancl.hawaii.edu\
 -p 3333\
 -z 126000\
 -Z 31536000