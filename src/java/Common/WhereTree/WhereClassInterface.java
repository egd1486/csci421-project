package Common.WhereTree;

import java.util.ArrayList;

public interface WhereClassInterface {

        boolean evaluate(ArrayList<Object> row);

        int getPrescendance();

}
