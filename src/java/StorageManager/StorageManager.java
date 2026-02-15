package StorageManager;

import Common.Page;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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

    //FreePage Functionality: When catalog creates a table get a page index

    /**
     * Does Database file exist
     * @param database_filename The name of the databse we finding
     * @return database exist?
     */
    public boolean doDatabaseFileExist(String database_filename) {
        File database_file = new File(database_filename);
        if(database_file.exists()){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Creates Database File
     * @param database_name The name of the database
     * @param byte_size the size of teh database
     */
    public void createDatabaseFile(String database_name, int byte_size) {
        try(RandomAccessFile database_access = new RandomAccessFile(database_name,"rw")){
            byte[] database = new byte[byte_size];
            database_access.write(database);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Idk the use case of this yet - Jason Ha
     */

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


    public void openDatabaseFile() {

    }

    public void closeDatabaseFile() {

    }

    public String getFilename() {
        return filename;
    }
    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}