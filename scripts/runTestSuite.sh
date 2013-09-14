#!/bin/bash
IMPLS='3 31 32 41 42 43 44 45'
echo summary,jvmopts,$JVM_OPTS
echo "summary,Same Core"
for IMPL in $IMPLS
do
	./repeatRunTestSC.sh $1 $IMPL
done
echo "summary,Cross Core"
for IMPL in $IMPLS
do 
    ./repeatRunTestCC.sh $1 $IMPL
done
echo "summary,Cross Socket"
for IMPL in $IMPLS
do 
    ./repeatRunTestCS.sh $IMPL
done
