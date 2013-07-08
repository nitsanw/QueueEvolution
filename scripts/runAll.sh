export JVM_OPTS='-XX:+UseNUMA -XX:+UseCondCardMark -XX:CompileThreshold=100000'
echo summary,jvmopts,$JVM_OPTS
echo "summary,Same Core"
for IMPL in 3 31 41 42 43 44 45
do
	./repeatRunTestSC.sh $IMPL
done
echo "summary,Cross Core"
for IMPL in 3 31 41 42 43 44 45
do 
        ./repeatRunTestCC.sh $IMPL
done
echo "summary,Cross Socket"
for IMPL in 3 31 41 42 43 44 45
do 
        ./repeatRunTestCS.sh $IMPL
done
