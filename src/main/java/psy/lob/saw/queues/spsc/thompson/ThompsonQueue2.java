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

import psy.lob.saw.queues.common.CircularArrayQueue2;
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
public final class ThompsonQueue2<E> extends CircularArrayQueue2<E>{
	private final AtomicLong producerIndex = new PaddedAtomicLong();
	private final AtomicLong consumerIndex = new PaddedAtomicLong();
	private final PaddedLong producerIndexCache = new PaddedLong();
	private final PaddedLong consumerIndexCache = new PaddedLong();
	public ThompsonQueue2(final int capacity) {
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
	
	private long lpConsumerIndexCache() {
	    return consumerIndexCache.value;
    }
	private void spConsumerIndexCache(long index) {
		consumerIndexCache.value = index;
	}
	private long lpProducerIndexCache() {
	    return producerIndexCache.value;
    }
	private void spProducerIndexCache(long index) {
		producerIndexCache.value = index;
	}
	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentProducerIndex = lvProducerIndex();
		final long wrapPoint = currentProducerIndex - capacity();
		if (lpConsumerIndexCache() <= wrapPoint) {
			spConsumerIndexCache(lvConsumerIndex());
			if (lpConsumerIndexCache() <= wrapPoint) {
				return false;
			}
		}

		final int offset = calcOffset(currentProducerIndex);
		SP_element(offset, e);
		soProducerIndex(currentProducerIndex + 1);
		return true;
	}


	@Override
	public E poll() {
		final long currentConsumerIndex = lvConsumerIndex();
		if (currentConsumerIndex >= lpProducerIndexCache()) {
			spProducerIndexCache(lvProducerIndex());
			if (currentConsumerIndex >= lpProducerIndexCache()) {
				return null;
			}
		}

		final int offset = calcOffset(currentConsumerIndex);
		final E e = LP_element(offset);
		SP_element(offset, null);
		soConsumerIndex(currentConsumerIndex + 1);
		return e;
	}

	@Override
	public E peek() {
		final int offset = calcOffset(lvConsumerIndex());
		return LP_element(offset);
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