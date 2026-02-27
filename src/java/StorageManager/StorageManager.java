package StorageManager;

import Common.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

import BufferManager.BufferManager;
import Catalog.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;



public class StorageManager {
    public static int PageSize;
    public static boolean CanShort;
    private static String Filename;
    private static int PageCount;
    private static Stack<Integer> FreePages;
    // Save 3 integers for page size, page counter, and top free page.
    private static final int PageStart = Integer.BYTES * 3; 

    public static void Init(String db_name, int page_size, int buffer_size) throws Exception {
        PageCount = 1; // Assume page 0 is reserved for the catalog.
        Filename = db_name;
        FreePages = new Stack<>();
        PageSize = page_size;
        // Determine whether we can use short indexing from page size
        CanShort = page_size <= Short.MAX_VALUE;
        
        // Initialize Buffer Manager with bufferSize
        BufferManager.initialize(buffer_size);


        File DBFile = new File(db_name);
        System.out.println("Accessing database location...");

        // Parse DB params, or prepare them for the new file
        RandomAccessFile raf = null;
        byte[] DBParameters = new byte[PageStart];

        // ByteBuffer for easy parsing,
        ByteBuffer DBParams = ByteBuffer.wrap(DBParameters); 

        if (DBFile.exists()) {
            System.out.println("Database found. Restarting database...");

            // DB exists, read params from the top,
            raf = new RandomAccessFile(DBFile, "r");
            raf.seek(0);
            raf.readFully(DBParameters);

            PageSize = DBParams.getInt(0);
            PageCount = DBParams.getInt(Integer.BYTES);
            int FreePagePtr = DBParams.getInt(Integer.BYTES * 2);

            // Re-determine whether we can use short indexing from page size, since size may have changed here.
            CanShort = PageSize <= Short.MAX_VALUE;
            
            System.out.println("Ignoring provided page size. Using prior size of " + PageSize);

            // If TopFree has a nonzero value, we have real free page(s) to consider.
            if (FreePagePtr > 0) {
                // Prepare a List so we can stack the pages in order.
                ArrayList<Integer> FreePageList = new ArrayList<>();
                
                while (FreePagePtr > 0) {
                    // Cache the page id if valid,
                    FreePageList.add(FreePagePtr);
                    // Get the next one,
                    raf.seek(PageStart + (FreePagePtr * PageSize));
                    FreePagePtr = raf.readInt();
                }

                // Now add the list in reverse order to the stack.
                for (int i = FreePageList.size() - 1; i >= 0; i--) 
                FreePages.add(FreePageList.get(i));
            }

            // And finally, load the catalog
            LoadCatalog();
        } else {
            System.out.println("Database file not found. Creating new database...");
            DBFile.createNewFile();

            // Write DB params to top of the file
            DBParams.putInt(0, page_size);
            DBParams.putInt(Integer.BYTES, PageCount);
            DBParams.putInt(Integer.BYTES * 2, 0); // No pages freed yet.

            raf = new RandomAccessFile(DBFile, "rw");
            raf.write(DBParameters);

            // Now handle the free pointer linked list if there are vacancies:
            if (!FreePages.isEmpty()) {
                // Make each node of the linked list point to each other,
                int Current = FreePages.pop(), Next;
                while (!FreePages.isEmpty()) {
                    raf.seek(PageStart + (Current * PageSize));
                    Next = FreePages.pop();
                    raf.writeInt(Next);
                    Current = Next;
                }
            }
        }

        raf.close();
    }

    public static void Shutdown() throws Exception{
        // Write DB params to top of the file
        RandomAccessFile raf = new RandomAccessFile(Filename, "rw");

        byte[] DBParameters = new byte[PageStart];
        ByteBuffer DBParams = ByteBuffer.wrap(DBParameters);

        DBParams.putInt(0, PageSize);
        DBParams.putInt(Integer.BYTES, PageCount);
        DBParams.putInt(Integer.BYTES * 2, FreePages.isEmpty() ? -1 : FreePages.peek());

        raf.seek(0);
        raf.write(DBParameters);
        raf.close();

        BufferManager.flush_all();
        BufferManager.writeSchemas();
    }

    public static int CreatePage() {
        // Return a free page if available,
        if (!FreePages.isEmpty()) return FreePages.pop();
        // Otherwise make a new one.
        return PageCount++;
    }

    public static void WritePage(Page P) throws Exception {
        // Write the page to disk.
        RandomAccessFile raf = new RandomAccessFile(Filename, "rw");
        raf.seek(PageStart + (P.get_pageid() * PageSize));
        raf.write(Encode(P));
        raf.close();
    }

