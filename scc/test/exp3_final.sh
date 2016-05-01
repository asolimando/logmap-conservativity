#!/bin/bash

matlabPattern="test2_5rep.txt"
matlabScript="exp3_final.m"
matlabFunc="exp3_final"

indir=test2
plotDir=plot/exp3

echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit"
matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit";

#cp -u plot/exp3/exp3.eps ../../../research.bzr/2013/MappingDebugging/img/exp3
