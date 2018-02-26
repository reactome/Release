#! /bin/bash

# This script will print out some basic disc usage information.
# It will print the output from "df -h"
# It will also check each filesystem (excep for the NFS filesystems) and it will send a warning if
# the percent of used disk space is greater than THRESHOLD.
#
# This job is meant to be run with cron. Cron will send anything on stderr to the email
# address in the cronfile's MAILTO field.
#
# This script requires the program 'bc' to be installed to perform calculations.

THRESHOLD=90

echo "----------"
date +"%Y-%m-%d %H:%M:%S"
df -h
for line in $(df -h  | grep -v "Use%" | grep -v nfs | tr -s ' ' | cut -d ' ' -f 4,5,6 | tr ' ' ',') ; do
	PERCENT_USED=$(echo $line | cut -f 2 -d ',')
	FS=$(echo $line | cut -f 3 -d ',' )
	SPACE_REMAINING=$(echo $line | cut -f 1 -d ',')
	# echo "Percent used: $PERCENT_USED on $FS"
	PERCENT_USED_N=${PERCENT_USED//'%'}
	# Use the existence of a temporary file to indicate if a warning was already sent.
	# If a warning was sent for a filesystem, don't send another one.
	# If a warning was sent for some filesystem but it's no longer past the threshold, remove the warning file
	# so that a warning can be sent the next time it exceeds the threshold.
	WARNING_FILE=/tmp/${FS/\//_}_low_disc_space_warning_sent
	# for bc, 1 => "true"
	if [[ 1 == $(bc <<< "$PERCENT_USED_N > $THRESHOLD" ) ]] ; then
		if [[ ! -e  $WARNING_FILE ]] ; then
			>&2 echo "WARNING: LOW ON DISC SPACE FOR \"$FS\" - $PERCENT_USED IS USED UP, MORE THAN THRESHOLD OF $THRESHOLD ; ONLY $SPACE_REMAINING IS STILL FREE"
			touch $WARNING_FILE
		fi
	else
		if [[ -e  $WARNING_FILE ]] ; then
			>&2 echo "Warning condition has cleared for filesystem \"$FS\". Usage is now $PERCENT_USED (less than threshold of $THRESHOLD) and $SPACE_REMAINING is now free."
			rm $WARNING_FILE
		fi
	fi
done
