package Common;
import Catalog.Catalog;
import Catalog.Schema;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    public static void parse(String command){
        if(command == null || command.isEmpty()){
            System.out.println("Invalid command");
            return;
        }

        // Getting rid of leading/trailing white space and ending semicolon
        command = command.trim().substring(0, command.length()-1);
        String[] keywords = command.split(" ");

        try {
            if(command.startsWith("CREATE TABLE")){
                int start = command.indexOf("(");
                int end = command.length() - 1;
                // Keywords = CREATE TABLE <tableName>
                keywords = command.substring(0, start).trim().split(" ");

                if(keywords.length != 3) throw new Exception("Invalid command");

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
            }
            else if(command.startsWith("SELECT * FROM")){
                // Only need to handle select all for first phase
                if(keywords.length == 4){
                    select(keywords[3]);
                }
            }
            else if(command.startsWith("INSERT")){
                int start = command.indexOf("(");
                int end = command.length();
                // Keywords = INSERT <tableName> VALUES
                keywords = command.substring(0, start).trim().split(" ");
                if(keywords.length != 3){
                    throw new Exception("Invalid command");
                }
                if(!keywords[2].equals("VALUES")){
                    throw new Exception("Invalid command");
                }

                // Values to pass to insert
                String tableName = keywords[1];
                String values = command.substring(start, end).trim();
                // Remove opening and closing parenthesis
                if(values.startsWith("(") && values.endsWith(")")){
                    values = values.substring(1, values.length()-1);
                }
                else{
                    throw new Exception("Invalid command");
                }
                ArrayList<ArrayList<String>> rowsList = new ArrayList<>();
                ArrayList<String> row = new ArrayList<>();
                boolean inQuotes = false;
                StringBuilder valuesBuilder = new StringBuilder();
                // Iterating through input values
                for(int i = 0; i < values.length(); i++){
                    // Checks every character and adds it to the current value
                    // Only splits when comma is found outside of quotes.
                    char c = values.charAt(i);
                    if(c == '"'){
                        inQuotes = !inQuotes;
                        valuesBuilder.append(c);
                    }
                    else if (c == ' ' && !inQuotes) {
                        String value = valuesBuilder.toString().trim();
                        if(!value.isEmpty()){
                            if(value.startsWith("\"") && value.endsWith("\"") && value.length() == 2){
                                row.add("");
                            }
                            else if(value.startsWith("\"") && value.endsWith("\"") && value.length() > 2){
                                row.add(value.substring(1, value.length()-1));
                            }
                            else{
                                row.add(value);
                            }
                        }
                        valuesBuilder.setLength(0);
                    }
                    else if(c == ',' && !inQuotes){
                        String value = valuesBuilder.toString().trim();
                        if(!value.isEmpty()) row.add(value);
                        rowsList.add(row);
                        row = new ArrayList<>();
                        valuesBuilder.setLength(0);
                    }
                    else{
                        valuesBuilder.append(c);
                    }
                }
                // Adding last value to list
                String lastValue = valuesBuilder.toString().trim();
                if(!lastValue.isEmpty()){
                    row.add(lastValue);
                }
                rowsList.add(row);

                insert(tableName, rowsList);
            }
            else if(command.startsWith("DROP TABLE")){
                if(keywords.length == 3){
                    dropTable(keywords[2]);
                }
            }
            else if(command.startsWith("ALTER TABLE")){
                keywords = command.split(" ");
                if (keywords.length < 5 || !keywords[0].equals("ALTER") || !keywords[1].equals("TABLE")) {
                    throw new Exception("Invalid command");
                }

                // Values to pass to createTable
                String tableName = keywords[2];
                String action = keywords[3];
                String attrName = keywords[4];

                // Checking if syntax is correct / calling drop command
                if(!action.equals("ADD") && !action.equals("DROP")){
                    throw new Exception("Invalid command");
                }
                if(action.equals("DROP")){
                    alterDrop(tableName, attrName);
                    return;
                }
                if(keywords.length < 6 || keywords.length > 9){
                    throw new Exception("Invalid command");

                }

                // Getting type and type size for ADD command
                Type type = null;
                int typeSize = -1;
                String typeString = keywords[5];
                if(typeString.startsWith("CHAR(") || typeString.startsWith("VARCHAR(")){
                    int left = typeString.indexOf("(");
                    int right = typeString.indexOf(")");
                    if(typeString.startsWith("CHAR")){
                        type = Type.CHAR;
                    }
                    else{
                        type = Type.VARCHAR;
                    }
                    typeSize = Integer.parseInt(typeString.substring(left+1, right));
                }
                else{
                    switch(typeString){
                        case "INTEGER":
                            type = Type.INT;
                            typeSize = Integer.BYTES;
                            break;
                        case "DOUBLE":
                            type = Type.DOUBLE;
                            typeSize = Double.BYTES;
                            break;
                        case "BOOLEAN":
                            type = Type.BOOLEAN;
                            typeSize = 1;
                            break;
                        default:
                            throw new Exception("Invalid command");
                    }
                }

                // Getting conditionals if needed
                // Calling Add command once all data obtained
                if(keywords.length == 6){
                    alterAdd(tableName, attrName, type, typeSize, false, false, null);
                }
                else if(keywords.length == 8){
                    String hasDefault = keywords[6];
                    String defaultVal =  keywords[7];
                    if(hasDefault.equals("DEFAULT")){
                        if(defaultVal != null && defaultVal.charAt(0) == '"' && defaultVal.charAt(defaultVal.length() - 1) == '"'){
                            String defaultNoQuotes = defaultVal.substring(1, defaultVal.length()-1);
                            alterAdd(tableName, attrName, type, typeSize, false, true, defaultNoQuotes);
                        }
                    }
                }
                else if(keywords.length == 9){
                    String condition1 = keywords[6];
                    String condition2 = keywords[7];
                    String defaultVal = keywords[8];
                    if(!condition1.equals("NOTNULL") || !condition2.equals("DEFAULT")){
                        throw new Exception("Invalid command");
                    }
                    if(defaultVal != null && defaultVal.charAt(0) == '"' && defaultVal.charAt(defaultVal.length() - 1) == '"'){
                        String defaultNoQuotes = defaultVal.substring(1, defaultVal.length()-1);
                        alterAdd(tableName, attrName, type, typeSize, true, true, defaultNoQuotes);
                    }
                    else{
                        alterAdd(tableName, attrName, type, typeSize, true, true, defaultVal);
                    }
                }
                else{
                    throw new Exception("Invalid command");
                }
            }
            else{
                throw new Exception("Invalid command");
            }
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Creates a database schema in the catalog
    public static void createTable(String tableName, String[] attr, Type[] type, int[] typeSize, String[] constraints){
        try{
            // Creating new table schema
            Schema schema = Catalog.AddSchema(tableName);
            System.out.println(Arrays.toString(attr));
            // Populating table schema with attributes
            for(int i = 0; i < attr.length; i++){
                boolean nullable = false;
                boolean primary = false;
                boolean unique = false;
                if(constraints != null && constraints[i] != null){
                    for(String c : constraints[i].split(" ")){
                        switch(c){
                            case "NOTNULL":
                                nullable = true;
                                break;
                            case "PRIMARYKEY":
                                primary = true;
                                break;
                            case "UNIQUE":
                                unique = true;
                                break;
                        }
                    }
                }
                schema.AddAttribute(attr[i], type[i], typeSize[i], nullable, primary, unique, null);
            }
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Selects and displays all data from a table
    public static void select(String tableName){
        Schema schema = Catalog.GetSchema(tableName);
        if(schema == null){
            System.out.println("Table: " + tableName + " does not exist");
            return;
        }

        ArrayList<ArrayList<Object>> Data = schema.Select();
        schema.DisplayTable(Data);
    }

    // Inserts a record into a table
    public static void insert(String tableName, ArrayList<ArrayList<String>> rows){
        Schema schema = Catalog.GetSchema(tableName);
        if(schema == null){
            System.out.println("Table: " + tableName + " does not exist");
            return;
        }
        int success = 0;
        try{
            for (ArrayList<String> S_Row : rows) {
                ArrayList<Attribute> attributes = schema.Attributes;

                // Checking if number of attributes match
                if (attributes.size() != S_Row.size())
                throw new Exception("Invalid number of attributes");


                ArrayList<Object> Row = new ArrayList<>();
                // Loop through row and parse strings via attribute method,
                for (int i = 0; i < attributes.size(); i++)
                Row.add(attributes.get(i).Parse(S_Row.get(i)));

                schema.Insert(Row);
                success++;
            }
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println(success + " rows inserted successfully");
    }

    // Removes table and all of its data from database, removes schema from catalog
    public static void dropTable(String tableName){
        try {Catalog.RemoveSchema(tableName);}

        catch (Exception e){
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.println("Table: " + tableName + " dropped successfully");
    }

    // Adds an attribute to a table
    public static void alterAdd(String tableName, String attrName, Type type, Integer typeSize, Boolean nullable, Boolean hasDefault, Object defaultVal){
        // Get schema from catalog, add new attribute with specifications given
        Schema schema = Catalog.GetSchema(tableName);
        if(schema == null){
            System.out.println("Table: " + tableName + " does not exist");
            return;
        }
        try{
            // Tries to add an attribute to the schema
            System.out.println(defaultVal);
            Catalog.AttributeAdd(tableName, attrName, type, typeSize, nullable, false, false, defaultVal);
        } catch(Exception e){
            System.out.println("Error2: " + e.getMessage());
            return;
        }

        System.out.println("Attribute: " + attrName + " added successfully");
    }

    // Removes an attribute from a table
    public static void alterDrop(String tableName, String attrName){
        // Get schema from catalog, delete attribute from it
        Schema schema = Catalog.GetSchema(tableName);
        if(schema == null){
            System.out.println("Table: " + tableName + " does not exist");
            return;
        }

        try{
            Catalog.AttributeDrop(tableName, attrName);
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.println("Attribute: " + attrName + " dropped successfully");
    }
}
