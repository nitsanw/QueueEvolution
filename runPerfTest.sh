#!/bin/bash
$JAVA_HOME/bin/java -server -XX:+UseCondCardMark -XX:CompileThreshold=100000 -cp target/examples-1.0-SNAPSHOT.jar -Dsparse.shift=$3 psy.lob.saw.queues.QueuePerfTest$1 $2

