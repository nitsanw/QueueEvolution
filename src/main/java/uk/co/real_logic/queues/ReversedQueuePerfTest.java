/*
 * Copyright 2012 Real Logic Ltd.
 *
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
package uk.co.real_logic.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import psy.lob.saw.queues.offheap.P1C1OffHeapQueue;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBuffer;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBufferUnsafe;
import psy.lob.saw.queues.spsc.fc.SPSPQueueFloatingCounters;
import psy.lob.saw.queues.spsc1.SPSCQueue1;
import psy.lob.saw.queues.spsc2.SPSCQueue2;
import psy.lob.saw.queues.spsc3.SPSCQueue3;
import psy.lob.saw.queues.spsc4.SPSCQueue4;
import psy.lob.saw.queues.spsc5.SPSCQueue5;

public class ReversedQueuePerfTest {
	// 15 == 32 * 1024
	public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
	public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
	public static final Integer TEST_VALUE = Integer.valueOf(777);

	public static void main(final String[] args) throws Exception {
		System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
		final Queue<Integer> queue = QueuePerfTest.createQueue(args[0]);

		final long[] results = new long[20];
		for (int i = 0; i < 20; i++) {
			System.gc();
			results[i] = performanceRun(i, queue);
		}
		// only average last 10 results for summary
		long sum = 0;
		for(int i = 10; i < 20; i++){
		    sum+=results[i];
		}
		System.out.format("summary,%s,%d\n", queue.getClass().getSimpleName(), sum/10);
	}

	private static long performanceRun(final int runNumber,
	        final Queue<Integer> queue) throws Exception {
        Consumer c = new Consumer(queue);
        final Thread thread = new Thread(c);
        thread.start();
        c.latch.await();
        int i = REPETITIONS;
        do {
            while (!queue.offer(TEST_VALUE)) {
                Thread.yield();
            }
        } while (0 != --i);
        thread.join();
        final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / c.duration;
        System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer
                .valueOf(runNumber), Long.valueOf(ops), queue.getClass()
                .getSimpleName(), c.result);        return ops;
    }

    public static class Consumer implements Runnable {
        final CountDownLatch latch = new CountDownLatch(1);
        private final Queue<Integer> queue;
        Integer result;
        long duration;
        public Consumer(final Queue<Integer> queue) {
            this.queue = queue;
        }

        public void run() {
            final long start = System.nanoTime();
            latch.countDown();
            Integer result;
            int i = REPETITIONS;
            do {
                while (null == (result = queue.poll())) {
                    Thread.yield();
                }
            } while (0 != --i);
            this.result = result;
            duration = System.nanoTime() - start;
        }
    }
}
