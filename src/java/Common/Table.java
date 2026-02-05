package Common;

import java.util.ArrayList;

public class Table {
    private String name;
    private ArrayList<Attribute> attributes; 

}

//  Table "students" (TableSchema)
// │   ├── Attributes: id, name, gpa, major
// │   ├── Record 1: (123, "Alice", 3.5, "CS")
// │   ├── Record 2: (124, "Bob", 3.8, "Math")
// │   └── ... (stored across Pages 5, 6, 7...)

