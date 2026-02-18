package Common;

import java.nio.ByteBuffer;
import java.util.ArrayList;


/**
 * Page:
 * Contains a list of binary data which are objects
 * pageId - Unique Identifier
 * next_page_id - A Linkage between pages
 * bytes[] - Page data
 * 
 * Bytes Contain a header for like numslots and freespaceptr
 * 
 * 
 * How does the Data would look like: Slotted Page
 * BASICALLLLLLLY its bytes = [Header | Pointers | Free Space | Records] TADAH SLOTTED PAGE APPROACH
 */
public class Page {
    private int pageId;
    private byte[] bytes;
    private int next_page_id;
    private boolean is_dirty;
    private long time;

    private static final int HEADER_SIZE = Integer.BYTES  * 2; //numslots (int = 4 bytes) + freeptr (int = 4 bytes) 
    private static final int SLOT_ENTRY_SIZE = Integer.BYTES * 2; //offset size (int = 4 bytes) + length size (int = 4 bytes) 

    public Page(int pageID, int pageSize){
        this.pageId = pageID;
        this.bytes = new byte[pageSize];
        is_dirty = false;
        time = System.currentTimeMillis();
       
        write_int(0, 0); //numslots
        write_int(4, pageSize); //freePtr

    }

    /**
     * These helper functions are here to help me write numbers in byte array since im really lazy to hardcode it
     */

    private void write_int(int where, int number){
        ByteBuffer.wrap(bytes, where, Integer.BYTES).putInt(number);
    }

    private int read_int(int where){
        return ByteBuffer.wrap(bytes, where, Integer.BYTES).getInt();
    }

    /**
     * Insert a row of data into the page byte array
     * @param row Data we're entering 
     */
    public int insert(Byte[] row){
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
     * Slot-Data returns the data by the slot_ID from the page bytes array
     * @param slotid where we looking at chat?
     * @return the data we looking at chat
     */
    public byte[] read_slot_data(int slotid){
        
        int slot_pos = HEADER_SIZE + (slotid * SLOT_ENTRY_SIZE); 
        int offset = read_int(slot_pos); //we get offset
        int length = read_int(slot_pos+4); //We get length

        byte[] data_return = new byte[length];
        for(int i = 0; i < length; i++){
            data_return[i] = bytes[offset + i];
        }
        return data_return;
    }

    // === Setter Functions ===

    public void set_nextpageid(int num){
        next_page_id = num;
    }

    public void set_pagedata(byte[] data){
        this.bytes = data;
    }

    public void set_isdirty(boolean type){
        is_dirty = type;
    }

    // === Getter Functions ===

    public void set_time(long newTime){
        this.time = newTime;
    }

    public long get_time(){
        return time;
    }

    public int get_next_pageid(){
        return next_page_id;
    }

    public byte[] get_data(){
        return bytes;
    }

    public int get_pageid(){
        return pageId;
    }

    public long get_page_life(){
        return time;
    }

    public boolean check_dirty(){
        return is_dirty;
    }

}