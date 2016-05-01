#!/bin/bash

jarpath="cycleBreaker.jar"
#multithread="false"
#aspTout=60
testNum=2
ram="$1"G
gbvm="$2"
maxRepet="$3"
resume="$4"

if [[ $# != 4 ]]; then
   echo "1) main jvm ram, 2) worker jvm ram, 3) repetitions 4) resume"
   exit 1
fi

echo "start"
cd ..

#for i in `seq 1 $maxRepet`
#do
echo java -jar -Xmx$ram -Xms$ram $jarpath $testNum $maxRepet $gbvm $resume
java -jar -Xmx$ram -Xms$ram $jarpath $testNum  $maxRepet $gbvm $resume &> out2.txt
#  java -jar -Xmx$ram -Xms$ram $jarpath $testNum $multithread $i $aspTout &> out2_$i.txt
#done

echo "finished"
