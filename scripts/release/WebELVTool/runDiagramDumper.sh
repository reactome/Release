#!/bin/bash
java -Xmx2048m -cp webELVTool.jar:mysql.jar:itext-2.0.4.jar org.gk.pathwaylayout.PathwayDiagramDumper $1 $2 $3 $4 $5 $6