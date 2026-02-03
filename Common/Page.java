package Common;

import java.util.ArrayList;

public class Page {
    private int pageSize;
    private int numOfRecords;
    private int endOfFreeSpace;
    private ArrayList<Slot> entries; //<Size of record, Location>


    public Page(int pageSize) {
        this.pageSize = pageSize;

    }

    // ! how to represent records in page

    // retrieve record
    public Record retrieveRecord(int slotNumber) {
        return new Record();
    }

    public void insertRecord(Record record) {
        // insert at endOfFreeSpace
        // add to entries

        // increment numOfRecords

        // update endOfFreeSpace


        //cases: page full
    }

    public void deleteRecord(int slotNumber) {
        // delete record from entries
        
        // decrement numOfRecords

        // update endOfFreeSpace
    }

    // update record
    public void updateRecord(int slotNumber) {

    }

    //enough free space to insert?
    public boolean checkEnoughFreeSpace() {
        return false;
    }

    public void calculateOffset() {
        // for inserting slot for a record
    }

  
    public void javaToBinary() {

    }

    public void binaryToJava() {

    }
}