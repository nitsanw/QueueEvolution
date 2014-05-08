package psy.lob.saw.queues.benchmarks.jmh;


import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class SingleThreadedOffer extends QueueBenchmark
{
    public static final int CAPACITY = 1 << 15;
    public static final Integer TOKEN = 1;
   
    
    @Setup(Level.Invocation)
    public void clear()
    {
        q.clear();
    }

    @GenerateMicroBenchmark
    @OperationsPerInvocation(CAPACITY)
    public void offer()
    {
        final Queue<Integer> lq = q;
        for (int i = 0; i < CAPACITY; i++)
        {
            lq.offer(TOKEN);
        }
    }
}
