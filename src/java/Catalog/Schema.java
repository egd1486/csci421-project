package Catalog;

import Common.*;
import java.util.ArrayList;

public class Schema {
    public String Name;
    private Integer Primary;
    private ArrayList<Attribute> Attributes;

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

    public Boolean Validate() {


        return true;
    }
}
