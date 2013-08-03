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
package psy.lob.saw.queues.spsc6;

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
 * </ul>
 */
class L0Pad {
    public long p00, p01, p02, p03, p04, p05, p06, p07;
}

class ColdFields<E> extends L0Pad {
    protected static final int BUFFER_PAD = 16;
    protected final int capacity;
    protected final int mask;
    protected final E[] buffer;

    @SuppressWarnings("unchecked")
    public ColdFields(int capacity) {
        if (Pow2.isPowerOf2(capacity)) {
            this.capacity = capacity;
        } else {
            this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        }
        mask = this.capacity - 1;
        // pad data on either end with some empty slots.
        buffer = (E[]) new Object[this.capacity + BUFFER_PAD * 2];
    }
}

class L1Pad<E> extends ColdFields<E> {
    public long p10, p11, p12, p13, p14, p15, p16;

    public L1Pad(int capacity) {
        super(capacity);
    }
}

class TailField<E> extends L1Pad<E> {
    protected int tail;

    public TailField(int capacity) {
        super(capacity);
    }
}

class L2Pad<E> extends TailField<E> {
    public long p20, p21, p22, p23, p24, p25, p26;

    public L2Pad(int capacity) {
        super(capacity);
    }
}

class HeadField<E> extends L2Pad<E> {
    protected int head;

    public HeadField(int capacity) {
        super(capacity);
    }
}

class L3Pad<E> extends HeadField<E> {
    public long p40, p41, p42, p43, p44, p45, p46;

    public L3Pad(int capacity) {
        super(capacity);
    }
}

public final class SPSCQueue6<E> extends L3Pad<E> implements Queue<E> {
    private final static long TAIL_OFFSET;
    private final static long HEAD_OFFSET;
    private static final long ARRAY_BASE;
    private static final int ELEMENT_SHIFT;
    static {
        try {
            TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(TailField.class.getDeclaredField("tail"));
            HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(HeadField.class.getDeclaredField("head"));
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
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public SPSCQueue6(final int capacity) {
        super(capacity);
    }

    private int getHead() {
        return UnsafeAccess.UNSAFE.getIntVolatile(this, HEAD_OFFSET);
    }

    private int getTail() {
        return UnsafeAccess.UNSAFE.getIntVolatile(this, TAIL_OFFSET);
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
        if (null != UnsafeAccess.UNSAFE.getObject(buffer, offset)) {
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
        final E e = (E) UnsafeAccess.UNSAFE.getObject(buffer, offset);
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
        int currentHead = getHead();
        return getElement(currentHead);
    }

    @SuppressWarnings("unchecked")
    private E getElement(int index) {
        return (E) UnsafeAccess.UNSAFE.getObject(buffer, computeOffsetInBuffer(index));
    }

    private long computeOffsetInBuffer(int index) {
        return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
    }

    public int size() {
        int cTail = getTail() & mask;
        int cHead = getHead() & mask;
        if (cHead > cTail) {
            return cTail + (capacity - cHead);
        } else {
            return cTail - cHead;
        }
    }

    public boolean isEmpty() {
        return getTail() == getHead();
    }

    public boolean contains(final Object o) {
        if (null == o) {
            return false;
        }
        int cTail = getTail() & mask;
        int cHead = getHead() & mask;
        int size;
        if (cHead > cTail) {
            size = cTail + (capacity - cHead);
        } else {
            size = cTail - cHead;
        }

        for (int i = 0; i < size; i++) {
            final E e = getElement(cHead + i);
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
