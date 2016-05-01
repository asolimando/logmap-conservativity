#!/bin/bash

matlabPattern="$1"
matlabScript="exp3old2.m"
matlabFunc="exp3old2"

indir=.
plotDir=.

echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit"
matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit";

#cp -u plot/exp3/exp3.eps ../../../research.bzr/2013/MappingDebugging/img/exp3