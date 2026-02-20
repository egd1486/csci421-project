package StorageManager;

import Common.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class StorageManager {
    private static int pageSize;
    private static String filename;
    private static int page_counter; // What page is created
    private static Stack<Integer> freepages;

    private static final int BOOLEAN_BYTES = 1; //hard coded since Boolean.BYTES dne

    public static byte[] encoder(Page page) throws IOException {

        byte[] slotted_page = new byte[pageSize];
        ByteBuffer slotted_buffer = ByteBuffer.wrap(slotted_page);


        int numslots = 0;
        int free_ptr = pageSize-1;

        int HEADER_SIZE = Integer.BYTES * 3; // next_Page_id, num_slots(entries), free_ptr
        int SLOT_ENTRY_SIZE = Integer.BYTES * 2;

        slotted_buffer.putInt(0, page.get_next_pageid());
        slotted_buffer.putInt(Integer.BYTES, numslots);
        slotted_buffer.putInt(Integer.BYTES * 2, free_ptr);
        for(List<Object> row : page.get_data()){

            ByteArrayOutputStream byte_array = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byte_array);
            BitSet bitmap = new BitSet(row.size());

            for(int index = 0; index < row.size(); index++){
               switch(row.get(index)){
                   case null -> bitmap.set(index);

                   case Integer i -> {
                       dos.writeInt(i);
                   }

                   case Boolean b -> {
                       dos.writeBoolean(b);
                   }

                   case Double d -> {
                       dos.writeDouble(d);
                   }

                   case Character c -> {
                       dos.writeChar(c);
                   }

                   case String s -> {
                       byte[] str = s.getBytes();
                       dos.writeInt(str.length);
                       dos.write(str);
                   }
                   default -> System.out.println("what the hell is this: " + row.get(index));
               }
            }
            byte[] row_data = byte_array.toByteArray();
            int bitmapSize = (row.size() + 7) / 8;
            byte[] fixedBitmap = new byte[bitmapSize];
            System.arraycopy(bitmap.toByteArray(), 0, fixedBitmap, 0, bitmap.toByteArray().length);

            numslots++;
            int slot_index = HEADER_SIZE + numslots * SLOT_ENTRY_SIZE;
            int new_free_ptr = free_ptr - row_data.length;

            System.arraycopy(fixedBitmap, 0, slotted_page, new_free_ptr, fixedBitmap.length);
            System.arraycopy(row_data, 0, slotted_page, new_free_ptr + fixedBitmap.length, row_data.length);

            slotted_buffer.putInt(slot_index, new_free_ptr);
            slotted_buffer.putInt(slot_index + Integer.BYTES, row_data.length);

            slotted_buffer.putInt(Integer.BYTES, numslots);
            slotted_buffer.putInt(Integer.BYTES * 2, free_ptr);
        }
        return slotted_page;
    }


    /**
     * WritePage writes the page into disk
     * @param page_adding_to_memory what page were addinginto memory
     * @throws IOException self-explantory
     */
    public static void writePage(Page page_adding_to_memory) throws IOException {
        try(RandomAccessFile file = new RandomAccessFile(filename, "rw")){
            byte[] adding = encoder(page_adding_to_memory);
            file.seek((long) pageSize * page_adding_to_memory.get_pageid());
            file.write(adding);
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
                        size = 1; //! get length of attr from schema
                        row.add(new String(Arrays.copyOfRange(data, offset, size*Character.BYTES+offset), StandardCharsets.UTF_8));
                        offset+= size * Character.BYTES;
                        break;
                    case DOUBLE:
                        row.add(ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Double.BYTES+offset)).getDouble());
                        offset += Double.BYTES;
                        break;
                    case VARCHAR: //! offset = offset----location and location------size
                        int location = ByteBuffer.wrap(Arrays.copyOfRange(data, offset, Integer.BYTES+offset)).getInt();
                        size = ByteBuffer.wrap(Arrays.copyOfRange(data, Integer.BYTES+offset, 2*Integer.BYTES+offset)).getInt();
                        Object[] add = {new String(Arrays.copyOfRange(data, location, size*Character.BYTES+location), StandardCharsets.UTF_8),size};
                        row.add(add);
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