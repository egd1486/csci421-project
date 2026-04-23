package Catalog;

import Common.WhereTree.InterfaceOperandNode;
import Common.WhereTree.WhereClassInterface;
import java.nio.charset.StandardCharsets;
import StorageManager.StorageManager;
import Common.WhereTree.WhereResult;
import BufferManager.BufferManager;
import java.util.ArrayList;

import javax.xml.crypto.Data;

import org.w3c.dom.Attr;

import Common.*;


public class Schema {
    public String Name;
    public Integer Primary;
    public Integer PageId;
    public Boolean DuplicateKeys = false;
    public ArrayList<Common.Attribute> Attributes;
    public BPlus Index;

    public Schema(String Name) throws Exception {
        // Check if name is alphanumeric
        if (!isAlphaNumeric(Name))
        throw new Exception("Schema name contains non-alphanumeric characters");
        // Otherwise, proceed.
        this.Attributes = new ArrayList<>();
        this.Name = Name;
        this.Index = null;
    }

    // COPIES SCHEMA BUT NOT DATA
    public Schema Copy(WhereResult Where, InterfaceOperandNode Update, String ColumnName) throws Exception {
        Schema newSchema = new Schema(this.Name);
        newSchema.Primary = this.Primary;
        newSchema.DuplicateKeys = this.DuplicateKeys;
        newSchema.PageId = BufferManager.getEmptyPage(newSchema, null).get_pageid();

        for (Attribute A : this.Attributes) newSchema.Attributes.add(A);
        
        // Set up B+ tree
        if (Parser.Indexing) {
            Attribute PrimaryA = newSchema.Attributes.get(newSchema.Primary);
            BPlus B = new BPlus(newSchema, PrimaryA, null);
            PrimaryA.bTree = B.Root; //so attribute knows where tree is (location)
            newSchema.Index = B;
            for (Attribute A : newSchema.Attributes){
                if(!A.primaryKey && A.unique) {
                    BPlus tree = new BPlus(newSchema, A, null);
                    A.bTree = tree.Root;
                    A.BPlusTree = tree;
                }
                else {
                    A.bTree = null;
                    A.BPlusTree = null;
                }
            }
        }

        // If a where clause was given, we are applying a filter to the existing data
        // If an Update parameter was given, we know we are updating specific columns if the condition is met,
        // If Update is not provided, we assume we are deleting, and not inserting rows that match the Where
        boolean Updating = Update != null;
        boolean Condition = Where != null;
        try {
            if (Condition || Updating) {
                // Let's grab what index we're updating first, (if we are updating)
                int UpdateIndex = -1;
                if (Updating) {
                    for (int i = 0; i < this.Attributes.size(); i++)
                    if (this.Attributes.get(i).name.equals(ColumnName.toUpperCase())) UpdateIndex = i;

                    if (UpdateIndex == -1) throw new Exception("Schema does not have column named " + ColumnName);
                }
                // Getting first page where this schema's data is stored
                int currPageId = this.PageId;
                // Getting all row data from this schema starting from the first
                // page and then any subsequent pages
                while(currPageId != -1){
                    boolean Passes = false;
                    Page page = BufferManager.getPage(currPageId, this);
                    if(page == null) break;
                    // For each row on this page,
                    for (ArrayList<Object> row : page.get_data())
                    // If there is a condition, 
                    if (Condition) {
                        // Define if the row passes the condition
                        Passes = Where.WhereNode.evaluate(row, this);
                        // And we are not updating, we know we delete. If not passing the condition, we wont delete it, so insert.
                        if (!Updating && !Passes) 
                        newSchema.Insert(row);

                        else // Otherwise, when we are updating, and every row gets inserted, but those who pass get altered,
                        if (Updating) {
                            if (Passes) row.set(UpdateIndex, Update.evaluate(row));
                            newSchema.Insert(row);
                        }
                    } // We also need to consider updating with no condition,
                    else if (Updating) {
                        row.set(UpdateIndex, Update.evaluate(row)); // Everything gets altered.
                        newSchema.Insert(row);
                    }

                    currPageId = page.get_next_pageid();
                }
            }
        } catch (Exception e) {
            // Clean up B+ tree
            if (newSchema.Index != null) {
                newSchema.Index.Clear();
            }
            for (Attribute Attr : newSchema.Attributes)
                if (Attr.BPlusTree != null) Attr.BPlusTree.Clear();
            // Clean up pages
            int currPageId = newSchema.PageId;
            while (currPageId != -1) {
                Page pageToFree = BufferManager.getPage(currPageId, newSchema);
                int nextPageId = pageToFree.get_next_pageid();
                currPageId = nextPageId;
                StorageManager.FreePage(pageToFree);
            }
            throw new Exception("Copy failed");
        }
        return newSchema;
    }

