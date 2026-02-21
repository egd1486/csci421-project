package BufferManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
<<<<<<< HEAD
=======
import Common.Type;
>>>>>>> 7393b54afbc8bb2577a6ae0ef82781821d456f5a
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
            StorageManager.writePage(page_to_remove);
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
     * Flush all the dirty pages down into memory and evict them
     * @throws IOException
     */
    public static void flush_all() throws IOException {
        for(int i = 0; i<buffer.length; i++) {
            Page check_page = buffer[i];
            if (check_page != null) {
                if (check_page.check_dirty()) {
                    StorageManager.writePage(check_page);
                    check_page.set_isdirty(false);
                    //evict
                    mapId.remove(check_page.get_pageid());
                    buffer[i] = null;
                }
            }
        }
    }


     public static Page[] getBuffer() {
        return buffer;
    }


    public static void testCreationEmptyPage() throws IOException {
        System.out.println("Start testing Create Empty Page");
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
        Page testPage = getEmptyPage(schema); //id 0
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

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Create Empty Page");
        System.out.println();
    }


    public static void testEvictionEmptyPage() throws Exception {
        System.out.println("Start testing Evict Empty Page");
        int bufferSize = 3;
        int pageSize = 256;

        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //testing getEmptyPage
            //creation
        Page testPage = getEmptyPage(schema); //id 1
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

        //testing EVICTION
            //fill buffer, use lru,
        Page testPage2 = getEmptyPage(schema); //id 2
        Page testPage3 = getEmptyPage(schema);//id 3
            //verify buffer full
        if (mapId.size() != 3) {
            System.out.println("Filling buffer failed");
            System.out.println(mapId.size());
            return;
        }
        Page testEvictPage = getEmptyPage(schema); //id 4
            //do page exist
        if (testEvictPage == null) { // empty
            System.out.println("testEvictPageId: False - Null page");
            return;
        }
        //test eviction now
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
            System.out.println("mapId grew - failed evict");
            System.out.println(mapId.size());
            return;
        }
        if (buffer.length!= 3) {
            System.out.println("buffer grew - failed evict");
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

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Evict Empty Page");
        System.out.println();
    }

    public static void testDirtyEmptyPage() throws Exception {
        System.out.println("Start testing Dirty Empty Page");
        int bufferSize = 3;
        int pageSize = 256;

        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Schema schemaDirty = null;
        try {
            schemaDirty = new Schema("Table2");
        } catch (Exception e) {
            e.printStackTrace();
        }
        schemaDirty.AddAttribute("personId", Type.INT, null, false, true, true, null);
        schemaDirty.AddAttribute("name", Type.VARCHAR, 10, null, false, null, null);
        ArrayList<Object> row = new ArrayList<Object>();
        row.add(1);
        row.add("Frank");
        Page testPage1 = getEmptyPage(schemaDirty); //id 5
        int testPage1Id = testPage1.get_pageid();
        schemaDirty.setPageId(testPage1Id);
        schemaDirty.Insert(row);
        testPage1.set_isdirty(true);

        Page testPage2 = getEmptyPage(schema); // id 6
        Page testPage3 = getEmptyPage(schema); //cuz buffer size 3 - id 7

        //testing dirty page eviction
        Page testPage4 = getEmptyPage(schema); //id 8
            //read back from disk to see if data survived
        Page dirtyPage = StorageManager.decode(schemaDirty, testPage1Id);
        ArrayList<ArrayList<Object>> dirtyData = dirtyPage.get_data();
        System.out.println(dirtyData.get(0)); //[1, Frank]
            //see if still exists in mapId and buffer
        for (Page p: buffer) { //id 5 should be gone
            System.out.println("dirty page still existing?");
            System.out.println(p.get_pageid());
            System.out.println(p.check_dirty());
        }

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Dirty Empty Page");
        System.out.println();
    }

    public static void testGetPage() throws Exception{
        System.out.println("Start testing Get Page");
        int bufferSize = 3;
        int pageSize = 256;

        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //testing getEmptyPage
            //creation
        Page testPage = getEmptyPage(schema); //id 9
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

        //testing EVICTION
            //fill buffer, use lru,
        Page testPage2 = getEmptyPage(schema); //id 10
        Page testPage3 = getEmptyPage(schema); //id 11
            //verify buffer full
        if (mapId.size() != 3) {
            System.out.println("Filling buffer failed");
            System.out.println(mapId.size());
            return;
        }
        Page testEvictPage = getEmptyPage(schema); //id 12
            //do page exist
        if (testEvictPage == null) { // empty
            System.out.println("testEvictPageId: False - Null page");
            return;
        }
        //test eviction now
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
            System.out.println("mapId grew - failed evict");
            System.out.println(mapId.size());
            return;
        }
        if (buffer.length!= 3) {
            System.out.println("buffer grew - failed evict");
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

        Page samePage = getPage(testEvictPageId, schema);
        if (testEvictPage == samePage) {
            System.out.println("getPagePassed: True");
        } else {
            System.out.println("getPagePassed: False");
        }

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Get Page");
        System.out.println();
    }

    public static void testFlushAll() throws Exception {
        System.out.println("Start testing Flush all");
        int bufferSize = 3;
        int pageSize = 256;

        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Schema schemaDirty = null;
        try {
            schemaDirty = new Schema("Table2");
        } catch (Exception e) {
            e.printStackTrace();
        }
        schemaDirty.AddAttribute("personId", Type.INT, null, false, true, true, null);
        schemaDirty.AddAttribute("name", Type.VARCHAR, 10, null, false, null, null);
        ArrayList<Object> row = new ArrayList<Object>();
        row.add(1);
        row.add("Frank");
        Page testPage1 = getEmptyPage(schemaDirty); //id 13
        int testPage1Id = testPage1.get_pageid();
        schemaDirty.setPageId(testPage1Id);
        schemaDirty.Insert(row);
        testPage1.set_isdirty(true);

        Page testPage2 = getEmptyPage(schema); // id 14
        Page testPage3 = getEmptyPage(schema); //cuz buffer size 3 - id 15

        //testing flush all dirty page
        flush_all();
        //buffer and mapId should only have 2 pages
        for (Page p: buffer) { //id 13 should be gone
            System.out.println("dirty page still existing?");
            if (p == null) {
                System.out.println("null page");
            } else {
                System.out.println(p.get_pageid());
                System.out.println(p.check_dirty());
            }
        }
        mapId.forEach((key, value) ->  System.out.println(key + " " + value.check_dirty()));

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Flush all");
        System.out.println();
    }

    public static void testWritingAllTypes() throws Exception {
         System.out.println("Start testing Write all types ");
        int bufferSize = 3;
        int pageSize = 256;

        BufferManager.initialize(bufferSize);
        Schema schema = null;
        try {
            schema = new Schema("Table");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Schema schemaDirty = null;
        try {
            schemaDirty = new Schema("Table2");
        } catch (Exception e) {
            e.printStackTrace();
        }
        schemaDirty.AddAttribute("personId", Type.INT, null, false, true, true, null);
        schemaDirty.AddAttribute("name", Type.VARCHAR, 10, null, false, null, null);
        schemaDirty.AddAttribute("isHuman", Type.BOOLEAN, null, null, false, null, true);
        schemaDirty.AddAttribute("areaCode", Type.CHAR, 5, null, false, true, null);
        schemaDirty.AddAttribute("pay", Type.DOUBLE, null, false, false, null, null);
        ArrayList<Object> row = new ArrayList<Object>();
        row.add(1);
        row.add("Frank");
        row.add(false);
        row.add("19901");
        row.add(14.29);
        Page testPage1 = getEmptyPage(schemaDirty); //id 16
        int testPage1Id = testPage1.get_pageid();
        schemaDirty.setPageId(testPage1Id);
        schemaDirty.Insert(row);
        System.out.println("INSERTED ROW");
        //add more data
        ArrayList<Object> row2= new ArrayList<Object>();
        row2.add(2);
        row2.add("Bob");
        row2.add(true);
        row2.add("000");
        row2.add(null);
        schemaDirty.Insert(row2);
        System.out.println("INSERTED ROW2");

        testPage1.set_isdirty(true);

        Page testPage2 = getEmptyPage(schema); // id 17
        Page testPage3 = getEmptyPage(schema); //cuz buffer size 3 - id 18

        //testing dirty page eviction
         System.out.println("HERE before");
        Page testPage4 = getEmptyPage(schema); //id 19
            //read back from disk to see if data survived
             System.out.println("HERE after");
        Page dirtyPage = StorageManager.decode(schemaDirty, testPage1Id);
        // System.out.println("HERE");
        ArrayList<ArrayList<Object>> dirtyData = dirtyPage.get_data();
        // System.out.println("HERE");
        System.out.println(dirtyData.get(0)); //[1, Frank]
            //see if still exists in mapId and buffer
        for (Page p: buffer) { //id 16 should be gone
            System.out.println("dirty page still existing?");
            System.out.println(p.get_pageid());
            System.out.println(p.check_dirty());
        }

        //reset
        mapId = new HashMap<>();
        buffer = new Page[buffer.length];
        System.out.println("End testing Write all types");
        System.out.println();
    }

    public static void testCleanPageEvict() {

    }

    public static void writeSchemas(){
        try {
            Page current = getPage(1, Catalog.AttributeTable);
            Page next;
            if (current.get_next_pageid() != -1){
                next = getPage(current.get_next_pageid(), Catalog.AttributeTable); 
            }
            else{
                next = getEmptyPage(Catalog.AttributeTable);
            }
            current.set_freebytes(0);
            current.set_data(new ArrayList<ArrayList<Object>>());
            for (Schema table : Catalog.Schemas){
                for(Attribute attr : table.Attributes){
                    if(current.get_slots_remaining()>0){
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
                        ArrayList<ArrayList<Object>> newTable = current.get_data();
                        newTable.add(newRow);
                        current.set_data(newTable);
                    }
                    else{
                        current = next;
                        if (current.get_next_pageid() != -1){
                            next = getPage(current.get_next_pageid(), Catalog.AttributeTable); 
                        }
                        else{
                            next = getEmptyPage(Catalog.AttributeTable);
                        }
                        current.set_data(new ArrayList<ArrayList<Object>>());
                        current.set_freebytes(0);

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
                        ArrayList<ArrayList<Object>> newTable = current.get_data();
                        newTable.add(newRow);
                        current.set_data(newTable);
                    }
                }
            }
        }
        catch (Exception e){
            System.err.println(e);
        }
    }



    public static void main(String[] args) throws Exception {
        testCreationEmptyPage();
        testEvictionEmptyPage();
        testDirtyEmptyPage();
        testGetPage();
        testFlushAll();
        // testWritingAllTypes();

        // testCleanPageEvict();




    }
}


