#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.2.0-jar-with-dependencies.jar;

export SOURCE="TEST01_001CTDXXXXR00";

# run the FileSource driver, opening the data file at /data/spool/PIAS01_001CTDXXXXR00.log
# and to the RBNB server @ realtime.pacioos.hawaii.edu:3333, defining the source name as
# 'PIAS01_001CTDXXXXR00' and the data channel as 'DecimalASCIISampleData'.  The 
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

java edu.hawaii.soest.kilonalu.utilities.FileSource\
 -F "${REALTIME_DATA}/test/resources/edu/hawaii/soest/kilonalu/utilities/NS01-example-data.txt"\
 -e "# *.*, *.*, *.*, *.*, *.*, *.*, *\d{2} [A-Z][a-z][a-z] *\d{4} *\d{2}:\d{2}:\d{2}\s*"\
 -S $SOURCE\
 -C DecimalASCIISampleData\
 -d "dd MMM yyyy HH:mm:ss"\
 -t "HST"\
 -f "7"\
 -s 127.0.0.1\
 -p 3333\
 -z 126000\
 -Z 31536000
