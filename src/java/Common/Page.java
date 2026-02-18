package Common;

// import java.lang.Record;
import java.util.ArrayList;

public class Page {
    private int pageSize; //given by user
    private int numOfRecords;
    // private int endOfFreeSpace; 
    // private int recordIdx; //where next record goes
    private ArrayList<Slot> slots; //<Size of record, Location>
    private ArrayList<Record> records;
    // private byte[] pageData;
    private int pageId;

    private int usedSpace; 

    private static final int HEADER_SIZE = Integer.BYTES  * 2; //numOfRecords (int = 4 bytes) + endOfFreeSpace (int = 4 bytes) 
    private static final int SLOT_ENTRY_SIZE = Integer.BYTES * 2; //offset size (int = 4 bytes) + length size (int = 4 bytes) 

    //Create a new empty page
    public Page(int pageSize) {
        this.pageSize = pageSize;
        this.numOfRecords = 0;
        // this.endOfFreeSpace = pageSize; // no records at first so it would start at end of pageSize
        // this.recordIdx = 0;
        this.slots = new ArrayList<Slot>();
        this.records = new ArrayList<Record>();
        this.usedSpace = HEADER_SIZE; //cuz all pages have headers
    }

    //reconstruct a page from deserialized data - used by storage manager
    public Page(int pageSize, int numOfRecords, ArrayList<Slot> readSlots,
                            ArrayList<Record> readRecords, int pageId, int usedSpace) {
        this.pageSize = pageSize;
        this.numOfRecords = numOfRecords;
        // this.endOfFreeSpace = endOfFreeSpace; 
        // this.recordIdx = recordIdx;
        this.slots = readSlots;
        this.pageId = pageId;
        this.usedSpace = usedSpace;
        this.records = readRecords;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public Record retrieveRecord(int slotNumber) {
        //validate slot number - in range? slot existing?
        int slotSize = slots.size();
        if ((slotNumber >= slotSize) || (slotNumber < 0)) { //slot num in range
            //throw error gracefully
        } 
        Slot slotInfo = slots.get(slotNumber); //get the slot at slot number
        int recordIdx = slotInfo.getRecordIdx(); //get current record index from slot
        if (recordIdx == -1) {  //dead slot?
            //throw error gracefully
        }
        Record record = records.get(recordIdx);
        if (record == null) { //record exist?
            //throw error gracefully
        }
        return record;
    }

    public int insertRecord(Record newRecord) {
        int recordLength = newRecord.getRecordLength();
        if (checkEnoughFreeSpace(recordLength)) {
            records.add(newRecord);
            int recordIdx = records.size()-1;
            Slot newSlot = new Slot(recordLength, recordIdx);
            slots.add(newSlot);
            numOfRecords++;
            int slotNumber = slots.size() -1;
            usedSpace += SLOT_ENTRY_SIZE + recordLength;
            return slotNumber; 
        } else {
            // create new page? throw error?
            return -1;
        }
        //cases: page full
    }

    public boolean deleteRecord(int slotNumber) {
        //validate slotnumber
        int slotSize = slots.size();
        if ((slotNumber >= slotSize) || (slotNumber < 0)) {
            //throw error gracefully
            return false;
        } 
        Slot currSlot = slots.get(slotNumber);
        int recordIdx = currSlot.getRecordIdx();
        if (recordIdx == -1) {  //dead slot already?
            //throw error gracefully
            return false;
        }
        Record record = records.get(recordIdx);
        if (record == null) {
            //throw error gracefully
        }

        //mark slot as deleted 
        currSlot.setRecordIdx(-1);
        //update used space
        int recordLength = record.getRecordLength();
        usedSpace -= recordLength; //slot space stays as it is 
        //set  the record to null in array list 
        records.set(recordIdx, null);
        numOfRecords--;
        return true;
    }

    // update record
    public boolean updateRecord(int slotNumber, Record newRecord) {
        //validate slot number
        int slotSize = slots.size();
        if ((slotNumber >= slotSize) || (slotNumber < 0)) { 
            //throw error gracefully
            return false;
        } 
        Slot slotInfo = slots.get(slotNumber);
        int recordIdx = slotInfo.getRecordIdx();
        if (recordIdx == -1) {  //dead slot?
            //throw error gracefully
            return false;
        }
        Record currRecord = records.get(recordIdx);
        if (currRecord == null) {
            //throw error gracefully
            return false;
        }

        //compare sizes and replace record
        int newRecordLength = newRecord.getRecordLength();
        int currRecordLength = currRecord.getRecordLength();
        // old size == new size, overwrite old record
        if (currRecordLength == newRecordLength) {
            records.set(recordIdx, newRecord);
        } else if (currRecordLength > newRecordLength) {
            // old size > new size, overwrite record, update slot length
                //! how to deal with hole - leave it or compress it 
                //!gonna leave it
            records.set(recordIdx, newRecord);
            //update slot length 
            slotInfo.setRecordLength(newRecordLength);
        } else if (currRecordLength < newRecordLength) {
            // old size < new size, 
                // ! 1)delete old, insert new, 2)delete and shift to make room, 3)throw error if not enough free space ???
                //!goes to delete old and insert new 
            if (checkEnoughFreeSpace(newRecordLength - currRecordLength)) {
                records.set(recordIdx, null);
                records.add(newRecord);
                slotInfo.setRecordIdx(records.size()-1); //last inserted record
                //update slot length
                slotInfo.setRecordLength(newRecordLength);
            } else {
                //not enough free space
                return false;
            }
        }
        //slot stays the same
        usedSpace -= currRecordLength;
        usedSpace += newRecordLength;
        return true;
    }

    //enough free space to insert?
    //need enough space for record data and new slot entry 
    public boolean checkEnoughFreeSpace(int recordLength) {
        int slotsSize = (slots.size() + 1) * SLOT_ENTRY_SIZE; // +1 for new slot
        int recordsSize = 0;
        for (Record r : records) { //get all record sizes
            if (r != null) {
                recordsSize += r.getRecordLength();
            }
        }
        recordsSize += recordLength; //add new record size
        int totalSize = HEADER_SIZE + slotsSize + recordsSize;
        return totalSize <= pageSize;
    }

    // public int calculateEndOfSlotDirectory() {
    //     int endOfSlotDirectory = HEADER_SIZE + (numOfRecords * SLOT_ENTRY_SIZE); 
    //     return endOfSlotDirectory;
    // }

    // public int getPageId() {
    //     return pageId;
    // }

    // public byte[] getPageData() {
    //     return pageData;
    // }
    
    // for inserting slot for a record
    // public int calculateRecordOffset(int recordLength) {
    //     return endOfFreeSpace - recordLength;
    // }

}