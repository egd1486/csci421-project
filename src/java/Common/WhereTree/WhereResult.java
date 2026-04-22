package Common.WhereTree;

public class WhereResult {
    public WhereClassInterface WhereNode;
    public int Index;
    public WhereClassInterface Simplified;

    public WhereResult(WhereClassInterface whereNode, int index) {
        WhereNode = whereNode;
        Index = index;
        Simplified = null;
    }

    public WhereResult(WhereClassInterface whereNode, int index, WhereClassInterface simplified) {
        WhereNode = whereNode;
        Index = index;
        Simplified = simplified;
    } 
}
