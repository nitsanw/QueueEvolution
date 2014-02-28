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
package psy.lob.saw.queues.spsc.thompson;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

import psy.lob.saw.util.Pow2;
import psy.lob.saw.util.UnsafeAccess;

abstract class ThompsonQueue5L0Pad<E> extends AbstractQueue<E>{
    protected long p00, p01, p02, p03, p04, p05, p06, p07;
}

abstract class ThompsonQueue5Fields<E> extends ThompsonQueue5L0Pad<E> {
    protected static final int BUFFER_PAD = 32;
    protected static final int SPARSE_SHIFT = Integer.getInteger("sparse.shift", 0);
    protected final int capacity;
    protected final long mask;
    protected final E[] buffer;
    protected final VolatileLongCell tail = new VolatileLongCell(0);
    protected final VolatileLongCell head = new VolatileLongCell(0);

    protected final LongCell tailCache = new LongCell();
    protected final LongCell headCache = new LongCell();

    @SuppressWarnings("unchecked")
    ThompsonQueue5Fields(int capacity) {
        if (Pow2.isPowerOf2(capacity)) {
            this.capacity = capacity;
        } else {
            this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        }
        mask = this.capacity - 1;
        buffer = (E[]) new Object[(this.capacity << SPARSE_SHIFT) + BUFFER_PAD * 2];
    }
}

public final class ThompsonQueue5<E> extends ThompsonQueue5Fields<E> implements
        Queue<E> {
    protected long p00, p01, p02, p03, p04, p05, p06, p07;
    private static final long ARRAY_BASE;
    private static final int ELEMENT_SHIFT;
    static {
        final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);

        if (4 == scale) {
            ELEMENT_SHIFT = 2 + SPARSE_SHIFT;
        } else if (8 == scale) {
            ELEMENT_SHIFT = 3 + SPARSE_SHIFT;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }
        ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class)
                + (BUFFER_PAD << (ELEMENT_SHIFT - SPARSE_SHIFT));
    }

    public ThompsonQueue5(final int capacity) {
        super(capacity);
    }

    private long offset(long index) {
        return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
    }

    @Override
    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        final long currTail = tail.get();
        final long wrapPoint = currTail - capacity + 32;
        if (headCache.get() <= wrapPoint) {
            final long currHead = head.get();
            headCache.set(currHead);
            if (currHead <= wrapPoint) {
                return false;
            }
        }
        UnsafeAccess.UNSAFE.putObject(buffer, offset(currTail), e);
        tail.lazySet(currTail + 1);

        return true;
    }

    @Override
    public E poll() {
        final long currHead = head.get();
        if (currHead >= tailCache.get()) {
            final long currTail = tail.get();
            tailCache.set(currTail);
            if (currHead >= currTail) {
                return null;
            }
        }

        final long offset = offset(currHead);
        final E[] lb = buffer;
        @SuppressWarnings("unchecked")
        final E e = (E) UnsafeAccess.UNSAFE.getObject(lb, offset);
        UnsafeAccess.UNSAFE.putObject(lb, offset, null);

        head.lazySet(currHead + 1);;

        return e;
    }

    @Override
    public E peek() {
        long currentHead = head.get();
        return getElement(currentHead);
    }

    @SuppressWarnings("unchecked")
    private E getElement(long index) {
        final long offset = offset(index);
        return (E) UnsafeAccess.UNSAFE.getObject(buffer, offset);
    }

    public int size() {
        return (int) (tail.get() - head.get());
    }

    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }
}
