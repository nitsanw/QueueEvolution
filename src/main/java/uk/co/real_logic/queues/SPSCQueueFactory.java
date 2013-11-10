package uk.co.real_logic.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import psy.lob.saw.ff.FFBuffer;
import psy.lob.saw.ff.FFBufferOrderedArrayReadWrite;
import psy.lob.saw.ff.FFBufferOrderedArrayWrite;
import psy.lob.saw.ff.FFBufferOrderedCounterWrite;
import psy.lob.saw.ff1.FFBufferOrdered1;
import psy.lob.saw.ff2.FFBufferOrdered2;
import psy.lob.saw.ff3.FFBufferOrdered3;
import psy.lob.saw.ff31.FFBufferOrdered31;
import psy.lob.saw.ff32.FFBufferOrdered32;
import psy.lob.saw.queues.offheap.P1C1OffHeapQueue;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBuffer;
import psy.lob.saw.queues.offheap.P1C1Queue4CacheLinesHeapBufferUnsafe;
import psy.lob.saw.queues.spsc.bq.BQueue;
import psy.lob.saw.queues.spsc.bq.BQueueNoOfferProbe;
import psy.lob.saw.queues.spsc.fc.SPSPQueueFloatingCounters3;
import psy.lob.saw.queues.spsc.fc.SPSPQueueFloatingCounters4;
import psy.lob.saw.queues.spsc1.SPSCQueue1;
import psy.lob.saw.queues.spsc2.SPSCQueue2;
import psy.lob.saw.queues.spsc3.SPSCQueue3;
import psy.lob.saw.queues.spsc4.SPSCQueue4;
import psy.lob.saw.queues.spsc5.SPSCQueue5;
import psy.lob.saw.queues.spsc6.SPSCQueue6;
import psy.lob.saw.queues.spsc7.SPSCQueue7;
import psy.lob.saw.queues.spsc8.SPSCQueue8;
import psy.lob.saw.queues.spsc81.SPSCQueue81;
import psy.lob.saw.queues.spsc82.SPSCQueue82;
import psy.lob.saw.queues.spsc83.SPSCQueue83;

public final class SPSCQueueFactory {

    public static Queue<Integer> createQueue(int qId, int qScale) {
        int qCapacity = 1 << qScale;
        switch (qId) {
        case 0:
            return new ArrayBlockingQueue<Integer>(qCapacity);
        case -1:
            return new ConcurrentLinkedQueue<Integer>();
        case 1:
            return new P1C1QueueOriginal1<Integer>(qCapacity);
        case 12:
            return new P1C1QueueOriginal12<Integer>(qCapacity);
        case 2:
            return new P1C1QueueOriginal2<Integer>(qCapacity);
        case 21:
            return new P1C1QueueOriginal21<Integer>(qCapacity);
        case 22:
            return new P1C1QueueOriginal22<Integer>(qCapacity);
        case 23:
            return new P1C1QueueOriginal23<Integer>(qCapacity);
        case 3:
            return new P1C1QueueOriginal3<Integer>(qCapacity);
        case 31:
            return new P1C1QueueOriginal3PadData<Integer>(qCapacity);
        case 32:
            return new SPSPQueueFloatingCounters4<Integer>(qCapacity);
        case 33:
            return new SPSPQueueFloatingCounters3<Integer>(qCapacity);
        case 34:
            return new P1C1QueueOriginal3VReadCache<Integer>(qCapacity);
        case 41:
            return new SPSCQueue1<Integer>(qCapacity);
        case 42:
            return new SPSCQueue2<Integer>(qCapacity);
        case 43:
            return new SPSCQueue3<Integer>(qCapacity);
        case 44:
            return new SPSCQueue4<Integer>(qCapacity);
        case 45:
            return new SPSCQueue5<Integer>(qCapacity);
        case 46:
            return new SPSCQueue6<Integer>(qCapacity);
        case 47:
            return new SPSCQueue7<Integer>(qCapacity);
        case 48:
            return new SPSCQueue8<Integer>(qCapacity);
        case 481:
            return new SPSCQueue81<Integer>(qCapacity, Integer.getInteger("sparse.shift", 2));
        case 482:
            return new SPSCQueue82<Integer>(qCapacity);
        case 483:
            return new SPSCQueue83<Integer>(qCapacity);
        case 5:
            return new P1C1Queue4CacheLinesHeapBuffer<Integer>(qCapacity);
        case 6:
            return new P1C1Queue4CacheLinesHeapBufferUnsafe<Integer>(qCapacity);
        case 7:
            return new P1C1OffHeapQueue(qCapacity);
        case 8:
            return new BQueue<Integer>(qCapacity);
        case 81:
            return new BQueueNoOfferProbe<Integer>(qCapacity);
        case 9:
            return new FFBuffer<Integer>(qScale,Integer.getInteger("sparse.shift", 2));
        case 91:
            return new FFBufferOrderedCounterWrite<Integer>(qScale,Integer.getInteger("sparse.shift", 2));
        case 92:
            return new FFBufferOrderedArrayWrite<Integer>(qScale,Integer.getInteger("sparse.shift", 2));
        case 93:
            return new FFBufferOrderedArrayReadWrite<Integer>(qScale,Integer.getInteger("sparse.shift", 2));
        case 94:
            return new FFBufferOrdered1<Integer>(qCapacity);
        case 95:
            return new FFBufferOrdered2<Integer>(qCapacity);
        case 96:
            return new FFBufferOrdered3<Integer>(qCapacity);
        case 961:
            return new FFBufferOrdered31<Integer>(qCapacity,Integer.getInteger("sparse.shift", 2));   
        case 962:
            return new FFBufferOrdered32<Integer>(qCapacity);   
        default:
            throw new IllegalArgumentException("Invalid option: " + qId);
        }
    }

}
