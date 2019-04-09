#!/bin/bash

# Displays the help message.
usage() {
    echo "Usage: $0 [-h|--help]"
}

HELP=false     # Display help.

# The standard option parsing idiom.
while true; do
    case "$1" in
        -h | --help )    HELP=true; shift ;;
        * ) break ;;
    esac
done

# Display help if asked.
if $HELP; then
    usage
    exit 0
fi

# Arguments are not yet supported.
if (( "$#" > 0 )); then
    usage
    exit 1
fi

# Taken from http://forums.devshed.com/unix-help-35/absolute-value-fuction-unix-267780.html
# Function to evaluate the absolute value of a number
abs() {
    # Check for numeric input
    if expr $1 + 0 2>/dev/null 1>&2 ; then
        # Is the number negative?
        if [ $1 -lt 0 ] ; then
            echo `expr 0 - $1`
        else
            echo $1
        fi
        return 0 # OK
    else
        return 1 # Not a number
    fi
}

emit_and_log_info() {
    msg=$1
    log_file=$2
    echo "$msg" >&1
    echo "$(date) INFO $msg" >> $log_file
}

emit_and_log_error() {
    msg=$1
    log_file=$2
    echo "$msg" >&2
    echo "$(date) ERROR $msg" >> $log_file
}

# Note - 0 is true and 1 is false
difference_too_large() {
    percent_difference=$1
    if (( $(echo "$percent_difference > 0" | bc -l) )); then
        return 1; # An increase in size is acceptable
    else
        tolerable_percent_drop=$2
        absolute_percent_difference=$(abs $percent_difference)
        if (( $(echo "$absolute_percent_difference > $tolerable_percent_drop" | bc -l) )); then
            return 0;
        else
            return 1;
        fi
    fi
}

# Test ability of recent database to be restored
database_backup_restorable() {
    zipped_db_dump=$1
    placeholder_database="test_backup_restore_$today"
    # Create placeholder database for attempting restoring of the db dump file
    mysql -e "DROP DATABASE IF EXISTS $placeholder_database" 2>> $log
    if mysql -e "CREATE DATABASE $placeholder_database" 2>> $log; then
        # Load the db dump into the empty placeholder database
        if zcat $zipped_db_dump | mysql $placeholder_database 2>> $log; then
            # Drop the placeholder database if loading was successful
            $(mysql -e "DROP DATABASE $placeholder_database" 2>> $log)
            # Return "true" that the database backup is restorable
            return 0;
        else
            emit_and_log_error "Could not successfully load data from db dump" $log
            return 1;
        fi
    else
        emit_and_log_error "Could not create empty placeholder database $placeholder_database" $log
        return 1;
    fi
}

###########################
#### Main script logic ####
###########################

# Defaults.
# TODO - it might be useful to allow command line options to override
# some of these settings.
today=$(date +"%Y_%m_%d")
yesterday=$(date -d "yesterday" +"%Y_%m_%d")
backup_dir="/nfs/reactome/reactomecurator/aws_mysqldump"
todays_backup="$backup_dir/gk_central_$today.sql.gz"
yesterdays_backup="$backup_dir/gk_central_$yesterday.sql.gz"
log="/var/log/gk_central_backup_checker.log"

todays_backup_filesize=$(stat -c%s "$todays_backup")
yesterdays_backup_filesize=$(stat -c%s "$yesterdays_backup")

byte_size_difference=$(expr $todays_backup_filesize - $yesterdays_backup_filesize)
percent_difference=$(echo "scale=10;$byte_size_difference*100/$yesterdays_backup_filesize" | bc)

tolerable_percent_drop=0.1 # 0.1 percent NOT 10 percent
if difference_too_large "$percent_difference" "$tolerable_percent_drop"; then
    msg="Difference between gk_central back-ups from today $today and yesterday $yesterday is too large.  Check $backup_dir"
    emit_and_log_error "$msg" "$log"
else
    msg="Difference in size between today's and yesterday's backups was acceptable: $byte_size_difference bytes"
    emit_and_log_info "$msg" "$log"
fi

if ! database_backup_restorable $todays_backup; then
    msg="$todays_backup could not be restored locally as a database.  Check if backup was corrupted in $backup_dir"
    emit_and_log_error "$msg" "$log"
else
    msg="$todays_backup was successfully restored"
    emit_and_log_info "$msg" "$log"
fi