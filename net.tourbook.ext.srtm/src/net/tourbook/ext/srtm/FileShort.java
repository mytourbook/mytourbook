package net.tourbook.ext.srtm;
/*
 * 
 * shorts werden hier von Java big-endian interpretiert
 * => big-endian Variante der Datenfiles verwenden
 *
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
//import java.util.regex.*;


public class FileShort {

   private FileChannel fileChannel;
   private ShortBuffer shortBuffer;
   private boolean exists = false;
   String zipName;
   
   public FileShort(String fileName) throws Exception {
      init(fileName, false);
   }

   public FileShort(String fileName, boolean zipFtp) throws Exception {
      init(fileName, zipFtp);
   }

   private void init(String fileName, boolean zipFtp) throws Exception {
      
      try {
         open(fileName);
         
      } catch (FileNotFoundException e1) {
         
         if (!zipFtp) {
            handleError(fileName, e1);
            return;
         }
         try {
            // zip-File <fileName>.zip per FTP downloaden und entzippen
            zipName = getZipName(fileName); 
            transfer(zipName);
            unzip(zipName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // sonstige Fehler
         handleError(fileName, e1);
      }
   }
   
   private void open(String fileName) throws Exception {

      try {

         fileChannel = new FileInputStream(new File(fileName)).getChannel();
         
         shortBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, 
                                       fileChannel.size()).asShortBuffer();
         
      } catch (Exception e) {
         
         throw(e);
         
      }
      exists = true;
   }
   
   public short get(int index) {
      if (!exists) {
         return (-32767); 
      }
      return shortBuffer.get(index);
   }

   private void close() throws Exception {
      fileChannel.close();
   }

   private String getZipName(String fileName) {
      String zipName;
//      Pattern praefixPattern = Pattern.compile("^(.*)\\.+[A-Za-z]{3,4}$");// Praefix + Punkt + Endung
//      Matcher praefixMatcher = praefixPattern.matcher(fileName);
//      if (praefixMatcher.matches())
//         zipName = praefixMatcher.group(1)+".zip";
//      else
      zipName = fileName+".zip";
      return zipName;
   }
   
   private void unzip(String zipName) throws Exception {
      FileZip.unzip(zipName);
   }

   private void transfer(String localName) throws Exception {
      String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
      FileSRTM3FTP.get(remoteName, localName);
   }

   private void handleError(String fileName, Exception e) { // throws Exception{
      exists = false;
   }
   
   public static void main(String[] args) throws Exception {

   }
}
