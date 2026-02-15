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

    private final Frame[] buffer;
    private final Map<Integer, Frame> mapId = new HashMap<>();
    private final StorageManager storageManager; // What database we editing

    public BufferManager(int set_buffer_size, StorageManager storageManager) {
        this.storageManager = storageManager;
        buffer = new Frame[set_buffer_size];
        BufferSize = set_buffer_size;
    }

    /**
     * Load page from disk
     * @param pageId who we readin
     * @return what we readin
     */
    public Page loadPagefromDisk(int pageId){
        try(RandomAccessFile db = new RandomAccessFile(storageManager.getFilename(), "r")){
            int index = storageManager.getPageSize() * pageId;
            byte[] data = new byte[storageManager.getPageSize()];
            db.seek(index);
            db.readFully(data); //what we're reading :)

            return new Page(pageId, data);
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * we're writing page to disk
     * @param page we gotta write this shyte to the disk
     *
     */
    public void writePage(Page page){
        try(RandomAccessFile db = new RandomAccessFile(storageManager.getFilename(), "rw")) {
            int index = storageManager.getPageSize() * page.getPageId(); //Without this we're always writing the beginning
            db.seek(index);
            db.write(page.getPageData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * LRU remove the frame who have the smallest counter and return the frame to modify
     * @return page with the smallest counter to evict
     */
    public Frame lru(){
        int least_recent = -1;
        for(Frame frame : buffer){
            if(frame.counter < least_recent || least_recent == -1){
                least_recent = frame.counter;
            }
        }
        Frame page_to_remove = buffer[least_recent];
        if(page_to_remove.is_dirty){
            writePage(page_to_remove.page);
        }
        mapId.remove(page_to_remove.pageId);
        return page_to_remove;
    }

    /**
     * GetPage checks if page is already in buffer
     * if not find empty frame or evict LRU frame
     * if evicted frame is dirty write back to disk and load new page into frame
     * update map and return page
     * @return page
     */
    public Page getPage(int pageId){

        //If a map contains the page id then we return page
        if(mapId.containsKey(pageId)){
            Frame frame = mapId.get(pageId);
            frame.counter = frame.counter + 1;
            return frame.page;
        }

        //Else map doesn't contain id we find a freeframe considering at some point in random index a frame can be free
        //due to removal of the page so linear scan O(N) check every index if we have empty page
        Frame frame = null;
        for(Frame check_frame : buffer){
            if(check_frame.page == null){
                frame = check_frame;
                break;
            }
        }

        //If the frame is full we do LRU method
        //LRU: Everytime we use a frame we increment by 1 the smallest number is LRU
        //This isn't optimal for a long use case because at some point we will go pass the size of a long
        //LRU another method could use:
        //Use System.time comparing the old time (least recently use) vs current time who ever have the largest is LRU
        if(frame == null){
            frame = lru();
        }

//        Now if the frame is dirty as hell we put it back to the disk and load new page into frame
//        TODO Create a Page Object and load page from disk
        Page page = loadPagefromDisk(pageId);
        frame.pageId = pageId;
        frame.page = page;
        frame.is_dirty = false;
        frame.counter = frame.counter + 1;
        mapId.put(pageId, frame);
        return page;

    }


}
