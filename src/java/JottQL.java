import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import Common.Parser;

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
        Parser parser =  new Parser();
        Path path = Paths.get(dbLocation);
        System.out.println("Accessing database location...");
        if(Files.exists(path)){
            // Restart existing database
            System.out.println("Database found. Restarting database...");
            System.out.println("Ignoring provided page size. Using prior size of ____...");
        }
        else{
            System.out.println("No database found. Creating new database...");
        }

        // Entering infinite loop and prompting for JottQL commands
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("Enter Command: ");
            String command = scanner.nextLine();
            // Do something with command here
            if(command.equalsIgnoreCase("quit")){
                // Write the catalog to hardware
                // Purge the page buffer
                break;
            }
        }
        scanner.close();
    }
}