#! /bin/bash

# First argument to this script should be the name of the database.
# Second argument to this script should be the host of the database.
# Third argument should be either "true" or "false" - this will indicate if the script will actually execute updates.
# Output will be a log file with date-formatted name: fix_chars_<YYYYMMDD_HH24mmss_Z>.log

DATABASE_NAME=$1
DATABASE_HOST=$2
DATABASE_USER=$3
DATABASE_PASSWORD=$4
DO_UPDATE=$5
DATE_STR=$(date +%Y%m%d_%H%M%S_%Z)
mysql --default-character-set=utf8 --table -D $DATABASE_NAME -h $DATABASE_HOST -u$DATABASE_USER -p$DATABASE_PASSWORD -e "SET @run_update = $DO_UPDATE; `cat $(pwd)/generic_fix_chars_proc.sql`" > fix_chars_$DATE_STR.log
