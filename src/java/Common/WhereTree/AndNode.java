package Common.WhereTree;

public class AndNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    public AndNode(WhereClassInterface left, WhereClassInterface right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evalute() {
        return left.evalute() && right.evalute();
    }

    @Override
    public int getPrescendance() {
        return 1;
    }
}
