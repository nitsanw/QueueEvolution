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
package uk.co.real_logic.queues;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public class MPSCFairQueuePerfTest {
	// 15 == 32 * 1024
	public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
	public static final int PRODUCERS = Integer.getInteger("producers", 4);
	public static final long REPETITIONS = PRODUCERS
	        * ((Integer.getInteger("reps", 10) * 1000 * 1000) / PRODUCERS);
	public static final Integer TEST_VALUE = Integer.valueOf(777);
	private static final Comparator<Producer> START_TIME_COMPARATOR = new Comparator<Producer>() {

		@Override
		public int compare(Producer o1, Producer o2) {
			return (int) (o2.start - o1.start);
		}

	};
	private static final int ITERATIONS = 14;

	public static void main(final String[] args) throws Exception {
		System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS + " producers:" + PRODUCERS);
		final Queue<Integer> queue = MPSCQueueFactory.createQueue(Integer.parseInt(args[0]),
		        Integer.getInteger("scale", 15), PRODUCERS);

		final long[] results = new long[ITERATIONS];
		for (int i = 0; i < ITERATIONS; i++) {
			System.gc();
			results[i] = performanceRun(i, queue);
			Thread.sleep(100); //cool down between runs
		}
		// only average last 10 results for summary
		long sum = 0;
		for (int i = ITERATIONS/2; i < ITERATIONS; i++) {
			sum += results[i];
		}
		System.out.format("summary,MPSCFairQueuePerfTest,%s,%d,%d\n", queue.getClass().getSimpleName(), PRODUCERS, sum / (ITERATIONS/2));
	}

	private static long performanceRun(final int runNumber, final Queue<Integer> queue) throws Exception {
		CountDownLatch cLatch = new CountDownLatch(1);
		CountDownLatch pLatch = new CountDownLatch(PRODUCERS);

		Consumer c = new Consumer(queue, pLatch, cLatch);
		Producer[] p = new Producer[PRODUCERS];
		for (int i = 0; i < PRODUCERS; i++) {
			p[i] = new Producer(queue, pLatch, cLatch);
		}
		perfTest(c, p);
		Arrays.sort(p, START_TIME_COMPARATOR);
		final long duration = c.end - p[0].start;
		;
		final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
		System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer.valueOf(runNumber), Long.valueOf(ops),
		        queue.getClass().getSimpleName(), c.result);
		return ops;
	}

	private static void perfTest(Consumer c, Producer[] p) throws InterruptedException {
		final Thread[] pThread = new Thread[PRODUCERS];
		final Thread cThread = new Thread(c);
		for (int i = 0; i < PRODUCERS; i++) {
			pThread[i] = new Thread(p[i]);
			pThread[i].start();
		}
		cThread.start();
		for (int i = 0; i < PRODUCERS; i++) {
			pThread[i].join();
		}
		cThread.join();
	}

	public static class Consumer implements Runnable {
		final Queue<Integer> queue;
		final CountDownLatch cLatch;
		final CountDownLatch pLatch;
		Integer result;
		long end;

		public Consumer(Queue<Integer> queue, CountDownLatch pLatch, CountDownLatch cLatch) {
			this.queue = queue;
			this.cLatch = cLatch;
			this.pLatch = pLatch;
		}

		public void run() {
			try {
				pLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			cLatch.countDown();
			Integer result;
			long i = REPETITIONS;
			do {
				while (null == (result = queue.poll())) {
					Thread.yield();
				}
			} while (0 != --i);
			this.result = result;
			end = System.nanoTime();
		}
	}

	public static class Producer implements Runnable {
		private final Queue<Integer> queue;
		final CountDownLatch pLatch;
		final CountDownLatch cLatch;
		long start;

		public Producer(final Queue<Integer> queue, CountDownLatch pLatch, CountDownLatch cLatch) {
			this.queue = queue;
			this.pLatch = pLatch;
			this.cLatch = cLatch;
		}

		public void run() {
			pLatch.countDown();
			try {
				cLatch.await();
				// delay producer start, let consumer warm up
				LockSupport.parkNanos(100 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			start = System.nanoTime();
			long i = REPETITIONS / PRODUCERS;
			do {
				while (!queue.offer(TEST_VALUE)) {
					Thread.yield();
				}
			} while (0 != --i);
		}
	}
}
