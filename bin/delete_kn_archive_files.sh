#!/bin/bash

# This script is put in place as a cron job to trim down
# the size of the Kilo Nalu file archive directory. Since
# there is limited space on the shore station server,
# files are mirrored to the campus server, and this script
# deletes local files that are greater than 14 days old
# if and only if the file exists on the mirror server.

MIRROR_SERVER="192.168.103.50";
MIRROR_USER="apache";
MIRROR_COMMAND="ls";
SSH_COMMAND="/usr/bin/ssh";

FIND_COMMAND="/usr/bin/find";
FIND_DIR="/data1/kilonalu";
FIND_OPTIONS="-mtime +14 -type f -print";

# search for files older than the threshold
for i in `$FIND_COMMAND $FIND_DIR $FIND_OPTIONS`;
do
  # test if the file is listed on the mirror server
  result=$(sudo su - $MIRROR_USER -c "$SSH_COMMAND $MIRROR_SERVER '($MIRROR_COMMAND $i)'");
  # if the mirror result and the local result are an exact match, delete the file locally
  if [ $result == $i ]; then
    rm -f $i;
    echo $(basename $result) "exists on $MIRROR_SERVER. Deleted " $(basename $i) " locally.";
  fi

done
