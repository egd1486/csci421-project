package Common;

public class Parser {
    // Creates a database schema in the catalog
    public void createTable(String tableName, String attr, String type, String[] constraints){
        // check if table exists in catalog
        // if so, display error
        // if not, use catalog to create schema and add it to catalog
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
    public void drop(String tableName){
        // call buffer manager to get data from database
        // delete all data
        // call catalog to delete schema from catalog
    }

    // Adds an attribute to a table
    public void alterAdd(String tableName, String attrName, Type type, Boolean notNull, String value){
        // get schema from catalog, add new attribute with specifications given
    }

    // Removes an attribute from a table
    public void alterDrop(String tableName, String attrName){
        // get schema from catalog, delete attribute from it
    }
}
