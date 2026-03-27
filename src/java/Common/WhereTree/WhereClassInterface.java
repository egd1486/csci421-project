package Common.WhereTree;

import java.util.ArrayList;

import Catalog.Schema;

public interface WhereClassInterface {

        boolean evaluate(ArrayList<Object> row, Schema S);

        int getPrescendance();

        String print();
}
