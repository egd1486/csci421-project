package BufferManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Arrays;

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
        long highest_time = Long.MAX_VALUE;
        int removal_page = -1;
        for(int i = 0; i < buffer.length; i++){
            if (buffer[i] == null) continue;
            if(buffer[i].get_page_life() < highest_time){
                highest_time = buffer[i].get_page_life();
                removal_page = i;
            }
        }
        Page page_to_remove = buffer[removal_page];

        if(page_to_remove.check_dirty()){
            StorageManager.writePage(page_to_remove.get_pageid(), page_to_remove.get_data());
        }
        mapId.remove(page_to_remove.get_pageid());
        buffer[removal_page] = null;
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
        Page return_page = mapId.get(pageId);
        if(return_page != null){
            return_page.set_newtime();
            return return_page;
        }

        // Get the page from disk
        Page page_from_disk = StorageManager.decode(schema, pageId);

        //Else map doesn't contain id we find a free page considering at some point in random index a frame can be free
        // due to removal of the page so linear scan O(N) check every index if we have empty page
        for(int i = 0; i < buffer.length; i++){
            if(buffer[i] == null){
                buffer[i] = page_from_disk;
                page_from_disk.set_newtime();
                mapId.put(pageId, page_from_disk);
                return page_from_disk;
            }
        }

        //LRU method:
        //Use System.time comparing the old time (least recently use) vs current time who ever have the largest is LRU
        buffer[lru()] = page_from_disk;
        page_from_disk.set_newtime();
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
        for(int i = 0; i < buffer.length; i++){
            if(buffer[i] == null) {
                Page newEmptyPage = new Page(newPageId, schema);
                buffer[i] = newEmptyPage;
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


     public static Page[] getBuffer() {
        return buffer;
    }



    public static void main(String[] args) throws IOException {
        int bufferSize = 3;
        int pageSize = 256; 
        StorageManager.initializeDatabaseFile("tester", pageSize);
        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //testing getEmptyPage
            //creation
        Page testPage = getEmptyPage(schema);
        boolean hasPassedEmptyPage = true;
        if (testPage == null) { // empty
            System.out.println("hasPassedEmptyPage: False - Null page");
            return;
        }
        int testpageId = testPage.get_pageid();
        int currPagesCreated = StorageManager.getPageCounter();
        if (testpageId < 0 || testpageId >=  currPagesCreated) { //not a valid pageId
            System.out.println("hasPassedEmptyPage: False - invalid pageId");
            return;
        }
        Stack<Integer> free_pages = StorageManager.getFreePages();
        if (free_pages.contains(testpageId)) { //page id given is dead page?
            System.out.println("hasPassedEmptyPage: False - dead pageId");
            return;
        }
        if (mapId.get(testpageId) == null) { //not in buffer
            System.out.println("hasPassedEmptyPage: False - not in buffer");
            return;
        }
        System.out.println("hasPassedEmptyPage: True");
            //fill buffer, use lru,
        boolean passedEvictPage = true;
        Page testPage2 = getEmptyPage(schema);
        Page testPage3 = getEmptyPage(schema);
        //verify buffer full
        if (mapId.size() != 3) {
            System.out.println("Filling buffer failed");
            return;
        }
        Page testEvictPage = getEmptyPage(schema);
            //do page exist
        if (testEvictPage == null) { // empty
            System.out.println("testEvictPageId: False - Null page");
            return;
        }
        int testEvictPageId = testEvictPage.get_pageid();
        currPagesCreated = StorageManager.getPageCounter();
        if (testEvictPageId < 0 || testEvictPageId >=  currPagesCreated) { //not a valid pageId
            System.out.println("testEvictPageId: False - invalid pageId");
            return;
        }
        free_pages = StorageManager.getFreePages();
        if (free_pages.contains(testEvictPageId)) { //page id given is dead page?
            System.out.println("testEvictPageId: False - dead pageId");
            return;
        }
        if (mapId.get(testEvictPageId) == null) { //not in buffer
            System.out.println("testEvictPageId: False - not in buffer");
            return;
        }
            //did buffer and mapId grow
        if (mapId.size() != 3) {
            System.out.println("mapId grew - failed evict"); //!
            System.out.println(mapId.size());
            return;
        }
        if (buffer.length!= 3) {
            System.out.println("buffer grew - failed evict"); //!
            System.out.println(buffer.length);
            return;
        }
            //is testPage gone from mapId and buffer
        if (mapId.get(testpageId) != null) { // in buffer
            System.out.println("testEvictPageId: False - evicted still in mapId");
            return;
        }
        if (Arrays.asList(buffer).contains(testPage))  {
            System.out.println("testEvictPageId: False - evicted still in buffer");
            return;
        }
        System.out.println("testEvictPageId: True");

        //testing - getPage
        Page samePage = getPage(testpageId, schema);
        if (testPage == samePage) {
            System.out.println("getPagePassed: True");
        } else {
            System.out.println("getPagePassed: False");
        }



    }
}
