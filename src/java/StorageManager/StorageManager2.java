package StorageManager;

import Catalog.Catalog;
import Common.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import Catalog.Schema;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Array;

public class StorageManager {
    public static int PageSize;
    public static boolean CanShort;
    private static String Filename;
    private static int PageCount;
    private static Stack<Integer> FreePages;
    // Save 3 integers for page size, page counter, and top free page.
    private static final int PageStart = Integer.BYTES * 3; 

    public static void Init(String database_name, int page_size) throws IOException {
        PageCount = 1; // Assume page 0 is reserved for the catalog.
        Filename = database_name;
        FreePages = new Stack<>();
        
        File DBFile = new File(database_name);
        System.out.println("Accessing database location...");

        RandomAccessFile raf = new RandomAccessFile(DBFile, "r");
        raf.seek(0);

        // Parse DB params, or prepare them for the new file
        byte[] DBParameters = new byte[PageStart];

        // ByteBuffer for easy parsing,
        ByteBuffer DBParams = ByteBuffer.wrap(DBParameters); 

        if (DBFile.exists()) {
            System.out.println("Database found. Restarting database...");

            // DB exists, read params from the top,
            
            raf.readFully(DBParameters);

            PageSize = DBParams.getInt(0);
            PageCount = DBParams.getInt(Integer.BYTES);
            int TopFree = DBParams.getInt(Integer.BYTES * 2);
            
            System.out.println("Ignoring provided page size. Using prior size of " + PageSize);

            // If TopFree has a nonzero value, we have real free page(s) to consider.
            if (TopFree > 0) {
                // Prepare a List so we can stack the pages in order.
                ArrayList<Integer> FreePageList = new ArrayList<>();
                FreePageList.add(TopFree);

                while (true) {
                    raf.seek(PageStart + (TopFree * PageSize));
                    

                }
            }
        } else {
            System.out.println("Database file not found. Creating new database...");
            DBFile.createNewFile();

            // Write DB params to top of the file
            DBParams.putInt(0, page_size);
            DBParams.putInt(Integer.BYTES, PageCount);
            DBParams.putInt(Integer.BYTES * 2, 0); // No pages freed yet.

            raf.write(DBParameters);
        }

        raf.close();

        // Determine whether we can use short indexing from page size
        CanShort = page_size <= Short.MAX_VALUE;
    }

    public static int CreatePage() {
        if (FreePages.size() == 1)

        if (!FreePages.isEmpty()) return FreePages.pop();
    }
}