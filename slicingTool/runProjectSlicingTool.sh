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

#if [[ -z $CURATOR_DB || -z $CURATOR_HOST ]]
#then
#    echo -e "Missing dbName or dbHost in slicingTool.prop -- please edit the file to set them\n"
#    exit
#fi

echo "Fixing bad character sequences."
bash ../scripts/fix_characters.sh $CURATOR_DB $CURATOR_HOST $USER $PASS $(pwd) true

if [[ -n $DB && -n $USER && -n $PASS ]]
then
    if [[ $USER =~ "curator" ]]
    then
	echo Dropping $DB ...
	mysql -u$USER -p$PASS -e "drop database if exists $DB"
#	mysql -u$USER -p$PASS -e "drop database if exists ${DB}_myisam"
#	mysql -u$USER -p$PASS -e "create database ${DB}_myisam"
#	mysql -u$USER -p$PASS -e "drop database if exists gk_central"
#	mysql -u$USER -p$PASS -e "create database gk_central"
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
