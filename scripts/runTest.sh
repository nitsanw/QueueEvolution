java -server -XX:+UseNUMA -XX:+UseCondCardMark -XX:CompileThreshold=100000 -cp examples-1.0-SNAPSHOT.jar -Dscale=$1 uk.co.real_logic.queues.QueuePerfTest $2
