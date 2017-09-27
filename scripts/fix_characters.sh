#! /bin/bash

# First argument to this script should be the name of the database.
# Second argument to this script should be the host of the database.
# Third argument should be database user name
# Fourth argument should be database user's password
# Fifth argument is a path. This script will dump a backup of the database before
#   executing, so you should specify a path to a *directory* of where you want that backup to go.
# Sixth argument should be either "true" or "false" - this will indicate if the
#   script will actually execute updates, or just generate a report.
#
# Output will be a log file with date-formatted name: fix_chars_<YYYYMMDD_HH24mmss_Z>.log

DATABASE_NAME=$1
DATABASE_HOST=$2
DATABASE_USER=$3
DATABASE_PASSWORD=$4
DB_DUMP_DIR=$5
DO_UPDATE=$6
DATE_STR=$(date +%Y%m%d_%H%M%S_%Z)
BACKUP_FILE_NAME=${DB_DUMP_DIR}/${DATABASE_NAME}_pre_character_fix.$DATE_STR.sql.gz
echo "Creating a backup: $BACKUP_FILE_NAME"
mysqldump -u$DATABASE_USER -p$DATABASE_PASSWORD -h$DATABASE_HOST $DATABASE_NAME | gzip -c > $BACKUP_FILE_NAME
echo "Executing generic_fix_chars_proc.sql."
# mysql --default-character-set=utf8 --table -D$DATABASE_NAME -h$DATABASE_HOST -u$DATABASE_USER -p$DATABASE_PASSWORD \
#   -e "ALTER DATABASE $DATABASE_NAME default character set utf8; ALTER DATABASE $DATABASE_NAME default collate utf8_general_ci;"
mysql --default-character-set=utf8 --table -D$DATABASE_NAME -h$DATABASE_HOST -u$DATABASE_USER -p$DATABASE_PASSWORD  -e "SET @run_update = $DO_UPDATE ; `cat $(dirname $BASH_SOURCE)/generic_fix_chars_proc.sql`" > fix_chars_$DATE_STR.log
echo "Process finished, check the log file for information on what characters were fixed: fix_chars_$DATE_STR.log"
