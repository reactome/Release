#!/bin/bash

##############################
# Install basic dependencies #
##############################

# Abort the script if anything fails.
set -e
MROOT=$1

# this value should be interpolated at release time
RELEASE=RELEASENUM

if [[ $EUID -ne 0 ]] ; then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

if [[ ! -n $MROOT ]] ; then
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

if [[ -n $APT ]] 
then
    echo -e "\nThis is probably Debian/Ubuntu Linux"
else 
    echo -e "\nThis is probably not Debian or Ubuntu Linux; I can't proceed";
    exit 1 
fi
sleep 1

PWD=$(pwd)

sleep 1
echo -e "Installing system software for Debian/Ubuntu..."
apt-get clean  -yq
apt-get update -yq

# mysql to be install non-interactively
export DEBIAN_FRONTEND=noninteractive
if [[ ! -n $HAVEMYSQL ]] ; then
	apt-get install -yq mysql-server
fi

# Install other dependencies
apt-get install -yq \
	build-essential \
	perl \
	curl \
	cpanminus \
	apache2 \
	php5 \
	php5-mysql \
	openjdk-7-jre-headless \
	openjdk-7-jdk\
	libexpat1 \
	libexpat1-dev \
	libgd-gd2-perl \
    libbio-perl-perl \
	wget

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
		PDF::API2 \
		Log::Log4perl \
		common::sense \
		Email::Valid \
		URI::Encode

echo -e "\nDownloading the reactome software..."
mkdir -p /usr/local/reactomes/Reactome/production
cd /usr/local/reactomes/Reactome/production
rm -f reactome.tar.gz
wget http://www.reactome.org/download/current/reactome.tar.gz

echo -e "\nUnpacking the software..."
tar zxf reactome.tar.gz
rm -f /usr/local/gkb
rm -f reactome.tar.gz
cd /usr/local
ln -s /usr/local/reactomes/Reactome/production/GKB gkb

###########################
# Get necessary           #
# fireworks/diagram files #
###########################
echo "getting fireworks files"
mkdir -p /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/fireworks
cd /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/fireworks
wget -nd -nv -r --no-parent http://reactome.org/download/current/fireworks/
rm index.html* robots.txt

echo "getting diagram files"
mkdir -p /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/diagram
cd /usr/local/reactomes/Reactome/production/GKB/website/html/download/current/diagram
# this is a big download, so -q option might be nice here
wget -nd -nv -r --no-parent http://reactome.org/download/current/diagram/
rm index.html* robots.txt

####################
# Database section #
####################
echo -e "Updating configuration..."
cd /
tar zxvf /usr/local/gkb/third_party_install/config.tar.gz
rm -fr /usr/local/gkb/third_party_install

cd /usr/local/gkb/website/html/download

echo -e "\nDownloading the reactome databases..."

mkdir databases
cd databases
wget -nv -t 5 http://www.reactome.org/download/current/databases/gk_current.sql.gz
wget -nv -t 5 http://www.reactome.org/download/current/databases/gk_stable_ids.sql.gz
wget -nv -t 5 http://www.reactome.org/download/current/databases/gk_wordpress.sql.gz
# wget http://www.reactome.org/download/current/databases/gk_current_dn.sql.gz
service mysql start
if [[ ! $HAVEMYSQL && -n $MROOT ]] ; then
	mysqladmin -u root password $MROOT
fi

service  mysql restart
mysql -uroot -p$MROOT -e 'DROP DATABASE IF EXISTS gk_current'
mysql -uroot -p$MROOT -e 'CREATE DATABASE gk_current'
mysql -uroot -p$MROOT -e 'DROP DATABASE IF EXISTS gk_current_dn'
mysql -uroot -p$MROOT -e 'CREATE DATABASE gk_current_dn'
mysql -uroot -p$MROOT -e 'DROP DATABASE IF EXISTS stable_identifiers'
mysql -uroot -p$MROOT -e 'CREATE DATABASE stable_identifiers'
mysql -uroot -p$MROOT -e 'DROP DATABASE IF EXISTS gk_wordpress'
mysql -uroot -p$MROOT -e 'CREATE DATABASE gk_wordpress'


echo -e "\nLoading main reactome database..."
zcat gk_current.sql.gz | mysql -uroot -p$MROOT gk_current
rm -f gk_current.sql.gz

echo -e "\nLoading simplified reactome database..."
zcat gk_current_dn.sql.gz | mysql -uroot -p$MROOT gk_current_dn
rm -f gk_current.sql.gz


echo -e "\nLoading reactome_wordpress database..."
zcat gk_wordpress.sql.gz | mysql -uroot -p$MROOT gk_wordpress
rm -f gk_wordpress.sql.gz

echo -e "\nLoading main reactome_stable_identifiers database..."
zcat gk_stable_ids.sql.gz | mysql -uroot -p$MROOT stable_identifiers
rm -f gk_stable_ids.sql.gz

echo -e "\nSetting database permissions..."
mysql -uroot -p$MROOT -e "GRANT SELECT ON stable_identifiers.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot -p$MROOT -e "GRANT SELECT ON gk_current.*  \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot -p$MROOT -e "GRANT SELECT ON gk_current_dn.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"
mysql -uroot -p$MROOT -e "GRANT ALL ON gk_wordpress.* \
TO 'reactome_user'@'localhost' IDENTIFIED BY 'reactome_pass'"

# reset the worpress permalinks so they will work on a new server
mysql -uroot -p$MROOT gk_wordpress -e "update wp_options set option_value=NULL where option_name = 'permalink_structure'"

rm -fr databases*

#######################
# Get and execute     #
# Solr install script #
#######################

APACHE=apache2

# set up solr
echo -e "\nSetting up Solr..."
mkdir -p /usr/local/gkb/scripts/
cd /usr/local/gkb/scripts/
echo "Getting Solr installer..."
wget https://raw.githubusercontent.com/reactome/Search/master/install_solr.sh
echo "Executing Solr installer..."
chmod a+x /usr/local/gkb/scripts/install_solr.sh
apt-get install unzip
/usr/local/gkb/scripts/install_solr.sh -i reactome_pass

rm /usr/local/reactomes/Reactome/production/apache-tomcat/conf/Catalina/localhost/solr.xml
chown -R solr:solr /usr/local/reactomes/Reactome/production/Solr

#####################
# Web server setup. #
#####################
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
service apache2 start
service apache2 reload
#a2dissite default
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
set +e
exit 0