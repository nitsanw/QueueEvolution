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
import java.util.concurrent.atomic.AtomicLong;

import psy.lob.saw.util.Pow2;

/**
 * <ul>
 * <li>Lock free, observing single writer principal.
 * <li>Replacing the long fields with AtomicLong and using lazySet instead of
 * volatile assignment.
 * <li>Using the power of 2 mask, forcing the capacity to next power of 2.
 * <li>Adding head and tail cache fields. Avoiding redundant volatile reads.
 * <li>Padding head/tail AtomicLong fields. Avoiding false sharing.
 * <li>Padding head/tail cache fields. Avoiding false sharing.
 * </ul>
 */
public final class ThompsonQueue2<E> extends AbstractQueue<E> implements Queue<E> {
	private final int capacity;
	private final int mask;
	private final E[] buffer;

	private final AtomicLong tail = new PaddedAtomicLong(0);
	private final AtomicLong head = new PaddedAtomicLong(0);

	private final PaddedLong tailCache = new PaddedLong();
	private final PaddedLong headCache = new PaddedLong();

	@SuppressWarnings("unchecked")
	public ThompsonQueue2(final int capacity) {
		this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		mask = this.capacity - 1;
		buffer = (E[]) new Object[this.capacity];
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

		buffer[(int) currentTail & mask] = e;
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

		final int index = (int) currentHead & mask;
		final E e = buffer[index];
		buffer[index] = null;
		head.lazySet(currentHead + 1);

		return e;
	}

	public E peek() {
		return buffer[(int) head.get() & mask];
	}

	public int size() {
		return (int) (tail.get() - head.get());
	}

	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
}