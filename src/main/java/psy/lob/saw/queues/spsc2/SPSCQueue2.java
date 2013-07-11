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
package psy.lob.saw.queues.spsc2;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import psy.lob.saw.util.Pow2;
import psy.lob.saw.util.UnsafeAccess;

/**
 * <ul>
 * <li>Inlined counters
 * <li>Counters are padded
 * <li>Data is padded
 * </ul>
 */
class ColdFields<E> {
    protected static final int BUFFER_PAD = 16;
    protected final int capacity;
    protected final int mask;
    protected final E[] buffer;
    @SuppressWarnings("unchecked")
    public ColdFields(int capacity) {
        if(Pow2.isPowerOf2(capacity)){
            this.capacity = capacity;
        }
        else{
            this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        }
        mask = this.capacity - 1;
        buffer = (E[]) new Object[this.capacity + BUFFER_PAD * 2];
    }
}
class L1Pad<E> extends ColdFields<E> {
    public long p10, p11, p12, p13, p14, p15, p16;
    public L1Pad(int capacity) { super(capacity);}
}
class TailField<E> extends L1Pad<E> {
    protected volatile long tail;
    public TailField(int capacity) { super(capacity);}
}
class L2Pad<E> extends TailField<E> {
    public long p20, p21, p22, p23, p24, p25, p26;
    public L2Pad(int capacity) { super(capacity);}
}
class HeadCache<E> extends L2Pad<E> {
    protected long headCache;
    public HeadCache(int capacity) { super(capacity);}
}
class L3Pad<E> extends HeadCache<E> {
    public long p30, p31, p32, p33, p34, p35, p36;
    public L3Pad(int capacity) { super(capacity);}
}
class HeadField<E> extends L3Pad<E> {
    protected volatile long head;
    public HeadField(int capacity) { super(capacity);}
}
class L4Pad<E> extends HeadField<E> {
    public long p40, p41, p42, p43, p44, p45, p46;
    public L4Pad(int capacity) { super(capacity);}
}
class TailCache<E> extends L4Pad<E> {
    protected long tailCache;
    public TailCache(int capacity) { super(capacity);}

}
class L5Pad<E> extends TailCache<E> {
    public long p50, p51, p52, p53, p54, p55, p56;
    public L5Pad(int capacity) { super(capacity);}
}

public final class SPSCQueue2<E> extends L5Pad<E> implements Queue<E> {
    private final static long TAIL_OFFSET;
    private final static long HEAD_OFFSET;
    static {
        try {
            TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(TailField.class.getDeclaredField("tail"));
            HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(HeadField.class.getDeclaredField("head"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    public SPSCQueue2(final int capacity) {
        super(capacity);
    }
    private void headLazySet(long v) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, HEAD_OFFSET, v);
    }
    private long getHead() {
        return head;
    }
    private void tailLazySet(long v) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, TAIL_OFFSET, v);
    }
    private long getTail() {
        return tail;
    }
    public boolean add(final E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue is full");
    }

    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        final long currentTail = getTail();
        final long wrapPoint = currentTail - capacity;
        if (headCache <= wrapPoint) {
            headCache = getHead();
            if (headCache <= wrapPoint) {
                return false;
            }
        }

        buffer[getArrayIdx(currentTail)] = e;
        tailLazySet(currentTail + 1);

        return true;
    }

    public E poll() {
        final long currentHead = getHead();
        if (currentHead >= tailCache) {
            tailCache = getTail();
            if (currentHead >= tailCache) {
                return null;
            }
        }

        final int index = getArrayIdx(currentHead);
        final E e = buffer[index];
        buffer[index] = null;
        headLazySet(currentHead + 1);

        return e;
    }
    private int getArrayIdx(final long idx) {
        return BUFFER_PAD + ((int) idx & mask);
    }

    public E remove() {
        final E e = poll();
        if (null == e) {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public E element() {
        final E e = peek();
        if (null == e) {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public E peek() {
        return buffer[getArrayIdx(getHead())];
    }

    public int size() {
        return (int) (getTail() - getHead());
    }

    public boolean isEmpty() {
        return getTail() == getHead();
    }

    public boolean contains(final Object o) {
        if (null == o) {
            return false;
        }

        for (long i = getHead(), limit = getTail(); i < limit; i++) {
            final E e = buffer[getArrayIdx(i)];
            if (o.equals(e)) {
                return true;
            }
        }

        return false;
    }

    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    public boolean addAll(final Collection<? extends E> c) {
        for (final E e : c) {
            add(e);
        }

        return true;
    }

    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        Object value;
        do {
            value = poll();
        } while (null != value);
    }
}
