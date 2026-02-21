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

        // Handle not null.
        if (O.toString().toUpperCase().equals("NULL"))
        if (this.notNull) throw new Exception(this.name + " cannot be null");
        else return null;

        try {
            switch (this.type) {
                case INT: O = Integer.parseInt(O.toString()); break;
                case CHAR: 
                    if (O.toString().length() != this.typeLength)
                    throw new Exception("Char value must be " + this.typeLength + " characters");
                    O = String.format("%-"+this.typeLength.toString()+"s",O); 
                    break;
                case VARCHAR:
                    if (O.toString().length() > this.typeLength) {
                        throw new Exception("VARCHAR values must be between 0 and " + this.typeLength + " characters");
                    }
                    O = String.format("%-"+this.typeLength.toString()+"s",O);
                    break;
                case DOUBLE: 
                    if (!O.toString().contains("."))
                    throw new Exception("Double values must contain a decimal");
                    O = Double.parseDouble(O.toString()); break;
                case BOOLEAN:
                    boolean True = O.toString().equals("True");
                    // If true or false, return which it is
                    if(True || O.toString().equals("False"))
                    return True;
                    // Otherwise, throw error for syntax
                    throw new Exception("Boolean values must be True, False, or NULL");
                default: break;
            }
        } catch (Exception e) {
            throw e;
        }

        return O;
    }
}
