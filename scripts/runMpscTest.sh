#!/bin/bash
taskset -c 1-7 java -server -XX:+UseCondCardMark -XX:CompileThreshold=100000 -Dproducers=$2 -Dcas.backoff=$3 -Dsparse.data=$4 -cp ../target/examples-1.0-SNAPSHOT.jar uk.co.real_logic.queues.MPSCFairQueuePerfTest $1
