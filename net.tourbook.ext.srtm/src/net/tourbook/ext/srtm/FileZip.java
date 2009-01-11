package net.tourbook.ext.srtm;
/*
 * 
 * ZIP-Tools
 * 
 */

import java.io.*;
import java.util.zip.*;

public final class FileZip {

   public final static String unzip(String zipName) throws Exception {
      
      String outFileName = null;
      String zipEntryName = null;
      
      try {
         // Open the ZIP file
         ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipName));
         // Get the firstStrecke entry
         ZipEntry zipEntry = zipInputStream.getNextEntry();
         zipEntryName = zipEntry.getName();
                  
         if (zipEntryName.indexOf(File.separator) != -1) 
            // Delimiter im Namen (z.B. bei selbsterzeugten kmz-Files)
            zipEntryName = zipEntryName.substring(zipEntryName.lastIndexOf(File.separator)+1);
         
         outFileName = zipName.substring(0, zipName.lastIndexOf(File.separator))
                     + File.separator + zipEntryName;
         
         OutputStream fileOutputStream = new FileOutputStream(outFileName);

         // Transfer bytes from the ZIP file to the output file
         byte[] buf = new byte[1024];
         int len;
         while ((len = zipInputStream.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, len);
         }

         fileOutputStream.close();
         zipInputStream.close();
         
         return zipEntryName;
         
      } catch (IOException e) {
         throw(e); // Exception weitergeben
      }
   }

   
   public final static void zip(String fileName, String zipName) throws Exception {
      try {
         // Compress the file         
         File file = new File(fileName);
         FileInputStream fileInputStream = new FileInputStream(file);
                  
         // Create the ZIP file
         ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipName));

         // Add ZIP entry to output stream (Filename only)
         if (fileName.indexOf(File.separator) != -1) 
            fileName = fileName.substring(fileName.lastIndexOf(File.separator)+1);
         zipOutputStream.putNextEntry(new ZipEntry(fileName));

         // Create a buffer for reading the files
         byte[] buf = new byte[1024];
         
         // Transfer bytes from the file to the ZIP file
         int len;
         while ((len = fileInputStream.read(buf)) > 0) {
             zipOutputStream.write(buf, 0, len);
         }
 
         // Complete the entry
         zipOutputStream.closeEntry();
         fileInputStream.close();
         // Complete the ZIP file
         zipOutputStream.close();
         
      } catch (IOException e) {
         throw(e); // Exception weitergeben
      }
   }
   

   public static void main(String[] args) throws Exception {
   }
}
