package BufferManager;

import Common.*;

public class Frame {
    int pageId;
    Page page;
    boolean is_dirty = false;
    int counter;

    public Frame(int pageId, Page page){
        this.pageId = pageId;
        this.page = page;
        is_dirty = false;
        counter = 0;
    }
}
