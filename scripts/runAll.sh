#!/bin/bash
export JVM_OPTS=''
./runTestSuite.sh $1
export JVM_OPTS='-XX:+UseCondCardMark'
./runTestSuite.sh $1
export JVM_OPTS='-XX:+UseCondCardMark -XX:CompileThreshold=100000'
./runTestSuite.sh $1
export JVM_OPTS='-XX:+UseNUMA -XX:+UseCondCardMark -XX:CompileThreshold=100000'
./runTestSuite.sh