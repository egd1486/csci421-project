package StorageManager;
import Common.*;

class StorageManager {
    private String filepath; //?
    private int pageSize; //?


    // ! how to represent pages in file
    // ! how do we delete/insert pages
    // ! keep track of free/used pages 

    // ! how to keep track of total pages in the file?

    //random file access

    public boolean doDatabaseFileExist() {
        return false;
    }

    public void openDatabaseFile() {

    }

    public void closeDatabaseFile() {

    }

    public void createDatabaseFile() {
        // if it doesn't already exist
        // create page 0 with meta data? 
    }

    public void createPage() {
        // what params does it need
        // return page? page number?
    }

    public Page readPage(int pageNumber) {
        return new Page(pageNumber);
    }

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