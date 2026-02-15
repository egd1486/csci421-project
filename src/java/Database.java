package StorageManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/* Jason */
public class Database{

    //Each StorageManager.Database will have a number of pages and the name of the database e
    public static String database_name;
    public static int database_buffer_size;

    public Database(String name, int size_bytes) throws IOException {
        database_name = name;
        database_buffer_size = size_bytes;

        //Create a RandomAccessFile
        RandomAccessFile database_access = new RandomAccessFile(database_name,"rw");
        byte[] database = new byte[database_buffer_size];
        database_access.write(database);
        database_access.close();

    }



    public static String[] exist_database(String name){
        database_name = name;
        File database_file = new File(database_name);
        if(database_file.exists()){
            return new String[]{"True", String.valueOf(database_file.length())};
        }
        else{
            return new String[]{"False"};
        }
    }





    public static void main(String[] args) throws IOException {
        new Database("test", 1000);
        System.out.println(Arrays.toString(exist_database("test")));
    }
}