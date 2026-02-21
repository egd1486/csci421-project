package Common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import Catalog.Schema;
import StorageManager.StorageManager;


/**
 * Page:
 * Contains a list of binary data which are objects
 * pageId - Unique Identifier
 * next_page_id - A Linkage between pages
 * Objects[][] rows -> Each Row will have a list of object so [Row1: ("Jason", 21), Row2: ("Joseph", 67")]
 * 
 * Bytes Contain a header for like numslots and freespaceptr
 * 
 *
 */
public class Page {
    private int pageId;
    private ArrayList<ArrayList<Object>> data;
    private int next_page_id;
    private int freebytes;
    private boolean is_dirty;
    private long time;
    private Schema schema;

    private static final int HEADER_SIZE = Integer.BYTES  * 2; //numslots (int = 4 bytes) + freeptr (int = 4 bytes) 
    private static final int SLOT_ENTRY_SIZE = Integer.BYTES * 2; //offset size (int = 4 bytes) + length size (int = 4 bytes) 

    public Page(int pageID, Schema schema){
        this.pageId = pageID;
        this.schema = schema;
        data = new ArrayList<>();
        is_dirty = false;
        time = System.currentTimeMillis();
        next_page_id = -1;
    }

    /**
     * These helper functions are here to help me write numbers in byte array since im really lazy to hardcode it
     */

//    private void write_int(int where, int number){
//        ByteBuffer.wrap(bytes, where, Integer.BYTES).putInt(number);
//    }
//
//    private int read_int(int where){
//        return ByteBuffer.wrap(bytes, where, Integer.BYTES).getInt();
//    }



    /**
     * Slot-Data returns the data by the slot_ID from the page bytes array
     * @param slotid where we looking at chat?
     * @return the data we looking at chat
     */
//    public byte[] read_slot_data(int slotid){
//
//        int slot_pos = HEADER_SIZE + (slotid * SLOT_ENTRY_SIZE);
//        int offset = read_int(slot_pos); //we get offset
//        int length = read_int(slot_pos+4); //We get length
//
//        byte[] data_return = new byte[length];
//        for(int i = 0; i < length; i++){
//            data_return[i] = bytes[offset + i];
//        }
//        return data_return;
//    }

    // === Setter Functions ===

    public void set_nextpageid(int num){
        next_page_id = num;
    }

    public void set_isdirty(boolean type){
        is_dirty = type;
    }

    public void set_freebytes(int num){
        freebytes = num;
    }

    public void set_data(ArrayList<ArrayList<Object>> data){
        this.data = data;

        //Compute the size of what we currently have
        for(List<Object> row : data){
            for(Object object : row){

            }
        }
    }

    public void set_newtime(){
        time = System.currentTimeMillis();
    }

    // === Getter Functions ===

    public int get_slots_remaining() {
        int page_metadata = 3 * Integer.BYTES; // next_page_id + num_slots + free_ptr

        int header_size = 2 * Integer.BYTES; //Location + TotalSize

        int max_row_size = schema.GetMaxRowSize();

        System.out.println("Max Row Size: " + max_row_size);

        int rows = data.size();

        int available_space;
        // if freebytes not provided,
        if (freebytes == 0)
        // calculate the available space from pageSize and other constants
        available_space = (StorageManager.pageSize - page_metadata - (header_size*rows) - (max_row_size*rows));
        // otherwise just use the specified freebytes.
        else available_space = freebytes;

        System.out.println("Available Space: " + available_space+ " " + available_space / (max_row_size + header_size));
        return (int) available_space / (max_row_size + header_size);
    }

    public ArrayList<ArrayList<Object>> get_data() {
        return data;
    }

    public long get_time(){
        return time;
    }
    
    public Schema get_schema(){
        return schema;
    }

    public int get_next_pageid(){
        return next_page_id;
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

    public int get_data_length(){
        return data.size();
    }
}