package psy.lob.saw.queues.common;

import java.util.AbstractQueue;
abstract class CircularArrayQueue3PrePad<E> extends AbstractQueue<E> {
    protected long p00, p01, p02, p03, p04, p05, p06, p07;
	protected long p10, p11, p12, p13, p14, p15, p16, p17;
}

public abstract class CircularArrayQueue3<E> extends CircularArrayQueue3PrePad<E> {
	private final int capacity;
	private final int mask;
	private final E[] buffer;

	@SuppressWarnings("unchecked")
	public CircularArrayQueue3(int capacity) {
		this.capacity = Pow2.findNextPositivePowerOfTwo(capacity);
		mask = capacity() - 1;
		buffer = (E[]) new Object[capacity];
	}

	protected final void spElement(int offset, final E e) {
		buffer[offset] = e;
	}

	protected final E lpElement(final int offset) {
		return buffer[offset];
	}

	protected final int calcOffset(final long index) {
		return ((int) index) & mask;
	}

	protected final int capacity() {
		return capacity;
	}

}