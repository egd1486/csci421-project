package Common;

import StorageManager.StorageManager;
import BufferManager.BufferManager;
import java.util.ArrayList;
import java.util.List;
import Catalog.Schema;

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
    public int pageId;
    private ArrayList<ArrayList<Object>> data;
    private int next_page_id;
    public int freebytes;
    public boolean bnode;
    public boolean leafnode;
    public Attribute attr;
    public boolean evicted = false;
    private boolean is_dirty;
    private long time;
    private Schema schema;

    private static final int HEADER_SIZE = Integer.BYTES  * 2; //numslots (int = 4 bytes) + freeptr (int = 4 bytes) 

    public Page(int pageID, Schema schema){
        this.pageId = pageID;
        this.schema = schema;
        data = new ArrayList<>();
        is_dirty = false;
        time = System.currentTimeMillis();
        next_page_id = -1;
        freebytes = StorageManager.PageSize - HEADER_SIZE; // next_page_id + num_slots
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

    public Page split_page(boolean count_freebytes) throws Exception {
        // Define useful variables for iterating,
        int size = this.data.size();
        int half = size / 2;
        int i;

        ArrayList<Object>[] HalfData = new ArrayList[size-half];

        // For each iteration, do the following, then remove from the original list and decrement.
        for (i=size-1; i>=half; this.data.remove(i--))
        // Copying to the native array here first,
        HalfData[i-half] = this.data.get(i);
        // Then in the update piece of the for loop, removing it.

        // Get the index of the new page, and of the current next page.
        int OldNext = this.get_next_pageid(), Next = StorageManager.CreatePage();

        // Point current to the new one,
        this.set_nextpageid(Next);

        // Mark page dirty now that it has lost rows, and now that it points to the new page.
        this.set_isdirty(true);

        // Get new page,
        Page NewPage = BufferManager.getEmptyPage(this.schema, Next);

        // Add the values to the page.
        for (i=0; i<HalfData.length; i++) NewPage.data.add(HalfData[i]);

        // Copy bnode state:
        NewPage.bnode = this.bnode;
        NewPage.leafnode = this.leafnode;
        NewPage.attr = this.attr;

        // Set its next value to the old next,
        NewPage.set_nextpageid(OldNext);

        // Mark new page dirty as it now has rows and a new next :)
        NewPage.set_isdirty(true);

        // Force freebyte recalculation for both pages now :)
        if (count_freebytes) {
            this.recalculate_freebytes();
            NewPage.recalculate_freebytes();
        }

        return NewPage;
    }

    public int bsearch_page(Object Key, int Index) throws Exception {
        Attribute A = (this.attr != null) ? this.attr : this.schema.Attributes.get(Index);
        int L=0, R=data.size(), M=0, C=0;
        while (L < R)
        // Get middle index, defining M when getting the entry.
        // Compare the keys, defining C when running Compare
        // If C is 0, then the keys are equivalent, which is not allowed.
        if ((C = A.Compare(Key, data.get(M =(L+R)/2).get(Index))) == 0) return M;

        // If the pkey is less than the middle pkey
        // Move the right bound down past it, as the true spot is left of it.
        else if (C < 0) R = M;

        // Otherwise, we need to shift the left edge up, as the spot is right of it.
        else L = M+1;
        
        // We reach here if the loop terminates, which would leave L on our desired position.
        return L;
    }

    public void recalculate_freebytes() throws Exception {
        freebytes = StorageManager.PageSize - HEADER_SIZE;
        
        for (ArrayList<Object> row : data) freebytes -= this.schema.GetRowByteSize(row);
    }

    // === Setter Functions ===

    public void set_nextpageid(int num){
        next_page_id = num;
    }

    public void set_isdirty(boolean type){
        is_dirty = type;

        // If this page is evicted, but we are marking it dirty, we care about it.
        // Let's remediate it.
        if (evicted && type) 
        try {BufferManager.remediate_evicted(this);}
        catch (Exception e) {System.out.println("Failed to remediate evicted page");}
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