    public static Page ReadPageFromDisk(Schema S, int PageId) throws Exception {
        // Read page from disk, and decode it to a page object.
        RandomAccessFile raf = new RandomAccessFile(Filename, "rw");
        raf.seek(PageStart + (PageId * PageSize));

        byte[] PageData = new byte[PageSize];
        raf.readFully(PageData);
        raf.close();

        return Decode(S, PageId, PageData);
    }

    public static void FreePage(Page P) {
        // Add the page id to the free page stack, and empty its values.
        FreePages.push(P.get_pageid());

        P.get_data().clear();
    }

    public static void LoadCatalog() throws Exception {
        ArrayList<ArrayList<Object>> Data = Catalog.AttributeTable.Select();

        for (ArrayList<Object> Row : Data) {
            Schema S;
            String name = Row.get(0).toString();

            // Create the schema in catalog if not created yet
            if((S = Catalog.GetSchema(name)) == null) {
                Catalog.Schemas.add(S = new Schema(name));

                S.PageId = Integer.parseInt(Row.get(1).toString());
            }

            // Load the attributes from the schemas
            Integer Size = (Integer) Row.get(4); 
            Type T = Type.values()[(Integer) Row.get(3)];
            Boolean NotNull = (Boolean)Row.get(5), Primary = (Boolean)Row.get(7), Unique = (Boolean)Row.get(6);
            String AName = Row.get(2).toString();

            // Recreate attributes
            S.AddAttribute(AName, T, Size, NotNull, Primary, Unique, Row.get(8));
        }
    }


    /**
     * 
     * Page Encoding builds a byte array with this structure: 
     * 
     * [int nextpage][int num entries][fixed size row entries 1..n]
     * [empty space]
     * [empty space]
     * [empty space]
     * [varchar stacked]
     * [varchar stacked]
     * [varchar stacked]
     * 
     * varchar pointer and size will be encoded either as [int,short] or [short,short], depending on CanShort
    */
    public static byte[] Encode(Page P) throws Exception {
        // Prepare byte array for writing the encoded page data.
        byte[] Data = new byte[PageSize];

        // Utilize a wrapper for ease of access, and writing.
        ByteBuffer Wrapper = ByteBuffer.wrap(Data);

        // Keep a pointer for variable length values
        int VarPtr = PageSize, FixedPtr = Integer.BYTES*2;

        // Write next page value,
        Wrapper.putInt(0,P.get_next_pageid());

        // Get rows,
        ArrayList<ArrayList<Object>> Rows = P.get_data();

        // Write number of rows,
        Wrapper.putInt(Integer.BYTES, Rows.size());

        // Grab Schema and Type array,
        Schema S = P.get_schema();
        Type[] Types = S.GetTypes();

        // Define null byte size 
        int NullByteSize = (Types.length + 7)/8;

        // Write the rows,
        for (ArrayList<Object> Row : Rows) {
            // byte[] RowData = new byte[NullByteSize + FixedSize];
            // ByteBuffer RowWrapper = ByteBuffer.wrap(RowData);
            // Place header past Null bytes,
            // RowWrapper.position(NullByteSize);
            int NBPtr = FixedPtr; // NullBytePointer, cache at beginning so we can mark nulls even after some shifts
            FixedPtr += NullByteSize;
            for (int index = 0; index < Row.size(); index++) {
                Object Value = Row.get(index);

                // Null values get marked in the bitmap,
                if (Value == null) Data[NBPtr + (index/8)] |= 1 << (index % 8);

                // Otherwise switch through the types to handle writing the value
                else switch(Types[index]) {
                    case INT -> {Wrapper.putInt(FixedPtr,(int) Value); FixedPtr += Integer.BYTES;}
                    
                    case DOUBLE -> {Wrapper.putDouble(FixedPtr,(double) Value); FixedPtr += Double.BYTES;}
                    case BOOLEAN -> {Wrapper.put(FixedPtr++, (byte) ((boolean) Value ? 1 : 0));}
                    case CHAR -> {
                        byte[] EncodedChar = Value.toString().getBytes(StandardCharsets.UTF_8);
                        Wrapper.put(FixedPtr, EncodedChar);
                        FixedPtr += EncodedChar.length;
                    }
                    case VARCHAR -> {
                        byte[] VarLenBytes = Value.toString().getBytes(StandardCharsets.UTF_8);
                        // Move VarPtr up the stack,
                        VarPtr -= VarLenBytes.length;
                        // Write varchar on the stack,
                        Wrapper.put(VarPtr, VarLenBytes);
                        // Write pointer to the varchar
                        // Will use short if page size is small enough
                        if (CanShort) {Wrapper.putShort(FixedPtr,(short) VarPtr); FixedPtr += Short.BYTES;}
                        else {Wrapper.putInt(FixedPtr,VarPtr); FixedPtr += Integer.BYTES;}
                        // And now put varchar length
                        Wrapper.putShort(FixedPtr, (short) VarLenBytes.length);
                        FixedPtr += Short.BYTES;
                    }
                }
            }
            // // Row completed, write the fixed data to the page (varchars already handled).
            // Wrapper.put(FixedPtr, RowData);
            // // Move the fixed pointer foward
            // FixedPtr += RowData.length;

            // Sanity check, if the pointers have inverted, this page has too many rows.
            if (VarPtr < FixedPtr) throw new Exception("Too many rows in page.");
        }

        return Data;
    }

