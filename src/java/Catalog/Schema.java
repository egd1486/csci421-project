package Catalog;

import BufferManager.BufferManager;
import Common.*;
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
        if (Primary != null) // If the attribute should be primary,
        // If we already have a primary key, throw.
        if (this.Primary != null) throw new Exception("Schema already has a primary key");
        // Otherwise, handle it.
        else this.Primary = Attributes.size();

        // Force uppercase
        Name = Name.toUpperCase();

        // Check if Attribute is alphanumeric
        if (!isAlphaNumeric(Name)) 
        throw new Exception("Attribute name contains non-alphanumeric characters");

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

    public Integer GetMaxRowSize() {
        Integer Size = 0;

        for (Attribute A : Attributes) Size += A.GetByteSize();
        
        return Size;
    }

    // Gets all row data from the table specified by this schema.
    // Returns a list of lists where each inner list represents a row
    // containing the row's data.
    public ArrayList<List<Object>> Select() {
        ArrayList<List<Object>> entries = new ArrayList<>();
        try{
            // Getting first page where this schema's data is stored
            int currPageId = this.PageId;
            // Getting all row data from this schema starting from the first
            // page and then any subsequent pages
            while(currPageId != -1){
                Page page = BufferManager.getPage(currPageId);
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
    public void DisplayTable(ArrayList<List<Object>> rows){
        // Calculating width of each column
        int numAttributes = this.Attributes.size();
        int[] columnWidths = new int[numAttributes];
        for(int i = 0; i < numAttributes; i++){
            columnWidths[i] = this.Attributes.get(i).name.length();
        }
        for(List<Object> row : rows){
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
        for(List<Object> row : rows){
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

    public static void Insert(ArrayList<Object> rows) throws Exception {
        // TODO: Insert all rows into the table's pages, splitting when necessary.
    }
}
