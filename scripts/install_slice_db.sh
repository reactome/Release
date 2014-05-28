#!/bin/bash

API_CONF=/usr/local/reactomes/Reactome/development/apache-tomcat-6.0.20/webapps/ReactomeRESTfulAPI/WEB-INF/applicationContext.xml
WEB_CONF=/usr/local/reactomes/Reactome/development/GKB/modules/GKB/Config.pm
RESTART=/usr/local/reactomes/Reactome/development/GKB/scripts/tomcat.pl

if [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root!" 1>&2
    exit 1
fi

db_name=$1
if [[ ! -n $db_name ]]; then 
    echo "Usage ./install_slice_db.pl slice_db_name" 1>&2
    exit 1
fi

perl -i -pe "s/<constructor-arg index=\"1\" value=\"\w+\"/<constructor-arg index=\"1\" value=\"${db_name}\"/" $API_CONF
AOK=`grep $db_name $API_CONF`
if [[ ! -n $AOK ]]; then
    echo "Database renaming for $API_CONF failed" 1>&2
    exit 1
fi

perl -i -pe "s/GK_DB_NAME = '\w+';/GK_DB_NAME = '$db_name';/" $WEB_CONF; 
BOK=`grep $db_name $WEB_CONF`
if [[ ! -n $BOK ]]; then
    echo "Database renaming for $WEB_CONF failed" 1>&2
    exit 1
fi


# restart tomcat
$RESTART -f



