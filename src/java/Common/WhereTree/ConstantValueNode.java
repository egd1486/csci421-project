package Common.WhereTree;

import Common.Type;

public class ConstantValueNode implements InterfaceOperandNode {

    //Idk if we rlly need this lol
    Object value;
    Type type;

    public ConstantValueNode(Object value, Type type) {
        this.value = value;
        this.type = type;
    }
    @Override
    public Type evalute(){
        return type;
    }
}
