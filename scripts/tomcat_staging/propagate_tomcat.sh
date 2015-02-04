#!/bin/bash

# This widget grabs the apache-tomcat webapps from the production server
# and synchs them with the release server once a day (by cronjob) 

if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

cd /usr/local/reactomes/Reactome/production

path=apache-tomcat/webapps

cd staging/webapps
rm -f *.war

for name in 'ReactomeRESTfulAPI' 'solr' 'content' 'PathwayBrowser' 'AnalysisService'
do
    echo $name
    rm -fr staging/webapps/$name
    cp -r ../../apache-tomcat/webapps/$name .
done

./pack.pl

cd ../
chown -R :gkb webapps
chmod -R g+w webapps
cd webapps

sudo -u smckay scp *.war reactomerelease:development/staging/webapps

