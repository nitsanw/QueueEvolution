#!/bin/bash
for (( c=1; c<=30; c++ ))
do
   taskset -c 0,12 ./runTest.sh $1
done