    public Attribute AddAttribute(String Name, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique, Object Default, boolean Override) throws Exception {
        if (Primary != null && Primary) // If the attribute should be primary,
        // If we already have a primary key, throw.
        if (this.Primary != null) throw new Exception("Schema already has a primary key");
        // Otherwise, handle it.
        else this.Primary = Attributes.size();
        // Force uppercase
        Name = Name.toUpperCase();

        if (!Override) {
            // Check if Attribute is alphanumeric
            if (!isAlphaNumeric(Name)) 
            throw new Exception("Attribute name " + Name + " contains non-alphanumeric characters");
        } //else Override so skip check


        // Iterate over attributes to see if name is in use.
        for (Attribute A : Attributes) 
        if (A.name.equals(Name)) 
        throw new Exception("Schema already has an attribute named " + Name);

        boolean isPrimary  = Primary  != null && Primary;
        boolean isNullable = Nullable != null && Nullable;
        boolean isUnique   = Unique   != null && Unique;
        
        Attribute A = new Attribute(Name, T, Size, isPrimary, isNullable, isUnique, Default);
        // Attribute A = new Attribute(Name, T, Size, Primary, Nullable, Unique, Default);
        if(!isPrimary && isUnique && Parser.Indexing) {
            A.BPlusTree = new BPlus(this, A, null);
            A.bTree = A.BPlusTree.Root;  
        }
        else{
            A.BPlusTree = null;
            A.bTree = null;
        }

        Attributes.add(A);
        return A;
    }

    public void RemoveAttribute(String Name) throws Exception {
        // Force uppercase
        Name = Name.toUpperCase();

        for (Attribute A : Attributes) 
        // If the name is found,
        if (A.name.equals(Name))
        // And the attribute is not a primary key, toss it.
        if (!A.primaryKey && this.Attributes.remove(A)) return;
        // Otherwise, we have to abort.
        else throw new Exception("Cannot remove primary key");

        // If we didn't find any matches..
        throw new Exception("Schema does not have an attribute named " + Name);
    }

    public void Validate() throws Exception {
        if (this.Primary == null) // Force schema to have a primary key.
        throw new Exception("Schema does not have a primary key");
    }

    private static boolean isAlphaNumeric(String S) {return S.matches("[a-zA-Z0-9]+");}

    public void setName(String name){
        this.Name = name;
    }

    public Integer GetFixedSize() {
        int Size = 0;

        for (Attribute A : Attributes) Size += A.GetFixedSize();

        return Size;
    }

    // Returns the maximum size of the row data in bytes.
    public Integer GetRowByteSize(ArrayList<Object> Row) {
        int Size = ( (Attributes.size() + 7) / 8); // Null bitmap

        for (int i = 0; i < Row.size(); i++) {
            Attribute A = Attributes.get(i);
            Object Value = Row.get(i);

            if (Value == null) continue; // Skip nulls,

            // Add fixed size
            Size += A.GetFixedSize();

            // If varchar add literal size to stack, as fixed is just pointer and length
            if (A.type == Type.VARCHAR) Size += Value.toString().getBytes(StandardCharsets.UTF_8).length;
        }
        
        return Size;
    }

    public Type[] GetTypes() {
        Type[] types = new Type[Attributes.size()];
        for (int i = 0; i < Attributes.size(); i++) types[i] = Attributes.get(i).type;
        return types;
    }