    public static Page Decode(Schema S, int PageId, byte[] Data) throws Exception {
        // Prepare wrapper for reading the encoded page data.
        ByteBuffer Wrapper = ByteBuffer.wrap(Data);

        // Create page, grab its rows storage.
        Page P = new Page(PageId, S);
        ArrayList<ArrayList<Object>> Rows = P.get_data();

        // Read next page value,
        int Next = Wrapper.getInt(0);
        if (Next > 0) P.set_nextpageid(Next);

        // Read total number of rows,
        int RowCount = Wrapper.getInt(Integer.BYTES);

        // Grab Type and Attribute array from schema,
        Type[] Types = S.GetTypes();
        ArrayList<Attribute> Attributes = S.Attributes;

        // Define null byte size 
        int NullByteSize = (Types.length + 7)/8;
        // Define ptr for slots, and free ptr to track top of varchar stack
        int FixedPtr = Integer.BYTES*2, FreePtr=PageSize;

        // might make a lot of rows, so define variables here first
        ArrayList<Object> Row = null;
        Object Value = null;
        int VarPtr = PageSize, Len; // for varchars

        // Now start iterating the fixed size data,
        for (int i = 0; i < RowCount; i++) {
            // Use a nullmap to see which entries to skip,
            boolean[] nullmap = new boolean[Types.length];
            // iterate through boolean array, setting bools from the nullbytes.
            for (int i2 = 0; i2 < nullmap.length; i2++)
            // Set true if bit is 1
            nullmap[i2] = ((Data[FixedPtr + (i2/8)] >> (i2%8)) & 1) == 1;

            // Now shift FixedPtr forward to the actual values
            FixedPtr += NullByteSize;

            // Create row,
            Row = new ArrayList<>();

            for (int i2 = 0; i2 < Types.length; i2++) {
                // Skip null values,
                if (nullmap[i2]) Value = null;
                
                // Otherwise switch and parse:
                else switch(Types[i2]) {
                    case INT -> {Value = Wrapper.getInt(FixedPtr); FixedPtr += Integer.BYTES;}
                    case DOUBLE -> {Value = Wrapper.getDouble(FixedPtr); FixedPtr += Double.BYTES;}
                    case BOOLEAN -> {Value = Data[FixedPtr] != 0; FixedPtr += 1;}
                    case CHAR -> {
                        Len = Attributes.get(i2).typeLength;
                        Value = new String(Data, FixedPtr, Len, StandardCharsets.UTF_8);
                        FixedPtr += Len;
                    }
                    case VARCHAR -> {
                        // Parse varchar's location (short if CanShort)
                        if (CanShort) {
                            VarPtr = Short.toUnsignedInt(Wrapper.getShort(FixedPtr)); 
                            FixedPtr += Short.BYTES;
                        } else {
                            VarPtr = Wrapper.getInt(FixedPtr); 
                            FixedPtr += Integer.BYTES;
                        }
                        // Parse varchar's length
                        Len = Short.toUnsignedInt(Wrapper.getShort(FixedPtr)); FixedPtr += Short.BYTES;
                        // Now parse actual varchar
                        Value = new String(Data, VarPtr, Len, StandardCharsets.UTF_8);

                        if (VarPtr < FreePtr) FreePtr = VarPtr; // Update free ptr if we moved above it
                    }
                }
                Row.add(Value);
            }
            Rows.add(Row);
        }

        // Set the freebytes of the page now that we have climbed the stack and traversed the slots.
        P.set_freebytes(FreePtr - FixedPtr);

        return P;
    }
}