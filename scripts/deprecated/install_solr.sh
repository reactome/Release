#!/bin/bash

#-----------------------------------------------------------
# Script that automates the Reactome Solr initial setup.   
# Execute the files as $sudo ./install_solr.sh -h
#
#
# 19 October 2015
# Florian Korninger - fkorn@ebi.ac.uk
# Guilherme Viteri  - gviteri@ebi.ac.uk
#  
#-----------------------------------------------------------

usage="$(basename "$0") -i <password>  [-c <solr_core_name> -d <solr_home> —v <solr_version> -p <port> -u <user>] -- program to auto setup the Apache Lucene Solr in Reactome environment.

where:
    -h  Program help/usage
    -i  Solr Password
    -c  Solr Core name. DEFAULT: reactome
    -d  Solr Home directory. DEFAULT: /usr/local/reactomes/Reactome/production/Solr
    -v  Solr Version. DEFAULT: 5.3.1
    -p  Solr Port. DEFAULT: 8983
    -u  Solr User. DEFAULT: admin"

_SOLR_HOME="/usr/local/reactomes/Reactome/production/Solr"
_SOLR_VERSION="5.3.1"
_SOLR_PORT=8983
_SOLR_USER="admin"
_SOLR_PASSWORD=""
_SOLR_CORE="reactome"

while getopts ':d:v:p:u:i:c:h' option; do
  case "$option" in
    h) echo "$usage"
       exit
       ;;
    d) _SOLR_HOME=$OPTARG
       ;;
    v) _SOLR_VERSION=$OPTARG
       ;;
    p) _SOLR_PORT=$OPTARG
       ;;
    u) _SOLR_USER=$OPTARG
       ;;
    i) _SOLR_PASSWORD=$OPTARG
       ;; 
    c) _SOLR_CORE=$OPTARG
       ;;
    :) printf "missing argument for -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
   \?) printf "illegal option: -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
  esac
done
shift $((OPTIND - 1))


if [ -z $_SOLR_PASSWORD ]; then
       printf "missing argument for -i <password>\n"  >&2
       echo "$usage" >&2
       exit 1
fi;
#------------------

_CWD=$(pwd)

echo "Deleting old solr installed instances..."

echo "Stopping solr service"
service solr stop 
rm -r /var/solr
rm -r /opt/solr-*
rm -r /opt/solr
rm /etc/init.d/solr

deluser --remove-home solr
deluser --group solr

wget http://archive.apache.org/dist/lucene/solr/$_SOLR_VERSION/solr-$_SOLR_VERSION.tgz
if [ ! -f solr-$_SOLR_VERSION.tgz ]; then
     echo "Could not download Solr version $_SOLR_VERSION. Please check the specified version and try again"
     exit 1;
fi

tar xzf solr-$_SOLR_VERSION.tgz solr-$_SOLR_VERSION/bin/install_solr_service.sh --strip-components=2
bash ./install_solr_service.sh solr-$_SOLR_VERSION.tgz -d $_SOLR_HOME -p $_SOLR_PORT

echo "Check if solr is running..."
service solr status

_BRANCH=master
_SOLR_CONF_GIT="https://github.com/reactome/Search/archive/$_BRANCH.zip"


echo "Downloading Solr Reactome configuration files..."
wget $_SOLR_CONF_GIT
if [ ! -f $_BRANCH.zip ]; then
	echo "Could not download Solr configuration files. Please check $_SOLR_CONF_GIT"
	rm solr-$_SOLR_VERSION.tgz
	rm install_solr_service.sh 
	exit 1;
fi

unzip -q $_BRANCH.zip -d .

#create and service solr status wont work anymore after setting authentication to the jetty server
echo "Creating Solr core..."
su - solr -c "/opt/solr/bin/solr create -c $_SOLR_CORE -d $_CWD/Search-$_BRANCH/solr-conf"

#echo "Increasing solr heap..."
#sed -i "s/^\(SOLR_HEAP\s*=\s*\).*$/\1\"1024m\"/" $_SOLR_HOME/solr.in.sh 

echo "Enabling Solr admin authentication in Jetty..."
cp Search-$_BRANCH/solr-jetty-conf/jetty.xml /opt/solr-$_SOLR_VERSION/server/etc/
cp Search-$_BRANCH/solr-jetty-conf/webdefault.xml /opt/solr-$_SOLR_VERSION/server/etc/

#_OBF_PASSWORD=echo -n $_SOLR_PASSWORD | md5sum
cat > realm.properties << EOF1
$_SOLR_USER: $_SOLR_PASSWORD,solr-admin
EOF1
cp realm.properties /opt/solr-$_SOLR_VERSION/server/etc/

echo "Restart solr service..."
service solr restart

rm realm.properties
rm solr-$_SOLR_VERSION.tgz
rm install_solr_service.sh
rm $_BRANCH.zip
rm -r Search-$_BRANCH

echo "Done!"
