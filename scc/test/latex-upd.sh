#!/bin/bash

cd ../../../research.bzr/2013/MappingDebugging
pdflatex ontoMappingDebug.tex
if [[ $# != 1 ]]; then
   echo "param: 0 = recompile, >1 = recompile and open pdf"
   exit 1
fi

if [[ $1 != 0 ]]; then
	okular ontoMappingDebug.pdf &
fi
