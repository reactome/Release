#! /bin/bash
java -Xmx2048m -Dfile.encoding=UTF-8 -cp lib/jdom/jdom.jar:lib/owl/xercesImpl.jar:lib/owl/xml-apis.jar:lib/owl/xmlParserAPIs.jar:lib/mysql/mysql.jar:lib/reactome/reactome.jar:classes org.reactome.biopax.SpeciesAllPathwaysConverter $1 $2 $3 $4 $5 $6
