#!/bin/bash
if [ "$REVERSED" = "true" ]; then
    MAIN=ReversedQueuePerfTest
else
    MAIN=QueuePerfTest
fi
echo java -server $JVM_OPTS -cp examples-1.0-SNAPSHOT.jar uk.co.real_logic.queues.$MAIN $1
