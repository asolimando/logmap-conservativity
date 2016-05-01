#!/bin/bash

jarpath="cycleBreaker.jar"
multithread="true"
testNum=1
ram="$1"G

if [[ $# != 1 ]]; then
   echo "ram amount must be specified as param"
   exit 1
fi

echo "start"
cd ..

echo java -jar -Xmx$ram -Xms$ram $jarpath $testNum 6 0 1
java -jar -Xmx$ram -Xms$ram $jarpath $testNum 6 0 1 &> out1_prec.txt
echo "finished"
