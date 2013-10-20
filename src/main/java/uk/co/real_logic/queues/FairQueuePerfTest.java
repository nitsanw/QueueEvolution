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

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public class FairQueuePerfTest {
    enum MainThread {
        PRODUCER, CONSUMER, NEUTRAL
    }

    // 15 == 32 * 1024
    public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
    public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
    public static final boolean PRODUCER_TRIGGERS = Boolean.getBoolean("producer.triggers");
    public static final MainThread MAIN_TYPE;
    static {
        String typeName = System.getProperty("main.type");
        MainThread type;
        try {
            type = MainThread.valueOf(typeName);
        } catch (Exception e) {
            type = MainThread.CONSUMER;
        }
        MAIN_TYPE = type;
    }
    public static final Integer TEST_VALUE = Integer.valueOf(777);

    public static void main(final String[] args) throws Exception {
        System.out.println("type:" + MAIN_TYPE + " trigger:"+(PRODUCER_TRIGGERS?"P":"C")+" capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
        final Queue<Integer> queue = SPSCQueueFactory.createQueue(Integer.parseInt(args[0]), Integer.getInteger("scale", 15));

        final long[] results = new long[20];
        for (int i = 0; i < 20; i++) {
            System.gc();
            results[i] = performanceRun(i, queue);
        }
        // only average last 10 results for summary
        long sum = 0;
        for (int i = 10; i < 20; i++) {
            sum += results[i];
        }
        System.out.format("summary,FairQueuePerfTest,%s,%d,%s,%s\n", queue.getClass().getSimpleName(), sum / 10,MAIN_TYPE.toString(),PRODUCER_TRIGGERS?"P":"C");
    }

    private static long performanceRun(final int runNumber, final Queue<Integer> queue) throws Exception {
        CountDownLatch cLatch = new CountDownLatch(1);
        CountDownLatch pLatch = new CountDownLatch(1);

        Consumer c = new Consumer(queue, pLatch, cLatch);
        Producer p = new Producer(queue, pLatch, cLatch);
        switch (MAIN_TYPE) {
        case PRODUCER:
            producerIsMain(c, p);
            break;
        case CONSUMER:
            consumerIsMain(c, p);
            break;
        case NEUTRAL:
            mainIsMain(c, p);
            break;
        }
        final long duration = c.end - p.start;
        final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
        System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer.valueOf(runNumber), Long.valueOf(ops), queue
                .getClass().getSimpleName(), c.result);
        return ops;
    }

    private static void producerIsMain(Consumer c, Producer p) throws InterruptedException {
        final Thread cThread = new Thread(c);
        cThread.start();
        p.run();
        cThread.join();
    }

    private static void consumerIsMain(Consumer c, Producer p) throws InterruptedException {
        final Thread pThread = new Thread(p);
        pThread.start();
        c.run();
        pThread.join();
    }

    private static void mainIsMain(Consumer c, Producer p) throws InterruptedException {
        final Thread pThread = new Thread(p);
        final Thread cThread = new Thread(c);
        pThread.start();
        cThread.start();
        pThread.join();
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
            if (PRODUCER_TRIGGERS) {
                cLatch.countDown();
                try {
                    pLatch.await();
                    // delay consumer start
                    LockSupport.parkNanos(100*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            } else {
                try {
                    pLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                cLatch.countDown();
            }
            Integer result;
            int i = REPETITIONS;
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
            if (PRODUCER_TRIGGERS) {
                try {
                    cLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                pLatch.countDown();
            } else {
                pLatch.countDown();
                try {
                    cLatch.await();
                    // delay producer start
                    LockSupport.parkNanos(100*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            start = System.nanoTime();
            int i = REPETITIONS;
            do {
                while (!queue.offer(TEST_VALUE)) {
                    Thread.yield();
                }
            } while (0 != --i);
        }
    }
}
