package psy.lob.saw.queues.spsc.fc;


abstract class LongCellP0{long p0,p1,p2,p3,p4,p5,p6;}
abstract class LongCellValue extends LongCellP0 {
    protected long value;
}
public final class LongCell extends LongCellValue {
    long p10,p11,p12,p13,p14,p15,p16;
    public void set(long v){ this.value = v; }
    public long get(){ return this.value; }
}
