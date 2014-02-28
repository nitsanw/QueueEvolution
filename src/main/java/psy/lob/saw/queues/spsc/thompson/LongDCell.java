package psy.lob.saw.queues.spsc.thompson;

abstract class LongDCellP0 {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class LongDCellValue extends LongDCellP0 {
    protected long value;
}

public final class LongDCell extends LongDCellValue {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;

    public void set(long v) {
        this.value = v;
    }

    public long get() {
        return this.value;
    }
}
