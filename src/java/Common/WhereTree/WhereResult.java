package Common.WhereTree;

public class WhereResult {
    public WhereClassInterface WhereNode;
    public int Index;

    public WhereResult(WhereClassInterface whereNode, int index) {
        WhereNode = whereNode;
        Index = index;
    }
}
