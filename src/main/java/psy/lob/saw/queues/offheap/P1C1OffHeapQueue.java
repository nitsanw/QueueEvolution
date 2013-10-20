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
package psy.lob.saw.queues.offheap;

import static psy.lob.saw.util.UnsafeDirectByteBuffer.CACHE_LINE_SIZE;
import static psy.lob.saw.util.UnsafeDirectByteBuffer.alignedSlice;
import static psy.lob.saw.util.UnsafeDirectByteBuffer.allocateAlignedByteBuffer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import psy.lob.saw.util.UnsafeAccess;
import psy.lob.saw.util.UnsafeDirectByteBuffer;

public final class P1C1OffHeapQueue implements Queue<Integer> {
	public final static byte PRODUCER = 1;
	public final static byte CONSUMER = 2;
	public static final int INT_ELEMENT_SCALE = 2 + Integer.getInteger("sparse.shift", 3);
	// 24b,8b,8b,24b | pad | 24b,8b,8b,24b | pad
	private final ByteBuffer buffy;
	private final long headAddress;
	private final long tailCacheAddress;
	private final long tailAddress;
	private final long headCacheAddress;

	private final int capacity;
	private final int mask;
	private final long arrayBase;

	public P1C1OffHeapQueue(final int capacity) {
		this(allocateAlignedByteBuffer(
		        getRequiredBufferSize(capacity),
		        CACHE_LINE_SIZE),
		        findNextPositivePowerOfTwo(capacity),(byte)(PRODUCER | CONSUMER));
	}
	public static int getRequiredBufferSize(final int capacity) {
	    return 4 * CACHE_LINE_SIZE + (findNextPositivePowerOfTwo(capacity) << INT_ELEMENT_SCALE);
    }
	/**
	 * This is to be used for an IPC queue with the direct buffer used being a memory
	 * mapped file.
	 * 
	 * @param buff
	 * @param capacity
	 * @param viewMask 
	 */
	public P1C1OffHeapQueue(final ByteBuffer buff, 
			final int capacity, byte viewMask) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		buffy = alignedSlice(4 * CACHE_LINE_SIZE + (this.capacity << INT_ELEMENT_SCALE), 
									CACHE_LINE_SIZE, buff);

		long alignedAddress = UnsafeDirectByteBuffer.getAddress(buffy);

		headAddress = alignedAddress;
		tailCacheAddress = headAddress + 8;
		tailAddress = headAddress + 2 * CACHE_LINE_SIZE;
		headCacheAddress = tailAddress + 8;
		arrayBase = alignedAddress + 4 * CACHE_LINE_SIZE;
		// producer owns tail and headCache
		if((viewMask & PRODUCER) == PRODUCER){
    		setHeadCache(0);
    		setTail(0);
		}
		// consumer owns head and tailCache 
		if((viewMask & CONSUMER) == CONSUMER){
	    	setTailCache(0);
			setHead(0);
		}
		mask = this.capacity - 1;
	}
	public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}

	public boolean add(final Integer e) {
		if (offer(e)) {
			return true;
		}

		throw new IllegalStateException("Queue is full");
	}

	public boolean offer(final Integer e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentTail = getTailPlain();
		final long wrapPoint = currentTail - capacity;
		if (getHeadCache() <= wrapPoint) {
			setHeadCache(getHead());
			if (getHeadCache() <= wrapPoint) {
				return false;
			}
		}

		long offset = arrayBase
		        + ((currentTail & mask) << INT_ELEMENT_SCALE);
		UnsafeAccess.UNSAFE.putInt(offset, e.intValue());

		setTail(currentTail + 1);

		return true;
	}

	public Integer poll() {
		final long currentHead = getHeadPlain();
		if (currentHead >= getTailCache()) {
			setTailCache(getTail());
			if (currentHead >= getTailCache()) {
				return null;
			}
		}

		final long offset = arrayBase + ((currentHead & mask) << INT_ELEMENT_SCALE);
		final int e = UnsafeAccess.UNSAFE.getInt(offset);
		setHead(currentHead + 1);

		return e;
	}
	public int pollInt() {
		final long currentHead = getHeadPlain();
		if (currentHead >= getTailCache()) {
			setTailCache(getTail());
			if (currentHead >= getTailCache()) {
				return -1;
			}
		}

		final long offset = arrayBase + ((currentHead & mask) << INT_ELEMENT_SCALE);
		final int e = UnsafeAccess.UNSAFE.getInt(offset);
		setHead(currentHead + 1);

		return e;
    }
	public Integer remove() {
		final Integer e = poll();
		if (null == e) {
			throw new NoSuchElementException("Queue is empty");
		}

		return e;
	}

	public Integer element() {
		throw new UnsupportedOperationException();
	}

	public Integer peek() {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return (int) (getTail() - getHead());
	}

	public boolean isEmpty() {
		return getTail() == getHead();
	}

	public boolean contains(final Object o) {
		return false;
	}

	public Iterator<Integer> iterator() {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	public <T> T[] toArray(final T[] a) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}

		return true;
	}

	public boolean addAll(final Collection<? extends Integer> c) {
		for (final Integer e : c) {
			add(e);
		}

		return true;
	}

	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		Object value;
		do {
			value = poll();
		} while (null != value);
	}

	private long getHeadPlain() {
		return UnsafeAccess.UNSAFE.getLong(null, headAddress);
	}
	private long getHead() {
		return UnsafeAccess.UNSAFE.getLongVolatile(null, headAddress);
	}

	private void setHead(final long value) {
		UnsafeAccess.UNSAFE.putOrderedLong(null, headAddress, value);
	}

	private long getTailPlain() {
		return UnsafeAccess.UNSAFE.getLong(null, tailAddress);
	}
	private long getTail() {
		return UnsafeAccess.UNSAFE.getLongVolatile(null, tailAddress);
	}

	private void setTail(final long value) {
		UnsafeAccess.UNSAFE.putOrderedLong(null, tailAddress, value);
	}

	private long getHeadCache() {
		return UnsafeAccess.UNSAFE.getLong(null, headCacheAddress);
	}

	private void setHeadCache(final long value) {
		UnsafeAccess.UNSAFE.putLong(headCacheAddress, value);
	}

	private long getTailCache() {
		return UnsafeAccess.UNSAFE.getLong(null, tailCacheAddress);
	}

	private void setTailCache(final long value) {
		UnsafeAccess.UNSAFE.putLong(tailCacheAddress, value);
	}

}
