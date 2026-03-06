package Common;

import Catalog.Schema;

public class AttributeValueNode implements InterfaceOperandNode {
    String ColumnName; // Good to have
    Attribute attribute_node;

    public AttributeValueNode(Schema S, String ColumnName) throws Exception {
        //We then get the Attribute_node by getting it form the Schema by column name
        attribute_node = Schema.getAttribute(ColumnName, S);

        //For safe measures
        this.ColumnName = ColumnName;
    }

    public Attribute eval() {
        return attribute_node;
    }

    @Override
    public Object evalute() {
        return null;
    }
}
