#!/bin/sh

# Generates stable identifiers for a given release.
# Uses a java program to do the job.  Copies the shell command
# line arguments directly to the java program - see
# ../../java/authorTool/src/org/gk/IDGeneration/IDGenerationCommandLine.java
# for full details of these arguments.  Once it has finished, it will print
# out a summary of what it has done to STDOUT.
#
# Here's an example:
#
# generate_stable_ids.sh -f -host picard.ebi.ac.uk -user croft -port 3306 -prnum 18 -cdbname test_slice_19_st_id -crnum 19 -idbname test_reactome_stable_identifiers -gdbname test_gk_central
#
# This will transfer stable IDs from (an already existing) release 18 to (the new)
# release 19, using the database "test_gk_central" as a source of information about
# which instances have been marked dnr and the database "test_reactome_stable_identifiers"
# for release tracking.  All databases are assumed to run on host "picard.ebi.ac.uk",
# port 3306, user "croft".  It will do all of this without asking you any questions
# about whether you *really* want to do this, i.e. if you don't know what you are
# doing, it could cause quite a lot of damage.

# First, find the location of the root of the GKB hierarchy
#export path_to_script=$0
#export script_dir=`echo $path_to_script | awk '{ sub(/\/[^\/]+$/, ""); print }'`
#export script_dir=`pwd`
export GKB=/usr/local/gkbdev

echo generate_stable_ids.sh: GKB=$GKB

# Set up some handy varaibles
export RELEASE=$GKB/scripts/release
export AUTHOR_TOOL=$GKB/java/authorTool
export LIB=$AUTHOR_TOOL/lib
export SRC=$AUTHOR_TOOL/src

# Collect stuff for classpath
export GENERAL_CLASSPATH=$LIB/mysql-connector-java-3.0.11-stable-bin.jar:$LIB/mysql-connector-java-3.1.7-bin.jar:$LIB/osxAdapter.jar:$LIB/commons-lang-2.3.jar
export SKIN_CLASSPATH=$LIB/skin/skinlf.jar
export CLASSPATH=$GENERAL_CLASSPATH:$SKIN_CLASSPATH:$AUTHOR_TOOL:$AUTHOR_TOOL/src

echo generate_stable_ids.sh: LIB=$LIB

# Compile the java source
echo generate_stable_ids.sh: Compiling....
(cd $AUTHOR_TOOL;./compile.sh)

# Run command
echo generate_stable_ids.sh: Running...
#java -Xmx500m -classpath $CLASSPATH org.gk.IDGeneration.IDGenerationCommandLine $*
#java -classpath $CLASSPATH org.gk.IDGeneration.IDGenerationCommandLine $*
#java -Xmx1000m -classpath $CLASSPATH org.gk.IDGeneration.IDGenerationCommandLine $*
java -Xmx2000m -classpath $CLASSPATH org.gk.IDGeneration.IDGenerationCommandLine $*

# Check the new stable identifiers
echo generate_stable_ids.sh: Checking...
export PATH=/usr/local/bin:${PATH}
export PERL5LIB=$PWD
export PERL5LIB=${PERL5LIB}:$GKB/modules
export PERL5LIB=${PERL5LIB}:/usr/local/bioperl-1.0
perl $RELEASE/generate_stable_ids/check_stable_identifiers.pl $*

echo generate_stable_ids.sh has finished its job

