#!/bin/csh

# Generate a monster flattened table containing everything that Reactome knows about.
#
# Uses a java program to do the job.  Copies the shell command
# line arguments directly to the java program - see
# ../../ReactomeGWT/src/org/reactome/analysis/statictable/StaticTableGenerator.java
# for full details of these arguments.
# out a summary of what it has done to STDOUT.
#
# Here's an example:
#
# generate_website_cached_data.sh -host picard.ebi.ac.uk -user croft -port 3306
#


setenv GKB /usr/local/gkbdev

echo GKB=$GKB

# Set up some handy varaibles
setenv REACTOME_GWT ${GKB}/ReactomeGWT
#setenv LIB $REACTOME_GWT/war/WEB-INF/lib
setenv LIB $REACTOME_GWT/libs/default
setenv SRC $REACTOME_GWT/src

echo SRC=$SRC

# Collect stuff for classpath
setenv CLASSPATH ${SRC}:${LIB}/reactome.jar:${LIB}/mysql-connector-java-5.1.14-bin.jar

echo LIB=$LIB
echo CLASSPATH=$CLASSPATH

# Compile the java source
echo Compiling....
(cd $REACTOME_GWT;./compile.sh)

# Run command
echo Running...
java -Xmx6144m -classpath $CLASSPATH org.reactome.analysis.statictable.StaticTableGenerator $*

echo generate_website_cached_data.sh has finished its job

