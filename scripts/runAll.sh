#!/bin/bash
JVM_OPTS=''
./runTestSuite.sh
export JVM_OPTS='-XX:+UseCondCardMark'
./runTestSuite.sh
export JVM_OPTS='-XX:+UseCondCardMark -XX:CompileThreshold=100000'
./runTestSuite.sh
export JVM_OPTS='-XX:+UseNUMA -XX:+UseCondCardMark -XX:CompileThreshold=100000'
./runTestSuite.sh