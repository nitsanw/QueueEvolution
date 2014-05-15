#/bin/sh
for BURST in 1 10 100 1000
do
	echo $1-$BURST
	java -Dburst.size=$BURST -XX:+UseCondCardMark -jar target/microbenchmarks.jar -f 5  ".*.QueueR.*" > Queue-Rtt-$1-$BURST.out
done


