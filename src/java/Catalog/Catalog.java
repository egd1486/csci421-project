package Catalog;

import java.util.ArrayList;

public class Catalog {
    public static ArrayList<Schema> Schemas = new ArrayList<Schema>();

    public static Schema AddSchema(String Name) throws Exception {
        Schema S;
        // Check if Schema name is already in use
        if ((S = GetSchema(Name)) != null) 
        throw new Exception("Schema already exists");

        // Create the new schema if there was no issue, and return it
        Schemas.add(S = new Schema(Name));
        return S;
    }

    public static Schema GetSchema(String Name) {
        for (Schema S : Schemas) if (S.Name.equals(Name)) return S;
        return null; 
    }

    public static Boolean RemoveSchema(String Name) {
        int i;
        // If schema was found, it gets removed
        if ((i = Schemas.indexOf(GetSchema(Name))) != -1)
        Schemas.remove(i);
        // Then return the same conditional,
        return i != -1;
    }
}
