package Common;

class RecordPointer {
    // used to reference records (indexes, buffer manager, query executor, catalog)

    private int pageNumber;
    private int slotNumber; //in slot array

    public RecordPointer(int pageNumber, int slotNumber) {
        this.pageNumber = pageNumber;
        this.slotNumber = slotNumber;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int newPageNumber) {
        this.pageNumber = newPageNumber;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int newSlotNumber) {
        this.slotNumber = newSlotNumber;
    }

}