package Common;

public class Slot {
    private int recordLength; //size
    // private int recordOffset; //location in page
    private int recordIdx; //location of record in arraylist

    public Slot(int recordLength, int recordIdx) {
        this.recordLength = recordLength;
        // this.recordOffset = recordOffset;
        this.recordIdx = recordIdx;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public void setRecordLength(int newRecordLength) {
        this.recordLength = newRecordLength;   
    }

    public int getRecordIdx() {
        return recordIdx;
    }

    public void setRecordIdx(int newRecordIdx) {
        this.recordIdx = newRecordIdx;
    }

    // public int getRecordOffset() {
    //     return recordOffset;
    // }


}
