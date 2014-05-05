package psy.lob.saw.queues.common;

import java.util.AbstractQueue;

import psy.lob.saw.util.Pow2;

public abstract class CircularArrayQueue2<E> extends AbstractQueue<E> {
	private final int capacity;
	private final int mask;
	private final E[] buffer;

	@SuppressWarnings("unchecked")
	public CircularArrayQueue2(int capacity) {
		this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		mask = capacity() - 1;
		buffer = (E[]) new Object[capacity];
	}

	protected final void SP_element(int offset, final E e) {
		buffer[offset] = e;
	}

	protected final E LP_element(final int offset) {
		return buffer[offset];
	}

	protected final int calcOffset(final long index) {
		return ((int) index) & mask;
	}

	protected final int capacity() {
		return capacity;
	}

}