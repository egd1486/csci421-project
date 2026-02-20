package StorageManager;

import Common.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

public class StorageManager {
    private static int pageSize;
    private static String filename;
    private static int page_counter; // What page is created
    private static Stack<Integer> freepages;

    private static final int BOOLEAN_BYTES = 1; //hard coded since Boolean.BYTES dne



    private byte[] write_int(int where, int number, byte[] data){
        ByteBuffer.wrap(data, where, Integer.BYTES).putInt(number);
        return data;
    }

    private int read_int(int where, byte[] data){
        return ByteBuffer.wrap(data, where, Integer.BYTES).getInt();
    }

    /**
     * Insert a row of data into the page byte array
     * @param row Data we're entering
     */
    public boolean insert_data(byte[] row){
        int numslots = read_int(0); //Getting Number of Slots
        int free_ptr = read_int(4); //Getting the Free Pointer

        int calculate_slot_index = HEADER_SIZE + (numslots * SLOT_ENTRY_SIZE); // We calculate this because we already have the HEADER at the beginning and we need Slot entry size for (Offset, Length) at the end of each row data
        int check_space = free_ptr - calculate_slot_index; // We check if can even fit the data

        if((row.length + SLOT_ENTRY_SIZE) > check_space){
            //TODO: Split Page work on it later
            return -1;
        }

        //Write stuff in offset
        int offset = free_ptr - row.length; //We find
        int nextpos = offset;
        for(byte bit : row){
            bytes[nextpos++] = bit;
        }

        //Now to store (offset,length) :(
        int next_slot_index = HEADER_SIZE + numslots * SLOT_ENTRY_SIZE;
        write_int(next_slot_index, offset); //Offset
        write_int(next_slot_index + 4, row.length); //Length

        write_int(0, numslots + 1); //Next slot
        write_int(4, offset); //what offset we got left

        return numslots; //Return ID of the slot
    }

    /**
     * Encoder we get the data from page, and we make it to binary format
     * How does the Data would look like: Slotted Page
     * BASICALLLLLLLY its bytes = [Header | Pointers | Free Space | Records] TADAH SLOTTED PAGE APPROACH
     * @param page
     * @return
     */
    public boolean serializePage(Page page) throws IOException{

        byte[] whole_data = new byte[pageSize];

        for(List<Object> row : page.get_data()){
            ByteArrayOutputStream byte_array = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byte_array);
            for(Object obj : row){
                switch (obj) {
                   case null -> dos.writeByte(0);
                   case Integer i -> {
                       dos.writeByte(1);
                       dos.writeInt(i);
                   }
                   case Boolean b -> {
                       dos.writeByte(2);
                       dos.writeBoolean(b);
                   }
                   case String s -> {
                       dos.writeByte(3);
                       byte[] bytes = s.getBytes();
                       dos.writeInt(bytes.length);
                       dos.write(bytes);
                   }
                   case Character c -> {
                       dos.writeByte(4);
                       dos.writeChar(c);
                   }
                   case Double v -> {
                       dos.writeByte(5);
                       dos.writeDouble(v);
                   }
                   default -> {
                       System.out.println("Invalid data object: " + obj + " Serialize Function Line 72");
                   }
                }
                byte[] data = byte_array.toByteArray();
           }
        }
    }
    /**
     * Decode the data from a page in the memory.
     * @param pagenumber The name of the database
     */
    public Page decode(int pageNumber){
        ArrayList<List<Object>> fullPage = new ArrayList<List<Object>>();
        byte[] data = new byte[pageSize];
        try(RandomAccessFile file = new RandomAccessFile(filename, "r")){
            file.seek(pageNumber * pageSize);
            file.readFully(data);
        }
        catch(Exception e){
            System.err.println(e);
        }
        // Page new_page = new Page(pageNumber);
        int numEntries = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, Integer.BYTES)).getInt();
        int free_ptr= ByteBuffer.wrap(Arrays.copyOfRange(data, Integer.BYTES, 2*Integer.BYTES)).getInt()+1;
        int size; //char and varchar
        Type[] schema = {}; //! Need way to get list of attributes, or add as parameter
        for (int index = 1; index <= numEntries; index++){
            int offset = ByteBuffer.wrap(Arrays.copyOfRange(data, Integer.BYTES*2*index, Integer.BYTES*(2*index+1))).getInt();
            int length = ByteBuffer.wrap(Arrays.copyOfRange(data, Integer.BYTES*(2*index+1), Integer.BYTES*(2*index+2))).getInt();
            ArrayList<Object> row = new ArrayList<Object>();
            String nullPtr = "";
            for (int nullByte = 0; nullByte < Math.ceil(1/8); nullByte++){
                nullPtr += ByteBuffer.wrap(Arrays.copyOfRange(data, offset, offset+Integer.BYTES)).getInt();
                offset += Integer.BYTES;
            }
            for (int attr = 0; attr < schema.length; attr++){
                if (nullPtr.charAt(attr) == '1')
                    row.add(null);
                else
                    switch (schema[attr]){
                    case INT:
                        row.add(ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Integer.BYTES+offset)).getInt());
                        offset += Integer.BYTES;
                        break;
                    case BOOLEAN:
                        row.add(data[offset] == 1); //to get true or false
                        offset += BOOLEAN_BYTES;
                        break;
                    case CHAR: //!add size to copyOfRange so you get full char array?
                        row.add(ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Character.BYTES+offset)).getChar());
                        size = 0; //! get length of attr from schema
                        offset+= size * Character.BYTES;
                        break;
                    case DOUBLE:
                        row.add(ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Double.BYTES+offset)).getDouble());
                        offset += Double.BYTES;
                        break;
                    case VARCHAR: //! offset = offset----location and location------size
                        int location = ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Integer.BYTES+offset)).getInt();
                        size = ByteBuffer.wrap(Arrays.copyOfRange(data, Integer.BYTES+offset, 2*Integer.BYTES+offset)).getInt();
                        row.add(ByteBuffer.wrap(Arrays.copyOfRange(data, location, size+location)).getInt(), size);
                        offset += 2*Integer.BYTES;
                        break;
                    }
            }
            fullPage.add(row);
        }
        Page decoded = new Page(pageNumber);
        decoded.set_data(fullPage);
        return decoded;
    }


    /**
     * Creates Database File, or Reads existing one.
     * @param database_name The name of the database
     * @param page_size the size of teh database
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
            freepages = new Stack<Integer>();
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
        return page_counter++;
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
            file.seek((long) pageNumber * pageSize);
            file.readFully(PageData);
        }
        Page new_page = new Page(pageNumber);





        new_page.set_data(PageData);
        return new Page(pageNumber, pageSize);
    }





    /**
     * WritePage writes the page into disk
     * @param pageNumber what page
     * @param objects list of objects 
     * @throws IOException self-explantory
     */
    public static void writePage(int pageNumber, ArrayList<List<Object>> data) throws IOException {
       try(RandomAccessFile file = new RandomAccessFile(filename, "rw")){
           //Serialize it into bytes and then write it
          //file.write(data, pageNumber * pageSize, pageSize);
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