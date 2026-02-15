package Common;

public class Attribute { //for one column
    public String name;
    public Type type; 
    public int typeLength; 
    public boolean primaryKey; //is publicKey
    public boolean notNull; //is not null
    public boolean unique; //is unique

    public Attribute(String name, Type type, int typeLength, boolean primaryKey, boolean notNull, boolean unique) {
        this.name = name;
        this.type = type;
        this.typeLength = typeLength;
        this.primaryKey = primaryKey;
        this.notNull = notNull;
        this.unique = unique;
    }

    public Integer GetByteSize() {
        switch (this.type) {
            case INT:
                return Integer.BYTES;
            case CHAR:
                return this.typeLength;
            case DOUBLE:
                return Double.BYTES;
            case VARCHAR:
                return this.typeLength;
            case BOOLEAN:
                return 1;
            default:
                return 0;
        }
    }
}
