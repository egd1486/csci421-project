package Common.WhereTree;

import Common.TokenType;
import Common.Type;
import java.util.ArrayList;
import Catalog.Schema;

import static Common.TokenType.EQUAL;
import static Common.TokenType.GREATER;
import static Common.TokenType.GREATER_EQUAL;
import static Common.TokenType.LESS;
import static Common.TokenType.LESS_EQUAL;
import static Common.TokenType.NOT_EQUAL;
import static Common.Type.*;

public class BinaryOpNode implements WhereClassInterface {

    public InterfaceOperandNode Left;
    public InterfaceOperandNode Right;
    public TokenType Operator;
    public BinaryOpNode(InterfaceOperandNode left, TokenType Operator, InterfaceOperandNode right) {
        this.Left = left;
        this.Operator = Operator;
        this.Right = right;
    }


    @Override
    public boolean evaluate(ArrayList<Object> row, Schema S){
        Object leftval = Left.evaluate(row);
        Object rightval = Right.evaluate(row);
        if (Left.getType() != Right.getType()){
            // Error message
            // throw new Exception("Unexpected " + rightType + ", expected " + leftType ".");
        }
        if(Operator == TokenType.IS) return Right.evaluate(row) == null && Left.evaluate(row) == null;
        if(Operator == TokenType.NOT) return !(Right.evaluate(row) == null && Left.evaluate(row) == null);
        if(leftval == null || rightval == null){
            return false;
        }
        switch(Left.getType()){
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
            default ->{
                int compare = ((String)Left.evaluate(row)).compareTo((String)Right.evaluate(row));
                switch(Operator) {
                    case EQUAL -> {return compare == 0;}
                    case NOT_EQUAL -> {return compare != 0;}
                    case GREATER_EQUAL -> {return compare >= 0;}
                    case LESS_EQUAL -> {return compare <= 0;}
                    case GREATER ->  {return compare > 0;}
                    case LESS ->  {return compare < 0;}
                    default ->  { }
                }
            }
        }
        return false;
    }

    @Override
    public int getPrescendance() {
        return 0;
    }

    @Override
    public String print() {
        return "(" + Left.print() + " " + Operator + " " + Right.print() + ")";
    }


}
