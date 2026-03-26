package Common.WhereTree;
import java.util.ArrayList;

public class AndNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    public AndNode(WhereClassInterface left, WhereClassInterface right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(ArrayList<Object> row) {
        return left.evaluate(row) && right.evaluate(row);
    }

    @Override
    public int getPrescendance() {
        return 1;
    }

    @Override
    public String print() {
        return "(" + left.print() + " AND " + right.print() + ")";
    }

}
