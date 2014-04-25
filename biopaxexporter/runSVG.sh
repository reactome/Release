#! /bin/bash
java -cp lib/jdom/jdom.jar:lib/owl/xercesImpl.jar:lib/owl/xml-apis.jar:lib/owl/xmlParserAPIs.jar:lib/mysql/mysql.jar:lib/reactome/reactome.jar:classes org.gk.gkCurator.GraphvizDotGenerator $1 $2 $3 $4 $5 $6 $7 $8

