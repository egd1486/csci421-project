package Common;

public class Attribute { //for one column
    private String name;
    private String type; //incorportate enum type
    private int typeLength; // ! how to store 5 in varchar(5) ?
    private boolean privateKey; //is privateKey
    private boolean notNull; //is not null
    private boolean unique; //is unique

    public Attribute(String name, String type, int typeLength, boolean privateKey, boolean notNull, boolean unique) {
        this.name = name;
        this.type = type;
        this.typeLength = typeLength;
        this.privateKey = privateKey;
        this.notNull = notNull;
        this.unique = unique;
    }
}
