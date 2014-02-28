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
package psy.lob.saw.queues.spsc.thompson;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

import psy.lob.saw.util.Pow2;

/**
 * <ul>
 * <li>Fully padded counters
 * </ul>
 */
public final class ThompsonQueue3<E> extends AbstractQueue<E> implements Queue<E> {
    protected final int capacity;
    protected final int mask;
    protected final E[] buffer;
    protected final VolatileLongCell tail = new VolatileLongCell(0);
    protected final VolatileLongCell head = new VolatileLongCell(0);

    protected final LongCell tailCache = new LongCell();
    protected final LongCell headCache = new LongCell();
    @SuppressWarnings("unchecked")
    public ThompsonQueue3(int capacity) {
        if(Pow2.isPowerOf2(capacity)){
            this.capacity = capacity;
        }
        else{
            this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
        }
        mask = this.capacity - 1;
        buffer = (E[]) new Object[this.capacity];
    }

    @Override
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

		buffer[((int) currentTail & mask)] = e;
		tail.lazySet(currentTail + 1);

		return true;
	}

    @Override
	public E poll() {
		final long currentHead = head.get();
		if (currentHead >= tailCache.value) {
			tailCache.value = tail.get();
			if (currentHead >= tailCache.value) {
				return null;
			}
		}

		final int index = ((int) currentHead & mask);
		final E e = buffer[index];
		buffer[index] = null;
		head.lazySet(currentHead + 1);

		return e;
	}
    @Override
	public E peek() {
		return buffer[((int) head.get() & mask)];
	}
    @Override
	public int size() {
		return (int) (tail.get() - head.get());
	}
    @Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
}
