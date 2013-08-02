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

import psy.lob.saw.ff.FFBuffer;
import psy.lob.saw.queues.offheap.P1C1OffHeapQueue;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBuffer;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBufferUnsafe;
import psy.lob.saw.queues.spsc.fc.SPSPQueueFloatingCounters3;
import psy.lob.saw.queues.spsc.fc.SPSPQueueFloatingCounters4;
import psy.lob.saw.queues.spsc1.SPSCQueue1;
import psy.lob.saw.queues.spsc2.SPSCQueue2;
import psy.lob.saw.queues.spsc3.SPSCQueue3;
import psy.lob.saw.queues.spsc4.SPSCQueue4;
import psy.lob.saw.queues.spsc5.SPSCQueue5;
import psy.lob.saw.queues.spsc6.SPSCQueue6;

public class QueuePerfTest {
	// 15 == 32 * 1024
	public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
	public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
	public static final Integer TEST_VALUE = Integer.valueOf(777);

	public static void main(final String[] args) throws Exception {
		System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
		final Queue<Integer> queue = createQueue(args[0]);

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
		final long start = System.nanoTime();
        final Thread thread = new Thread(new Producer(queue));
        thread.start();
        
		Integer result;
		int i = REPETITIONS;
		do {
			while (null == (result = queue.poll())) {
				Thread.yield();
			}
		} while (0 != --i);

		thread.join();
		
		final long duration = System.nanoTime() - start;
		final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
		System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer
		        .valueOf(runNumber), Long.valueOf(ops), queue.getClass()
		        .getSimpleName(), result);
		return ops;
	}

	public static class Producer implements Runnable {
		private final Queue<Integer> queue;
        
		public Producer(final Queue<Integer> queue) {
			this.queue = queue;
		}

		public void run() {
			int i = REPETITIONS;
			do {
				while (!queue.offer(TEST_VALUE)) {
					Thread.yield();
				}
			} while (0 != --i);
		}
	}
    public static Queue<Integer> createQueue(final String option) {
        switch (Integer.parseInt(option)) {
        case 0:
            return new ArrayBlockingQueue<Integer>(QUEUE_CAPACITY);
        case 1:
            return new P1C1QueueOriginal1<Integer>(QUEUE_CAPACITY);
        case 12:
            return new P1C1QueueOriginal12<Integer>(QUEUE_CAPACITY);
        case 2:
            return new P1C1QueueOriginal2<Integer>(QUEUE_CAPACITY);
        case 21:
            return new P1C1QueueOriginal21<Integer>(QUEUE_CAPACITY);
        case 22:
            return new P1C1QueueOriginal22<Integer>(QUEUE_CAPACITY);
        case 23:
            return new P1C1QueueOriginal23<Integer>(QUEUE_CAPACITY);
        case 3:
            return new P1C1QueueOriginal3<Integer>(QUEUE_CAPACITY);
        case 31:
            return new P1C1QueueOriginal3PadData<Integer>(QUEUE_CAPACITY);
        case 32:
            return new SPSPQueueFloatingCounters4<Integer>(QUEUE_CAPACITY);
        case 33:
            return new SPSPQueueFloatingCounters3<Integer>(QUEUE_CAPACITY);
        case 41:
            return new SPSCQueue1<Integer>(QUEUE_CAPACITY);
        case 42:
            return new SPSCQueue2<Integer>(QUEUE_CAPACITY);
        case 43:
            return new SPSCQueue3<Integer>(QUEUE_CAPACITY);
        case 44:
            return new SPSCQueue4<Integer>(QUEUE_CAPACITY);
        case 45:
            return new SPSCQueue5<Integer>(QUEUE_CAPACITY);
        case 46:
            return new SPSCQueue6<Integer>(QUEUE_CAPACITY);
        case 5:
            return new P1C1Queue4CacheLinesHeapBuffer<Integer>(QUEUE_CAPACITY);
        case 6:
            return new P1C1Queue4CacheLinesHeapBufferUnsafe<Integer>(QUEUE_CAPACITY);
        case 7:
            return new P1C1OffHeapQueue(QUEUE_CAPACITY);
        case 8:
            return new P1C1QueueOriginalPrimitive(QUEUE_CAPACITY);
        case 9:
            return new FFBuffer<Integer>(Integer.getInteger("scale", 15),2);
        case 91:
            return new FFBuffer<Integer>(Integer.getInteger("scale", 15),2);

        default:
            throw new IllegalArgumentException("Invalid option: " + option);
        }
    }
}
