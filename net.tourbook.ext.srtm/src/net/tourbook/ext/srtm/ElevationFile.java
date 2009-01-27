package net.tourbook.ext.srtm;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class ElevationFile {

   private FileChannel fileChannel;
   private ShortBuffer shortBuffer;
   private boolean exists = false;
   
   public ElevationFile(String fileName, int elevationTyp) throws Exception {
      switch (elevationTyp) {
      case Constants.ELEVATION_TYPE_ETOPO: initETOPO(fileName); break;
      case Constants.ELEVATION_TYPE_GLOBE: initGLOBE(fileName); break;
      case Constants.ELEVATION_TYPE_SRTM3: initSRTM3(fileName); break;
      case Constants.ELEVATION_TYPE_SRTM1: initSRTM1(fileName); break;
      }
   }

   private void initETOPO(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (FileNotFoundException e1) {
         try {
            // File downloaden
            String localName = fileName; 
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadETOPO.get(remoteName, localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // sonstige Fehler
         handleError(fileName, e1);
      }
   }
   
   private void initGLOBE(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (FileNotFoundException e1) {
         try {
            // gzip-File <fileName>.gz downloaden und entgzippen
            String localName = fileName+".gz"; 
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadGLOBE.get(remoteName, localName);
            FileZip.gunzip(localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // sonstige Fehler
         handleError(fileName, e1);
      }
   }
   
   private void initSRTM3(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (FileNotFoundException e1) {
         try {
            // zip-File <fileName>.zip per FTP downloaden und entzippen
            String localName = fileName+".zip"; 
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadSRTM3.get(remoteName, localName);
            FileZip.unzip(localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // sonstige Fehler
         handleError(fileName, e1);
      }
   }
   
   private void initSRTM1(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (Exception e) {
         handleError(fileName, e);
      }
   }
   
   private void open(String fileName) throws Exception {

      try {
         fileChannel = new FileInputStream(new File(fileName)).getChannel();
         shortBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, 
                                       fileChannel.size()).asShortBuffer();
      } catch (Exception e) {
         System.out.println(e.getMessage()); 
         throw(e);
      }
      System.out.println("open " + fileName);
      exists = true;
   }
   
   public short get(int index) {
      if (!exists) {
         return (-32767); 
      }
      return shortBuffer.get(index);
   }

//   private void close() throws Exception {
//      fileChannel.close();
//   }

   private void handleError(String fileName, Exception e) { // throws Exception{
      System.out.println("handleError: " + fileName + ": "+ e.getMessage());
      // e.printStackTrace();
      exists = false;
      // keine Exception weitergeben      
   }
   
   public static void main(String[] args) throws Exception {

   }
}
