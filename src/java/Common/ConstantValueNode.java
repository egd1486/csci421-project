package Common;

public class ConstantValueNode implements InterfaceOperandNode {

    //Idk if we rlly need this lol
    Object value;
    public ConstantValueNode(Object value) {
        this.value = value;
    }
    @Override
    public Object evalute(){
        return value;
    }
}
