#!/bin/bash

matlabPattern="umls.text"
matlabScript="exp5.m"
matlabFunc="exp5"

indir=test3
plotDir=plot/exp5

echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$matlabPattern'); end; quit"
matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$matlabPattern'); end; quit";

cp -u umls.tex ../../../research.bzr/2013/MappingDebugging/tables
