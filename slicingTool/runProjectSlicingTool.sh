#!/bin/bash

# Make sure we have all of the necessary params
LOG=$(grep    logFileName slicingTool.prop | perl -pe 's/^\S+=//');
DB=$(grep   slicingDbName slicingTool.prop | perl -pe 's/^\S+=//');
USER=$(grep slicingDbUser slicingTool.prop | perl -pe 's/^\S+=//');
PASS=$(grep  slicingDbPwd slicingTool.prop | perl -pe 's/^\S+=//');

if [[ -n $DB && -n $USER && -n $PASS ]]
then
  echo Dropping $DB ...
  mysql -u$USER -p$PASS -e "drop database if exists $DB"
else
  echo -e "\nMissing parameters!  Please complete slicingTool.prop\n"
  exit
fi

echo "
Running ProjectSlicingTool...
"

sleep 2

echo "
 " > SlicingTool.log
echo "
 " > $LOG

screen -d -m java -Xmx8G -jar ProjectSlicingTool.jar

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

All Done!
"
exit 0;


