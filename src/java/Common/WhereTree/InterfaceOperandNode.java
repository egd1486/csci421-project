package Common.WhereTree;

import Common.Type;
import java.util.ArrayList;

public interface InterfaceOperandNode {
    Type getType();
    Object evaluate(ArrayList<Object> row);
}