    // Gets all row data from the table specified by this schema.
    // Returns a list of lists where each inner list represents a row
    // containing the row's data.
    public ArrayList<ArrayList<Object>> Select() {
        ArrayList<ArrayList<Object>> entries = new ArrayList<ArrayList<Object>>();

        Object[] defaults = new Object[this.Attributes.size()];
        for (int i=0; i<this.Attributes.size(); i++) 
        if (this.Attributes.get(i).defaultVal != null)
        defaults[i] = this.Attributes.get(i).defaultVal;

        try{
            // Getting first page where this schema's data is stored
            int currPageId = this.PageId;
            // Getting all row data from this schema starting from the first
            // page and then any subsequent pages
            while(currPageId != -1){
                Page page = BufferManager.getPage(currPageId, this);
                if(page == null) break;

                // Set default values
                ArrayList<ArrayList<Object>> pageData = page.get_data();
                for (ArrayList<Object> row : pageData)
                for (int i=0; i<row.size(); i++)
                // if there is a default, use it wheen the value is null
                if (row.get(i) == null && defaults[i] != null)
                row.set(i, defaults[i]);
                    
                entries.addAll(pageData);
                currPageId = page.get_next_pageid();
            }
        } catch (Exception e){
            System.out.println("Error: " + e);
        }
        return entries;
    }

