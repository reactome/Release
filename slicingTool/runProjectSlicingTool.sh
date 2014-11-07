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
I am going to echo the output to the screen below

Hit Control-C to exit when you are done...

Running ProjectSlicingTool..."

sleep 5

screen -d -m java -Xmx8G -jar ProjectSlicingTool.jar

tail -f SlicingTool.log $LOG

exit 0;


