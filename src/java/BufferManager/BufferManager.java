package BufferManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import Common.Page;
import StorageManager.StorageManager;
// Author: Jason Ha
public class BufferManager {

    /*
    BufferManager
    Get Frame size to know what pages are being used and be accessed.
    Track Dirty Pages
    Implement LRU
    Decide when a page must be written back
    */

    /**
     * Frame[] Buffer = List of Frame which contain a page
     * PageTable maps each frame with pageID
     */

    public static int BufferSize;

    private final Page[] buffer;
    private final Map<Integer, Page> mapId = new HashMap<>();

    public BufferManager(int set_buffer_size) {
        buffer = new Page[set_buffer_size];
        BufferSize = set_buffer_size;
    }

    /**
     * LRU remove the page who have the highest time and return the page to modify
     * @return page with the highest time to evict
     */
    public int lru() throws IOException {
        long highest_time = -1;
        int removal_page = -1;
        for(int i = 0; i < buffer.length; i++){
            if(buffer[i].get_page_life() > highest_time){
                highest_time = buffer[i].get_page_life();
                removal_page = i;
            }
        }
        Page page_to_remove = buffer[removal_page];

        if(page_to_remove.check_dirty()){
            StorageManager.writePage(page_to_remove.get_pageid(), page_to_remove.get_data());
        }
        mapId.remove(page_to_remove.get_pageid());
        return removal_page;
    }

    /**
     * GetPage checks if page is already in buffer
     * if not find empty page or evict LRU page
     * if evicted page is dirty write back to disk
     * update map and return page
     * @return page
     */
    public Page getPage(int pageId) throws IOException {

        //If a map contains the page id then we return page
        if(mapId.containsKey(pageId)){
            Page page = mapId.get(pageId);
            return page;
        }

        //Else map doesn't contain id we find a free page considering at some point in random index a frame can be free
        //due to removal of the page so linear scan O(N) check every index if we have empty page
        for(Page check_page : buffer){
            if(check_page == null){
                return check_page;
            }
        }

        //LRU method:
        //Use System.time comparing the old time (least recently use) vs current time who ever have the largest is LRU
        Page page_from_disk = StorageManager.readPage(pageId);
        buffer[lru()] = page_from_disk;
        mapId.put(pageId, page_from_disk);
        return page_from_disk;
    }

    /**
     * Flush all the dirty pages down into memory
     * @throws IOException
     */
    public void flush_all() throws IOException {
        for(Page check_page : buffer){
            if(check_page != null){
              if(check_page.check_dirty()){
                  StorageManager.writePage(check_page.get_pageid(), check_page.get_data());
              }
            }
        }
    }


}
