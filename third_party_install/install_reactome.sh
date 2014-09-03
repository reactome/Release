#!/bin/bash

# A script to automate the installation of reactome 
# software and data on an Ubuntu or Debian server
# Sheldon McKay <sheldon.mckay@gmail.com>


if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

MROOT = $1

if [[ ! -n $MROOT ]]
then
    echo -e "
\nIMPORTANT: If you have already set up MySQL, provide the
           mysql 'root' user (administrator) password here.
           Otherwise, the password you enter here will be
           configured as the mysql root password.
           The password can be left blank if desired\n\n"

    read -s -p "Enter the password for mysql user 'root' [default: none]: " MROOT
fi

if [[ -n $(command -v mysql) ]]
then
    until mysql -uroot --password=$MROOT -e ";" 2>/dev/null
    do
        echo -e "\nError: Can't connect to mysql"
        read -s -p "Please retry password: " MROOT
    done
    echo -e "\n"
    HAVEMYSQL=1
fi

APT=$(command -v apt-get)

if [[ -n $APT ]] 
then
    echo -e "\nThis is probably Debian/Ubuntu Linux"
else 
    echo -e "\nThis is probably not Debian or Ubuntu Linux; I can't proceed";
    exit 1 
fi
sleep 1

PWD=$(pwd)

if [[ -n $APT ]]
then
    echo -e "Installing system software for Debian/Ubuntu..."
    apt-get clean  -yq
    apt-get update -yq

    # mysql to be install non-interactively
    export DEBIAN_FRONTEND=noninteractive
    if [[ ! -n $HAVEMYSQL ]]
    then
	apt-get install -yq mysql-server
    fi

    apt-get install -yq \
	build-essential \
	perl \
	curl \
	apache2 \
	php5 \
	php5-mysql \
	openjdk-7-jre-headless \
	openjdk-7-jdk\
	libexpat1 \
	libexpat1-dev \
	libgd-gd2-perl \
        libbio-perl-perl
fi

# cgi scripts look here for perl
cd /usr/local/bin
ln -s /usr/bin/perl
cd $PWD

# We will use the cpanminus app to bring in the perl modules
if [[ ! -n $(command -v cpanm) ]]
then
    echo -e "\nInstalling APP::cpanminus";
    curl -L http://cpanmin.us | perl - --sudo App::cpanminus
fi

echo -e "\nInstalling perl modules..."

cpanm -q \
HTTP::Tiny \
IO::String \
LWP::UserAgent \
MIME::Lite \
Net::OpenSSH \
XML::Simple \
Search::Tools \
Capture::Tiny \
WWW::SearchResult \
JSON \
PDF::API2

echo -e "\nDownloading the reactome software..."
mkdir /usr/local/reactomes
mkdir /usr/local/reactomes/Reactome
cd /usr/local/reactomes/Reactome
rm -f reactome.tar.gz
wget http://www.reactome.org/download/current/reactome.tar.gz

echo -e "\nUnpacking the software..."
tar zxf reactome.tar.gz
mv reactome production
rm -f /usr/local/gkb
rm -f reactome.tar.gz
cd /usr/local
ln -s /usr/local/reactomes/Reactome/production/GKB gkb

echo -e "Updating configuration..."
cd /
tar zxvf /usr/local/gkb/third_party_install/config.tar.gz
rm -fr /usr/local/gkb/third_party_install

cd $PWD
echo -e "\nDownloading the reactome databases..."

mkdir databases
cd databases
wget http://www.reactome.org/download/current/databases/gk_current.sql.gz
wget http://www.reactome.org/download/current/databases/gk_stable_ids.sql.gz
wget http://www.reactome.org/download/current/databases/gk_wordpress.sql.gz

if [[ ! $HAVEMYSQL && -n $MROOT ]]
then
    mysqladmin -u root password $MROOT
fi

if [[ -n $MROOT ]]
then
   MROOT="--password=$MROOT"
fi

mysql -uroot $MROOT -e 'DROP DATABASE IF EXISTS gk_current'
mysql -uroot $MROOT -e 'CREATE DATABASE gk_current'
mysql -uroot $MROOT -e 'DROP DATABASE IF EXISTS gk_stable_ids'
mysql -uroot $MROOT -e 'CREATE DATABASE gk_stable_ids'
mysql -uroot $MROOT -e 'DROP DATABASE IF EXISTS gk_wordpress'
mysql -uroot $MROOT -e 'CREATE DATABASE gk_wordpress'

echo -e "\nLoading main reactome database..."
zcat gk_current.sql.gz | mysql -uroot $MROOT gk_current
rm -f gk_current.sql.gz

echo -e "\nLoading reactome_wordpress database..."
zcat gk_wordpress.sql.gz | mysql -uroot $MROOT gk_wordpress
rm -f gk_wordpress.sql.gz

echo -e "\nLoading main reactome_stable_identifiers database..."
zcat gk_stable_ids.sql.gz | mysql -uroot $MROOT gk_stable_ids
rm -f gk_stable_ids.sql.gz

echo -e "\nSetting database permissions..."
mysql -uroot $MROOT -e "GRANT SELECT ON gk_stable_ids.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot $MROOT -e "GRANT SELECT ON gk_current.*  \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot $MROOT -e "GRANT ALL ON gk_current_dn.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot $MROOT -e "GRANT ALL ON gk_wordpress.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"

rm -fr databases*

echo -e "Setting up Apache..."

cd /usr/local/gkb/website/html
mkdir img-fp
mkdir img-tmp
chown -R www-data img-*

cd /etc/apache2/mods-available
a2enmod \
    mime \
    include \
    autoindex \
    dir \
    cgi \
    alias \
    proxy \
    proxy_http \
    rewrite

cd ../sites-available
ln -s /usr/local/gkb/website/conf/httpd.conf reactome.conf
cd ../sites-enabled
a2ensite reactome
a2dissite default
rm -f *default*

# are we apache > 2.4?  # New syntax
if [[ -d /etc/apache2/conf-available ]]
then
    echo -e "\napache 2.4 or greater detected"
    perl -i -pe 's/\#Require all/Require all/' reactome.conf
fi

APACHE=apache2

echo -e "\nSetting up Tomcat..."

# This is the user that will own the tomcat process
groupadd tomcat7
useradd -g tomcat7 -s /sbin/nologin -d /opt/tomcat/temp tomcat7
cd /usr/local/reactomes/Reactome/production
chown -R tomcat7:tomcat7 apache-tomcat-7.0.50
chown -R tomcat7:tomcat7 AnalysisService Solr RESTful

cd $PWD

echo -e "\nStarting the tomcat server..."
/etc/init.d/tomcat7 restart

echo -e "\nStarting the apache web server..."
/etc/init.d/$APACHE restart

# auto start on reboot
echo -e "\nSetting up automatic tomcat start on boot"
sudo update-rc.d tomcat7 defaults

echo -e "Done Installation!\n"

exit 0

