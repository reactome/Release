#!/bin/bash

API=/usr/local/reactomes/Reactome/production/apache-tomcat/webapps/ReactomeRESTfulAPI/WEB-INF/applicationContext.xml
WEB=/usr/local/reactomes/Reactome/production/GKB/modules/GKB/Secrets.pm

#if [[ $EUID -ne 0 ]]; then
#    echo "This script must be run as root!" 1>&2
#    exit 1
#fi

db_name=$1
if [[ ! -n $db_name ]]; then 
    echo "Usage ./install_slice_db.pl slice_db_name" 1>&2
    exit 1
fi


perl -i -pe "s/<constructor-arg index=\"1\" value=\"\w+\"/<constructor-arg index=\"1\" value=\"${db_name}\"/" $API
AOK=`grep $db_name $API`
echo $AOK
if [[ ! -n $AOK ]]; then
    echo "Database renaming for $API failed" 1>&2
    exit 1
fi

perl -i -pe "s/GK_DB_NAME\s*=\s*'\S+';/GK_DB_NAME  = '$db_name';/" $WEB; 
BOK=`grep $db_name $WEB`
echo $BOK
if [[ ! -n $BOK ]]; then
    echo "Database renaming for $WEB failed" 1>&2
    exit 1
fi

sudo /etc/init.d/tomcat7 restart
