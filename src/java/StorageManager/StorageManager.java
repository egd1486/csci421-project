package StorageManager;

import Common.Page;
import Common.Record;
import Common.Slot;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class StorageManager {
    private String filepath; //? directory path
    private int pageSize;
    private String filename;
    //private Database database;


    // ! how to represent pages in file
    // ! how do we delete/insert pages
    // ! keep track of free/used pages 

    // ! how to keep track of total pages in the file?

    //random file access

    public boolean doDatabaseFileExist() {
        //return Database.exist_database(filepath + "\\database.txt");  // Currently a String[] for testing
        return false;
    }

    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {}

    public void openDatabaseFile() {

    }

    public void closeDatabaseFile() {

    }

    public void createDatabaseFile() {
        // if it doesn't already exist
        // create page 0 with meta data? 
        File database = new File(filepath + "\\database.txt");
        try{
            database.createNewFile();
        }
        catch(IOException e){
            // Handle error
            System.err.println(e);
        }
    }

    public void createPage() {
        // what params does it need
        // return page? page number?
    }

    //! Important for readPage and writePage: Is pageNumber 0 indexed? If no pageNumber-1 in seek.
    //read binary data
    public Page readPage(int pageNumber) {
        byte[] pageData = new byte[pageSize];
        try (RandomAccessFile file = new RandomAccessFile(filepath + "/database.txt","r"))
        {
            file.seek(pageNumber*this.pageSize);
            file.readFully(pageData);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
        int recordCount = 0;
        ArrayList<Slot> slots = new ArrayList<Slot>();
        ArrayList<Record> records = new ArrayList<Record>();
        int usedSpace=0;
        //! Read the page


        //return new Page(pageNumber, pageData);
        return new Page(this.pageSize, recordCount, slots, records, pageNumber, usedSpace);
    }

    //write binary data
    public void writePage(int pageNumber, byte[] data) {
        try (RandomAccessFile file = new RandomAccessFile(filepath + "/database.txt","rw"))
        {
            file.seek(pageNumber*pageSize);
            for (int i = 0; i < data.length; i++)
            {
                file.write(data[i]);
            }
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }

    public void markFreePage(int pageNumber) {
        // when "deleting page" mark as free
    }

    public void markUsedPage(int pageNumber) {
        //mark as used
    }

    public int getNumPages() {
        return -1;
    }

    public int calculatePageOffset(int pageNumber) {
        // pageSize * pageNumber = where does page start
        return -1;
    }

    public boolean hasFreePages() {
        // Free pages not implemented so this won't work yet. Smth like
        /*
        return freePages.length > 0;
        */
        
        return false;
    }

    public int getNextFreePage() {
        // where next free page?
        return -1; //page number
    }

    public String getFilename() {
        return filename;
    }
}