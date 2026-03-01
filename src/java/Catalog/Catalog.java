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
        // do not give this table a primary key, the attributes will get sorted and cause havoc :(
        try {
            AttributeTable = new Schema("ATTRIBUTETABLE");
            AttributeTable.PageId = 0;

            AttributeTable.AddAttribute("SchemaName", Type.VARCHAR, 50, false, false, false, null);
            AttributeTable.AddAttribute("StartPage", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("AttributeName", Type.VARCHAR, 50, false, false, false, null);
            AttributeTable.AddAttribute("Type", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("Length", Type.INT, 50, false, false, false, null);
            AttributeTable.AddAttribute("NotNull", Type.BOOLEAN, 50, false, false, false, null);
            AttributeTable.AddAttribute("Unique", Type.BOOLEAN, 0, false, false, false, null);
            AttributeTable.AddAttribute("Primary", Type.BOOLEAN, 0, false, false, false, null);
            AttributeTable.AddAttribute("DefaultValue", Type.VARCHAR, 50, false, false, false, null);
            
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Schema AddSchema(String Name) throws Exception {
        // Check if Schema name is already in use
        if (GetSchema(Name) != null) 
        throw new Exception("Schema already exists");

        //Force uppercase
        Name = Name.toUpperCase();

        // Create the new schema if there was no issue, and return it
        Schema S;
        // Officially add it to the catalog
        Schemas.add(S = new Schema(Name));
        // Assign a free page into the buffer, and grab its id.
        S.PageId = BufferManager.getEmptyPage(S, null).get_pageid();

        System.out.println("Schema: " + Name + " created successfully");

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
            page.set_isdirty(false);
            int page_id = page.get_pageid();
            if (page_id > 1) StorageManager.FreePage(page);
            page = page.get_next_pageid() != -1 ? BufferManager.getPage(page.get_next_pageid(), S) : null;
        }

        // Now remove the Schema from the Catalog entirely.
        Schemas.remove(S);
    }

    public static void AttributeAdd(String schemaName, String attributeName, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique, Object Default) throws Exception{
        // Creating new schema and setting its name, primary key, page id, and attributes
        Schema oldSchema = GetSchema(schemaName);

        // cant be not null but also not have a default :/
        if(Nullable && Default==null)
        throw new Exception("NOT NULL attribute missing default value");

        // Force uppercase
        attributeName = attributeName.toUpperCase();

        // Copy the old to a new schema under an unused name,
        Schema newSchema = oldSchema.Copy();

        // Add the new unique attribute,
        newSchema.AddAttribute(attributeName, T, Size, Nullable, Primary, Unique, Default);

        Page P = BufferManager.getPage(oldSchema.PageId, oldSchema);
        while (P != null) {
            for (ArrayList<Object> Row : P.get_data()) {
                Row = (ArrayList<Object>) Row.clone(); 
                Row.add(null); // Add space for new values.
                newSchema.Insert(Row);
            }
            P = P.get_next_pageid() != -1 ? BufferManager.getPage(P.get_next_pageid(), oldSchema) : null;
        }
        
        // Remove the old schema (and its pages),
        RemoveSchema(oldSchema.Name);

        // Drop the new schema into the catalog in place of the old one.
        Schemas.add(newSchema);
    }

    public static void AttributeDrop(String schemaName, String attributeName) throws Exception {
        // Creating new schema and setting its name, primary key, page id, and attributes
        Schema oldSchema = GetSchema(schemaName);

        // Force uppercase
        attributeName = attributeName.toUpperCase();

        // Make the new schema :)
        Schema newSchema = oldSchema.Copy();
        // Remove attribute from it,
        newSchema.RemoveAttribute(attributeName);

        // If remove ran fine, we know it existed.
        // Grab this existing attribute index
        int AIndex = -1, i=0;

        // Iterate through attribute list, stopping once AIndex is set.
        for (;(AIndex<0);i++)
        // If name matches,
        if (oldSchema.Attributes.get(i).name.equals(attributeName)) 
        AIndex = i;

        // Now insert the old data into the new schema with this index removed,
        Page P = BufferManager.getPage(oldSchema.PageId, oldSchema);
        while (P != null) {
            for (ArrayList<Object> Row : P.get_data()) {
                Row = (ArrayList<Object>) Row.clone(); 
                Row.remove(AIndex); // Remove old attribute's value.
                newSchema.Insert(Row);
            }
            P = P.get_next_pageid() != -1 ? BufferManager.getPage(P.get_next_pageid(), oldSchema) : null;
        }
        
        // Remove old schema,
        RemoveSchema(oldSchema.Name);

        // Add the replacement back.
        Schemas.add(newSchema);
    }
}
