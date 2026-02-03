package StorageManager;
import Common.*;

class StorageManager {


    // ! how to represent pages in file
    // ! how do we delete/insert pages
    // ! keep track of free/used pages 

    public Page readPage(int pageSize) {
        return new Page(pageSize);
    }

    public void writePage(int pageNumber, byte[] data) {

    }

    public int calculatePageOffset() {
        // pageSize * pageNumber = where does page start
        return 0;
    }
}