package StorageManager;

import Catalog.Catalog;
import Common.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import Catalog.Schema;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class StorageManager {
    public static int pageSize;
    private static String filename;
    private static int page_counter; // What page is created and 1 because page 0 stores information
    private static Stack<Integer> freepages;

    private static final int BOOLEAN_BYTES = 1; //hard coded since Boolean.BYTES dne

  public static byte[] encoder(Page page) throws IOException {
        Schema schema = page.get_schema();

        byte[] slotted_page = new byte[pageSize];
        ByteBuffer slotted_buffer = ByteBuffer.wrap(slotted_page);

        int numslots = 0;
        int free_ptr = pageSize-1;

        int HEADER_SIZE = Integer.BYTES * 3; // next_Page_id, num_slots(entries), free_ptr
        int SLOT_ENTRY_SIZE = Integer.BYTES * 2;

        slotted_buffer.putInt(0, page.get_next_pageid());

        Type[] type = schema.GetTypes();
        for(List<Object> row : page.get_data()){

            ByteArrayOutputStream byte_array = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byte_array);
            BitSet bitmap = new BitSet(row.size());

            for(int index = 0; index < row.size(); index++){
            // Check if the value is null, and mark if so.
            if(row.get(index) == null) bitmap.set(index);
            // Otherwise, switch through the types to handle writing the value
            else switch(type[index]){
                   case INT -> dos.writeInt((int) row.get(index));

                   case BOOLEAN -> dos.writeBoolean((boolean) row.get(index));

                   case DOUBLE -> dos.writeDouble((double) row.get(index));

                   default -> {
                        // This case handles char/varchar which are stored in equivalent fashion :)
                        String string = (String) row.get(index);

                        // Write for string length
                        dos.writeInt(string.length());

                        // Write each char byte.
                        for(int i = 0; i < string.length(); i++)
                        dos.writeChar(string.charAt(i));
                   }
               }
            }

            byte[] row_data = byte_array.toByteArray();
            int bitmapSize = (row.size() + 7) / 8;
            byte[] fixedBitmap = new byte[bitmapSize];
            System.arraycopy(bitmap.toByteArray(), 0, fixedBitmap, 0, bitmap.toByteArray().length);

            numslots++;
            int slot_index = HEADER_SIZE + (numslots - 1) * SLOT_ENTRY_SIZE;
            int new_free_ptr = free_ptr - row_data.length - fixedBitmap.length;

            System.arraycopy(fixedBitmap, 0, slotted_page, new_free_ptr, fixedBitmap.length);
            System.arraycopy(row_data, 0, slotted_page, new_free_ptr + fixedBitmap.length, row_data.length);

            // Store this rows location above,
            slotted_buffer.putInt(slot_index, new_free_ptr);
            // And how long it is :)
            slotted_buffer.putInt(slot_index + Integer.BYTES, fixedBitmap.length + row_data.length);
        }

        // Write number of entries.
        slotted_buffer.putInt(Integer.BYTES, numslots);

        slotted_buffer.putInt(Integer.BYTES * 2, free_ptr);

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
    public static Page decode(Schema schema, int pageNumber){
        Page decoded = new Page(pageNumber, schema);
        ArrayList<ArrayList<Object>> fullPage = new ArrayList<ArrayList<Object>>();
        byte[] data = new byte[pageSize];
        try(RandomAccessFile file = new RandomAccessFile(filename, "r")){
            file.seek(pageNumber * pageSize);
            file.readFully(data);
        }
        catch(Exception e){
            System.err.println(e);
        }


        ByteBuffer decode_wrapper = ByteBuffer.wrap(data);
        int HEADER_SIZE = Integer.BYTES * 3; // next_Page_id, num_slots(entries), free_ptr
        int SLOT_ENTRY_SIZE = Integer.BYTES * 2;

        int nextPage = decode_wrapper.getInt(0);
        decoded.set_nextpageid(nextPage);
        int numEntries = decode_wrapper.getInt(Integer.BYTES);
        int freeptrstored = decode_wrapper.getInt(Integer.BYTES * 2);

        // Page new_page = new Page(pageNumber);
        int free_ptr = freeptrstored + 1; //Hold onto this for now
        decoded.set_freebytes(free_ptr - numEntries*(2*Integer.BYTES) - 3*Integer.BYTES); // End of free space - slotSize * numEntries - headerSize

        Type[] attributes = schema.GetTypes(); //! Need way to get list of attributes, or add as parameter
        int bitmapsize = (attributes.length + 7) / 8;

        for (int index = 0; index < numEntries; index++){
            int slotPos = HEADER_SIZE + index * SLOT_ENTRY_SIZE;
            int offset = decode_wrapper.getInt(slotPos);
            int length = decode_wrapper.getInt(slotPos + Integer.BYTES);

            int ptr = offset + bitmapsize; //keep offset maybe
            boolean[] nullmap = new boolean[attributes.length];
            byte[] nullbytes = new byte[bitmapsize];

            // populate nullbytes
            for (int i = 0; i < bitmapsize; i++) nullbytes[i] = data[offset+i];

            // iterate through boolean array, setting nulls from nullbytes.
            for (int i = 0; i < attributes.length; i++)
            // Set true if bit is 1
            nullmap[i] = ((nullbytes[i/8] >> (i%8)) & 1) == 1;

            int size;
            ArrayList<Object> row = new ArrayList<Object>();
            for (int attr = 0; attr < attributes.length; attr++){
                if (nullmap[attr]) {
                    row.add(null);
                    continue;
                }
                switch (attributes[attr]){
                    case INT:
                        row.add(decode_wrapper.getInt(ptr));
                        ptr += Integer.BYTES;
                        break;
                    case BOOLEAN:
                        row.add(data[ptr] != 0); //to get true or false
                        ptr += 1;
                        break;
                    case CHAR:
                        size = decode_wrapper.getInt(ptr); // get length of string at the front
                        // shift pointer over,
                        ptr += Integer.BYTES;
                        // construct string from here,
                        String s = new String(data, ptr, size * Character.BYTES, StandardCharsets.UTF_16BE);
                        row.add(s);
                        ptr += size;
                        break;
                    case DOUBLE:
                        row.add(decode_wrapper.getDouble(ptr));
                        ptr += Double.BYTES;
                        break;
                    case VARCHAR: //! offset = offset----location and location------size
                        size = decode_wrapper.getInt(ptr);
                        ptr += Integer.BYTES;
                        String object = new String(data, ptr, size, StandardCharsets.UTF_8);
                        ptr += size;
                        row.add(object);
                        break;
                }

            }
            fullPage.add(row);
        }
        decoded.set_data(fullPage);
        return decoded;
    }



    /**
     * Creates Database File, or Reads existing one.
     * @param database_name The name of the database
     * @param page_size the size of teh database
     */
    public static void initializeDatabaseFile(String database_name, int page_size) throws IOException {
        // First check if the file exists
        File database_file = new File(database_name);
        System.out.println("Accessing database location...");

        page_counter = 2; //Were moving the page counter because page 0-1 will contain all of our basic db info

        if (database_file.exists()) {
            System.out.println("Database found. Restarting database...");
            filename = database_name;
            try(RandomAccessFile database_access = new RandomAccessFile(database_name, "r")){
                database_access.seek(0);
                pageSize = database_access.readInt();
            }
            System.out.println("Ignoring provided page size. Using prior size of " + page_size);

            // TODO: Read schema values, and initialize into Catalog.
            Page schemaTable = decode(Catalog.AttributeTable, 1);
            ArrayList<ArrayList<Object>> data = schemaTable.get_schema().Select();
            for (ArrayList<Object> row : data){
                if(Catalog.GetSchema(row.get(0).toString()) == null){
                    try{
                        Catalog.AddSchema(row.get(0).toString());
                    }
                    catch(Exception e){
                        System.err.println(e);
                    }
                }
                try{
                    Catalog.AttributeAdd(row.get(0).toString(), row.get(2).toString(), Type.values()[(Integer)row.get(3)], (Integer)row.get(4), (Boolean)row.get(5), (Boolean)row.get(7), (Boolean)row.get(6), row.get(8));
                }
                catch (Exception e){
                    System.err.println(e);
                }
            }
            return;
        }
        System.out.println("No database found. Creating new database...");
        freepages = new Stack<Integer>();
        filename = database_name;
        pageSize = page_size;
        // Otherwise, create the database from scratch. Assume we make the first page contains the information about the database
        try(RandomAccessFile database_access = new RandomAccessFile(database_name,"rw")){
            byte[] database_info = new byte[page_size];
            ByteBuffer database_wrapped = ByteBuffer.wrap(database_info);

            database_wrapped.putInt(0, page_size);
            database_wrapped.putInt(Integer.BYTES, 1); //Number of pages
            database_wrapped.putInt(Integer.BYTES * 2, 1); //SchemePageId or the pointer to catalogPageId
            database_wrapped.putInt(Integer.BYTES * 3, -1); //Bitmap of free_list_pages

            database_access.seek(0);
            database_access.write(database_info);

            byte[] catalog_page = new byte[page_size];
            ByteBuffer catalog_buffer = ByteBuffer.wrap(catalog_page);
            catalog_buffer.putInt(0, -1); //nextCatalogPage
            catalog_buffer.putInt(Integer.BYTES, 0); //ENTRIES
            catalog_buffer.putInt(Integer.BYTES * 2, page_size); //OH BOI ANOTHER SLOTTED PAGE APPROACH

            database_access.seek(page_size);
            database_access.write(catalog_page);

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
     * WritePage writes the page into disk
     * @param pageNumber what page
     * @param objects list of objects
     * @throws IOException self-explantory
     */
    // public static void writePage(int pageNumber, ArrayList<ArrayList<Object>> data) throws IOException {
    //    try(RandomAccessFile file = new RandomAccessFile(filename, "rw")){
    //        //Serialize it into bytes and then write it
    //       //file.write(data, pageNumber * pageSize, pageSize);
    //    }
    // }

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


    public static int getPageCounter() {
        return page_counter;
    }

    public static Stack<Integer> getFreePages() {
        return freepages;
    }
}