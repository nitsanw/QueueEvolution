package uk.co.real_logic.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import psy.lob.saw.queues.mpsc.MPSCQueue32;
import psy.lob.saw.queues.mpsc.MPSCQueue31;

public final class MPSCQueueFactory {

    public static Queue<Integer> createQueue(int qId, int qScale, int producers) {
        int qCapacity = 1 << qScale;
        switch (qId) {
        case 0:
            return new ArrayBlockingQueue<Integer>(qCapacity);
        case 1:
            return new ConcurrentLinkedQueue<Integer>();
        case 2:
            return new LinkedTransferQueue<Integer>();   
        case 31:
            return new MPSCQueue31<Integer>(qCapacity);   
        case 32:
            return new MPSCQueue32<Integer>(qCapacity);   
        default:
            throw new IllegalArgumentException("Invalid option: " + qId);
        }
    }

}
