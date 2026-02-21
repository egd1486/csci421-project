package Catalog;

import BufferManager.BufferManager;
import Common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

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

    public void AddAttribute(String Name, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique, Object Default) throws Exception {
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

        Attribute A = new Attribute(Name, T, Size, Primary, Nullable, Unique, Default);
        Attributes.add(A);
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

    private static boolean isAlphaNumeric(String S) {return S.matches("[a-zA-Z0-9]+");}

    public void setName(String name){
        this.Name = name;
    }

    // Returns the maximum size of the row data in bytes.
    public Integer GetMaxRowSize() {
        Integer Size = 0;

        for (Attribute A : Attributes) Size += A.GetByteSize();
        
        return Size + ((int) this.Attributes.size() / 8);
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
        try{
            // Getting first page where this schema's data is stored
            int currPageId = this.PageId;
            // Getting all row data from this schema starting from the first
            // page and then any subsequent pages
            while(currPageId != -1){
                Page page = BufferManager.getPage(currPageId, this);
                if(page == null) break;
                entries.addAll(page.get_data());
                currPageId = page.get_next_pageid();
            }
        } catch (Exception e){
            System.out.println("Error: " + e);
        }
        return entries;
    }

    // Displays a table in an easy to read format
    public void DisplayTable(ArrayList<ArrayList<Object>> rows){
        // Calculating width of each column
        int numAttributes = this.Attributes.size();
        int[] columnWidths = new int[numAttributes];
        for(int i = 0; i < numAttributes; i++){
            columnWidths[i] = this.Attributes.get(i).name.length();
        }
        for(ArrayList<Object> row : rows){
            for(int i = 0; i < numAttributes; i++){
                Object value = row.get(i);
                String valueString;
                if(value == null) valueString = "NULL";
                else valueString = value.toString();
                columnWidths[i] = Math.max(columnWidths[i], valueString.length());
            }
        }

        // Printing header (attribute names + separator)
        System.out.print("|");
        for(int i = 0; i < numAttributes; i++){
            System.out.printf(" %-" + columnWidths[i] + "s |", this.Attributes.get(i).name);
        }
        System.out.println();
        System.out.print("-");
        for(int width : columnWidths){
            for(int i = 0; i < width + 2; i++){
                System.out.print("-");
            }
            System.out.print("-");
        }
        System.out.println();

        // Printing row data
        for(ArrayList<Object> row : rows){
            System.out.print("|");
            for(int i = 0; i < numAttributes; i++){
                Object value = row.get(i);
                String valueString;
                if(value == null) valueString = "NULL";
                else valueString = value.toString();
                System.out.printf(" %-" + columnWidths[i] + "s |", valueString);
            }
            System.out.println();
        }
    }

    public void Insert(ArrayList<Object> Row) throws Exception {
        // First check if the row to be inserted is valid.
        if (Row.size() != Attributes.size())
        throw new Exception("Row must have " + Attributes.size() + " values");

        // Check for uniqueness if needed.
        for (int i=0; i<Row.size(); i++) {
            Attribute A = Attributes.get(i);
            if (A.unique || A.primaryKey) {
                for (ArrayList<Object> R : this.Select()) {
                    if (R.get(i).equals(Row.get(i)))
                    throw new Exception(A.name + " must be unique.");
                }
            }

            if (A.notNull && Row.get(i) == null)
            throw new Exception(A.name + " cannot be null.");
        }

        

        Page P = BufferManager.getPage(this.PageId, this);
        int Next;
        // If there are no slots remaining, grab the next page until you find a spot.
        while (P.get_slots_remaining() == 0)
        // If there is a next page, grab it
        if ((Next = P.get_next_pageid()) > -1)        
        P = BufferManager.getPage(P.get_next_pageid(), this);
        // Otherwise grab a brand new page and make it the next page.
        else {
            Page newPage = BufferManager.getEmptyPage(this);
            P.set_nextpageid(newPage.get_pageid());
            P = newPage;
        }

        // Now that we have a page with room, insert into it.
        P.get_data().add(Row);
    }
}
