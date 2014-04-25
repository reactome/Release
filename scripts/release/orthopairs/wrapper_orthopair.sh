#!/bin/sh

#This script wraps the various steps needed for preparing orthopair files for orthology inference. It takes care of the cvs update of the Ensembl code, and runs the orthopair script first for the default setting, handling only species represented in Pan Compara, then with the -core flag, to handle species flagged for Core Ensembl


reactome_release=$1
ensembl_release=$2

echo reactome_release=$reactome_release
echo ensembl_release==$ensembl_release

GKB=/usr/local/gkbdev
ensembl_dir=$GKB/modules/ensembl_api

#do a cvs update of the Ensembl code
export CVSROOT=:pserver:cvsuser:CVSUSER@cvs.sanger.ac.uk:2401/cvsroot/ensembl
cvs -d $CVSROOT login
cd $ensembl_dir/ensembl
cvs -q update -d -P -r branch-ensembl-$ensembl_release
cd $ensembl_dir/ensembl-compara
cvs -q update -d -P -r branch-ensembl-$ensembl_release
cvs -d $CVSROOT logout

#run orthopair scripts, separately for pancompara species only (=default) and for core species only
orthopairs=$GKB/scripts/release/orthopairs
cd $orthopairs
echo Running prepare_orthopair_files.pl for PanCompara
perl $orthopairs/prepare_orthopair_files.pl -release $reactome_release -ensembl $ensembl_release
echo Finished running prepare_orthopair_files.pl for PanCompara

echo Running prepare_orthopair_files.pl for core species
perl $orthopairs/prepare_orthopair_files.pl -release $reactome_release -ensembl $ensembl_release -core
echo Finished running prepare_orthopair_files.pl for core species

echo wrapper_orthopair.pl has finished its job

