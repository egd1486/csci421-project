package Catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public static void AttributeAdd(String schemaName, String attributeName, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique, Object Default) throws Exception{
        // Creating new schema and setting its name, primary key, page id, and attributes
        Schema oldSchema = GetSchema(schemaName);

        // Creating the new schema under an unused name,
        String newName = schemaName + "new!schema!name";
        Schema newSchema = AddSchema(newName);
        // Copy over primary key if used,
        newSchema.Primary = oldSchema.Primary;

        // Now loop over old attributes and add to the new schema,
        for(Attribute A : oldSchema.Attributes) newSchema.Attributes.add(A);

        // Add the new unique attribute,
        newSchema.AddAttribute(attributeName, T, Size, Nullable, Primary, Unique, Default);

        // Adding all data with new attribute added
        int currPageId = oldSchema.PageId;
        ArrayList<List<Object>> newData = new ArrayList<>();
        while(currPageId != -1){
            Page currPage = BufferManager.getPage(currPageId, oldSchema);
            for(List<Object> row : currPage.get_data()){
                List<Object> newRow = new ArrayList<>(row);
                newRow.add(Default);
                newData.add(newRow);
            }
            currPageId = currPage.get_next_pageid();
        }
        for(List<Object> row : newData){
            newSchema.Insert(row);
        }
        newSchema.setName(schemaName);
        RemoveSchema(oldSchema.Name);
    }

    public static void AttributeDrop(String schemaName, String attributeName) throws Exception {
        // Creating new schema and setting its name, primary key, page id, and attributes
        Schema oldSchema = GetSchema(schemaName);

        // Check if attribute to be removed exists
        for(Attribute A : oldSchema.Attributes) if(Objects.equals(A.name, attributeName)) break;
        else throw new Exception("Attribute does not exist");

        String newName = schemaName + "new!schema!name";
        Schema newSchema = AddSchema(newName);
        newSchema.Primary = oldSchema.Primary;

        // the index of the attribute we want to remove
        int attributeIndex = -1;
        for(int i = 0; i < oldSchema.Attributes.size(); i++){
            Attribute A = oldSchema.Attributes.get(i);
            // If the attribute is found, mark its index and dont add it
            if(A.name.equals(attributeName)) attributeIndex = i;
            // Otherwise just add it.
            else newSchema.Attributes.add(A);
        }
        if(attributeIndex == -1){
            RemoveSchema(newName);
            throw new Exception("Attribute does not exist");
        }

        // Adding data with old attribute removed
        int currPageId = BufferManager.getPage(oldSchema.PageId, oldSchema).get_pageid();
        ArrayList<List<Object>> newData = new ArrayList<>();
        while(currPageId != -1){
            Page currPage = BufferManager.getPage(currPageId, oldSchema);
            ArrayList<List<Object>> currPageData = currPage.get_data();
            for(List<Object> row : currPageData){
                List<Object> newRow = new ArrayList<>(row);
                newRow.remove(attributeIndex);
                newData.add(newRow);
            }
            currPageId = currPage.get_next_pageid();
        }
        for(List<Object> row : newData){
            newSchema.Insert(row);
        }
        newSchema.setName(schemaName);
        RemoveSchema(oldSchema.Name);
    }
}
