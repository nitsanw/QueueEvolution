package psy.lob.saw.queues.common;

import java.util.AbstractQueue;

public abstract class CircularArrayQueue1<E> extends AbstractQueue<E> {
	private final E[] buffer;

	@SuppressWarnings("unchecked")
	public CircularArrayQueue1(int capacity) {
		buffer = (E[]) new Object[capacity];
	}

	protected final void spElement(int offset, final E e) {
		buffer[offset] = e;
	}

	protected final E lpElement(final int offset) {
		return buffer[offset];
	}

	protected final int calcOffset(final long index) {
		return (int) (index % buffer.length);
	}

	protected final int capacity() {
		return buffer.length;
	}
}