/*
 * Copyright 2012 Real Logic Ltd.
 *
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
package psy.lob.saw.queues.spsc.fc;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import psy.lob.saw.util.Pow2;

/**
 * <ul>
 * <li>Fully padded counters
 * <li>Fully padded data
 * <li>Fully padded class fields
 * 
 * </ul>
 */
abstract class SPSPQueueFloatingCounters4P0 {long p00, p01, p02, p03, p04, p05, p06, p07;}
abstract class SPSPQueueFloatingCounters4Fields<E> extends SPSPQueueFloatingCounters4P0 {
     protected static final int BUFFER_PAD = 16;
     protected final int capacity;
     protected final int mask;
     protected final E[] buffer;
     protected final VolatileLongCell tail = new VolatileLongCell(0);
     protected final VolatileLongCell head = new VolatileLongCell(0);

     protected final LongCell tailCache = new LongCell();
     protected final LongCell headCache = new LongCell();
     @SuppressWarnings("unchecked")
     public SPSPQueueFloatingCounters4Fields(int capacity) {
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
public final class SPSPQueueFloatingCounters4<E> extends SPSPQueueFloatingCounters4Fields<E> implements Queue<E> {
    long p10, p11, p12, p13, p14, p15, p16, p17;

	public SPSPQueueFloatingCounters4(final int capacity) {
		super(capacity);
	}

	public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
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

		final long currentTail = tail.get();
		final long wrapPoint = currentTail - capacity;
		if (headCache.value <= wrapPoint) {
			headCache.value = head.get();
			if (headCache.value <= wrapPoint) {
				return false;
			}
		}

		buffer[BUFFER_PAD + ((int) currentTail & mask)] = e;
		tail.lazySet(currentTail + 1);

		return true;
	}

	public E poll() {
		final long currentHead = head.get();
		if (currentHead >= tailCache.value) {
			tailCache.value = tail.get();
			if (currentHead >= tailCache.value) {
				return null;
			}
		}

		final int index = BUFFER_PAD+((int) currentHead & mask);
		final E e = buffer[index];
		buffer[index] = null;
		head.lazySet(currentHead + 1);

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
		return buffer[BUFFER_PAD + ((int) head.get() & mask)];
	}

	public int size() {
		return (int) (tail.get() - head.get());
	}

	public boolean isEmpty() {
		return tail.get() == head.get();
	}

	public boolean contains(final Object o) {
		if (null == o) {
			return false;
		}

		for (long i = head.get(), limit = tail.get(); i < limit; i++) {
			final E e = buffer[BUFFER_PAD + ((int) i & mask)];
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
