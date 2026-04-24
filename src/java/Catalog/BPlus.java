package Catalog;

import StorageManager.StorageManager;
import BufferManager.BufferManager;
import java.util.ArrayList;
import java.util.HashSet;
import Common.Attribute;
import java.util.Stack;
import java.util.List;
import java.util.Set;
import Common.Page;


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

    // For handling splitting in Insert.
    public class SplitResult {
        public Comparable<Object> PromotedKey;
        public Page RightPage;

        public SplitResult(Comparable<Object> PromotedKey, Page RightPage) {
            this.PromotedKey = PromotedKey;
            this.RightPage = RightPage;
        }
    }

    private SplitResult SplitInternal(Page Left) throws Exception {
        if (Left.leafnode) throw new Exception("Cannot internal split a leaf.");

        // Get the index of the new page, and of the current next page.
        int OldNext = Left.get_next_pageid(), Next = StorageManager.CreatePage();

        // Get the new page,
        Page Right = BufferManager.getEmptyPage(this.Schema, Next);

        // Grab data and its size,
        ArrayList<ArrayList<Object>> Data = Left.get_data(), RightData, LeftData;
        RightData = Right.get_data();
        LeftData = new ArrayList<>();
        int Size = Data.size();

        // Grab index of key to promote,
        int Mid = Size / 2;

        // Set promoted key,
        Comparable<Object> PromotedKey = (Comparable<Object>) Data.get(Mid).get(1);

        // Match type of new page to older page.
        Right.attr = this.Attribute;
        Right.bnode = true;
        Right.leafnode = false;

        // Copy over rightmost ptr,
        Right.set_nextpageid(OldNext);

        // Copy past divider 
        for (int i=Mid+1; i<Size; i++) RightData.add(Data.get(i));
        // Copy before divider
        for (int i=0; i<Mid; i++) LeftData.add(Data.get(i));

        // Left page's rightmost ptr becomes middle ptr
        Left.set_nextpageid((int) Data.get(Mid).get(0));
        // Update left's data
        Left.set_data(LeftData);

        // Mark dirty
        Left.set_isdirty(true);
        Right.set_isdirty(true);

        return new SplitResult(PromotedKey, Right);
    }

    private SplitResult SplitLeaf(Page Left) throws Exception {
        if (!Left.leafnode) throw new Exception("Cannot leaf split a non leaf.");

        int OldNext = Left.get_next_pageid(), Next = StorageManager.CreatePage();

        // Get the new page,
        Page Right = BufferManager.getEmptyPage(this.Schema, Next);

        // Set up right's properties
        Right.bnode = true;
        Right.leafnode = true;
        Right.attr = this.Attribute;

        // Grab the data, size, and midpoint
        ArrayList<ArrayList<Object>> Data = Left.get_data();
        int Size = Data.size();
        int Mid = Size / 2;

        // Prepare left and right halves
        ArrayList<ArrayList<Object>> LeftData = new ArrayList<>(), RightData = Right.get_data();

        // Add before mid point to left,
        for (int i = 0; i < Mid; i++) LeftData.add(Data.get(i));
            
        // Add at and after mid point to right
        for (int i = Mid; i < Size; i++) RightData.add(Data.get(i));
            

        // Maintain the leaf chain
        Left.set_nextpageid(Next);
        Right.set_nextpageid(OldNext);

        // Replace left data
        Left.set_data(LeftData);

        // Promoted key = first key of right leaf
        Comparable<Object> PromotedKey = (Comparable<Object>) RightData.get(0).get(1);

        // Mark dirty,
        Left.set_isdirty(true);
        Right.set_isdirty(true);

        return new SplitResult(PromotedKey, Right);
    }

    // Returns the Leaf node that should contain a given key.
    private Page FindLeaf(Comparable<Object> Key) throws Exception {
        // Traverse the tree to the node that should contain the search key, and return it.
        Page Current = BufferManager.getBNode(Root, Attribute);

        while (!Current.leafnode) {
            // Search if next move is within the rows, or to the right
            int Index = Current.bsearch_page(Key, 1);
            // Grab rows for size check and possibly grabbing a row.
            ArrayList<ArrayList<Object>> Rows = Current.get_data();
            // If the index is at the size, we need to move right.
            // Otherwise, use the pointer of what we're looking at.
            Index = (Index==Rows.size()) ? Current.get_next_pageid():(int) Rows.get(Index).get(0);
            // Move to that node.
            Current = BufferManager.getBNode(Index, Attribute);
        }

        return Current;
    }

    // Returns the page a key would be inserted into.
    public Integer FindHome(Comparable<Object> Key, boolean AllowDuplicate) throws Exception {
        // Find the leaf node where this key should be, and search for your page within it.
        Page Leaf = FindLeaf(Key);

        ArrayList<ArrayList<Object>> Rows = Leaf.get_data();

        if (Rows.isEmpty()) return null; // If there are no entries, return null.
        // let insert handle the rest there, since this table is empty in that case.

        // Otherwise, we need to find the right page for this key.
        // Reuse our binary search logic
        int Index = Leaf.bsearch_page(Key, 1);

        // If the key is largest in the leaf, it will go out of bounds, so let's clamp it.
        ArrayList<Object> Entry = Rows.get((Index == Rows.size()) ? Rows.size()-1 : Index);

        // Validate if duplicate, and if allowed,
        if (!AllowDuplicate && Entry.get(1).equals(Key)) 
        throw new Exception("Duplicate entry in B+ Tree.");

        // Otherwise, return the position as expected.
        return (Integer) Entry.get(0);
    }

    // Method for updating entries from a split page WITHOUT directly splitting.
    public void UpdateOnSplit(Page P) throws Exception {
        int Index = this.Schema.Attributes.indexOf(this.Attribute), Ptr = P.get_pageid();

        // Update the pointers of all the rows in the new page to point to it.
        // Build set so we can track which page ids we have to update.
        Set<Object> PageIds = new HashSet<>();

        // Add the the keys to the set,
        ArrayList<ArrayList<Object>> Rows = P.get_data();
        for (ArrayList<Object> Row : Rows)
            PageIds.add(Row.get(Index));

        // Get least key to find left-most leaf.
        Object Key = Rows.get(0).get(Index);

        // Get left-most leaf,
        Page Leaf = FindLeaf((Comparable<Object>) Key);

        // Now, we need to systematically update entries from the set until it's empty.
        while (!PageIds.isEmpty() && Leaf != null) {
            int Size = PageIds.size(), Next = Leaf.get_next_pageid();

            // For each ptr,key
            for (ArrayList<Object> Row : Leaf.get_data())
                // Try removing it from the set, if succeeds,
                if (PageIds.remove(Row.get(1)))
                    // Then we need to update it to ptr.
                    Row.set(0, Ptr);

            // Mark leaf dirty if anything was removed (which updated leaf)
            if (PageIds.size() != Size) Leaf.set_isdirty(true);

            // Move to next leaf, if anything is remaining.
            Leaf = (Next > 0) ? BufferManager.getBNode(Leaf.get_next_pageid(), Attribute) : null;
        }

        if (!PageIds.isEmpty()) throw new Exception("Not all page ids were updated?");
    }

    public void Insert(Comparable<Object> Key, Integer Ptr) throws Exception {
        Page Current = BufferManager.getBNode(Root, Attribute);

        Stack<Integer> PageStack = new Stack<>();

        // Traverse to the leaf node, noting all pages we traverse in the stack as we move.
        while (!Current.leafnode) {
            PageStack.push(Current.get_pageid());

            // Search for sub-page
            int Index = Current.bsearch_page(Key, 1);

            // Grab rows for indexing
            ArrayList<ArrayList<Object>> Rows = Current.get_data();

            // Define next index from ptr, or next_page if out of bounds:
            Index = (Index!=Rows.size()) ?(int) Rows.get(Index).get(0) : Current.get_next_pageid();

            // Traverse to next page, using next_page if out of bounds:
            Current = BufferManager.getBNode(Index, Attribute);
        }

        // Now that Current is on the relevant leaf node,
        // We have to first validate if there is room:
        int N = this.Attribute.GetBNodeN();
        
        ArrayList<ArrayList<Object>> Rows = Current.get_data();

        // First check if the key already exists in here.
        int Index = Current.bsearch_page(Key, 1);
        if (Index != Rows.size() && Rows.get(Index).get(1).equals(Key))
        throw new Exception("Duplicate entry in B+ Tree, cannot insert.");
        
        // Inserting might pop values back up the tree, so let's iterate to prepare for that.
        while (true) {
            // Update rows in case we are in a deeper iteration,
            Rows = Current.get_data();
            // Now we can insert our value.
            ArrayList<Object> NewRow = new ArrayList<>(List.of(Ptr,Key));

            // Find where to insert it,
            Index = Current.bsearch_page(Key, 1);

            // Handle different if leaf vs internal:
            if (Current.leafnode) Rows.add(Index, NewRow);

            // internal:
            else {
                if (Index == Rows.size()) {
                    NewRow.set(0, Current.get_next_pageid()); // Rightmost ptr is the next page id
                    Current.set_nextpageid(Ptr); // Update next page id to have the new pointer to page (or other bnode)
                } else {
                    // Since we came from a split, our new inserted node's left pointer is the one we came from, 
                    // otherwise known as the left ptr of whats currently at i.
                    ArrayList<Object> Here = Rows.get(Index);
                    NewRow.set(0, Here.get(0));
                    // Then the left ptr of what's here will be the right ptr of our new insertion, aka Ptr.
                    Here.set(0, Ptr);
                }
                // Now we insert it in.
                Rows.add(Index, NewRow);
            }

            // Mark current page dirty.
            Current.set_isdirty(true);

            // If the node is now overfull, we need to determine splitting logic.
            if (Rows.size() >= N)
            // If working on the root,
            if (Current.get_pageid() == this.Root) {
                // Split according to node type,
                SplitResult split = (Current.leafnode) ? SplitLeaf(Current) : SplitInternal(Current);

                Comparable<Object> Promoted = split.PromotedKey;
                Page Right = split.RightPage;

                // Create left child (copy of old root)
                Page Left = BufferManager.getEmptyPage(null, null);
                Left.bnode = true;
                Left.leafnode = Current.leafnode;
                Left.attr = this.Attribute;
                Left.set_data(Current.get_data());
                Left.set_nextpageid(Current.get_next_pageid());
                Left.set_isdirty(true);

                // Repurpose root as new internal node
                ArrayList<ArrayList<Object>> rootRows = new ArrayList<>();
                rootRows.add(new ArrayList<>(List.of(Left.get_pageid(), Promoted)));

                // Fix the current node to be the proper root again.
                Current.set_data(rootRows);
                Current.set_nextpageid(Right.get_pageid());
                Current.leafnode = false;
                Current.bnode = true;
                Current.set_isdirty(true);

                break;
            }
            // Otherwise, we are on leaf or internal.
            else {
                // If working with a leaf node or internal node and breaking maximum,
                SplitResult Split = (Current.leafnode) ? SplitLeaf(Current) : SplitInternal(Current); // Split leaf or internal node.

                // Update key and ptr.
                Key = Split.PromotedKey;
                Ptr = Split.RightPage.get_pageid();

                // Set current to parent page for promotion,
                Current = BufferManager.getBNode(PageStack.pop(), this.Attribute); 

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
}
