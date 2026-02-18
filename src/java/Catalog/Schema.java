package Catalog;

import Common.*;

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

    public static ArrayList<Object> Select() {
        ArrayList<Object> Entries = new ArrayList<>();

        // TODO: Read all rows from the table's pages, and return it.

        return Entries;
    }

    public static void Insert(ArrayList<Object> rows) throws Exception {
        // TODO: Insert all rows into the table's pages, splitting when necessary.
    }
}
