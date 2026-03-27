package Common.WhereTree;

import Common.TokenType;
import Common.Type;

import javax.lang.model.type.ArrayType;
import java.util.ArrayList;

public class ArithmeticOpNode implements InterfaceOperandNode {
    InterfaceOperandNode Left;
    InterfaceOperandNode Right;
    TokenType AlgebraOperator;

    public ArithmeticOpNode(InterfaceOperandNode Left, TokenType AlgebraOperator, InterfaceOperandNode Right) {
        this.Left = Left;
        this.Right = Right;
        this.AlgebraOperator = AlgebraOperator;
    }

    @Override
    public Type getType() {
        Type leftType = Left.getType();
        Type rightType = Right.getType();

        if (leftType == Type.DOUBLE || rightType == Type.DOUBLE) {
            return Type.DOUBLE;
        }
        return Type.INT;
    }

    @Override
    public Object evaluate(ArrayList<Object> row) {
        switch (Left.getType()) {
            case INT -> {
                switch (AlgebraOperator) {
                    case PLUS -> {
                        return (int) Left.evaluate(row) + (int) Right.evaluate(row);
                    }
                    case MINUS -> {
                        return (int) Left.evaluate(row) - (int) Right.evaluate(row);
                    }
                    case MULT -> {
                        return (int) Left.evaluate(row) * (int) Right.evaluate(row);
                    }
                    case DIV -> {
                        return (int) Left.evaluate(row) / (int) Right.evaluate(row);
                    }
                }
            }
            case DOUBLE -> {
                switch (AlgebraOperator) {
                    case PLUS -> {
                        return (double) Left.evaluate(row) + (double) Right.evaluate(row);
                    }
                    case MINUS -> {
                        return (double) Left.evaluate(row) - (double) Right.evaluate(row);
                    }
                    case MULT -> {
                        return (double) Left.evaluate(row) * (double) Right.evaluate(row);
                    }
                    case DIV -> {
                        return (double) Left.evaluate(row) / (double) Right.evaluate(row);
                    }
                }
            }
            default -> {return null;}
        }
        return null;
    }

    @Override
    public String print() {
        return "(" + Left.print() + " " + AlgebraOperator + " " + Right.print() + ")";
    }
}
