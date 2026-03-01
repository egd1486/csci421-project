package Catalog;

import Common.*;
import BufferManager.BufferManager;
import StorageManager.StorageManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class Schema {
    public String Name;
    public Integer Primary;
    public Integer PageId;
    public ArrayList<Common.Attribute> Attributes;

    public Schema(String Name) throws Exception {
        // Check if name is alphanumeric
        if (!isAlphaNumeric(Name))
        throw new Exception("Schema name contains non-alphanumeric characters");
        // Otherwise, proceed.
        this.Attributes = new ArrayList<>();
        this.Name = Name;
    }

    // COPIES SCHEMA BUT NOT DATA
    public Schema Copy() throws Exception {
        Schema newSchema = new Schema(this.Name);
        newSchema.Primary = this.Primary;
        newSchema.PageId = BufferManager.getEmptyPage(newSchema, null).get_pageid();

        for (Attribute A : this.Attributes) newSchema.Attributes.add(A);

        return newSchema;
    }

    public Attribute AddAttribute(String Name, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique, Object Default) throws Exception {
        if (Primary != null && Primary) // If the attribute should be primary,
        // If we already have a primary key, throw.
        if (this.Primary != null) throw new Exception("Schema already has a primary key");
        // Otherwise, handle it.
        else this.Primary = Attributes.size();

        // Force uppercase
        Name = Name.toUpperCase();

        // Check if Attribute is alphanumeric
        if (!isAlphaNumeric(Name)) 
        throw new Exception("Attribute name" + Name + " contains non-alphanumeric characters");

        // Iterate over attributes to see if name is in use.
        for (Attribute A : Attributes) 
        if (A.name.equals(Name)) 
        throw new Exception("Schema already has an attribute named " + Name);

        boolean isPrimary  = Primary  != null && Primary;
        boolean isNullable = Nullable != null && Nullable;
        boolean isUnique   = Unique   != null && Unique;
        
        Attribute A = new Attribute(Name, T, Size, isPrimary, isNullable, isUnique, Default);
        // Attribute A = new Attribute(Name, T, Size, Primary, Nullable, Unique, Default);
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
    public void DisplayTable(){
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

                // Define the column size for formating,
                int numAttributes = this.Attributes.size();
                int[] columnWidths = new int[numAttributes];
                // Set minimum width as the length of the attribute name
                for(int i = 0; i < numAttributes; i++) columnWidths[i] = this.Attributes.get(i).name.length();
                // Then find the longest attribute there, and set its length instead.
                for(ArrayList<Object> row : pageData)
                for(int i = 0; i < numAttributes; i++){
                    Object value = row.get(i);
                    if(value == null) value = "NULL";
                    columnWidths[i] = Math.max(columnWidths[i], value.toString().length());
                }

                // Printing header (page # + attribute names + separator)
                // calculate total dash padding needed for the separator
                int dashes = 1;
                for(int width : columnWidths) dashes += width + 3;
                // page # time
                String Title = " [Page " + PageCount++ + "]";
                for(int i = 0; i < dashes; i++) System.out.print("-");
                System.out.print(Title);
                System.out.println();
                // names
                System.out.print("|");
                for(int i = 0; i < numAttributes; i++)
                System.out.printf(" %-" + columnWidths[i] + "s |", this.Attributes.get(i).name);
                System.out.println();
                // separator
                for(int i = 0; i < dashes; i++) System.out.print("-");
                System.out.println();
                // Increase row counter
                RowCount += pageData.size();
                // Now print the rows.
                for (ArrayList<Object> row : pageData) {
                    System.out.print("|");
                    for (int i=0; i<row.size(); i++) {
                        Object value = row.get(i);

                        // If value is null,
                        if(value == null) 
                        // And there's a default, use it.
                        if (defaults[i] != null) value = defaults[i]; 
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
    }

    public void Insert(ArrayList<Object> Row) throws Exception {
        // First check if the row to be inserted is valid.
        if (Row.size() != Attributes.size())
        throw new Exception("Row must have " + Attributes.size() + " values");

        // Define row size for insertion validation,
        int RowSize = this.GetRowByteSize(Row);

        // If this table has a primary key, the entries are required to be sorted,
        // Meaning this row must go into its proper spot.
        if (Primary != null) {
            // Grab our row's pkey,
            Object PKey = Row.get(Primary), PagePKey;
            // Grab attribute for comparison functionality
            Attribute A = Attributes.get(Primary);

            Page P = BufferManager.getPage(this.PageId, this);
            while (P != null) {
                // Grab all data,
                ArrayList<ArrayList<Object>> Data = P.get_data();

                // If this page is empty, then we know we are at the tail, and this PKey is the biggest element.
                if (Data.size() == 0) {
                    Data.add(Row);
                    P.set_isdirty(true);
                    return;
                }

                // Otherwise, we need to parse to see if the key even belongs here.
                // Grab the primary key of the last row to validate 
                PagePKey = Data.get(Data.size()-1).get(Primary);

                // Compare the keys
                int C = A.Compare(PKey, PagePKey), Next;

                // If pkey is equal then quit, that's not allowed.
                if (C == 0) throw new Exception("Primary Key already in use.");

                // If pkey is greater, we advance pages until we find the right one, or run out of spots.
                boolean Greatest = C > 0;
                if (Greatest && (Next = P.get_next_pageid()) > 0) {
                    P = BufferManager.getPage(Next, this);
                    continue;
                }

                // Otherwise we should fit this in the current page, and split if necessary.
                // Now find what index to shove it in.
                if (Greatest) Data.add(Row); // we know it goes at the end if it is greatest.
                else {
                    // If we dont know then we should...
                    // Binary search!
                    int L=0, R=Data.size()-1, M=0;
                    while (L < R) {
                        // Get middle index,
                        M = (L+R)/2;
                        // Compare the keys,
                        C = A.Compare(PKey, Data.get(M).get(Primary));

                        // If C is 0, then the keys are equivalent, which is not allowed.
                        if (C == 0) throw new Exception("Primary Key already in use.");
                        // If the pkey is less than the middle pkey
                        // Move the right bound down past it, as the true spot is left of it.
                        if (C < 0) R = M-1;
                        // Otherwise, we need to shift the left edge up, as the spot is right of it.
                        else L = M+1;
                    }
                    // Insert at L, as it is now at the ideal index.
                    Data.add(L, Row);
                }
                // Make page dirty,
                P.set_isdirty(true);
                // Split page if it is now overfull.
                if (P.freebytes < RowSize) P.split_page();
                // Otherwise, decrement freebytes as you should be doing.
                else P.freebytes -= RowSize;
                return;
            }
        }

        // Otherwise, we are dealing with an unsorted table.
        Page P = BufferManager.getPage(this.PageId, this);
        int Next;
        // If there are no slots remaining, grab the next page until you find a spot.
        while (P.freebytes <= RowSize)
        // If there is a next page, grab it
        if ((Next = P.get_next_pageid()) > 0)        
        P = BufferManager.getPage(Next, this);
        // Otherwise grab a brand new page and make it the next page.
        else {
            // Grab a new page id, set it as next
            Next = StorageManager.CreatePage();
            P.set_nextpageid(Next);

            // Mark the page dirty so the updated next-page pointer is written to disk
            P.set_isdirty(true);

            // Now we can give it a page in the buffer
            Page newPage = BufferManager.getEmptyPage(this, Next);
            
            P = newPage;
        }
        // Now that we have a page with room, insert into it.
        P.get_data().add(Row);

        P.freebytes -= RowSize;

        P.set_isdirty(true);
    }

    public void setPageId(int newPageId) {
        this.PageId = newPageId;
    }

}
