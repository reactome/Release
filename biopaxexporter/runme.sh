#! /bin/bash
/home/wgm/jdk1.5.0_06/bin/java -cp lib/jdom/jdom.jar:lib/owl/xercesImpl.jar:lib/owl/xml-apis.jar:lib/owl/xmlParserAPIs.jar:lib/mysql/mysql.jar:lib/reactome/reactome.jar:classes org.reactome.biopax.ReactomeToBioPAXXMLConverter $1 $2 $3 $4 $5 $6