    // Displays a table in an easy to read format
    public void DisplayTable(WhereClassInterface WhereTree, ArrayList<String> Columns){
        // store the position of each requested column in the schema, ex [0, 2]
        ArrayList<Integer> ColIndices = new ArrayList<>(); 
        if (Columns.isEmpty()) { //All columns
            for(int i = 0; i<Attributes.size(); i++) {
                ColIndices.add(i);
            }
        } else { //Some Columns
            //get indices for the requested columns 
            for(String col : Columns) {
                // loop through the schema's attributes to find where the column lives
                for (int i = 0; i < this.Attributes.size(); i++) {
                    String attrName = this.Attributes.get(i).name;
                    String colName = "";
                    // check if attribute already has table prefix from cartesian
                    // if so match full name of TABLE.COL
                    if (attrName.contains(".")) { 
                        colName = col;
                    } else { 
                        //single table
                        //strip table prefix if present ex "TABLE1.COL1" -> "COL1"
                        colName = col.contains(".") ? col.split("\\.")[1] : col;
                    }

                    if (this.Attributes.get(i).name.contains(colName.toUpperCase())) {
                        ColIndices.add(i);
                        break; // found, stop loop
                    }
                }
            }
        }

        //build look up array of default values for each column - use when value is null
        Object[] defaults = new Object[this.Attributes.size()];
        for (int i=0; i<this.Attributes.size(); i++) 
        if (this.Attributes.get(i).defaultVal != null)
        defaults[i] = this.Attributes.get(i).defaultVal;

        int RowCount = 0;
        int PageCount = 1; // For printing each page title.

        try{
            // Getting first page where this schema's data is stored
            int currPageId = this.PageId;
            // Getting all row data from this schema starting from the first
            // page and then any subsequent pages
            while(currPageId != -1){
                Page page = BufferManager.getPage(currPageId, this);
                if(page == null) break;

                // Grab the page data,
                ArrayList<ArrayList<Object>> pageData = page.get_data();

                // Filter the rows based on the Where 
                ArrayList<ArrayList<Object>> filteredRows = new ArrayList<>(); 
                for (ArrayList<Object> row : pageData) {
                    if(WhereTree == null || WhereTree.evaluate(row, this)){
                        filteredRows.add(row);
                    }
                }
                if(filteredRows.isEmpty()) {
                    currPageId = page.get_next_pageid();
                    continue; //don't print header because it has no data to show
                }
                
                // Define the column size for formating,
                int numAttributes = ColIndices.size();
                int[] columnWidths = new int[numAttributes];
                // Set minimum width as the length of the attribute name
                for(int i = 0; i < numAttributes; i++) columnWidths[i] = this.Attributes.get(ColIndices.get(i)).name.length();
                // Then find the longest attribute there, and set its length instead.
                for(ArrayList<Object> row : filteredRows)
                for(int i = 0; i < numAttributes; i++){
                    Object value = row.get(ColIndices.get(i));
                    if(value == null) value = "NULL";
                    columnWidths[i] = Math.max(columnWidths[i], value.toString().length());
                }
                
                // Printing header (page # + attribute names + separator)
                // calculate total dash padding needed for the separator
                int dashes = 1;
                for(int width : columnWidths) dashes += width + 3;
                // page # time
                String Title = " [Page " + PageCount++ + "] (Page " + currPageId +")";
                for(int i = 0; i < dashes; i++) System.out.print("-");
                System.out.print(Title);
                System.out.println();
                // names
                System.out.print("|");
                for(int i = 0; i < numAttributes; i++)
                System.out.printf(" %-" + columnWidths[i] + "s |", this.Attributes.get(ColIndices.get(i)).name);
                System.out.println();
                // separator
                for(int i = 0; i < dashes; i++) System.out.print("-");
                System.out.println();

                // Now print the rows.
                for (ArrayList<Object> row : filteredRows) {
                    System.out.print("|");
                    RowCount++;
                    for (int i=0; i<ColIndices.size(); i++) {
                        Object value = row.get(ColIndices.get(i));

                        // If value is null,
                        if(value == null)
                            // And there's a default, use it.
                            if (defaults[ColIndices.get(i)] != null) value = defaults[ColIndices.get(i)];
                                // Otherwise..
                            else value = "NULL";

                        System.out.printf(" %-" + columnWidths[i] + "s |", value.toString());
                    }
                    System.out.println();
                }
                currPageId = page.get_next_pageid();
            }
        } catch (Exception e){
            System.out.println("Error: " + e);
        }
        System.out.println("Displaying " + RowCount + " rows.");

        if (Primary != null) {
            Attribute Prime = this.Attributes.get(this.Primary);
            try {
                BPlus B = new BPlus(this, Prime, Prime.bTree);
                B.PrintTree();
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }


    public void Insert(ArrayList<Object> Row) throws Exception {
        // First check if the row to be inserted is valid.
        if (Row.size() != Attributes.size())
        throw new Exception("Row must have " + Attributes.size() + " values");

        // Define row size for insertion validation,
        int RowSize = this.GetRowByteSize(Row);

        // Define indices that must be unique
        boolean[] Uniques = new boolean[Attributes.size()];
        boolean HasUnique = false;
        // Loop through and mark uniques, sets HasUnique to true if any are unique
        for (int i = 0; i < Attributes.size(); HasUnique |= Uniques[i++])
        Uniques[i] = Attributes.get(i).unique;

        // Now the crux of inserting here is first finding the proper page to insert into
        // If there is a unique attribute, we must check its uniqueness.
        int Goal = -1; // So let's find our goal (page to insert into)

        // If using indexing, and the schema has a primary, let's use B Tree's lookup.
        BPlus B = null;
        if (Parser.Indexing && this.Primary != null) {
            Attribute Prime = this.Attributes.get(this.Primary);
            B = new BPlus(this, Prime, Prime.bTree);
            Integer Home = B.FindHome((Comparable<Object>) Row.get(this.Primary), DuplicateKeys);
            // If we found a home, set it to our goal.
            Goal = (Home != null) ? Home : Goal;
        }

        // Grab start page,
        Page P = BufferManager.getPage(this.PageId, this);

        // If not using indexing, or indexing didn't find a proper result, we would have to iterate.
        if (!Parser.Indexing || Goal < 1)
        // Iterate until a goal.
        while (Goal < 0) {
            ArrayList<ArrayList<Object>> Data = P.get_data();
            int Next = P.get_next_pageid();
            if (Primary != null) {
                Object PKey = Row.get(Primary), PagePKey;
                Attribute PAttr = Attributes.get(Primary);

                // If empty, we at the tail. all other previous options were invalid.
                if (Data.isEmpty()) {Goal = P.get_pageid(); break;}
                // Otherwise, we have other values to consider:
                else {
                    // Grab the primary key of the last row to validate 
                    PagePKey = Data.get(Data.size()-1).get(Primary);

                    int C = PAttr.Compare(PKey, PagePKey);
                    boolean Greatest = C > 0;
                    // If PKey is the greatest in the page, either we need to place this row at the end, or move to the next page.
                    if (Greatest)
                    // If there is no next page, we are at the end.
                    if (Next == -1) {Goal = P.get_pageid(); break;}
                    // Otherwise we gotta look at next instead.
                    else {P = BufferManager.getPage(Next, this); continue;}

                    // If pkey is equal then quit, that's not allowed. (if no duplicates allowed.)
                    if (!DuplicateKeys && C == 0) throw new Exception("Primary Key already in use.");

                    // Otherwise, we are at the goal since duplicates are allowed, or we are less than.
                    // If the pkey is less than page's last pkey is greater than the row's pkey, we are in the right place.
                    // if (C < 0) 
                    Goal = P.get_pageid();
                }
            }
            // If there's no primary key, we just find a page with an opening.
            // If there's room here, we got a goal.
            else if (P.freebytes >= RowSize) Goal = P.get_pageid();
            // If theres not room. we either move forward, or create a new page if we exhausted em all
            else if (Next > 0) {P = BufferManager.getPage(Next, this);}
            else {                
                // Grab a new page id, set it as next (we addin a page.)
                Next = StorageManager.CreatePage();
                P.set_nextpageid(Next);

                // Mark the page dirty so the updated next-page pointer is written to disk
                P.set_isdirty(true);

                // Now we can give it a page in the buffer
                Page newPage = BufferManager.getEmptyPage(this, Next);
                
                P = newPage;
            }
        }

        if (Goal == -1) throw new Exception("Could not find a page to insert into?");

        // Grab page,
        P = BufferManager.getPage(Goal, this);
        // Grab its data,
        ArrayList<ArrayList<Object>> Data = P.get_data();

        // Regardless of sorted or not, inserting in an empty page always has the same approach:
        if (Data.isEmpty()) {
            Data.add(Row);
            P.set_isdirty(true);
            P.freebytes -= RowSize;

            // IF Btree, insert as well.
            int page = P.pageId;
            if (B != null) B.Insert((Comparable<Object>) Row.get(Primary), page);
            // Uniqueness checking
            if (Parser.Indexing){
                for (int i = 0; i < Attributes.size(); i++){
                    Attribute Attr = Attributes.get(i);
                    if (Attr.BPlusTree != null && Attr.primaryKey != true){
                        BPlus Tree = new BPlus(this, Attr, Attr.bTree);
                        try{
                            System.out.print("B");
                            Tree.Insert((Comparable<Object>) Row.get(i), page);
                        }
                        catch(Exception e){
                            throw new Exception("Values of " + Attr.name + " must be unique.");
                        }
                    }
                }
            }
            return;
        }

        // Otherwise, with existing entries, if this table has a primary key, the entries are required to be sorted,
        // Meaning this row must go into its proper spot in our found page.
        if (Primary != null) {
            // Grab our row's pkey,
            Object PKey = Row.get(Primary), PagePKey;
            // Grab attribute for comparison functionality
            Attribute A = Attributes.get(Primary);

            // Grab the primary key of the last row for validation (if we can just toss it on the end.)
            PagePKey = Data.get(Data.size()-1).get(Primary);

            // Compare the keys
            int C = A.Compare(PKey, PagePKey);

            // If pkey is equal then quit, that's not allowed.
            if (!DuplicateKeys && C == 0) throw new Exception("Primary Key already in use.");

            if (C > 0) Data.add(Row); // If pkey is greater than the last pkey, we add it to the end.
            // Otherwise, we know its somewhere in the middle
            else {
                // So we run a binary search to check where it goes,
                int Index2 = P.bsearch_page(PKey, Primary);

                // Validate we didn't collide with a duplicate :/
                if (!DuplicateKeys && PKey.equals(Data.get(Index2).get(Primary))) 
                throw new Exception("Primary Key already in use."); 

                // Otherwise we add it just fine.
                Data.add(Index2, Row);
                // Mark page dirty,
                P.set_isdirty(true);
            }
            int page = P.pageId;
            // We just inserted above, so an existing Btree would need it as well.
            if (B != null) B.Insert((Comparable<Object>) PKey, page);
            P = BufferManager.getPage(page, this);
            // Uniqueness checking
            if (Parser.Indexing){
                for (int i = 0; i < Attributes.size(); i++){
                    Attribute Attr = Attributes.get(i);
                    if (Attr.BPlusTree != null && Attr.primaryKey != true){
                        BPlus Tree = new BPlus(this, Attr, Attr.bTree);
                        try{
                            System.out.print("A");
                            Tree.Insert((Comparable<Object>) Row.get(i), page);
                        }
                        catch(Exception e){
                            throw new Exception("Values of " + Attr.name + " must be unique.");
                        }
                    }
                }
            }
            P = BufferManager.getPage(page, this);
            
            // Split page if it is now overfull.
            // If we have a btree we need to use its wrapper instead.
            if (P.freebytes < RowSize) 
            // We got one! split in the special way :)
            if (B != null) {
                B.Split(P, true); 
                for (Attribute Attr : this.Attributes){
                    if (Attr.BPlusTree != null) Attr.BPlusTree.Split(P, true);
                }
            }
            // We don't have a btree so we split normally.
            else {
                P.split_page(true);
            }

            // Otherwise, decrement freebytes as you would normally be doing.
            else P.freebytes -= RowSize;

            return;
        }

        // The final case is that our table is unsorted and we found a page with enough room.
        // so... we just add it the normal way.
        Data.add(Row);

        P.freebytes -= RowSize;

        P.set_isdirty(true);
    }

    public void setPageId(int newPageId) {
        this.PageId = newPageId;
    }

    public static Attribute getAttribute(String attributeName, Schema S) {
        try{
            for (Attribute attribute : S.Attributes) {
                if (attribute.name.equals(attributeName.toUpperCase())){
                    return attribute;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    // cartesian join in select for schema
    // call and provide new schema 
    // implement schema method - called from parser
    // pass schema to schema
    // creates new schema with new data joined
    // display table 
    // ellie
    public Schema cartesianJoin(Schema schema1, Schema schema2) throws Exception {
        Schema joinedSchema = new Schema(schema1.Name + "join" + schema2.Name); 
        //create pageId
        joinedSchema.PageId = BufferManager.getEmptyPage(joinedSchema, null).get_pageid(); 
        // get attributes of both schemas
        ArrayList<Attribute> schema1AttrLst = schema1.Attributes;
        ArrayList<Attribute> schema2AttrLst = schema2.Attributes;
        //loop through and add schema1 and schema2 attributes to joinedSchema
        for (Attribute attr : schema1AttrLst) {
            //Naming convention avoids name collisions 
                //get rid of other schema named when joined
            String columnName = attr.name;
            if (!columnName.contains(".")) { //already has format of schema.attr
                columnName = schema1.Name + "." + attr.name;
            }
            joinedSchema.AddAttribute(columnName, attr.type,
                                     attr.typeLength, attr.notNull, null, attr.unique, attr.defaultVal, true);
        }
        for (Attribute attr : schema2AttrLst) {
            //get rid of other schema named when joined
            String columnName = attr.name;
            if (!columnName.contains(".")) { //already has format of schema.attr
                columnName = schema2.Name + "." + attr.name;
            }
            joinedSchema.AddAttribute(columnName, attr.type,
                                     attr.typeLength, attr.notNull, null, attr.unique, attr.defaultVal, true);
        }

        // Getting first page where this schema's data is stored
        int currPageId1 = schema1.PageId;
        // Getting all row data from schema 1 starting from the first
        // page and then any subsequent pages
        while(currPageId1 != -1){
            Page page1 = BufferManager.getPage(currPageId1, schema1);
            if(page1 == null) break;

            // Grab the page data
            ArrayList<ArrayList<Object>> pageData1 = page1.get_data();
            for (ArrayList<Object> row1 : pageData1) {

                // Getting first page where this schema's data is stored
                int currPageId2 = schema2.PageId;
                // Getting all row data from schema 2 starting from the first
                // page and then any subsequent pages
                while(currPageId2 != -1){ 
                Page page2 = BufferManager.getPage(currPageId2, schema2);
                    if(page2 == null) break;
                    
                    // Grab the page data
                    ArrayList<ArrayList<Object>> pageData2 = page2.get_data();
                    for (ArrayList<Object> row2 : pageData2) {
                        //join
                        ArrayList<Object> joinedRow = new ArrayList<Object>(row1);
                        for (Object val: row2) {
                            joinedRow.add(val);
                        }
                        joinedSchema.Insert(joinedRow);
                    }

                    currPageId2 = page2.get_next_pageid();
                }
            }

            currPageId1 = page1.get_next_pageid();
        }
        return joinedSchema;
    }
}
