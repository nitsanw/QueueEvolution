package psy.lob.saw.queues.spsc.ff;

/*
 *  License: GPL
 *  Origin: FastForwd (C++ template library)
 */

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

import psy.lob.saw.util.UnsafeAccess;

@SuppressWarnings("unused")
final public class FFBufferNoBarriers<T> extends AbstractQueue<T> implements Queue<T> {
    private int _pad000, _pad001, _pad002, _pad003, _pad004, _pad005, _pad006, _pad007;
    private int _pad008, _pad009, _pad00a, _pad00b, _pad00c, _pad00d, _pad00e, _pad00f;
    private int pread, _pread0;
    private int _pad100, _pad101, _pad102, _pad103, _pad104, _pad105, _pad106, _pad107;
    private int _pad108, _pad109, _pad10a, _pad10b, _pad10c, _pad10d, _pad10e, _pad10f;
    private int pwrite, _pwrite0;
    private int _pad200, _pad201, _pad202, _pad203, _pad204, _pad205, _pad206, _pad207;
    private int _pad208, _pad209, _pad20a, _pad20b, _pad20c, _pad20d, _pad20e, _pad20f;
    private final int size;
    private int size0;
    private final int POW;
    private int POW0;
    private final int mask;
    private int mask0;
    private T _pad300, _pad301, _pad302, _pad303, _pad304, _pad305, _pad306, _pad307;
    private T _pad308, _pad309, _pad30a, _pad30b, _pad30c, _pad30d, _pad30e, _pad30f;
    private final T data[];
    private T _pad400, _pad401, _pad402, _pad403, _pad404, _pad405, _pad406, _pad407;
    private T _pad408, _pad409, _pad40a, _pad40b, _pad40c, _pad40d, _pad40e, _pad40f;

    public FFBufferNoBarriers(int sizeByPowerOfTwo, int pow) {
        this.size = 1 << sizeByPowerOfTwo;
        this.mask = size - 1;
        this.POW = pow;
        this.data = (T[]) new Object[size << POW];
    }

    private int id(int n) {
        return (n & mask) << POW;
    }

    public boolean offer(T obj) {
        if (null == obj)
            throw new IllegalArgumentException("elem is null");
        int id = id(pwrite);
        if (null != data[id])
            return false;
        data[id] = obj;
        pwrite++;
        return true;
    }

    public T poll() {
        int id = id(pread);
        T rc = data[id];
        if (null == rc)
            return null;
        data[id] = null; 
        pread++;
        return rc;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    public T peek() {
        throw new UnsupportedOperationException();
    }
}