#!/bin/bash

topdir=.
indir=test1

#outfile="$topdir/$indir/merged.text"

matlabScript="exp2eswc.m"
matlabFunc="exp2eswc"

plotDir=../../../research.bzr/2013/MappingDbgESWC/tables/
matlabOutFileBasename="table"
matlabOutFileExt=".tex"
matlabPattern="merged.text"

#cat $topdir/$indir/*.txt > $outfile
#header=`head -n 1 $outfile`
#sed -i '/'"${header}"'*$/d' $outfile
#echo $header | cat - $outfile > temp && mv temp $outfile

# for the following two vars: 0 = skip it, 1 = matcher, 2 = track
for matlabAggrColExt in `seq 0 0`
do
    for matlabAggrCol in `seq 1 2`
    do
	if [ $matlabAggrColExt -ne $matlabAggrCol ] ; then 
	  matlabOutFile="$matlabOutFileBasename$matlabAggrColExt$matlabAggrCol$matlabOutFileExt"
	  #/opt/matlab/bin/matlab -nodisplay -nodesktop -r "run `pwd`$matlabScript"
	  echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabOutFile','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit"
	  matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabOutFile','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit";
	fi
    done
done

matlabPattern="umls.text"
matlabScript="exp5eswc.m"
matlabFunc="exp5eswc"

indir=test3

echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit"
matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern'); end; quit";
