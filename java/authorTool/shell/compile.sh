#!/bin/sh
javac -verbose -sourcepath src \
-classpath .:\
lib/skin/skinlf.jar:\
lib/mysql-connector-java-3.0.11-stable-bin.jar:\
lib/osxAdapter.jar:\
lib/batik/batik-awt-util.jar:\
lib/batik/batik-bridge.jar:\
lib/batik/batik-css.jar:\
lib/batik/batik-dom.jar:\
lib/batik/batik-extension.jar:\
lib/batik/batik-ext.jar:\
lib/batik/batik-gui-util.jar:\
lib/batik/batik-gvt.jar:\
lib/batik/batik-parser.jar:\
lib/batik/batik-script.jar:\
lib/batik/batik-svg-dom.jar:\
lib/batik/batik-svggen.jar:\
lib/batik/batik-swing.jar:\
lib/batik/batik-transcoder.jar:\
lib/batik/batik-util.jar:\
lib/batik/batik-xml.jar:\
lib/batik/js.jar:\
lib/batik/pdf-transcoder.jar:\
lib/batik/xerces_2_5_0.jar:\
lib/batik/xml-apis.jar \
src/org/gk/database/*.java \
src/org/gk/gkCurator/*.java \
src/org/gk/gkEditor/*.java \
src/org/gk/graphEditor/*.java \
src/org/gk/model/*.java \
src/org/gk/pathView/*.java \
src/org/gk/persistence/*.java \
src/org/gk/property/*.java \
src/org/gk/render/*.java \
src/org/gk/schema/*.java \
src/org/gk/test/*.java \
src/org/gk/util/*.java \
-d .
