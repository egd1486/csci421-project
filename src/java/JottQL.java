import StorageManager.StorageManager;
import java.io.IOException;
import java.util.Scanner;
import Common.Tokenizer;
import Common.Parser;


public class JottQL{

    public static void main(String[] args) throws IOException {
        // Parsing command line arguments
        if(args.length != 4){
            System.out.println("Usage: java JottQL <dbLocation> <pageSize> <bufferSize> <indexing>");
            return;
        }
        String dbLocation = args[0] + "/database.txt";
        int pageSize = Integer.parseInt(args[1]);
        int bufferSize = Integer.parseInt(args[2]);
        boolean indexing = true;
        if(args[3].equalsIgnoreCase("true")){
            indexing = true;
            Parser.Indexing = true;
        }
        else if(args[3].equalsIgnoreCase("false")){
            indexing = false;
            Parser.Indexing = false;
        }
        else{
            System.out.println("<indexing> expected boolean value, got " + args[3]);
            return;
        }

        // Initialize database through Storage Manager,
        try {StorageManager.Init(dbLocation, pageSize, bufferSize);}
        catch (Exception e) {
            System.out.println(e);
            return;
        }

        // For logging purposes, running in a debug terminal.
        for (String arg : args)System.out.print(arg+" ");
        System.out.println();

        // Entering infinite loop and prompting for JottQL commands
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("Enter Command: ");
            String line = scanner.nextLine().trim();
            if(line.trim().equals("<QUIT>")){
                scanner.close();
                break;
            }
            StringBuilder builder = new StringBuilder(line);
            while(!line.endsWith(";")){
                line = scanner.nextLine().trim();
                builder.append(" ").append(line);
            }
            String command = builder.toString().trim();
            System.out.println("Command: " + command);

            long t1 = System.currentTimeMillis();
            try {Parser.parse(Tokenizer.tokenize(command));}
            catch (Exception e) {System.out.println(e);}
            System.out.println("Time: " + (System.currentTimeMillis() - t1) + "ms");
            System.out.println("Indexing " + (indexing ? "ON" : "OFF"));
        }

        // Shutdown once the loop ends.
        try {StorageManager.Shutdown();}

        catch (Exception e){
            System.out.println(e);
        }
    }
}