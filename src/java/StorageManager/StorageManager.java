package StorageManager;

import Common.Page;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Stack;

public class StorageManager {
    private static int pageSize;
    private static String filename;
    private static int current_page; // What page is created
    private static Stack<Integer> freepages;


    /**
     * Creates Database File, or Reads existing one.
     * @param database_name The name of the database
     * @param byte_size the size of teh database
     */
    public static void initializeDatabaseFile(String database_name, int page_size) {
        // First check if the file exists
        File database_file = new File(database_name);
        System.out.println("Accessing database location...");

        if (database_file.exists()) {
            System.out.println("Database found. Restarting database...");
            // TODO: Read existing database constants

            System.out.println("Ignoring provided page size. Using prior size of ____...");

            // TODO: Read schema values, and initalize into Catalog.

            return;
        }
        System.out.println("No database found. Creating new database...");

        // Otherwise, create the database from scratch.
        try(RandomAccessFile database_access = new RandomAccessFile(database_name,"rw")){
            byte[] database = new byte[page_size];
            filename = database_name;
            pageSize = page_size;
            database_access.write(database);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create page first checks if they're available free pages 
     * If there's no free pages we return a new page
     * @return id
     */
    public static int create_page(){
        if(!(freepages.isEmpty())){
            return freepages.pop();
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
            file.write(data, pageNumber * pageSize, pageSize);
       }
    }

    // === Getter Functions ===

    public static String getFilename() {
        return filename;
    }
    public static int getPageSize() {
        return pageSize;
    }
    public static void setPageSize(int newSize) {
        pageSize = newSize;
    }
}