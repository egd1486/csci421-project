package Common.WhereTree;

import java.util.ArrayList;

public class OrNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    public OrNode(WhereClassInterface left, WhereClassInterface right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(ArrayList<Object> row) {
        return left.evaluate(row) || right.evaluate(row);
    }

    @Override
    public int getPrescendance() {
        return 0;
    }
}
