package StorageManager;

import Common.Page;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class StorageManager {
    private static int pageSize;
    private static String filename;
    private static int maximum_pages; // How many pages can fit in the database
    private static int current_page; // What page is created
    private static Stack<Integer> freepages;

    /**
     * Does Database file exist
     * @param database_filename The name of the databse we finding
     * @return database exist?
     */
    public static boolean doDatabaseFileExist(String database_filename) {
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
    public static void createDatabaseFile(String database_name, int byte_size, int page_size) {
        try(RandomAccessFile database_access = new RandomAccessFile(database_name,"rw")){
            byte[] database = new byte[byte_size];
            maximum_pages = byte_size / page_size;
            filename = database_name;
            pageSize = page_size;
            database_access.write(database);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create page first checks if they're available free pages 
     * If there's no free pages we first check if the current_page exceed maximum page if so its full return -1
     * @return id or -1 if its full
     */
    public static int create_page(){
        if(!(freepages.isEmpty())){
            return freepages.pop();
        }

        if(current_page > maximum_pages){
            return -1; // Database is full
        }
        return current_page++;
    }

    /**
     * Whenever we delete a page from memory instead of just removing and creating a hole in database we can just mark a pageid as free
     * This way we can just replace it so we don't shift down <3
     * @param pageId
     */
    public static void markfreepage(int pageId){
        freepages.add(pageId);
    }


    /**
     * readPage reads the page data from a specific page number
     * @param pageNumber what we readin chat
     * @return A page that have the set of binary data we looking at
     */
    public static Page readPage(int pageNumber) throws IOException {
        byte[] PageData = new byte[pageSize];
        try(RandomAccessFile file = new RandomAccessFile(filename, "r")){
            file.seek(pageNumber * pageSize);
            file.readFully(PageData);
        }
        Page new_page = new Page(pageNumber, pageNumber);
        new_page.set_pagedata(PageData);
        return new Page(pageNumber, pageSize);
    }

    /**
     * WritePage writes the page into disk
     * @param pageNumber what page
     * @param objects list of objects 
     * @throws IOException self-explantory
     */
    public static void writePage(int pageNumber, byte[] data) throws IOException {
       try(RandomAccessFile file = new RandomAccessFile(filename, "rw")){
            file.seek(pageNumber * pageSize);
            for(byte each_byte : data){
                file.write(each_byte);
            }
       }
    }

    // === Getter Functions ===

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