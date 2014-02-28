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
package psy.lob.saw.queues.spsc.ff;

import static psy.lob.saw.util.UnsafeAccess.UNSAFE;

import java.util.AbstractQueue;
import java.util.Iterator;

import psy.lob.saw.util.Pow2;

/**
 * <ul>
 * <li>FFBuffer structure
 * <li>Counters are paDed
 * <li>Data is pre/post paDed
 * <li>Class is pre/post-paDed
 * <li>Use Unsafe to read/write to array
 * <li>putOrdered into array as Write Memory Barrier
 * <li>getVolatile from array as Read Memory Barrier
 * <li>Allow use of sparse data
 * <li>Double padding
 * </ul>
 */
abstract class FFBuffer11L0Pad<E> extends AbstractQueue<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;
	protected long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class FFBuffer11ColdFields<E> extends FFBuffer11L0Pad<E> {
	protected static final int BUFFER_PAD = 32;
	protected static final long ARRAY_BASE;
	private static final int ELEMENT_SHIFT;
	static {
		final int scale = UNSAFE.arrayIndexScale(Object[].class);
		if (4 == scale) {
			ELEMENT_SHIFT = 2;
		} else if (8 == scale) {
			ELEMENT_SHIFT = 3;
		} else {
			throw new IllegalStateException("Unknown pointer size");
		}
		// Including the buffer pad in the array base offset
		ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << ELEMENT_SHIFT);
	}
	protected final int capacity;
	protected final int elementShift;
	protected final long mask;
	protected final E[] buffer;

	@SuppressWarnings("unchecked")
	public FFBuffer11ColdFields(int capacity, int sparseShift) {
		if (Pow2.isPowerOf2(capacity)) {
			this.capacity = capacity;
		} else {
			this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		}
		mask = this.capacity - 1;
		// pad data on either end with some empty slots.
		buffer = (E[]) new Object[(this.capacity << sparseShift) + BUFFER_PAD * 2];
		elementShift = ELEMENT_SHIFT + sparseShift;
	}
}

abstract class FFBuffer11L1Pad<E> extends FFBuffer11ColdFields<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;
	protected long p10, p11, p12, p13, p14, p15, p16, p17;

	public FFBuffer11L1Pad(int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}
}

abstract class FFBuffer11TailField<E> extends FFBuffer11L1Pad<E> {
	protected long tail;

	public FFBuffer11TailField(int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}
}

abstract class FFBuffer11L2Pad<E> extends FFBuffer11TailField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;
	protected long p10, p11, p12, p13, p14, p15, p16, p17;

	public FFBuffer11L2Pad(int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}
}

abstract class FFBuffer11HeadField<E> extends FFBuffer11L2Pad<E> {
	protected long head;

	public FFBuffer11HeadField(int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}
}

abstract class FFBuffer11L3Pad<E> extends FFBuffer11HeadField<E> {
	protected long p00, p01, p02, p03, p04, p05, p06, p07;
	protected long p10, p11, p12, p13, p14, p15, p16, p17;

	public FFBuffer11L3Pad(int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}
}

public final class FFBuffer11<E> extends FFBuffer11L3Pad<E> {
	private static final long TAIL_OFFSET;
	private static final long HEAD_OFFSET;
	static {
		try {
			TAIL_OFFSET = UNSAFE.objectFieldOffset(FFBuffer11TailField.class.getDeclaredField("tail"));
			HEAD_OFFSET = UNSAFE.objectFieldOffset(FFBuffer11HeadField.class.getDeclaredField("head"));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	public FFBuffer11(final int capacity, int sparseShift) {
		super(capacity, sparseShift);
	}

	@Override
	public boolean offer(final E e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long offset = offset(tail);
		if (null != UNSAFE.getObjectVolatile(buffer, offset)) { // LoadLoad
			return false;
		}
		UNSAFE.putOrderedObject(buffer, offset, e); // StoreStore
		tail++;
		return true;
	}

	@Override
	public E poll() {
		final long offset = offset(head);
		@SuppressWarnings("unchecked")
		final E e = (E) UNSAFE.getObjectVolatile(buffer, offset); // LoadLoad
		if (null == e) {
			return null;
		}
		UNSAFE.putOrderedObject(buffer, offset, null); // StoreStore
		head++;
		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		return (E) UNSAFE.getObjectVolatile(buffer, offset(head));
	}

	private long offset(long index) {
		return ARRAY_BASE + ((index & mask) << elementShift);
	}

	@Override
	public int size() {
		return (int) (getTail() - getHead());
	}

	private long getHead() {
		return UNSAFE.getLongVolatile(this, HEAD_OFFSET);
	}

	private long getTail() {
		return UNSAFE.getLongVolatile(this, TAIL_OFFSET);
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
}
