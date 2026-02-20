package Catalog;

import java.util.ArrayList;

import BufferManager.BufferManager;
import Common.*;
import StorageManager.StorageManager;

public class Catalog {
    public static ArrayList<Schema> Schemas = new ArrayList<Schema>();
    public final static Schema AttributeTable;

    // Create the static Schema for reading the Catalog from the database.
    static {
        // This won't fail, but wrap it in a try catch so Java stops yelling at us.
        try {
            AttributeTable = new Schema("AttributeTable");
            AttributeTable.PageId = 0;

            AttributeTable.AddAttribute("Schema_Name", Type.VARCHAR, 50, false, false, false, null);
            AttributeTable.AddAttribute("Start_Page", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("Attribute_Name", Type.VARCHAR, 50, false, true, false, null);
            AttributeTable.AddAttribute("Type", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("Length", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("NotNull", Type.BOOLEAN, 50, false, false, false, null);
            AttributeTable.AddAttribute("Unique", Type.BOOLEAN, 0, false, false, false, null);
            AttributeTable.AddAttribute("Primary", Type.BOOLEAN, 0, false, false, false, null);
            AttributeTable.AddAttribute("DefaultValue", Type.VARCHAR, 50, false, false, false, null);
            
        } catch (Exception e) {throw new ExceptionInInitializerError(e);}
    }

    public static Schema AddSchema(String Name) throws Exception {
        // Check if Schema name is already in use
        if (GetSchema(Name) != null) 
        throw new Exception("Schema already exists");

        // Create the new schema if there was no issue, and return it
        Schema S;
        // Officially add it to the catalog
        Schemas.add(S = new Schema(Name));
        // Assign a free page into the buffer, and grab its id.
        S.PageId = BufferManager.getEmptyPage(S).get_pageid();

        return S;
    }

    public static Schema GetSchema(String Name) {
        // Force uppercase
        Name = Name.toUpperCase();

        for (Schema S : Schemas) if (S.Name.equals(Name)) return S;

        return null; 
    }

    public static void RemoveSchema(String Name) throws Exception {
        // Force uppercase
        Name = Name.toUpperCase();

        Schema S;
        // Grab Schema if it exists,
        if ((S = GetSchema(Name)) == null) 
        // Otherwise throw exception.
        throw new Exception("Schema does not exist");

        // Schema exists, begin freeing its pages
        Page page = BufferManager.getPage(S.PageId, S);
        while (page != null) {
            StorageManager.markfreepage(page.get_pageid());
            page = page.get_next_pageid() != 1 ? BufferManager.getPage(page.get_next_pageid(), S) : null;
        }

        // Now remove the Schema from the Catalog entirely.
        Schemas.remove(S);
    }
}
