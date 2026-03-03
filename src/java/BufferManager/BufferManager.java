package BufferManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import Common.Page;
import Common.Attribute;
import Catalog.*;
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
    public static int lru() throws Exception {
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

        if(page_to_remove.check_dirty()) StorageManager.WritePage(page_to_remove);

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
    public static Page getPage(int pageId, Schema schema) throws Exception {
        //If a map contains the page id then we return page
        Page return_page = mapId.get(pageId);
        if(return_page != null){
            return_page.set_newtime();
            return return_page;
        }

        // Get the page from disk
        Page page_from_disk = StorageManager.ReadPageFromDisk(schema, pageId);

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
    public static Page getEmptyPage(Schema schema, Integer pageId) throws Exception {
        // If a pageid is provided, assume we can empty it. Otherwise, get an available one.
        int newPageId = (pageId == null) ? StorageManager.CreatePage() : pageId;
        //check if have empty slot in buffer to place new empty page
        for(int i = 0; i < buffer.length; i++){
            if(buffer[i] == null) {
                Page newEmptyPage = new Page(newPageId, schema);
                newEmptyPage.set_isdirty(true);
                buffer[i] = newEmptyPage;
                mapId.put(newPageId, newEmptyPage);
                return newEmptyPage;
            }
        }

        //no empty slot in buffer so evict one
        Page newEmptyPage = new Page(newPageId, schema);
        newEmptyPage.set_isdirty(true);
        buffer[lru()] = newEmptyPage;
        mapId.put(newPageId, newEmptyPage);

        return newEmptyPage;
    }

   /**
     * Flush all the dirty pages down into memory and evict them
     * @throws IOException
     */
    public static void flush_all() throws Exception {
        for(int i = 0; i<buffer.length; i++) {
            Page check_page = buffer[i];
            if (check_page != null) {
                if (check_page.check_dirty()) {
                    StorageManager.WritePage(check_page);
                    check_page.set_isdirty(false);
                    //evict
                    mapId.remove(check_page.get_pageid());
                    buffer[i] = null;
                }
            }
        }
    }

      public static void writeSchemas(){
        try {
            ArrayList<ArrayList<Object>> newTable = new ArrayList<ArrayList<Object>>();

            for (Schema table : Catalog.Schemas){
                for(Attribute attr : table.Attributes){
                    ArrayList<Object> newRow = new ArrayList<Object>();
                    newRow.add(table.Name); //SchemaName
                    newRow.add(table.PageId); //StartPage
                    newRow.add(attr.name); //AttributeName
                    newRow.add(attr.type.ordinal()); //Type
                    newRow.add(attr.typeLength); //Length
                    newRow.add(attr.notNull); //NotNull
                    newRow.add(attr.unique); //Unique
                    newRow.add(attr.primaryKey); //Primary
                    newRow.add(attr.defaultVal); //DefaultValue
                    newTable.add(newRow);
                }
            }

            Catalog.Schemas.add(Catalog.AttributeTable);
            Catalog.RemoveSchema(Catalog.AttributeTable.Name);
            
            flush_all();

            Page newEmptyPage = new Page(0, Catalog.AttributeTable);
            buffer[0] = newEmptyPage;
            mapId.put(0, newEmptyPage);
            Catalog.AttributeTable.PageId = 0;
            
            for (ArrayList<Object> row : newTable) Catalog.AttributeTable.Insert(row);

            flush_all();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

     public static Page[] getBuffer() {
        return buffer;
    }
}


