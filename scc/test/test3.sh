#!/bin/bash

jarpath="cycleBreaker.jar"
testNum=3
ram="$1"G

if [[ $# != 1 ]]; then
   echo "ram amount must be specified as param"
   exit 1
fi

echo "start"
cd ..

echo java -jar -Xmx$ram -Xms$ram $jarpath $testNum
java -jar -Xmx$ram -Xms$ram $jarpath $testNum &> out3.txt
echo "finished"
