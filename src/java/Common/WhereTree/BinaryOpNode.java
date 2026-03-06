package Common.WhereTree;

import Common.TokenType;
import Common.Type;

public class BinaryOpNode implements WhereClassInterface {

    InterfaceOperandNode Left;
    InterfaceOperandNode Right;
    TokenType Operator;

    public BinaryOpNode(InterfaceOperandNode left, TokenType Operator, InterfaceOperandNode right) {
        this.Left = left;
        this.Operator = Operator;
        this.Right = right;
    }


    @Override
    public boolean evalute() {
        Type leftType = Left.evalute();
        Type rightType = Right.evalute();
        switch(Operator) {
            case EQUAL -> {   }
            case NOT_EQUAL -> { }
            case GREATER_EQUAL -> { }
            case LESS_EQUAL -> {  }
            case GREATER ->  {  }
            case LESS ->  {  }
            default ->  { }
        }
        return false;
    }

    @Override
    public int getPrescendance() {
        return 2;
    }



}
