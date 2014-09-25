#!/bin/sh

###
# #%L
# BioPAX Validator Assembly
# %%
# Copyright (C) 2008 - 2013 University of Toronto (baderlab.org) and Memorial Sloan-Kettering Cancer Center (cbio.mskcc.org)
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as 
# published by the Free Software Foundation, either version 3 of the 
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Lesser Public License for more details.
# 
# You should have received a copy of the GNU General Lesser Public 
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/lgpl-3.0.html>.
# #L%
###

# check java version
export JAVA_HOME=/usr
echo your JAVA_HOME=$JAVA_HOME
$JAVA_HOME/bin/java -version
echo Running BioPAX Validator...

VALIDATOR_OPTS="-javaagent:lib/spring-instrument-3.2.8.RELEASE.jar -Xmx2g -Dfile.encoding=UTF-8 -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider"

# run the validator with log4j.properties and obo.properties from current directory -
#$JAVA_HOME/bin/java -cp .:biopax-validator.jar $VALIDATOR_OPTS org.biopax.validator.Main "$1" "$2" "$3" "$4" "$5" "$6"

# run with default logging and OBO properties (from the default classpath)
$JAVA_HOME/bin/java $VALIDATOR_OPTS -jar biopax-validator.jar "$1" "$2" "$3" "$4" "$5" "$6"
