package Common.WhereTree;

import Common.TokenType;
import Common.Type;
import java.util.ArrayList;

import static Common.TokenType.*;

public class ConstantValueNode implements InterfaceOperandNode {

    //Idk if we rlly need this lol
    Object value;
    Type type;

    public ConstantValueNode(Object value, TokenType type) throws Exception {
        this.value = parseValue(value,type);
        this.type = fromTokenTypetoType(type);
    }

    private Object parseValue(Object value, TokenType tokenType) throws Exception {
        if (value == null) return null;

        return switch (tokenType) {
            case INT_LITERAL -> Integer.parseInt(value.toString());
            case DOUBLE_LITERAL -> Double.parseDouble(value.toString());
            case STRING_LITERAL -> value.toString();
            case TRUE -> true;
            case FALSE -> false;
            case NULL -> null;
            default -> throw new Exception("Cannot parse token type " + tokenType);
        };
    }

    private Type fromTokenTypetoType(TokenType tokenType) throws Exception {
        return switch (tokenType) {
            case INT_LITERAL -> Type.INT;
            case DOUBLE_LITERAL -> Type.DOUBLE;
            case STRING_LITERAL -> Type.VARCHAR; // usually varchar unless maybe fixed length idk
            case TRUE, FALSE -> Type.BOOLEAN;
            case NULL -> Type.NULL;
            default -> throw new Exception("Cannot convert token type " + tokenType + " to Type");
        };
    }
    @Override
    public Type getType(){
        return type;
    }
    @Override
    public Object evaluate(ArrayList<Object> row){
        return value;
    }

    @Override
    public String print() {
        if(value == null) return "null";
        return value.toString();
    }
}
