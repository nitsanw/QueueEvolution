#!/bin/bash
for (( c=1; c<=30; c++ ))
do
   taskset -c 3,7 ./runPerfTest.sh $1 $2 $3
done
