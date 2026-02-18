import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import Common.Parser;
import StorageManager.StorageManager;
import BufferManager.BufferManager;
import Catalog.Catalog;

public class JottQL{

    public static void main(String[] args){
        // Parsing command line arguments
        if(args.length != 4){
            System.out.println("Usage: java JottQL <dbLocation> <pageSize> <bufferSize> <indexing>");
            return;
        }
        String dbLocation = args[0];
        int pageSize = Integer.parseInt(args[1]);
        int bufferSize = Integer.parseInt(args[2]);
        boolean indexing = true;
        if(args[3].equalsIgnoreCase("true")){
            indexing = true;
        }
        else if(args[3].equalsIgnoreCase("false")){
            indexing = false;
        }
        else{
            System.out.println("<indexing> expected boolean value, got " + args[3]);
            return;
        }

        // Checking if DB exists at specified location
        Path path = Paths.get(dbLocation);
        System.out.println("Accessing database location...");
        StorageManager storageManager;
        if(Files.exists(path)){
            // Restart existing database
            System.out.println("Database found. Restarting database...");
            System.out.println("Ignoring provided page size. Using prior size of ____...");
            // How to get previous page size?
            storageManager = new StorageManager(dbLocation, pageSize);
        }
        else{
            System.out.println("No database found. Creating new database...");
            storageManager = new StorageManager(dbLocation, pageSize);
            //storageManager.createDatabaseFile();
        }

        BufferManager bufferManager = new BufferManager(bufferSize,storageManager);
        Parser parser =  new Parser(bufferManager, storageManager);

        // Entering infinite loop and prompting for JottQL commands
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("Enter Command: ");
            String line = scanner.nextLine().trim();
            if(line.trim().equals("<QUIT>")){
                scanner.close();
                return;
            }
            StringBuilder builder = new StringBuilder(line);
            while(!line.endsWith(";")){
                line = scanner.nextLine().trim();
                builder.append(" ").append(line);
            }
            String command = builder.toString().trim();
            System.out.println("Command: " + command);
            parser.parse(command);
        }
    }
}