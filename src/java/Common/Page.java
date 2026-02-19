package Common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
    private ArrayList<List<Object>> data;
    private int next_page_id;
    private boolean is_dirty;
    private long time;

    private static final int HEADER_SIZE = Integer.BYTES  * 2; //numslots (int = 4 bytes) + freeptr (int = 4 bytes) 
    private static final int SLOT_ENTRY_SIZE = Integer.BYTES * 2; //offset size (int = 4 bytes) + length size (int = 4 bytes) 

    public Page(int pageID){
        this.pageId = pageID;
        this.bytes = new byte[pageSize];
        is_dirty = false;
        time = System.currentTimeMillis();
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

    public void set_data(ArrayList<List<Object>> data){
        this.data = data;

        //Compute the size of what we currently have
        for(List<Object> row : data){
            for(Object object : row){

            }
        }
    }

    // === Getter Functions ===


    public ArrayList<List<Object>> get_data() {
        return data;
    }

    public long get_time(){
        return time;
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

}