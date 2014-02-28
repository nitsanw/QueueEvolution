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
package psy.lob.saw.queues.spsc.lamport;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/**
 * <ul>
 * <li>Lock free, observing single writer principal.
 * </ul>
 */
public final class LamportQueue1<E> extends AbstractQueue<E> implements Queue<E> {
	private final E[] buffer;

	private volatile long tail = 0;
	private volatile long head = 0;

	@SuppressWarnings("unchecked")
	public LamportQueue1(final int capacity) {
		buffer = (E[]) new Object[capacity];
	}

	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentTail = tail;
		final long wrapPoint = currentTail - buffer.length;
		if (head <= wrapPoint) {
			return false;
		}

		buffer[(int) (currentTail % buffer.length)] = e;
		tail = currentTail + 1;

		return true;
	}

	@Override
	public E poll() {
		final long currentHead = head;
		if (currentHead >= tail) {
			return null;
		}

		final int index = (int) (currentHead % buffer.length);
		final E e = buffer[index];
		buffer[index] = null;
		head = currentHead + 1;

		return e;
	}

	@Override
	public E peek() {
		return buffer[(int) (head % buffer.length)];
	}

	@Override
	public int size() {
		return (int) (tail - head);
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
}
