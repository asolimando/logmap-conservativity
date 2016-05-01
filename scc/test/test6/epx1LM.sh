#!/bin/bash

topdir=.
outdir=.
indir=.

matlabPattern="test6_hermit.txt"
matlabScript="exp1LM.m"
matlabFunc="exp1LM"

plotDir=.

echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit";
matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit";
