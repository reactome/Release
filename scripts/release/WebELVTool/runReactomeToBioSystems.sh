#!/bin/bash
# Generate all xml files
java -Xmx2048m -Dfile.encoding=UTF-8 -cp BioSystems.jar:webELVTool.jar:mysql.jar:jdom.jar:xerces.jar:xml-apis.jar org.gk.biosystems.ReactomeToBioSystemsConverter $1 $2 $3 $4 $5 $6
