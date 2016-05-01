#!/bin/bash

jarpath="cycleBreaker.jar"
testNum=4
ram="$1"G
gbvm="$2"
maxRepet="$3"
resume="$4"

if [[ $# != 4 ]]; then
   echo "1) main jvm ram, 2) worker jvm ram, 3) repetitions  4) resume"
   exit 1
fi

echo "start"
cd ..

echo java -jar -Xmx$ram -Xms$ram $jarpath $testNum $maxRepet $gbvm $resume
java -jar -Xmx$ram -Xms$ram $jarpath $testNum $maxRepet $gbvm $resume &> out4.txt
echo "finished"
