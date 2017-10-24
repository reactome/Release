#!/bin/bash

GKB=/usr/local/reactomes/Reactome/development/GKB
cd $GKB
mkdir -p BioMart
cd BioMart


echo -e "Downloading biomart-perl"
cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart login
cvs -d :pserver:cvsuser@cvs.sanger.ac.uk:/cvsroot/biomart co -r release-0_7 biomart-perl

echo -e "Installing biomart-perl -- PLEASE INSTALL ANY MISSING PERL MODULES AS INDICATED BELOW"
cd biomart-perl
perl bin/configure.pl -r conf/registryURLPointer.xml
cd ..