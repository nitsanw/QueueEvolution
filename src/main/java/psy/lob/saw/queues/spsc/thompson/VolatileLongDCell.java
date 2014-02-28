package psy.lob.saw.queues.spsc.thompson;

import static psy.lob.saw.util.UnsafeAccess.UNSAFE;

abstract class VolatileLongDCellP0 {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class VolatileLongDCellValue extends VolatileLongDCellP0 {
    protected volatile long value;
}

public final class VolatileLongDCell extends VolatileLongDCellValue {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;
    private final static long VALUE_OFFSET;
    static {
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(VolatileLongDCellValue.class
                    .getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public VolatileLongDCell() {
        this(0L);
    }

    public VolatileLongDCell(long v) {
        lazySet(v);
    }

    public void lazySet(long v) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, v);
    }

    public void set(long v) {
        this.value = v;
    }

    public long get() {
        return this.value;
    }
}
