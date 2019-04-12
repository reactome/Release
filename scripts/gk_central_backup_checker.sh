#!/bin/bash
set -e # Stop execution on error

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

# Test ability of recent database to be restored
database_backup_restorable() {
    zipped_db_dump=$1
    placeholder_database=$2

    # Create placeholder database for attempting restoring of the db dump file
    mysql -e "DROP DATABASE IF EXISTS $placeholder_database" 2>> $error_log
    if mysql -e "CREATE DATABASE $placeholder_database" 2>> $error_log; then
        # Load the db dump into the empty placeholder database
        if zcat $zipped_db_dump | mysql $placeholder_database 2>> $error_log; then
            # Return "true" that the database backup is restorable
            return 0
        else
            msg="Could not successfully load data from db dump"
            emit_and_log_error "$msg" "$log"
            emit_and_log_error "$msg" "$error_log"
            return 1
        fi
    else
        msg="Could not create empty placeholder database $placeholder_database"
        emit_and_log_error "$msg" "$log"
        emit_and_log_error "$msg" "$error_log"
        return 1
    fi
}

# Gets count of the database objects from a Reactome database
get_db_object_count() {
    db_name=$1
    echo $(mysql --raw --batch -e "use $db_name; SELECT count(*) from DatabaseObject;" -s)
}

# Gets the count from a file or from a restored backup if no value from the file can be obtained
get_yesterdays_db_object_count() {
    yesterdays_stored_count_file=$1
    yesterdays_backup=$2
    yesterdays_restored_db_name=$3

    yesterdays_db_object_count=$(<$yesterdays_stored_count_file)
    # Checks if there is a valid (i.e. numeric) count stored from yesterday
    is_numeric='^[0-9]+$'
    if [[ -n "$yesterdays_db_object_count" && $yesterdays_db_object_count =~ $is_numeric ]]; then
        echo $yesterdays_db_object_count
        return 0
    else
        # Restore yesterdays's backup to get the count if it wasn't cached
        if database_backup_restorable $yesterdays_backup $yesterdays_restored_db_name; then
            yesterdays_db_object_count=$(get_db_object_count $yesterdays_restored_db_name)
            echo $yesterdays_db_object_count
            return 0
        else
            echo "Unable to get yesterday's count from file $yesterdays_stored_count_file or backup $yesterdays_backup" 2>> $error_log
            return 1
        fi
    fi
}

drop_database() {
    db=$1

    if mysql -e "DROP DATABASE IF EXISTS $db" 2>> $error_log; then
        return 0;
    else
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
yesterdays_stored_count_file="/var/log/previous_gk_central_object_count.txt"
log="/var/log/gk_central_backup_checker.log"
error_log="/var/log/gk_central_backup_checker.err"
> $error_log # Creates new empty error log file

todays_backup_filesize=$(stat -c%s "$todays_backup")
yesterdays_backup_filesize=$(stat -c%s "$yesterdays_backup")

byte_size_difference=$(expr $todays_backup_filesize - $yesterdays_backup_filesize)

typical_size_change=30000 # bytes
typical_object_count_change=1000


# Checks if the absolute change in file size was more than the typical size change and reports to the info or error log
if [ $(abs $byte_size_difference) -le $typical_size_change ] ; then
    msg="Difference in size between today's and yesterday's backups was acceptable: $byte_size_difference bytes"
    emit_and_log_info "$msg" "$log"
else
    msg="Difference between compressed gk_central back-ups from today $today and yesterday $yesterday was atypical at $byte_size_difference bytes.  Check $backup_dir"
    emit_and_log_error "$msg" "$error_log"
    emit_and_log_error "$msg" "$log"
fi

# Attempts to restore today's database backup and compare the database object count with yesterday's backup
# Yesterday's count is checked from a stored value in the file defined above or yesterday's backup is restored
# to get a count if no valid stored count is found.  The info or error logs are written to depending on the results
# and the restored backup database(s) is/are dropped.
todays_restored_db_name="test_backup_restore_$today"
if database_backup_restorable $todays_backup $todays_restored_db_name; then
    msg="$todays_backup was successfully restored"
    emit_and_log_info "$msg" "$log"

    yesterdays_restored_db_name="test_backup_restore_$yesterday"
    yesterdays_object_count=$(get_yesterdays_db_object_count $yesterdays_stored_count_file $yesterdays_backup $yesterdays_restored_db_name)
    if [ -n $yesterdays_object_count ]; then
        todays_object_count=$(get_db_object_count $todays_restored_db_name)

        db_object_count_difference=$(expr $todays_object_count - $yesterdays_object_count)
        if [ $(abs $db_object_count_difference) -le $typical_object_count_change ] ; then
            msg="Difference in object count between today's and yesterday's backups was acceptable: $db_object_count_difference"
            emit_and_log_info "$msg" "$log"
        else
            msg="Difference between database object counts for gk_central back-ups from today $today and yesterday $yesterday was atypical at $db_object_count_difference.  Check $backup_dir"
            emit_and_log_error "$msg" "$error_log"
            emit_and_log_error "$msg" "$log"
        fi

        echo -n $todays_object_count > $yesterdays_stored_count_file
    else
        msg="Unable to compare database object counts: yesterday's database object count could not be obtained."
        emit_and_log_error "$msg" "$error_log"
        emit_and_log_error "$msg" "$log"
    fi
    drop_database "test_backup_restore_$yesterday"
else
    msg="Today's backup $todays_backup could not be restored locally as a database.  Check if backup was corrupted in $backup_dir"
    emit_and_log_error "$msg" "$error_log"
    emit_and_log_error "$msg" "$log"
fi
drop_database "test_backup_restore_$today"

# If anything was written to the error log, it is e-mailed as an alert to issues
if [ -s $error_log ]; then
    mail -s 'gk_central backup errors' help@reactome.org < $error_log
fi
