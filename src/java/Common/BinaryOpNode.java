package Common;

public class BinaryOpNode implements WhereClassInterface {

    @Override
    public boolean evalute() {
        return false;
    }

    @Override
    public int getPrescendance() {
        return 0;
    }

    public BinaryOpNode(WhereClassInterface left, WhereClassInterface right) {}
}
