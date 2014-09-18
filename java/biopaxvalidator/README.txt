##
#  BioPAX Validator, Version 4.0.0-SNAPSHOT
##
 
****************************************************************************** 
  INTRODUCTION
******************************************************************************

  The BioPAX Validator is a command line tool, Java library, and online
web service for BioPAX formatted pathway data validation. The validator
checks for more than a hundred BioPAX Level3 rules and best practices, 
provides human readable reports and can automatically fix some common 
mistakes in data (can also process Level1 and Level2 data, which are 
first auto-converted to the Level3, and then Level3 rules apply). 
The validator is in use by the BioPAX community and is continuously being
improved and expanded based on community feedback.

  BioPAX is a community developed standard language for integration, 
exchange and analysis of biological pathway data. BioPAX is defined 
in Web Ontology Language (OWL) and can represent a broad spectrum 
of biological processes including metabolic and signaling pathways, 
molecular interactions and gene networks. Pathguide.org lists the 
pathway databases and tools that support BioPAX.

Links: 
http://www.biopax.org/validation
http://www.biopax.org/validator
http://sourceforge.net/projects/biopax/


##
#  Copyright (C) 2008 - 2013 University of Toronto (baderlab.org) and Memorial Sloan-Kettering Cancer Center (cbio.mskcc.org)
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Lesser General Public License as 
#  published by the Free Software Foundation, either version 3 of the 
#  License, or (at your option) any later version.
#  
#  This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Lesser Public License for more details.
#  
#  You should have received a copy of the GNU General Lesser Public 
#  License along with this program.  If not, see
#  <http://www.gnu.org/licenses/lgpl-3.0.html>.
##


******************************************************************************
  INSTALLATION
******************************************************************************

Download the latest ZIP distribution from 

http://sourceforge.net/projects/biopax/files/validator/

Unpack and use (it also includes the WAR file). 


******************************************************************************
  CONSOLE APPLICATION
******************************************************************************

RUN

$sh path/to/validate.sh [args]

- it prints a brief help message when no arguments provided; 
then use as, e.g: 

sh validate.sh input_dir_or_file --profile=notstrict

Validation results are saved to the current directory.

For files under 100Mb total size, you can also use the 
CLIENT jar, and it's usually faster, because does not require initialization, 
parsing ontologies (try it without any arguments to see help):

java -jar biopax-validator-client.jar <in> <out> [optional parameters...]

