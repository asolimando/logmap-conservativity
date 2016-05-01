#!/bin/bash

matlabPattern="$1" #"test4.text"
matlabScript="exp7.m"
matlabFunc="exp7"

indir=test4
plotDir=plot/exp7/

for kind in `seq 1 1`
do
  echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$kind); end; quit"
  matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$kind); end; quit";
done

#cp -u $plotDir*.eps ../../../research.bzr/2013/MappingDebugging/img/exp7
