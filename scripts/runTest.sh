#!/bin/bash
java -server $JVM_OPTS -cp ../target/examples-1.0-SNAPSHOT.jar uk.co.real_logic.queues.$1 $2
