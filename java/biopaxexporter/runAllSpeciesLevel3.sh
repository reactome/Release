#! /bin/bash
java -Xmx4096m -Dfile.encoding=UTF-8 -cp lib/jdom/jdom.jar:lib/owl/xercesImpl.jar:lib/owl/xml-apis.jar:lib/owl/xmlParserAPIs.jar:lib/mysql/mysql.jar:lib/reactome/reactome.jar:class org.reactome.biopax.SpeciesAllPathwaysLevel3Converter $1 $2 $3 $4 $5 $6
