package Catalog;

import Common.Page;
import java.util.List;
import java.util.Stack;
import Common.Attribute;
import java.util.ArrayList;
import BufferManager.BufferManager;
import StorageManager.StorageManager;

// NOTE: BNODES ARE INTERPRETTED DIFFERENT BY TYPE
// Despite all being in the structure of rows of (ptr,key), ptr for a leaf node points to the page a key appears on.
// For internal nodes, a left ptr functions as normal.

public class BPlus {
    public Schema Schema;
    public Integer Root;
    public Attribute Attribute;

    // We will assume the table we are indexing is empty
    // If the table has data, you need to manually insert it.
    public BPlus(Schema S, Attribute A, Integer Root) throws Exception {
        this.Schema = S;
        this.Attribute = A;

        if (Root == null) {
            Root = StorageManager.CreatePage();

            // Populate first node for use, if creating BTree from scratch.
            Page BNode = BufferManager.getEmptyPage(null, Root);
            BNode.attr = A;
            BNode.bnode = true;
            BNode.leafnode = true;
            BNode.set_isdirty(true);
        }

        this.Root = Root;
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
                if (NotNext = (Key.compareTo(CKey) < 0)) {
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

            if (!NotNext) Current = BufferManager.getBNode(Current.get_next_pageid(), Attribute);
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
            // Update rows in case we are in a deeper iteration,
            Rows = Current.get_data();
            // Now we can insert our value.
            ArrayList<Object> NewRow = new ArrayList<>(List.of(Ptr,Key));

            // Find where to insert it,
            boolean found = false;
            int i = 0; 
            
            // marks found if one is found, i storing the position to insert.
            for (;i<Rows.size(); i++)
            if (found = (((Comparable<Object>) Rows.get(i).get(1)).compareTo(Key) >= 0)) break;

            // Handle different if leaf vs internal:
            if (Current.leafnode) Rows.add((found) ? i : Rows.size(), NewRow);

            // internal:
            else {
                if (!found) {
                    NewRow.set(0, Current.get_next_pageid()); // Rightmost ptr is the next page id
                    Rows.add(NewRow); // Add our row at the end,
                    Current.set_nextpageid(Ptr); // Update next page id to have the new pointer to page (or other bnode)
                } else {
                    // Since we came from a split, our new inserted node's left pointer is the one we came from, 
                    // otherwise known as the left ptr of whats currently at i.
                    ArrayList<Object> Here = Rows.get(i);
                    NewRow.set(0, Here.get(0));
                    // Then the left ptr of what's here will be the right ptr of our new insertion, aka Ptr.
                    Here.set(0, Ptr);
                    // Now we insert it in.
                    Rows.add(i, NewRow);
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
                Left.attr = Current.attr; // Copy attribute,
                Left.leafnode = Current.leafnode; // mark leaf
                Left.set_data(Rows); // Set left to the split page's remaining entries on the left,

                NewRow = new ArrayList<>(List.of(Left.get_pageid(), Key)); // Make a new row, and toss it in
                Rows = new ArrayList<>(List.of(NewRow)); // Create new arraylist for root

                Current.set_data(Rows);
                Current.set_nextpageid(Right.get_pageid()); // Set next page id to right page
                Current.leafnode = false; // mark internal/root.

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
        for (ArrayList<Object> Row : Rows) 

        // If the key is found,
        if (Row.get(1).equals(Key))

        // Return next page pointer, or nextpage pointer if at the end of the rows.
        return (Integer) Row.get(0);

        // Otherwise we know this key is not in the tree, return null.
        return null;
    }

    // Direction: 0 for equal, -1 for less than, 1 for greater than.
    public ArrayList<Integer> Filter(Comparable<Object> Key, int Direction) throws Exception {
        ArrayList<Integer> Result = new ArrayList<Integer>(); //PageIds
        // 0: The two values are equal.
        // Positive Integer (>0): The first value is greater than the second.
        // Negative Integer (<0): The first value is less than the second. 

        // Handle first leaf that we land on.
        Page Leaf = FindLeaf(Key);

        for (ArrayList<Object> Row : Leaf.get_data()) {
            Comparable<Object> CKey = (Comparable<Object>) Row.get(1);
            int C = CKey.compareTo(Key);

            if (C == Direction) Result.add((Integer) Row.get(0));
        }

        // Now that we filtered on this leaf, we need to traverse the others.
        if (Direction > 0) {
            int Next = Leaf.get_next_pageid();
            Leaf = (Next > 0) ? BufferManager.getBNode(Next, Attribute) : null;
            while (Leaf != null) {
                // Add all the pages of this leaf.
                for (ArrayList<Object> Row : Leaf.get_data()) 
                Result.add((Integer) Row.get(0));
                // Determine if next leaf exists.
                Next = Leaf.get_next_pageid();
                Leaf = (Next > 0) ? BufferManager.getBNode(Next, Attribute) : null;
            }
        } else if (Direction < 0) {
            // We need to find the leftmost leaf and traverse right until we hit the original leaf.
            int Target = Leaf.get_pageid();

            Leaf = BufferManager.getBNode(Root, Attribute);
            while (!Leaf.leafnode) {
                int Leftmost = (Integer) Leaf.get_data().get(0).get(0);
                
                // If the leftmost page is the target, we already did the bottomleft leaf, and can quit ahead.
                if (Leftmost == Target) return Result; 

                // Otherwise keep moving down.
                Leaf = BufferManager.getBNode(Leftmost, Attribute);
            }

            // Now that we are the bottomleft leaf, we scan right, adding pages until we hit the original leaf.
            int Next;
            while (true) {
                // Add all pages.
                for (ArrayList<Object> Row : Leaf.get_data())
                Result.add((Integer) Row.get(0));

                Next = Leaf.get_next_pageid();
                if (Next == Target) break; // Break if we are about to move into the original leaf.
                // Otherwise, advance.
                
                Leaf = BufferManager.getBNode(Next, Attribute);
            }
        }

        return Result;
    }

    public void Clear() throws Exception {
        Stack<Integer> PageStack = new Stack<>();

        PageStack.push(Root);

        while (!PageStack.isEmpty()) {
            Page Current = BufferManager.getBNode(PageStack.pop(), Attribute);
            StorageManager.FreePage(Current);

            // Skip rest of entries here if leaf, since they are table pages.
            if (Current.leafnode) continue;

            // Otherwise, we need to add all child pages to the stack to be freed as well
            for (ArrayList<Object> Row : Current.get_data())
            PageStack.push((Integer) Row.get(0));

            // Add rightmost for internal nodes only
            if (!Current.leafnode) PageStack.push(Current.get_next_pageid());
        }
    }

    public void PrintTree() throws Exception {
        System.out.println("==== B+ Tree Structure ====");
        PrintNode(Root, 0);
    }

    private void PrintNode(Integer pageId, int level) throws Exception {
        Page node = BufferManager.getBNode(pageId, Attribute);

        // Indentation for tree structure
        String indent = "  ".repeat(level);

        // Print node header
        System.out.println(indent + (node.leafnode ? "[LEAF]" : "[INTERNAL]") 
                        + " PageID=" + pageId);

        ArrayList<ArrayList<Object>> rows = node.get_data();

        // Print contents
        System.out.print(indent + "Keys: ");
        for (ArrayList<Object> row : rows) {
            System.out.print(row.get(1) + " ");
        }
        System.out.println();

        // Print pointers
        System.out.print(indent + "Ptrs: ");
        for (ArrayList<Object> row : rows) {
            System.out.print(row.get(0) + " ");
        }
        if (!node.leafnode) {
            System.out.print("| Next: " + node.get_next_pageid());
        }
        System.out.println();

        // If internal node, recurse into children
        if (!node.leafnode) {
            // Traverse left children
            for (ArrayList<Object> row : rows) {
                Integer childId = (Integer) row.get(0);
                PrintNode(childId, level + 1);
            }

            // Traverse rightmost child
            Integer rightmost = node.get_next_pageid();
            if (rightmost != null) {
                PrintNode(rightmost, level + 1);
            }
        }
    }
}
