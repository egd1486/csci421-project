package Common.WhereTree;

import Catalog.*;
import Common.Attribute;
import Common.Type;
import java.util.ArrayList;

public class AttributeValueNode implements InterfaceOperandNode {
    String ColumnName; // Good to have
    Attribute attribute_node;
    Schema S;
    int columnIndex;

    public AttributeValueNode(String table_name, String ColumnName, ArrayList<String> tables) throws Exception {

        Schema S = Catalog.GetSchema(table_name);

        //We then get the Attribute_node by getting it from the Schema by column name
        attribute_node = Schema.getAttribute(ColumnName, S);

        //For safe measures
        this.ColumnName = ColumnName;

        //Geting a ColumnIndex of ArrayList<row>
        this.columnIndex = S.Attributes.indexOf(attribute_node);

        // Updating column index if Cartesian product
        for(String table : tables){
            Schema curr = Catalog.GetSchema(table);
            if(curr == null) throw new Exception("Table " + table + " does not exist");
            if(curr.Name.equals(table_name.toUpperCase())) break;
            columnIndex += curr.Attributes.size();
        }

        //Saving what schema it belongs too
        this.S = S;
    }

    public Attribute get_attribute_node() {
        return attribute_node;
    }

    @Override
    public Type getType(){
        return attribute_node.type;
    }

    @Override
    public Object evaluate(ArrayList<Object> row) {
        return row.get(columnIndex);
    }

    @Override
    public String print() {
        if (S == null) {
            return ColumnName;
        }
        return S.Name + "." + ColumnName;
    }

    public Object get_Object(ArrayList<Object> row){
        return row.get(columnIndex);
    }

}