(the client parameters are slightly differend from the command-line validator's.)


NOTE

Smaller files validate quickly, however, actual time may vary for the same size data; 
it takes longer for networks that contain loops (e.g., in nextStep->PathwayStep sequence);

If you want to validate several files, it always much more efficient 
to copy them all in a directory and use that directory as the first 
parameter for the validate.sh. This is because Validator's initialization
is very time/resources consuming task (mainly, due to OBO files parsing); 
after it's done, next validations are performed much faster.

(Advanced use, usually not required)
One CAN also edit the URLs in the obo.properties file and classpath in valifdate.sh (.bat) 
script(s) to use alternative OBO files locations (or simply the latest versions).
However, when an external ontology is unavailable or broken, the validator fails with a message like: "Caused by: 
psidev.ontology_manager.impl.OntologyLoaderException: Failed loading/parsing ontology CL 
from http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/cell_type/cell.obo"
(look for it in the console output and try with another revision/location of that ontology
or revert to the default, built-in, validator's OBO files)


******************************************************************************
  WEB APPLICATION
******************************************************************************

EXEC-WAR

java -Xmx1g -jar biopax-validator-web.jar

or

sh server.sh

- will run the BioPAX Validator web app with the built-in Tomcat 7 server.
Thats it. Open http://localhost:8080 in a browser.
Use --help argument to see all the server options (e.g., httpPort, ajpPort)


Otherwise, - 
the following describes Java 6 and Tomcat 6 (or 7) Application Server configuration. 

CONFIGURE

Make sure tomcat runs with JAVA_OPTS="-Xmx2g -Xms256m -Dfile.encoding=UTF-8" 
(modify the launch script or environment to add these options), and also 
copy the lib/spring-instrument-tomcat-3.2.8.RELEASE.jar, which enables 
Tomcat-specific AspectJ load-time weaving, to $CATALINA_HOME/lib.

Note:
This should work for a GlassFish not in EE environment too.
For WebLogic, WebSphere, OC4J, Resin, GlassFish (EE), and JBoss, 
you do not have to copy any spring instrument library jar there.
For Jetty and any other application server, by the way,
adding "-javaagent:/path/to/spring-instrument-3.2.8.RELEASE.jar" 
Java option to the application server launch script works too, but 
then either build the new biopax-validator.war from the sources with 
'mvn clean install -P-tomcat' (disable tomcat profile) command, or manually 
remove the Validator's META-INF/context.xml after it's deployed (and failed 
to start there for the first time) and restart the app. server.
(See also: http://static.springsource.org/spring/docs/3.2.8.RELEASE/spring-framework-reference/html/aop.html#aop-aj-ltw-environments).


DEPLOY

Build from sources or get from the distribution archive 
and deploy the biopax-validator.war onto your application server.

Once it gets auto-deployed (check server logs),
open http://localhost:8080/biopax-validator in a browser. 

Also, one CAN edit URLs in the obo.properties file (usually - in the WEB-INF/classes) 
and restart the server to use other than default OBO ontologies/revisions 
(e.g., this helps when an ontology resource becomes unavailable or broken, 
e.g., the latest revision introduces bugs, etc..; i.e, if the validator fails to start for this reason, 
look for a log message like "Caused by: psidev.ontology_manager.impl.OntologyLoaderException: 
Failed loading/parsing ontology CL from http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/cell_type/cell.obo"
and try with another revision of that ontology.)


UNDEPLOY

If you delete the biopax-validator.war from the 'webapps', 
it is usually uninstalled automatically by Tomcat. 



******************************************************************************
  DESIGN
******************************************************************************

I. Framework

 1. Uses Paxtools Java library (the BioPAX API), version 4.3.0-SNAPSHOT.
 2. Spring Framework 3.2.8.RELEASE - to report errors (AOP), wire different modules,
    internationalize, and build web services (MVC).
 3. Java 6: VarArgs, generics, annotations, AOP load-time weaving (LTW),
 	@Resource and @PostConstruct annotations.
 4. @AspectJ AOP, LTW are very powerful things that much simplify 
 	intercepting the exceptions in paxtools.jar and other external
    libraries (e.g., in Jena RDF parser). 
 5. Many popular free libraries.
 
 
II. Validation

 1. Rules are java classes implementing Rule<E> interface and 
    extending AbstractRule<E>, where E is usually either a BioPAX 
    class or Model.
    Controlled Vocabulary class rules extend AbstractCvRule and use 
    CvRestriction and OBO Ontology Manager (derived from PSIDEV EBI sources, thank you)
    to lookup for valid ontology terms and synonyms.
    (Rules may call other rules, but usually it is not recommended, 
    for they are better keep simple and independent.)
    
 2. Post-model validation mode means:
    - check all the rules/objects after the BioPAX model is built (created in memory or read from a file).
         
 3. Fail-fast mode means two things:
    - catch critical BioPAX and/or RDF/XML syntax errors during the model is being read/built (by Paxtools)
    - the number of not fixed errors per validation/report is limited;

III. Errors, Logging, Behavior (actions to undertake)
 1. Logging 
    - commons-logging and log4j
 2. Errors 
    - Spring AOP, MessageSource, resource bundles, and OXM (JAXB)
    are used to collect errors, translate into human-readable messages, and create the validation report.
 3. Configuring
    - behavior (level), error types (code, category), and message templates 
    are configured via resource bundles: rules.properties, codes.properties, profiles.properties
    (in theory, one can create, e.g., /rules_fr_CA.properties on the classpath to see messages in a different language)


******************************************************************************
  DEVELOPER NOTES
******************************************************************************

Debugging tips:
- to disable LTW AOP, set <context:load-time-weaver aspectj-weaving="off"/> 
  in the applicationContext.xml; or edit the META-INF/aop.xml
- to "physically" exclude any rule from being checked - in java source file
   comment out the @Component annotation (the corresponding singleton rule 
   won't be automatically created nor added to the validator bean).
- set "<ruleClass>.behavior=ignore" in the profiles.properties file
   (good for testing AOP and beans configuration without real validation job).


 The validator-core module is not specific to BioPAX; 
 can be used for developing a validator and rules for another knowledge domain.
 
 Enjoy and give us a feedback!
