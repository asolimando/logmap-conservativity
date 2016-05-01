#!/bin/bash

topdir=.
indir=test1

outfile="$topdir/$indir/merged.text"

matlabScript="exp2.m"
matlabFunc="exp2"

plotDir=plot/exp2/
matlabOutFileBasename="table"
matlabOutFileExt=".tex"
matlabPattern="merged.text"

cat $topdir/$indir/*.txt > $outfile
header=`head -n 1 $outfile`
sed -i '/'"${header}"'*$/d' $outfile
echo $header | cat - $outfile > temp && mv temp $outfile

# for the following two vars: 0 = skip it, 1 = matcher, 2 = track
for matlabAggrColExt in `seq 0 2`
do
    for matlabAggrCol in `seq 1 2`
    do
	if [ $matlabAggrColExt -ne $matlabAggrCol ] ; then 
	  matlabOutFile="$matlabOutFileBasename$matlabAggrColExt$matlabAggrCol$matlabOutFileExt"
	  #/opt/matlab/bin/matlab -nodisplay -nodesktop -r "run `pwd`$matlabScript"
	  echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabOutFile','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit"
	  matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabOutFile','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit";
	  cp -u $plotDir$matlabOutFile ../../../research.bzr/2013/MappingDebugging/tables
	fi
    done
done
