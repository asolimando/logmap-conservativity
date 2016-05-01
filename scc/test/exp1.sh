#!/bin/bash

topdir=.
outdir=test1
indir=test1

matlabPattern="exp1.text"
matlabScript="exp1.m"
matlabFunc="exp1"

plotDir=plot/exp1

# for the following two vars: 0 = skip it, 1 = matcher, 2 = track, 4 = year
for matlabAggrColExt in `seq 1 2`
do
    for matlabAggrCol in `seq 0 0`
    do
	for multi in `seq 0 1`
	do 
	  if [ $matlabAggrColExt -ne $matlabAggrCol ] ; then
	    echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$matlabAggrColExt,$matlabAggrCol,$multi); end; quit"
	    matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$matlabAggrColExt,$matlabAggrCol,$multi); end; quit";
	  fi
	done
    done
done

cp -u ./$plotDir/* ../../../research.bzr/2013/MappingDebugging/img/exp1
