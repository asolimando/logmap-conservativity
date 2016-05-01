#!/bin/bash

jarpath="cycleBreaker.jar"
testNum=1
ram="$1"G

if [[ $# != 1 ]]; then
   echo "ram amount must be specified as param"
   exit 1
fi

echo "start"
cd ..

echo java -jar -Xmx$ram -Xms$ram $jarpath $testNum 6 0
java -jar -Xmx$ram -Xms$ram $jarpath $testNum 6 0 &> out1.txt
echo "finished"
