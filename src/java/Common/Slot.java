package Common;

public class Slot {
    private int recordLength; //size
    private int recordOffset; //location in page

    public Slot(int recordLength, int recordOffset) {
        this.recordLength = recordLength;
        this.recordOffset = recordOffset;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public int getRecordOffset() {
        return recordOffset;
    }


}
