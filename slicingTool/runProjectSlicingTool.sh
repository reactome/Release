#!/bin/bash

# Make sure we have all of the necessary params
LOG=$(grep    logFileName slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
DB=$(grep   slicingDbName slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
USER=$(grep slicingDbUser slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
PASS=$(grep  slicingDbPwd slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
PREV_DB=$(grep previousSlicingDbName slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
VER=$(grep releaseNumber slicingTool.prop | perl -pe 's/^\S+=//');
CURATOR_DB=$(grep dbName slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
CURATOR_HOST=$(grep dbHost slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');
RUN_CHAR_FIX=$(grep runCharacterFixer slicingTool.prop | grep -v '\#' |perl -pe 's/^\S+=//');

#if [[ -z $CURATOR_DB || -z $CURATOR_HOST ]]
#then
#    echo -e "Missing dbName or dbHost in slicingTool.prop -- please edit the file to set them\n"
#    exit
#fi

echo -n "Would you like to run the version topic comparison between this release and last? (y/n): "
read ver_topic
if [[ $ver_topic == y* ]] || [[ $ver_topic == Y* ]]
then
    java -jar VersionTopicComparer.jar 2>&1 | tee version_topic_comparison.out
fi

if [[ $RUN_CHAR_FIX == "true" ]]
then
    echo "Fixing bad character sequences."
    bash ../scripts/fix_characters.sh $CURATOR_DB $CURATOR_HOST $USER $PASS $(pwd) true
fi

if [[ -n $DB && -n $USER && -n $PASS ]]
then
    if [[ $USER =~ "piper" ]]
    then
        # Attempts to find and use database $DB and if successful it is backed up
        # before being dropped (errors, if any, stored in $DB_ERROR)
        DB_ERROR=$(mysql -u $USER -p$PASS -e "use $DB" 2>&1 > /dev/null)
        if [ -z "$DB_ERROR" ]
        then
            echo Backing up $DB ...
            mysqldump -u$USER -p$PASS $DB > $DB.dump
        elif [[ $DB_ERROR == *"Unknown database"* ]]
        then
            echo "Database $DB does not exist - no need to create back up"
        else
            echo -e "Problem accessing $DB: $DB_ERROR\nAborting slicing"
            exit
        fi

        echo Dropping $DB ...
        mysql -u$USER -p$PASS -e "drop database if exists $DB"
    else
        echo "I was expecting the database user to be 'curator'.  Please edit slicingTool.prop and try again"
    fi
else
    echo -e "\nMissing parameters!  Please complete slicingTool.prop\n"
    exit
fi

echo "
Running ProjectSlicingTool...
"

echo "
 " > SlicingTool.log
echo "
 " > $LOG

nohup java -Xmx8G -Djava.awt.headless=true -jar ProjectSlicingTool.jar &

tail -f SlicingTool.log $LOG 2>/dev/null &

WAIT=1
while [[ -n $WAIT ]]
do
  sleep 30
  DONE=$(grep 'Total time for slicing' SlicingTool.log | perl -pe 's/^.+Total/Total/')
  if [[ -n $DONE ]]
  then
    echo Finished slicing $DONE
    WAIT=
  fi
done

killall -9 tail >/dev/null 2>&1

#/usr/local/gkb/scripts/install_slice_db.pl $DB

echo "

All Done!
"

echo $DB `date` >> done.txt

exit 0;
