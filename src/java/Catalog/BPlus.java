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
                if (NotNext = Key.compareTo(CKey) < 0) {
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
                if (NotNext = Key.compareTo(CKey) < 0) {
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
        
        // Inserting might pop values back up the tree, so let's iterate to prepare for that.
        while (true) {
            ArrayList<Object> NewRow = new ArrayList<>();
            NewRow.add(0);
            NewRow.add(Key);

            // Find where to insert it,
            int i = 0; 
            for (;i++<Rows.size();)
            if (((Comparable<Object>) Rows.get(i).get(1)).compareTo(Key) >= 0) break;

            // In the case i is less than the size, we know it found an entry higher than it.
            // Meaning, we can insert inside, instead of adjusting the next_page ptr.
            if (i < Rows.size()) {
                Rows.get(i).set(0, Ptr); // Replace pointer right of our insertion to be greater than
                NewRow.set(0, (i>0) ? Rows.get(i-1).get(0) : 0); // Update left pointer of new insertion to be less, or 0 if n/a
                Rows.add(i, NewRow); // Insert new entry.
            }

            // Otherwise, we know our key is larger than all others in this node.
            else {
                // If dealing with a leaf node, we know the rightmost pointer must still point at the next leaf,
                // So inserting shouldn't affect any pointers in that case.

                // Otherwise, we are dealing with a root or internal node, which does require updates.
                if (!Current.leafnode) {
                    NewRow.set(0, Current.get_next_pageid()); // Rightmost ptr is the next page id
                    Rows.add(NewRow); // Add our row at the end,
                    Current.set_nextpageid(Ptr); // Update next page id to have the new pointer to page (or other bnode)
                }

            }

            // If the node is now overfull, we need to determine splitting logic.
            if (Rows.size() >= N)
            // If working on the root,
            if (Current.get_pageid() == this.Root) {
                // We then need to split the contents of the root into two new leaves.
                Page Right = Current.split_page(false); // Make right leaf.
                // Grab the key the root turns into:
                Key = (Comparable<Object>) Right.get_data().get(0).get(1); // Get middle key,
                Ptr = Right.get_pageid(); // Get right page id,
                // Create a left page so we can repurpose the root without replacing our number.
                Page Left = BufferManager.getEmptyPage(null, null);
                Left.bnode = true; // mark bnode
                Left.leafnode = Current.leafnode; // mark leaf
                Left.set_data(Rows); // Set left to the split page's remaining entries on the left,

                Rows = new ArrayList<>(); // Create new arraylist for root
                NewRow = new ArrayList<>(); // Make a new row, and toss it in
                NewRow.add(Left.get_pageid()); // Add left page id for less than ptr,
                NewRow.add(Key); // Add middle key,
                Rows.add(NewRow);

                Current.set_data(Rows);
                Current.set_nextpageid(Right.get_pageid()); // Set next page id to right page

                break; // Break as we successfully split the root.
            }
            // Otherwise, we are on leaf or internal.
            else {
                // If working with a leaf node or internal node and breaking maximum,

                // Leaf splits into two, then we must insert middle value with ptr on right page.
                Page Left = Current, Right = Current.split_page(false);
                Right.bnode = true; // mark bnode
                Right.leafnode = Left.leafnode; // mark leaf if applicable,

                // If working with an internal node, we need to remove the "pushed up" key.
                if (!Current.leafnode) Right.get_data().remove(0);

                Key = (Comparable<Object>) Right.get_data().get(0).get(1); // Get middle key,
                Ptr = Right.get_pageid(); // Get right page id,
                Current = BufferManager.getBNode(PageStack.pop(), this.Attribute); // Set current to parent page,
                continue; // Repeat loop to insert this into parent.
            } 

            // Otherwise the leaf has room and we just dropped it in.
            // Break out of the loop as our work is done.
            break;
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
