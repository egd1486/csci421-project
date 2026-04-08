package Catalog;

import Common.Page;
import Common.Attribute;
import java.util.ArrayList;
import BufferManager.BufferManager;
import StorageManager.StorageManager;

public class BPlus {
    Schema Schema;
    Integer Root;
    Attribute Attribute;

    // We will assume the table we are indexing is empty
    // If the table has data, you need to manually insert it.
    public BPlus(Schema S, Attribute A, Integer Root) {
        this.Schema = S;
        this.Attribute = A;
        this.Root = (Root != null) ? Root:StorageManager.CreatePage();
    }

    public void Insert(Object Key) {
        // TODO traverse the tree to insert this search key.
    }

    public Page Find(Object Key) {
        // TODO traverse the tree to find the page that contains this search key.
        return null;
    }

    // Direction: 0 for equal, -1 for less than, 1 for greater than.
    public ArrayList<Integer> Filter(Object Key, int Direction) {
        // TODO traverse the tree to find pages that satisfy the filter condition.
        return null;
    }

    public void Clear() {
        // TODO, traverse all nodes and free their pages.
    }
}
