#! /bin/bash

# This script will print out some basic memory usage information.
# It will print amount of memory available (in kb and gb) for allocation,
# the total amount of memory on the system (in kb and gb),
# And the amount available as a percentage.
# If the amount available is less than $THRESHOLD, a message will be printed to stderr.
#
# This job is meant to be run with cron. Cron will send anything on stderr to the email
# address in the cronfile's MAILTO field.
#
# This script requires the program 'bc' to be installed to perform calculations.

THRESHOLD=5

echo "----------"
date +"%Y-%m-%d %H:%M:%S"
MEM_AVAILABLE=$(cat /proc/meminfo | grep MemAvailable | tr -s ' ' | cut -f 2 -d ' ')
echo -e "Mem available:\t$MEM_AVAILABLE (kb)"
printf '('
printf "%0.2f" $(bc -l <<< "$MEM_AVAILABLE/1024^2")
echo " GB)"

MEM_TOTAL=$(cat /proc/meminfo | grep MemTotal | tr -s ' ' | cut -f 2 -d ' ')
echo -e "Mem total:\t$MEM_TOTAL (kb)"
printf '('
printf "%0.2f" $(bc -l <<< "$MEM_TOTAL/1024^2")
echo " GB)"


PERCENT_AVAIL=$(echo "$MEM_AVAILABLE.00/$MEM_TOTAL.00*100.00" | bc -l)

printf -v PERCENT_AVAIL_F  '%0.2f\n' $PERCENT_AVAIL

echo "% mem available: $PERCENT_AVAIL_F"
WARNING_FILE=/tmp/_low_memory_warning_sent
# for bc, 1 => "true"
if [[ 1 == $(bc <<< "$PERCENT_AVAIL < $THRESHOLD" ) ]] ; then
	if [[ ! -e $WARNING_FILE ]] ; then
		>&2  echo "WARNING: ONLY ${PERCENT_AVAIL_F//[$'\n']}% OF MEMORY IS AVAILABLE ; LESS THAN WARNING-THRESHOLD OF $THRESHOLD%!"
		touch $WARNING_FILE
	fi
else
	if [[ -e $WARNING_FILE ]] ; then
		>&2  echo "Warning condition has cleared. ${PERCENT_AVAIL_F//[$'\n']}% of memory is now available; more than warning-threshold of $THRESHOLD%."
		rm $WARNING_FILE
	fi
fi
