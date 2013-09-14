#!/bin/bash
for (( c=1; c<=30; c++ ))
do
   taskset -c 22,23 ./runTest.sh $1 $2
done
