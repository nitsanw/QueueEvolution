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
package psy.lob.saw.queues.lamport;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import psy.lob.saw.queues.common.CircularArrayQueue1;

/**
 * <ul>
 * <li>Lock free, observing single writer principal (except for buffer).
 * <li>Replacing the long fields with AtomicLong and using lazySet instead of
 * volatile assignment.
 * </ul>
 */
public final class LamportQueue2<E> extends CircularArrayQueue1<E> {
	private final AtomicLong producerIndex = new AtomicLong();
	private final AtomicLong consumerIndex = new AtomicLong();

	public LamportQueue2(final int capacity) {
		super(capacity);
	}

	private long lvProducerIndex() {
		return producerIndex.get();
	}

	private void soProducerIndex(long index) {
		producerIndex.lazySet(index);
	}

	private long lvConsumerIndex() {
		return consumerIndex.get();
	}

	private void soConsumerIndex(long index) {
		consumerIndex.lazySet(index);
	}

	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentProducerIndex = lvProducerIndex();// LoadLoad
		final long wrapPoint = currentProducerIndex - capacity();
		if (lvConsumerIndex() <= wrapPoint) { // LoadLoad
			return false;
		}

		final int offset = calcOffset(currentProducerIndex);
		spElement(offset, e);
		soProducerIndex(currentProducerIndex + 1); // StoreStore
		return true;
	}

	@Override
	public E poll() {
		final long currentConsumerIndex = lvConsumerIndex(); // LoadLoad
		if (currentConsumerIndex >= lvProducerIndex()) { // LoadLoad
			return null;
		}

		final int offset = calcOffset(currentConsumerIndex);
		final E e = lpElement(offset);
		spElement(offset, null);
		soConsumerIndex(currentConsumerIndex + 1); // StoreStore
		return e;
	}

	@Override
	public E peek() {
		final int offset = calcOffset(lvConsumerIndex());
		return lpElement(offset);
	}

	@Override
	public int size() {
		return (int) (lvProducerIndex() - lvConsumerIndex());
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
}