package psy.lob.saw.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import psy.lob.saw.queues.spsc.bq.BQueue;
import psy.lob.saw.queues.spsc.ff.FFBuffer0;
import psy.lob.saw.queues.spsc.ff.FFBuffer01;
import psy.lob.saw.queues.spsc.ff.FFBuffer1;
import psy.lob.saw.queues.spsc.ff.FFBuffer11;
import psy.lob.saw.queues.spsc.ff.FFBuffer2;
import psy.lob.saw.queues.spsc.ff.FFBuffer3;
import psy.lob.saw.queues.spsc.ff.FFBuffer4;
import psy.lob.saw.queues.spsc.lamport.LamportQueue1;
import psy.lob.saw.queues.spsc.lamport.LamportQueue2;
import psy.lob.saw.queues.spsc.lamport.LamportQueue3;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue1;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue2;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue3;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue4;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue5;
import psy.lob.saw.queues.spsc.thompson.ThompsonQueue6;
import psy.lob.saw.queues.yak.YakQueue1;
import psy.lob.saw.queues.yak.YakQueue2;
import psy.lob.saw.queues.yak.YakQueue3;
import psy.lob.saw.queues.yak.YakQueue4;
import psy.lob.saw.queues.yak.YakQueue5;
import psy.lob.saw.queues.yak.YakQueue6;
import psy.lob.saw.queues.yak.YakQueue7;

public final class SPSCQueueFactory {

    public static Queue<Integer> createQueue(int qId, int qScale) {
        int qCapacity = 1 << qScale;
        switch (qId) {
        case 0:
            return new ArrayBlockingQueue<Integer>(qCapacity);
        case -1:
            return new ConcurrentLinkedQueue<Integer>();
        case 11:
            return new LamportQueue1<Integer>(qCapacity);
        case 12:
            return new LamportQueue2<Integer>(qCapacity);
        case 13:
            return new LamportQueue3<Integer>(qCapacity);
        case 21:
            return new ThompsonQueue1<Integer>(qCapacity);
        case 22:
            return new ThompsonQueue2<Integer>(qCapacity);
        case 23:
            return new ThompsonQueue3<Integer>(qCapacity);
        case 24:
            return new ThompsonQueue4<Integer>(qCapacity);
        case 25:
            return new ThompsonQueue5<Integer>(qCapacity);
        case 26:
            return new ThompsonQueue6<Integer>(qCapacity);
        case 31:
            return new YakQueue1<Integer>(qCapacity);
        case 32:
            return new YakQueue2<Integer>(qCapacity);
        case 33:
            return new YakQueue3<Integer>(qCapacity);
        case 34:
            return new YakQueue4<Integer>(qCapacity);
        case 35:
            return new YakQueue5<Integer>(qCapacity);
        case 36:
            return new YakQueue6<Integer>(qCapacity);
        case 37:
            return new YakQueue7<Integer>(qCapacity);
        case 40:
            return new FFBuffer0<Integer>(qScale,Integer.getInteger("sparse.shift", 0));
        case 401:
            return new FFBuffer01<Integer>(qScale,Integer.getInteger("sparse.shift", 0));
        case 41:
            return new FFBuffer1<Integer>(qCapacity, Integer.getInteger("sparse.shift", 0));
        case 411:
            return new FFBuffer11<Integer>(qCapacity,Integer.getInteger("sparse.shift", 0)); 
        case 42:
            return new FFBuffer2<Integer>(qCapacity);
        case 43:
            return new FFBuffer3<Integer>(qCapacity);
        case 44:
            return new BQueue<Integer>(qCapacity);
        case 45:
            return new FFBuffer4<Integer>(qCapacity);
        default:
            throw new IllegalArgumentException("Invalid option: " + qId);
        }
    }

}
