package Catalog;

import Common.Page;
import java.util.Stack;
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

    // Returns the Leaf node that should contain a given key.
    private Page FindLeaf(Comparable<Object> Key) throws Exception {
        // Traverse the tree to the node that should contain the search key, and return it.
        Page Current = BufferManager.getBNode(Root, Attribute);

        while (!Current.leafnode) {
            ArrayList<ArrayList<Object>> Rows = Current.get_data();

            boolean NotNext = false; // Mark if sub-page found, otherwise next page id used.
            for (ArrayList<Object> Row : Rows) {
                Comparable<Object> CKey = (Comparable<Object>) Row.get(1);

                // Update the notnext flag, and check if we found the right child.
                if (NotNext = Key.compareTo(CKey) >= 0) {
                    Current = BufferManager.getBNode((Integer) Row.get(0), Attribute);
                    break;
                }
            }

            if (!NotNext) Current = BufferManager.getPage(Current.get_next_pageid(), this.Schema);
        }

        return Current;
    }

    public void Insert(Comparable<Object> Key, Integer Ptr) throws Exception {
        Page Current = BufferManager.getBNode(Root, Attribute);

        Stack<Integer> PageStack = new Stack<>();

        // Traverse to the leaf node, noting all pages we traverse in the stack as we move.
        while (!Current.leafnode) {
            PageStack.push(Current.get_pageid());
            ArrayList<ArrayList<Object>> Rows = Current.get_data();

            boolean NotNext = false; // Mark if sub-page found, otherwise next page id used.
            for (ArrayList<Object> Row : Rows) {
                Comparable<Object> CKey = (Comparable<Object>) Row.get(1);

                // Update the notnext flag, and check if we found the right child.
                if (NotNext = Key.compareTo(CKey) >= 0) {
                    Current = BufferManager.getBNode((Integer) Row.get(0), Attribute);
                    break;
                }
            }

            if (!NotNext) Current = BufferManager.getPage(Current.get_next_pageid(), this.Schema);
        }

        // Now that Current is on the relevant leaf node,
        // We have to first validate if there is room:
        int N = this.Attribute.GetBNodeN();
        
        ArrayList<ArrayList<Object>> Rows = Current.get_data();

        // First check if the key already exists in here.
        for (ArrayList<Object> Row : Rows)
        if (Row.get(1).equals(Key))
        throw new Exception("Duplicate entry in B+ Tree, cannot insert.");
        
        // Now we know the insertion is unique, so let's insert here.
        if (Rows.size() >= N - 1) {
            // If there is no room, we need to split.
            
        } 
        // Otherwise the leaf has room and we just need to drop it in.
        else {

        }
    }

    // Returns the page id containing the key. Null if not found.
    public Integer Find(Comparable<Object> Key, Page Leaf) throws Exception {
        // If leaf is not given, use FindLeaf, otherwise use what's provided.
        Leaf = (Leaf == null) ? FindLeaf(Key) : Leaf;

        // Get the rows,
        ArrayList<ArrayList<Object>> Rows = Leaf.get_data();

        // Return if key is found in this leaf node,
        // For each row,
        for (int i=0; i<Rows.size(); i++) 

        // If the key is found,
        if (Rows.get(i).get(1).equals(Key))

        // Return next page pointer, or nextpage pointer if at the end of the rows.
        return (i+1 != Rows.size()) ? (Integer) Rows.get(i+1).get(0) : Leaf.get_next_pageid();

        // Otherwise we know this key is not in the tree, return null.
        return null;
    }

    // Direction: 0 for equal, -1 for less than, 1 for greater than.
    public ArrayList<Integer> Filter(Object Key, int Direction) {
        ArrayList<Integer> Result = new ArrayList<Integer>();



        return Result;
    }

    public void Clear() {
        // TODO, traverse all nodes and free their pages.
    }
}
