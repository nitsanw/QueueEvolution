#echo "Same Core"
#for IMPL in 3 31 41 42 43 44 45
#do
#	./repeatRunTestSC.sh $IMPL
#done
echo "Cross Core"
for IMPL in 3 31 41 42 43 44 45
do 
        ./repeatRunTestCC.sh $IMPL
done
echo "Cross Socket"
for IMPL in 3 31 41 42 43 44 45
do 
        ./repeatRunTestCS.sh $IMPL
done
