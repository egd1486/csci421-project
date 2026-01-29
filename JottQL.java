import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        if(args[3].toLowerCase().equals("true")){
            indexing = true;
        }
        else if(args[3].toLowerCase().equals("false")){
            indexing = false;
        }
        else{
            System.out.println("<indexing> expected boolean value, got " + args[3]);
            return;
        }

        // Checking if DB exists at specified location
        Path path = Paths.get(dbLocation);
        if(Files.exists(path)){
            // Restart existing database
        }
        else{
            // Create new database
        }

        // Entering infinite loop and prompting for JottQL commands
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("Enter Command: ");
            String command = scanner.nextLine();
            // Do something with command here
            if(command.toLowerCase().equals("quit")){
                break;
            }
        }
        scanner.close();
    }
}