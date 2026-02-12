package Common;

// import java.lang.Record;
import java.util.ArrayList;

public class Page {
    private int pageSize;
    private int numOfRecords;
    private int endOfFreeSpace;
    private ArrayList<Slot> entries; //<Size of record, Location>
    private byte[] pageData;

    private static final int HEADER_SIZE = 8; //numOfRecords (int = 4 bytes) + endOfFreeSpace (int = 4 bytes) 
    private static final int SLOT_ENTRY_SIZE = 8; //offset size (int = 4 bytes) + length size (int = 4 bytes) 

    public Page(int pageSize) {
        this.pageSize = pageSize;
        this.numOfRecords = 0;
        this.endOfFreeSpace = pageSize; // no records at first so it would start at end of pageSize
        this.entries = new ArrayList<Slot>();
        this.pageData = new byte[pageSize];
    }

    public Page(int pageSize, int numOfRecords, int endOfFreeSpace, ArrayList<Slot> readEntries, byte[] pageBytes ) {
        this.pageSize = pageSize;
        this.numOfRecords = numOfRecords;
        this.endOfFreeSpace = endOfFreeSpace; 
        this.entries = readEntries;
        this.pageData = pageBytes;
    }

    // ! how to represent records in page ?

    public Record retrieveRecord(int slotNumber) {
        // get slot info
        Slot slotInfo = entries.get(slotNumber);
        // get bytes from offset
        int recordOffset = slotInfo.getRecordOffset();
        int recordLength =  slotInfo.getRecordLength();
        byte[] recordBytes = new byte[recordLength];
        System.arraycopy(pageData, recordOffset, recordBytes, 0, recordLength);
        // deserialize bytes to record object 
        Record record = new Record();
        //! how deserialize
        //! does the caller derserialize with schema returns bytes
        //! does page deserailize (slotNum, schema) using scehma to parse bytes and returns record
        
        return new Record();
    }

    public void insertRecord(Record record) {
        int recordLength = record.getRecordLength();
        if (checkEnoughFreeSpace(recordLength)) {
            int recordOffset = calculateRecordOffset(recordLength);

            //write bytes to pageData
            byte[] recordBytes = record.serialize(null);
            System.arraycopy(recordBytes, 0, pageData, recordOffset, recordLength);
            //create new slot with this offset and length 
            Slot newSlot = new Slot(recordLength, recordOffset);
            //add slot to entries
            entries.add(newSlot);
            // update endOfFreeSpace
            endOfFreeSpace = endOfFreeSpace - recordLength;
            //increment numOfRecords
            numOfRecords++;
        } else {
            // create new page
        }
        
        //cases: page full
    }

    public void deleteRecord(int slotNumber) {
        //check if slot number exists

        //check if slot has stuff

        //get record info 
        
        //delete - mark as invalid, set length to -1? have flag? keep list of deleted slots?

        // !  leave it as a hole ? 

        // decrement numOfRecords
    }

    // update record
    public void updateRecord(int slotNumber, Record record) {
        //get record to update
        Slot oldRecordSlot = entries.get(slotNumber);
        int oldOffset = oldRecordSlot.getRecordOffset();
        int oldLength = oldRecordSlot.getRecordLength();
        Record oldRecord = retrieveRecord(slotNumber);
        // get new record data and serialize
        byte[] byteRecord = record.serialize(null);
        int newRecordLength = byteRecord.length;
       
        // different sizes? also calc offset based on case
            // old size == new size, overwrite bytes
        
            // old size > new size, overwrite bytes, update slot length
            //! how to deal with hole - leave it or compress it
        
            // old size < new size, 
            // ! 1)delete old, insert new, 2)reognize to make room, 3)throw error if not enough free space ???
        
        //update the slot - modify slot entry with new offset if changed and new length
        //Slot newSlot = new Slot(newRecordLength, newRecordOffset);
        Slot newSlot = new Slot(newRecordLength, 0);
        entries.set(slotNumber, newSlot);
    }

    //enough free space to insert?
    //need enough space for record data and new slot entry 
    public boolean checkEnoughFreeSpace(int recordLength) {
        int endOfSlotDirectory = calculateEndOfSlotDirectory();
        int freeSpace = endOfFreeSpace - endOfSlotDirectory;

        // space needed = sum of all field sizes from schema in bytes  + slot entry size
        int spaceNeeded = recordLength + SLOT_ENTRY_SIZE ; //! unfinished
        if (freeSpace >= spaceNeeded) {
            return true;
        }
        return false;
    }

    public int calculateEndOfSlotDirectory() {
        int endOfSlotDirectory = HEADER_SIZE + (numOfRecords * SLOT_ENTRY_SIZE); 
        return endOfSlotDirectory;
    }
    
    // for inserting slot for a record
    public int calculateRecordOffset(int recordLength) {
        return endOfFreeSpace - recordLength;
    }

    //page java to binary 
    public byte[] writeToPage() {
        //create byte array
        byte[] binaryPage = new byte[pageSize];
        //write header to bytes
        // convert numOfRecords, endOfFreeSpace, 
            //use ByteBuffer ?
        // write slot directory to bytes
            //loop through entries
            //for each slot 
                //convert offset, length
                //write to appropriate posiiton in binaryPage array
                //pos = header_size + slotindex * slot_entry_size)
        //copy record data (already in pageData as bytes)
        return binaryPage;
    }

    //page binary to java
    public Page readToPage(byte[] pageBytes, int pageSize) {
        //extract header info
            //num of records
            //end of free space
            //use ByteBuffer?
        //extract slot directory
        ArrayList<Slot> readEntries = new ArrayList<Slot>();
        //loop  numOfRecords of times 
            //read offset bytes and convert to int
            //read length bytes and convert to int 
            //Slot readSlot = new Slot(readRecordLength, readRecordOffset);
            Slot readSlot = new Slot(0, 0);
            readEntries.add(readSlot);
        //store full page data
        //Page newPage = new Page(pageSize, readNumOfRecords, readEndOfFreeSpace, readEntries, pageBytes);
        Page newPage = new Page(pageSize, 0, 0, readEntries, pageBytes);
        return newPage;
    }
  
}