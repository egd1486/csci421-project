package Common;
import BufferManager.BufferManager;
import Catalog.Catalog;
import Catalog.Schema;
import java.util.ArrayList;
import java.util.Arrays;

public class Parser {
    private final BufferManager bufferManager;

    public Parser(BufferManager bufferManager) {
        this.bufferManager = bufferManager;
    }

    public void parse(String command){
        if(command == null || command.isEmpty()){
            System.out.println("Invalid command");
        }

        // Getting rid of leading/trailing white space and semicolon
        command = command.trim();
        command = command.substring(0, command.length()-1);
        String[] keywords = command.split(" ");
        try{
            if(command.startsWith("CREATE TABLE")){
                int start = command.indexOf("(");
                int end = command.length() - 1;
                // Keywords = CREATE TABLE <tableName>
                keywords = command.substring(0, start).trim().split(" ");
                if(keywords.length != 3){
                    throw new Exception("Invalid command");
                }

                // Values to pass to createTable
                String tableName = keywords[2];
                String[] attributes = command.substring(start+1, end).trim().split(",");
                String[] attr = new String[attributes.length];
                Type[] type = new Type[attributes.length];
                int[] typeSize = new int[attributes.length];
                String[] constraints = new String[attributes.length];
                int index = 0;

                // Iterating over given attributes and parsing into usable data
                for(String a : attributes){
                    String[] attrData = a.trim().split(" ");
                    attr[index] = attrData[0];
                    if(!attrData[1].equals(attrData[1].toUpperCase())){
                        throw new Exception("Invalid attribute");
                    }
                    switch(attrData[1]){
                        case "INTEGER":
                            type[index] = Type.INT;
                            typeSize[index] = Integer.BYTES;
                            break;
                        case "DOUBLE":
                            type[index] = Type.DOUBLE;
                            typeSize[index] = Double.BYTES;
                            break;
                        case "BOOLEAN":
                            type[index] = Type.BOOLEAN;
                            typeSize[index] = 1;
                            break;
                    }
                    int left = attrData[1].indexOf("(");
                    int right = attrData[1].indexOf(")");
                    if(attrData[1].startsWith("CHAR")){
                        type[index] = Type.CHAR;
                        typeSize[index] = Integer.parseInt(attrData[1].substring(left+1, right));
                    }
                    else if(attrData[1].startsWith("VARCHAR")){
                        type[index] = Type.VARCHAR;
                        typeSize[index] = Integer.parseInt(attrData[1].substring(left+1, right));
                    }
                    StringBuilder c = new StringBuilder();
                    for(int i = 2; i < attrData.length; i++){
                        c.append(attrData[i]).append(" ");
                    }
                    constraints[index] = c.toString();
                    index++;
                }
                createTable(tableName, attr, type, typeSize, constraints);
                return;
            }
            else if(command.startsWith("SELECT * FROM")){
                // Only need to handle select all for first phase
                if(keywords.length == 4){
                    select(keywords[3]);
                    return;
                }
            }
            else if(command.startsWith("INSERT")){
                return;
            }
            else if(command.startsWith("DROP TABLE")){
                if(keywords.length == 3){
                    dropTable(keywords[2]);
                    return;
                }
            }
            else if(command.startsWith("ALTER TABLE")){
                // Must handle alter add / alter drop
                return;
            }
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("Invalid Command");
    }

    // Creates a database schema in the catalog
    public void createTable(String tableName, String[] attr, Type[] type, int[] typeSize, String[] constraints){
        // Checking if table already exists
        if(Catalog.GetSchema(tableName) != null){
            System.out.println("Table: " + tableName + " already exists");
            return;
        }
        try{
            // Creating new table schema
            Schema schema = Catalog.AddSchema(tableName);
            // Populating table schema with attributes
            for(int i = 0; i < attr.length; i++){
                boolean nullable = false;
                boolean primary = false;
                boolean unique = false;
                if(constraints != null && constraints[i] != null){
                    for(String c : constraints[i].split(" ")){
                        switch(c){
                            case "NULLABLE":
                                nullable = true;
                                break;
                            case "PRIMARY":
                                primary = true;
                                break;
                            case "UNIQUE":
                                unique = true;
                                break;
                        }
                    }
                }
                schema.AddAttribute(attr[i], type[i], typeSize[i], nullable, primary, unique);
            }
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Selects and displays all data from a table
    public void select(String tableName){
        // use catalog to get location of data in database
        // when page/pages containing table data are found,
        // read all the data one page at a time
        // display all data in nice format

        // is buffer manager used instead of finding/reading data manually?
    }

    // Inserts a record into a table
    public void insert(String tableName, String[] values){
        // check catalog to see if table exists
        // if it doesn't exist, print error message
        // otherwise, use buffer manager to get data and add record to it
    }

    // Removes table and all of its data from database, removes schema from catalog
    public void dropTable(String tableName){
        // call buffer manager to get data from database
        // delete all data
        // call catalog to delete schema from catalog
    }

    // Adds an attribute to a table
    public void alterAdd(String tableName, String attrName, Type type, Boolean notNull, String value){
        // get schema from catalog, add new attribute with specifications given
        Schema schema = Catalog.GetSchema(tableName);

    }

    // Removes an attribute from a table
    public void alterDrop(String tableName, String attrName){
        // get schema from catalog, delete attribute from it
        Schema schema = Catalog.GetSchema(tableName);

    }
}
