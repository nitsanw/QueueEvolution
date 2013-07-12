package psy.lob.saw.queues.spsc.fc;

import psy.lob.saw.util.UnsafeAccess;

abstract class VolatileLongCellP0{
    public long p0,p1,p2,p3,p4,p5,p6;
}
abstract class VolatileLongCellValue extends VolatileLongCellP0 {
    protected volatile long value;
}
public class VolatileLongCell extends VolatileLongCellValue {
    private final static long VALUE_OFFSET;
    static {
        try {
            VALUE_OFFSET = UnsafeAccess.UNSAFE.objectFieldOffset(VolatileLongCellValue.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    public long p10,p11,p12,p13,p14,p15,p16;
    public VolatileLongCell(){
        this(0L);
    }
    public VolatileLongCell(long v){
        lazySet(v);
    }
    public void lazySet(long v) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, VALUE_OFFSET, v);
    }
    public void set(long v){
        this.value = v;
    }
    public long get(){
        return this.value;
    }
}
