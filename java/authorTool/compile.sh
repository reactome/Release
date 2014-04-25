#!/bin/sh

# This script compiles all of the java code, for the author tool, curator
# tool and for the stable ID generation tools.  The results of the
# compilation end up in the directories "launcher" and "org".  If
# you want to add the compiled stuff to your classpath, you will
# need to include the path up to and including the directory containing
# this script (currently authorTool).

# Make directories for compiled classes
mkdir launcher
mkdir org

CWD=`pwd`

# Collect stuff for classpath
GENERAL_CLASSPATH=lib/junit-4.4.jar:lib/javaws.jar:lib/mysql-connector-java-3.0.11-stable-bin.jar:lib/mysql-connector-java-3.1.7-bin.jar:lib/osxAdapter.jar
BATIK_CLASSPATH=lib/batik/batik-awt-util.jar:lib/batik/batik-bridge.jar:lib/batik/batik-css.jar:lib/batik/batik-dom.jar:lib/batik/batik-extension.jar:lib/batik/batik-ext.jar:lib/batik/batik-gui-util.jar:lib/batik/batik-gvt.jar:lib/batik/batik-parser.jar:lib/batik/batik-script.jar:lib/batik/batik-svg-dom.jar:lib/batik/batik-svggen.jar:lib/batik/batik-swing.jar:lib/batik/batik-transcoder.jar:lib/batik/batik-util.jar:lib/batik/batik-xml.jar:lib/batik/js.jar:lib/batik/pdf-transcoder.jar:lib/batik/xerces_2_5_0.jar:lib/batik/xml-apis.jar
JDOM_CLASSPATH=lib/jdom/jaxen-core.jar:lib/jdom/jaxen-jdom.jar:lib/jdom/jdom.jar:lib/jdom/saxpath.jar
SKIN_CLASSPATH=lib/skin/skinlf.jar
ANT_CLASSPATH=lib/ant/ant.jar
CLASSPATH=$GENERAL_CLASSPATH:$BATIK_CLASSPATH:$JDOM_CLASSPATH:$SKIN_CLASSPATH:$ANT_CLASSPATH

echo About to do compilation, CWD=$CWD
echo java=`which java`

# Do the compilation
#javac \
#-classpath \
#$CLASSPATH \
#src/launcher/*.java \
#src/org/gk/util/*.java \
#src/org/gk/property/*.java \
#src/org/gk/graphEditor/*.java \
#src/org/gk/persistence/*.java \
#src/org/gk/gkEditor/*.java \
#src/org/gk/render/*.java \
#src/org/gk/database/util/*.java \
#src/org/gk/database/*.java \
#src/org/gk/schema/*.java \
#src/org/gk/model/*.java \
#src/org/gk/elv/*.java \
#src/org/gk/gkCurator/authorTool/*.java \
#src/org/gk/IDGeneration/*.java \
#src/org/gk/slicing/*.java \
#src/org/gk/pathView/*.java \
#src/org/gk/qualityCheck/*.java \
#src/org/gk/gkCurator/*.java \
##-d $CWD
javac -classpath $CLASSPATH -sourcepath src src/org/gk/IDGeneration/IDGenerationCommandLine.java


echo Compilation complete

