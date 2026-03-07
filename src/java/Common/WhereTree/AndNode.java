package Common.WhereTree;

public class AndNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    public AndNode(WhereClassInterface left, WhereClassInterface right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate() {
        return left.evaluate() && right.evaluate();
    }

    @Override
    public int getPrescendance() {
        return 1;
    }
}
