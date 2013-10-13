/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import static com.lmax.disruptor.RingBuffer.createSingleProducer;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;

public class DisruptorSPSCPerfTest {
    // 15 == 32 * 1024
    public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
    public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
    public static final int TEST_VALUE = 777;

    private static final class StoringHandler implements EventHandler<ValueEvent> {
        int reps;
        int value;
        BatchEventProcessor<ValueEvent> processor;
        @Override
        public void onEvent(ValueEvent event, long sequence, boolean endOfBatch) throws Exception{
            this.value = event.value;
            if(0 == --reps){
                processor.halt();
            }
        }
        
    }

    static final class ValueEvent {
        public static final EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {
            @Override
            public ValueEvent newInstance() {
                return new ValueEvent();
            }

        };
        int value;
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
        WaitStrategy yieldStrategy = new WaitStrategy() {

            @Override
            public long waitFor(final long sequence, Sequence cursor, final Sequence dependentSequence,
                    final SequenceBarrier barrier) throws AlertException, InterruptedException {
                long availableSequence;

                while ((availableSequence = dependentSequence.get()) < sequence) {
                    Thread.yield();
                }

                return availableSequence;
            }

            @Override
            public void signalAllWhenBlocking() {

            }
        };
        RingBuffer<ValueEvent> ringBuffer = createSingleProducer(ValueEvent.EVENT_FACTORY, QUEUE_CAPACITY,
                yieldStrategy);
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        final StoringHandler handler = new StoringHandler();
        final BatchEventProcessor<ValueEvent> batchEventProcessor = new BatchEventProcessor<ValueEvent>(
                ringBuffer, sequenceBarrier, handler);
        {
            ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
        }
        handler.processor = batchEventProcessor;
        final long[] results = new long[20];
        for (int i = 0; i < 20; i++) {
            System.gc();
            handler.reps = REPETITIONS;
            results[i] = performanceRun(i, ringBuffer, batchEventProcessor, handler);
        }
        // only average last 10 results for summary
        long sum = 0;
        for (int i = 10; i < 20; i++) {
            sum += results[i];
        }
        System.out.format("summary,%s,%d\n", "Disruptor", sum / 10);
    }

    private static long performanceRun(final int runNumber, final RingBuffer<ValueEvent> ringBuffer, BatchEventProcessor<ValueEvent> batchEventProcessor, StoringHandler handler) throws Exception {
        final long start = System.nanoTime();
        final Thread thread = new Thread(new Producer(ringBuffer));
        thread.start();

        batchEventProcessor.run();

        thread.join();

        final long duration = System.nanoTime() - start;
        final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
        System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer.valueOf(runNumber), Long.valueOf(ops), ringBuffer
                .getClass().getSimpleName(), handler.value);
        return ops;
    }

    public static class Producer implements Runnable {
        private final RingBuffer<ValueEvent> rb;

        public Producer(final RingBuffer<ValueEvent> ringBuffer) {
            this.rb = ringBuffer;
        }

        public void run() {
            int i = REPETITIONS;
            do {
                long next = rb.next();
                rb.get(next).value  = TEST_VALUE;
                rb.publish(next);
            } while (0 != --i);
        }
    }
}
