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
package psy.lob.saw.ff31;

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
 * <li>Class is pre-padded
 * <li>Padding is doubled to dodge pre-fetch
 * <li>Use Unsafe to read out of array
 * <li>putOrdered into array as Write Memory Barrier
 * <li>getVolatile from array as Read Memory Barrier
 * </ul>
 */
class L0Pad {
    public long p00, p01, p02, p03, p04, p05, p06, p07;
    public long p30, p31, p32, p33, p34, p35, p36,p37;
}
class ColdFields<E> extends L0Pad {
    protected final static int BUFFER_PAD = 32;
    protected final static long ARRAY_BASE;
    private final static int ELEMENT_SHIFT;
    static {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }
        // Including the buffer pad in the array base offset
        ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << ELEMENT_SHIFT);
    }
    protected final int offsetShift;
    protected final int capacity;
    protected final long mask;
    protected final E[] buffer;
    @SuppressWarnings("unchecked")
    public ColdFields(int capacity, int sparseShift) {
        if(Pow2.isPowerOf2(capacity)){
            this.capacity = capacity;
        }
        else{
            this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        }
        mask = this.capacity - 1;
        this.offsetShift = sparseShift + ELEMENT_SHIFT;
        // pad data on either end with some empty slots.
        buffer = (E[]) new Object[(this.capacity<<sparseShift) + BUFFER_PAD * 2];
    }
}
class L1Pad<E> extends ColdFields<E> {
    public long p10, p11, p12, p13, p14, p15, p16;
    public long p30, p31, p32, p33, p34, p35, p36,p37;
    public L1Pad(int capacity, int sparseShift) { super(capacity, sparseShift);}
}
class TailField<E> extends L1Pad<E> {
    protected long tail;
    public TailField(int capacity, int sparseShift) { super(capacity, sparseShift);}
}
class L2Pad<E> extends TailField<E> {
    public long p20, p21, p22, p23, p24, p25, p26;
    public long p30, p31, p32, p33, p34, p35, p36,p37;
    public L2Pad(int capacity, int sparseShift) { super(capacity, sparseShift);}
}
class HeadField<E> extends L2Pad<E> {
    protected long head;
    public HeadField(int capacity, int sparseShift) { super(capacity, sparseShift);}
}
class L3Pad<E> extends HeadField<E> {
    public long p40, p41, p42, p43, p44, p45, p46;
    public long p30, p31, p32, p33, p34, p35, p36,p37;
    public L3Pad(int capacity, int sparseShift) { super(capacity, sparseShift);}
}
public final class FFBufferOrdered31<E> extends L3Pad<E> implements Queue<E> {
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

    public FFBufferOrdered31(final int capacity, final int sparseShift) {
        super(capacity, sparseShift);
    }

    private long getHead() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, HEAD_OFFSET);
    }

    private long getTail() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, TAIL_OFFSET);
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

        final long offset = computeOffsetInBuffer(tail);
        if (null != UnsafeAccess.UNSAFE.getObjectVolatile(buffer, offset)) {
            return false;
        }
        // STORE/STORE barrier, anything that happens before is visible
        // when the value in the buffer is visible
        UnsafeAccess.UNSAFE.putOrderedObject(buffer, offset, e);
        tail++;
        return true;
    }

    public E poll() {
        final long offset = computeOffsetInBuffer(head);
        @SuppressWarnings("unchecked")
        final E e = (E) UnsafeAccess.UNSAFE.getObjectVolatile(buffer, offset);
        if (null == e) {
            return null;
        }
        UnsafeAccess.UNSAFE.putOrderedObject(buffer, offset, null);
        head++;
        return e;
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
        long currentHead = getHead();
        return getElement(currentHead);
    }

    @SuppressWarnings("unchecked")
    private E getElement(long index) {
        return (E) UnsafeAccess.UNSAFE.getObject(buffer, computeOffsetInBuffer(index));
    }
    private long computeOffsetInBuffer(long index) {
        return ARRAY_BASE + ((index & mask) << offsetShift);
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
            final E e = getElement(i);
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
