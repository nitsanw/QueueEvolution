package psy.lob.saw.queues.common;

import static psy.lob.saw.util.UnsafeAccess.UNSAFE;

import java.util.AbstractQueue;

import psy.lob.saw.util.Pow2;
import psy.lob.saw.util.UnsafeAccess;

public abstract class CircularArrayQueue3<E> extends AbstractQueue<E> {
	private static final long ARRAY_BASE;
	private static final int ELEMENT_SHIFT;
	static {
		final int scale = UnsafeAccess.UNSAFE.arrayIndexScale(Object[].class);

		if (4 == scale) {
			ELEMENT_SHIFT = 2;
		} else if (8 == scale) {
			ELEMENT_SHIFT = 3;
		} else {
			throw new IllegalStateException("Unknown pointer size");
		}
		ARRAY_BASE = UnsafeAccess.UNSAFE.arrayBaseOffset(Object[].class);
	}
	private final int capacity;
	private final long mask;
	private final E[] buffer;

	@SuppressWarnings("unchecked")
	public CircularArrayQueue3(int capacity) {
		this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		mask = capacity() - 1;
		buffer = (E[]) new Object[capacity];
	}

	protected final void SP_element(final long offset, final E e) {
		UNSAFE.putObject(buffer, offset, e);
	}

	@SuppressWarnings("unchecked")
	protected final E LP_element(final long offset) {
		return (E) UNSAFE.getObject(buffer, offset);
	}

	protected final long calcOffset(final long index) {
		return ARRAY_BASE + ((index & mask) << ELEMENT_SHIFT);
	}

	protected final int capacity() {
		return capacity;
	}
}