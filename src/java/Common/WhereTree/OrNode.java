package Common.WhereTree;

public class OrNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    public OrNode(WhereClassInterface left, WhereClassInterface right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate() {
        return left.evaluate() || right.evaluate();
    }

    @Override
    public int getPrescendance() {
        return 0;
    }
}
