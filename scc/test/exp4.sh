#!/bin/bash

topdir=.
outdir=test1
indir=test1

outfile="$topdir/$indir/merged.text"

matlabScript="exp4.m"
matlabFunc="exp4"

plotDir=plot/exp4
matlabPattern="merged.text"

#if [ -f $outfile ]
#then
#    rm $outfile
#fi

cat $topdir/$indir/*.txt > $outfile
header=`head -n 1 $outfile`
sed -i '/'"${header}"'*$/d' $outfile
echo $header | cat - $outfile > temp && mv temp $outfile

# for the following two vars: 0 = skip it, 1 = matcher, 2 = track
for matlabAggrColExt in `seq 0 0`
do
    for matlabAggrCol in `seq 1 2`
    do
	if [ $matlabAggrColExt -ne $matlabAggrCol ] ; then 
	  matlabOutFile="$matlabOutFileBasename$matlabAggrColExt$matlabAggrCol$matlabOutFileExt"
	  #/opt/matlab/bin/matlab -nodisplay -nodesktop -r "run `pwd`$matlabScript"
	  echo matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit"
	  matlab -nodesktop -nosplash -nodisplay -r "try, $matlabFunc('$indir','$plotDir','$matlabPattern',$matlabAggrCol,$matlabAggrColExt); end; quit";
	fi
    done
done

cp -u ./$plotDir/* ../../../research.bzr/2013/MappingDebugging/img/exp4
