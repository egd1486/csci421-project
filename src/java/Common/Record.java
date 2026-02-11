package Common;

import java.util.ArrayList;

class Record {
   private ArrayList<Object> data;


   
   public byte[] serialize(Table schema) {
      return new byte[];
   }

   public Record deserialize(byte[] bytes, Table schema) {
      return new Record();
   }

   //in bytes
   public int getRecordLength() {
      //serialize then get length
      return -1;
   }
   
   
}