#!/bin/bash
for (( c=1; c<=30; c++ ))
do
   taskset -c 4,6 ./runTest.sh $1
done
