#!/bin/bash

if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

read -p "Enter mysql user name: " MUSER
read -s -p "Enter mysql password: " MPASS
echo -e "\n"

read -p "Enter wordpress user name: " WUSER
read -s -p "Enter wordpress password: " WPASS
echo -e "\n"

if [[ ! (-n $MUSER && -n $MPASS && -n $WUSER && -n $WPASS) ]]
then
    echo "Required information missing!"
    echo "MUSER=$MUSER MPASS=$MPASS WUSER=$WUSER WPASS=$WPASS"
    exit 1
fi

perl -i -pe "s/!USERNAME!/$MUSER/" ../modules/GKB/Config.pm
perl -i -pe "s/!PASSWORD!/$MPASS/" ../modules/GKB/Config.pm
perl -i -pe "s/!USERNAME!/$WUSER/" ../website/html/wordpress/wp-config.php 
perl -i -pe "s/!PASSWORD!/$WPASS/" ../website/html/wordpress/wp-config.php



