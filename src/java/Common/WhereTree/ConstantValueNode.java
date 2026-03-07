package Common.WhereTree;

import Common.Type;
import java.util.ArrayList;

public class ConstantValueNode implements InterfaceOperandNode {

    //Idk if we rlly need this lol
    Object value;
    Type type;

    public ConstantValueNode(Object value, Type type) {
        this.value = value;
        this.type = type;
    }
    @Override
    public Type getType(){
        return type;
    }
    @Override
    public Object evaluate(ArrayList<Object> row){
        return value;
    }
}
