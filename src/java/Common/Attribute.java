package Common;

public class Attribute { //for one column
    private String name;
    private Type type; 
    private int typeLength; 
    private boolean primaryKey; //is privateKey
    private boolean notNull; //is not null
    private boolean unique; //is unique

    public Attribute(String name, Type type, int typeLength, boolean primaryKey, boolean notNull, boolean unique) {
        this.name = name;
        this.type = type;
        this.typeLength = typeLength;
        this.primaryKey = primaryKey;
        this.notNull = notNull;
        this.unique = unique;
    }
}
