package Common.WhereTree;

import Catalog.Schema;
import Common.Attribute;
import Common.Type;

import java.util.ArrayList;

public class AttributeValueNode implements InterfaceOperandNode {
    String ColumnName; // Good to have
    Attribute attribute_node;
    int columnIndex;

    public AttributeValueNode(Schema S, String ColumnName) throws Exception {
        //We then get the Attribute_node by getting it form the Schema by column name
        attribute_node = Schema.getAttribute(ColumnName, S);

        //For safe measures
        this.ColumnName = ColumnName;

        //Geting a ColumnIndex of ArrayList<row>
        this.columnIndex = S.Attributes.indexOf(attribute_node);
    }

    public Attribute get_attribute_node() {
        return attribute_node;
    }

    @Override
    public Type evalute() {
        return attribute_node.type;
    }

    public Object get_Object(ArrayList<Object> row){
        return row.get(columnIndex);
    }

}
