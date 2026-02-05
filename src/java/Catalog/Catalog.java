package Catalog;

import java.util.ArrayList;

public class Catalog {
    private static ArrayList<Schema> Schemas = new ArrayList<Schema>();

    public Boolean AddSchema(String Name) {
        return true;
    }

    public Schema GetSchema(String Name) {
        return null;
    }

    public Boolean RemoveSchema(String Name) {
        return true;
    }
}
