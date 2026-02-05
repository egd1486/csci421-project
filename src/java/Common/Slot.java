package Common;

public class Slot {
    private int recordSize; //length
    private int recordLocation; //offset in page

    public Slot(int recordSize, int recordLocation) {
        this.recordSize = recordSize;
        this.recordLocation = recordLocation;
    }


}
