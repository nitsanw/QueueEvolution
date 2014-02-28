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
package psy.lob.saw.queues.yak;

import java.util.AbstractQueue;
import java.util.Iterator;

import psy.lob.saw.util.Pow2;
import psy.lob.saw.util.UnsafeAccess;

/**
 * <ul>
 * <li>Inlined counters
 * <li>Counters are padded
 * </ul>
 */
abstract class YakQueue1ColdFields<E> extends AbstractQueue<E> {
	protected final int capacity;
	protected final int mask;
	protected final E[] buffer;

	@SuppressWarnings("unchecked")
	public YakQueue1ColdFields(int capacity) {
		if (Pow2.isPowerOf2(capacity)) {
			this.capacity = capacity;
		} else {
			this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		}
		mask = this.capacity - 1;
		buffer = (E[]) new Object[this.capacity];
	}
}

abstract class YakQueue1L1Pad<E> extends YakQueue1ColdFields<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue1L1Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1TailField<E> extends YakQueue1L1Pad<E> {
	protected volatile long tail;

	public YakQueue1TailField(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1L2Pad<E> extends YakQueue1TailField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue1L2Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1HeadCache<E> extends YakQueue1L2Pad<E> {
	protected long headCache;

	public YakQueue1HeadCache(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1L3Pad<E> extends YakQueue1HeadCache<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue1L3Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1HeadField<E> extends YakQueue1L3Pad<E> {
	protected volatile long head;

	public YakQueue1HeadField(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1L4Pad<E> extends YakQueue1HeadField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue1L4Pad(int capacity) {
		super(capacity);
	}
}

abstract class YakQueue1TailCache<E> extends YakQueue1L4Pad<E> {
	protected long tailCache;

	public YakQueue1TailCache(int capacity) {
		super(capacity);
	}

}

abstract class YakQueue1L5Pad<E> extends YakQueue1TailCache<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;

	public YakQueue1L5Pad(int capacity) {
		super(capacity);
	}
}

public final class YakQueue1<E> extends YakQueue1L5Pad<E> {
	private final static long TAIL_OFFSET;
	private final static long HEAD_OFFSET;
	static {
		try {
			TAIL_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(YakQueue1TailField.class.getDeclaredField("tail"));
			HEAD_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(YakQueue1HeadField.class.getDeclaredField("head"));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	public YakQueue1(final int capacity) {
		super(capacity);
	}

	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentTail = getTail();
		final long wrapPoint = currentTail - capacity;
		if (headCache <= wrapPoint) {
			headCache = getHead();
			if (headCache <= wrapPoint) {
				return false;
			}
		}
		buffer[index(currentTail)] = e;
		tailLazySet(currentTail + 1);

		return true;
	}

	@Override
	public E poll() {
		final long currentHead = getHead();
		if (currentHead >= tailCache) {
			tailCache = getTail();
			if (currentHead >= tailCache) {
				return null;
			}
		}

		final int index = index(currentHead);
		final E e = buffer[index];
		buffer[index] = null;
		headLazySet(currentHead + 1);

		return e;
	}

	@Override
	public E peek() {
		return buffer[index(getHead())];
	}

	@Override
	public int size() {
		return (int) (getTail() - getHead());
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	private void headLazySet(long v) {
		UnsafeAccess.UNSAFE.putOrderedLong(this, HEAD_OFFSET, v);
	}

	private long getHead() {
		return head;
	}

	private void tailLazySet(long v) {
		UnsafeAccess.UNSAFE.putOrderedLong(this, TAIL_OFFSET, v);
	}

	private long getTail() {
		return tail;
	}

	private int index(final long cursor) {
		return ((int) cursor & mask);
	}
}