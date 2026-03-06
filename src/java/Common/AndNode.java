package Common;

public class AndNode implements WhereClassInterface {
    public WhereClassInterface left;
    public WhereClassInterface right;

    @Override
    public boolean evalute() {
        return false;
    }

    @Override
    public int getPrescendance() {
        return 0;
    }
}
