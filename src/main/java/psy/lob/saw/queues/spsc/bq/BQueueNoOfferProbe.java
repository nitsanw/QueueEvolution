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
package psy.lob.saw.queues.spsc.bq;

import static psy.lob.saw.util.UnsafeAccess.UNSAFE;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class BQueueNoOfferProbe<E> extends L3Pad<E> implements Queue<E> {
    public BQueueNoOfferProbe(final int capacity) {
        super(capacity);
    }

    public boolean add(final E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue is full");
    }

    private long offset(long index) {
        return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
    }

    public boolean offer(final E e) {
        if (null == e) {
            throw new NullPointerException("Null is not a valid element");
        }

        final long offset = offset(tail);
        if (null != UNSAFE.getObjectVolatile(buffer, offset)) {
            return false;
        }
        UNSAFE.putOrderedObject(buffer, offset, e);
        tail++;

        return true;
    }
    
    public E poll() {
        if (head >= batchHead) {
            if (!backtrackPoll()) {
                return null;
            }
        }

        final long offset = offset(head);
        @SuppressWarnings("unchecked")
        final E e = (E) UNSAFE.getObject(buffer, offset);
        UNSAFE.putOrderedObject(buffer, offset, null);
        head++;
        return e;
    }

    boolean backtrackPoll() {
        if (batchHistory < POLL_MAX_BATCH) {
            batchHistory = Math.min(POLL_MAX_BATCH, batchHistory << 1);
        }
        batchSize = batchHistory;
        batchHead = head + batchSize - 1;
        while (UNSAFE.getObjectVolatile(buffer, offset(batchHead)) == null) {
            spinWait();
            if (batchSize > 1) {
                batchSize = batchSize >> 1;
                batchHead = head + batchSize - 1;
            } else {
                batchHead = head;
                return false;
            }
        }
        batchHistory = batchSize;
        return true;
    }

    private void spinWait() {
        if(TICKS == 0){
            return;
        }
	    final long deadline = System.nanoTime() + TICKS;
	    while(deadline >= System.nanoTime());
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
    	throw new UnsupportedOperationException();
    }

    public int size() {
        return (int) (tail - head);
    }

    public boolean isEmpty() {
        return tail == head;
    }

    public boolean contains(final Object o) {
    	throw new UnsupportedOperationException();
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
    	throw new UnsupportedOperationException();
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
