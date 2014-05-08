package psy.lob.saw.queues.benchmarks.jmh;

import java.util.Queue;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import psy.lob.saw.queues.common.SPSCQueueFactory;

@State(Scope.Benchmark)
public abstract class QueueBenchmark {
	@Param(value={"11","12","21","22","23","24","25","31","32","33","41","42"})
	protected int queueType;
	@Param(value={"17"})
	protected int queueScale;
    protected static Queue<Integer> q;
    
    @Setup(Level.Trial)
    public void createQueue()
    {
    	q = SPSCQueueFactory.createQueue(queueType, queueScale);
    }
}
