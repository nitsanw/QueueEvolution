package psy.lob.saw.queues.jmh.benchmarks;

import java.util.Queue;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import psy.lob.saw.queues.SPSCQueueFactory;

@State(Scope.Group)
public abstract class QueueBenchmark {
	@Param(value={"-1","0","11","12","13","21","22","23","24","25","26","31","32","33","34","35","36",})
    private int queueType;
    protected  Queue<Integer> q;
    
    @Setup(Level.Trial)
    public void createQueue()
    {
    	q = SPSCQueueFactory.createQueue(queueType, 15);
    }
}
