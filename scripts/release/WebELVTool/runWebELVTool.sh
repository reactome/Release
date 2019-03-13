#!/bin/bash
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR
java -Xmx1024m -jar webELVTool.jar $1 $2 $3 $4 $5 $6 $7 $8 $9


