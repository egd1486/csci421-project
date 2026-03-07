package Common.WhereTree;

import Common.TokenType;
import Common.Type;
import java.util.ArrayList;

public class BinaryOpNode implements WhereClassInterface {

    InterfaceOperandNode Left;
    InterfaceOperandNode Right;
    TokenType Operator;
    //! Need to figure out where in tree row comes in
    ArrayList<Object> row;
    public BinaryOpNode(InterfaceOperandNode left, TokenType Operator, InterfaceOperandNode right) {
        this.Left = left;
        this.Operator = Operator;
        this.Right = right;
    }


    @Override
    public boolean evaluate(){
        Type leftType = Left.getType();
        Type rightType = Right.getType();
        if (leftType != rightType){
            // Error message
            // throw new Exception("Unexpected " + rightType + ", expected " + leftType ".");
        }
        switch(leftType){
            case INT ->{
                switch(Operator) {
                    case EQUAL -> {return (int)Left.evaluate(row) == (int)Right.evaluate(row);}
                    case NOT_EQUAL -> {return (int)Left.evaluate(row) != (int)Right.evaluate(row);}
                    case GREATER_EQUAL -> {return (int)Left.evaluate(row) >= (int)Right.evaluate(row);}
                    case LESS_EQUAL -> {return (int)Left.evaluate(row) <= (int)Right.evaluate(row);}
                    case GREATER ->  {return (int)Left.evaluate(row) > (int)Right.evaluate(row);}
                    case LESS ->  {return (int)Left.evaluate(row) < (int)Right.evaluate(row);}
                    default ->  { }
                }
            }
            case DOUBLE ->{
                switch(Operator) {
                    case EQUAL -> {return (double)Left.evaluate(row) == (double)Right.evaluate(row);}
                    case NOT_EQUAL -> {return (double)Left.evaluate(row) != (double)Right.evaluate(row);}
                    case GREATER_EQUAL -> {return (double)Left.evaluate(row) >= (double)Right.evaluate(row);}
                    case LESS_EQUAL -> {return (double)Left.evaluate(row) <= (double)Right.evaluate(row);}
                    case GREATER ->  {return (double)Left.evaluate(row) > (double)Right.evaluate(row);}
                    case LESS ->  {return (double)Left.evaluate(row) < (double)Right.evaluate(row);}
                    default ->  { }
                }
            }
            case BOOLEAN ->{
                switch(Operator) {
                    case EQUAL -> {return ((Boolean)Left.evaluate(row)).equals((Boolean)Right.evaluate(row));}
                    case NOT_EQUAL -> {return !(((Boolean)Left.evaluate(row)).equals((Boolean)Right.evaluate(row)));}
                    default ->  { }
                }
            }
            case CHAR ->{
                switch(Operator) {
                    case EQUAL -> {return ((String)Left.evaluate(row)).equals((String)Right.evaluate(row));}
                    case NOT_EQUAL -> {return !(((String)Left.evaluate(row)).equals((String)Right.evaluate(row)));}
                    case GREATER_EQUAL -> {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) >= 0;}
                    case LESS_EQUAL -> {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) <= 0;}
                    case GREATER ->  {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) > 0;}
                    case LESS ->  {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) < 0;}
                    default ->  { }
                }
            }
            case VARCHAR ->{
                switch(Operator) {
                    case EQUAL -> {return ((String)Left.evaluate(row)).equals((String)Right.evaluate(row));}
                    case NOT_EQUAL -> {return !(((String)Left.evaluate(row)).equals((String)Right.evaluate(row)));}
                    case GREATER_EQUAL -> {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) >= 0;}
                    case LESS_EQUAL -> {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) <= 0;}
                    case GREATER ->  {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) > 0;}
                    case LESS ->  {return ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row)) < 0;}
                    default ->  { }
                }
            }
            default -> { }
        }
        return false;
    }

    @Override
    public int getPrescendance() {
        return 2;
    }



}
