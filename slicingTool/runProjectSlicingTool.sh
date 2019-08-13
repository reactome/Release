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
    if [[ $USER =~ "curator" ]]
    then
        # Attempts to find and use database $DB and if successful it is backed up
        # before being dropped (errors, if any, stored in $DB_ERROR)
        DB_ERROR=$(mysql -u $USER -p$PASS -e "use $DB" 2>&1 > /dev/null)
        DB_EXIT_CODE=$?

        if [[ $DB_EXIT_CODE -eq 0 ]]; then
            if [[ -n $DB_ERROR && $DB_ERROR != *"password on the command line interface can be insecure"* ]]; then
                echo "WARNING: Error message received on checking database exists but exit code was 0: $DB_ERROR"
                echo "Will backup $DB and continue slicing"
            fi
            echo Backing up $DB ...
            mysqldump -u$USER -p$PASS $DB > $DB.dump

            echo Dropping $DB ...
            mysql -u$USER -p $PASS -e "drop database if exists $DB"
        elif [[ $DB_ERROR == *"Unknown database"* ]]; then
            echo "Database $DB does not exist - no need to create back up"
        else
            echo -e "Problem accessing $DB: $DB_ERROR\nAborting slicing"
            exit 1;
        fi
    else
        echo "I was expecting the database user to be 'curator'.  Please edit slicingTool.prop and try again"
    fi
else
    echo -e "\nMissing parameters!  Please complete slicingTool.prop\n"
    exit 1
fi

echo "Running ProjectSlicingTool..."

echo "" > SlicingTool.log
echo "" > $LOG

nohup java -Xmx8G -Djava.awt.headless=true -jar ProjectSlicingTool.jar

echo "All Done!"

echo $DB `date` >> done.txt

exit 0;

