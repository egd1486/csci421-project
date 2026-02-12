package StorageManager;

import Common.*;
import java.io.File;
import java.io.IOException;

class StorageManager {
    private String filepath; //? directory path
    private int pageSize; //?
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

    //read binary data
    public Page readPage(int pageNumber) {
        return new Page(pageNumber);
    }

    //write binary data
    public void writePage(int pageNumber, byte[] data) {

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
        return false;
    }

    public int getNextFreePage() {
        // where next free page?
        return -1; //page number
    }
}