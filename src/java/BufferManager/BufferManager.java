package BufferManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import Common.Page;
import Catalog.Schema;
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

    private static Page[] buffer;
    private static Map<Integer, Page> mapId = new HashMap<>(); //pages already in buffer

    public static void initialize(int set_buffer_size) {
        buffer = new Page[set_buffer_size];
        BufferSize = set_buffer_size;
    }

    /**
     * LRU remove the page who have the highest time and return the page to modify
     * @return page with the highest time to evict
     */
    public static int lru() throws IOException {
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
    public static Page getPage(int pageId, Schema schema) throws IOException {

        //If a map contains the page id then we return page
        if(mapId.containsKey(pageId)){
            return mapId.get(pageId);
        }

        //Else map doesn't contain id we find a free page considering at some point in random index a frame can be free
        //due to removal of the page so linear scan O(N) check every index if we have empty page
        for(Page check_page : buffer){
            if(check_page == null){
                Page adding_new_page = new Page(pageId, schema);
                mapId.put(pageId, adding_new_page);
                return adding_new_page;
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
    * GetEmptyPage - get an empty page
    * check buffer for empty space, if none evict
    * @return empty page
    */
    public static Page getEmptyPage(Schema schema) throws IOException {
        int newPageId = StorageManager.create_page();
        //check if have empty slot in buffer to place new empty page
        for(Page check_page : buffer){
            if(check_page == null){
                Page newEmptyPage = new Page(newPageId, schema);
                mapId.put(newPageId, newEmptyPage);
                return newEmptyPage;
            }
        }

        //no empty slot in buffer so evict one 
        Page newEmptyPage = new Page(newPageId, schema);
        buffer[lru()] = newEmptyPage;
        mapId.put(newPageId, newEmptyPage);
        return newEmptyPage;
    }

    /**
     * Flush all the dirty pages down into memory
     * @throws IOException
     */
    public static void flush_all() throws IOException {
        for(Page check_page : buffer){
            if(check_page != null){
              if(check_page.check_dirty()){
                  StorageManager.writePage(check_page.get_pageid(), check_page.get_data());
              }
            }
        }
    }


}
