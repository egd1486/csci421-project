import java.io.IOException;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import Common.Parser;
import StorageManager.StorageManager;
import BufferManager.BufferManager;

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

        // Initialize database through Storage Manager,
        StorageManager.initializeDatabaseFile(dbLocation, pageSize);

        // Initialize Buffer Manager with bufferSize
        BufferManager.initialize(bufferSize);

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
            Parser.parse(command);
        }
    }
}