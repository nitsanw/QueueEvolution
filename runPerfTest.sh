#!/bin/bash
$JAVA_HOME/bin/java -server -XX:+UseCondCardMark -cp target/QueueEvolution-1.0-SNAPSHOT.jar -Dsparse.shift=$3 psy.lob.saw.queues.benchmarks.handrolled.QueueThroughput$1 $2

