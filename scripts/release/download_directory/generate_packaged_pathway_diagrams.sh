#!/bin/csh

# Generates and packages up pathway diagrams for download or export purposes.
# Uses a java program to do the job.  Takes 6 arguments:
#
# host
# db
# user
# pass
# port
# folder for diagrams
#
# Here's an example:
#
# generate_packaged_pathway_diagrams.sh localhost test_reactome_43 gkreadonly 'passwd' 3306 diagrams
#

# First, find the location of the root of the GKB hierarchy
setenv path_to_script $0
setenv script_dir `echo $path_to_script | awk '{ sub(/\/[^\/]+$/, ""); print }'`
setenv GKB /usr/local/gkbdev

echo GKB=$GKB

# Set up some handy varaibles
setenv WEBELV ${GKB}/WebELVTool

echo WEBELV=$WEBELV

setenv CWD `pwd`

echo CWD=$CWD

cd $WEBELV
setenv diagram_dump_filename $6
echo diagram_dump_filename=$diagram_dump_filename
./runDiagramDumper.sh $*
cd $diagram_dump_filename
rm -f *.zip
echo Zipping PDF
zip -r diagrams.pdf.zip PDF
echo Zipping PNG
zip -r diagrams.png.zip PNG
cd $CWD
echo CWD=`pwd`
echo mv -f $WEBELV/$diagram_dump_filename/*.zip .
mv -f $WEBELV/$diagram_dump_filename/*.zip .
echo Completed diagram generation and packaging

