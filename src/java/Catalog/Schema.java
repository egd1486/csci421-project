package Catalog;

import Common.*;

import java.util.ArrayList;

public class Schema {
    public String Name;
    public Integer Primary;
    public ArrayList<Common.Attribute> Attributes;

    public Schema(String Name) {
        this.Name = Name;
    }

    public void AddAttribute(String Name, Type T, Integer Size, Boolean Nullable, Boolean Primary, Boolean Unique) throws Exception {
        if (Primary != null) // If the attribute should be primary,
        // If we already have a primary key, throw.
        if (this.Primary != null) throw new Exception("Schema already has a primary key");
        // Otherwise, handle it.
        else this.Primary = Attributes.size();

        // Iterate over attributes to see if name is in use.
        for (Attribute A : Attributes) 
        if (A.name.equals(Name)) 
        throw new Exception("Schema already has an attribute named " + Name);
        

        Attribute A = new Attribute(Name, T, Size, Primary, Nullable, Unique);
        Attributes.add(A);
    }

    public void RemoveAttribute(String Name) throws Exception {
        for (Attribute A : Attributes) 
        // If the name is found, remove it.
        if (A.name.equals(Name) && this.Attributes.remove(A))
        // Returns when condition is met.
        return;

        throw new Exception("Schema does not have an attribute named " + Name);
    }

    public Integer GetRowSize() {
        Integer Size = 0;

        for (Attribute A : Attributes) Size += A.GetByteSize();
        
        return Size;
    }

    public Boolean Validate() {
        return this.Primary != null;
    }
}
