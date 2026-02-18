package Catalog;

import java.util.ArrayList;

import BufferManager.BufferManager;
import Common.Page;
import StorageManager.StorageManager;

public class Catalog {
    public static ArrayList<Schema> Schemas = new ArrayList<Schema>();

    public static Schema AddSchema(String Name) throws Exception {
        // Check if Schema name is already in use
        if (GetSchema(Name) != null) 
        throw new Exception("Schema already exists");

        // Create the new schema if there was no issue, and return it
        Schema S;
        // Officially add it to the catalog
        Schemas.add(S = new Schema(Name));
        // Assign it a free page, and put it into the buffer.
        S.PageId = StorageManager.create_page();
        // TODO: Make sure the newly assigned page is added to the buffer
        BufferManager.getPage(S.PageId);
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
        Page page = BufferManager.getPage(S.PageId);
        while (page != null) {
            StorageManager.markfreepage(page.get_pageid());
            page = page.get_next_pageid() != 1 ? BufferManager.getPage(page.get_next_pageid()) : null;
        }

        // Now remove the Schema from the Catalog entirely.
        Schemas.remove(S);
    }
}
