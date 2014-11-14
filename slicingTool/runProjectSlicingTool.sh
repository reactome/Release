#!/bin/bash

# Make sure we have all of the necessary params
LOG=$(grep    logFileName slicingTool.prop | perl -pe 's/^\S+=//');
DB=$(grep   slicingDbName slicingTool.prop | perl -pe 's/^\S+=//');
USER=$(grep slicingDbUser slicingTool.prop | perl -pe 's/^\S+=//');
PASS=$(grep  slicingDbPwd slicingTool.prop | perl -pe 's/^\S+=//');

if [[ -n $DB && -n $USER && -n $PASS ]]
then
    if [[ $USER =~ "curator" ]]
    then
	echo Dropping $DB ...
	mysql -u$USER -p$PASS -e "drop database if exists $DB"
	mysql -u$USER -p$PASS -e "drop database if exists ${DB}_myisam"
	mysql -u$USER -p$PASS -e "create database ${DB}_myisam"
	mysql -u$USER -p$PASS -e "drop database if exists gk_central"
	mysql -u$USER -p$PASS -e "create database gk_central"
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

nohup java -Xmx8G -jar ProjectSlicingTool.jar &

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

echo "
Now converting the database to MyISAM tables...
"
mysqldump -u$USER -p$PASS ${DB} | perl -pe 's/InnoDB/MyISAM/' | mysql -u$USER -p$PASS ${DB}_myisam
mysqldump -u$USER -p$PASS ${DB}_myisam | perl -pe 's/InnoDB/MyISAM/' | mysql -u$USER -p$PASS $DB
mysql  -u$USER -p$PASS -e "drop database ${DB}_myisam"

echo "
Now installing the slice database
"

/usr/local/gkb/scripts/install_slice_db.pl $DB

echo "

All Done!
"

echo $DB `date` >> done.txt

exit 0;


