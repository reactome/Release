#!/bin/bash

echo -e "Downloading martj"
GKB=/usr/local/reactomes/Reactome/development/GKB
cd $GKB
mkdir -p BioMart
cd BioMart
wget --no-passive-ftp ftp://ftp.ebi.ac.uk/pub/software/biomart/martj_current/martj-bin.tar.gz
tar zxvf martj-bin.tar.gz

#cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart login
#cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart co -r release-0_7 martj
#cd martj
#ant jar
#cd ..

echo -e "Downloading biomart-perl"
cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart login
cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart co -r release-0_7 biomart-perl

echo -e "Installing biomart-perl -- PLEASE INSTALL ANY MISSING PERL MODULES AS INDICATED BELOW"
cd biomart-perl
perl bin/configure.pl -r conf/registryURLPointer.xml
cd ..

echo -e "Setting up Apache and ModPerl"
mkdir -p apache/source
cd apache/source
APACHE=$GKB/BioMart/apache
wget archive.apache.org/dist/httpd/httpd-2.2.14.tar.gz
tar zxvf httpd-2.2.14.tar.gz
cd httpd-2.2.14
./configure --enable-deflate --prefix=$APACHE
make install
cd ..
wget http://apache.sunsite.ualberta.ca/perl/mod_perl-2.0.8.tar.gz
tar zxvf mod_perl-2.0.8.tar.gz
cd mod_perl-2.0.8
perl Makefile.PL PREFIX=$APACHE MP_APXS=$APACHE/bin/apxs
make install