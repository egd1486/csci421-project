package Common;

public class Attribute { //for one column
    public String name;
    public Type type; 
    public Integer typeLength; 
    public boolean primaryKey; //is publicKey
    public boolean notNull; //is not null
    public boolean unique; //is unique
    public Object defaultVal;

    public Attribute(String name, Type type, Integer typeLength, boolean primaryKey, boolean notNull, boolean unique, Object defaultVal) throws Exception{
        this.name = name;
        this.type = type;
        this.typeLength = typeLength;
        this.primaryKey = primaryKey;
        this.notNull = notNull;
        this.unique = unique;
        this.defaultVal = this.Parse(defaultVal);
    }

    public Integer GetByteSize() {
        switch (this.type) {
            case INT:
                return Integer.BYTES;
            case CHAR:
                return this.typeLength + Integer.BYTES;
            case DOUBLE:
                return Double.BYTES;
            case VARCHAR:
                return this.typeLength + Integer.BYTES;
            case BOOLEAN:
                return 1;
            default:
                return 0;
        }
    }

    // Parses string representation to actual type. 
    // (Typically used for parsing a new table's default value)
    public Object Parse(Object O) throws Exception{
        if (O == null) return null;

        System.out.println(O.toString() + "object string :)");

        // Handle not null.
        if (O.toString().toUpperCase().equals("NULL"))
        if (this.notNull) throw new Exception(this.name + " cannot be null");
        else return null;

        try {
            switch (this.type) {
                case INT: O = Integer.parseInt(O.toString()); break;
                case CHAR: O = String.format("%-"+this.typeLength.toString()+"s",O); break;
                case DOUBLE: O = Double.parseDouble(O.toString()); break;
                case BOOLEAN: O = Boolean.parseBoolean(O.toString()); break;
                default: break;
            }
        } catch (Exception e) {
            throw new Exception("Invalid default value");
        }
        System.out.println(O.getClass().toString());
        return O;
    }
}
