#!/bin/bash
for (( c=1; c<=30; c++ ))
do
   taskset -c 6,12 ./runTest.sh 15 $1
done
