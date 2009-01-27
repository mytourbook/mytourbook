package net.tourbook.ext.srtm;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public final class DownloadETOPO {

   final static String addressPraefix = "http://www.ngdc.noaa.gov/mgg/global/relief/ETOPO5/TOPO/ETOPO5/";
   
   public final static void get(String remoteName, String localName) throws Exception {
    
      try {
         String address = addressPraefix + remoteName;
         System.out.println("load " + address);
         OutputStream outputStream = null;
         InputStream inputStream = null;
         try {
             URL url = new URL(address);
             outputStream = new BufferedOutputStream(
                                new FileOutputStream(localName));
             URLConnection urlConnection = url.openConnection();
             inputStream = urlConnection.getInputStream();
             byte[] buffer = new byte[1024];
             int numRead;
             long numWritten = 0;
             while ((numRead = inputStream.read(buffer)) != -1) {
             outputStream.write(buffer, 0, numRead);
             numWritten += numRead;
             }
             System.out.println("# Bytes localName = " + numWritten);
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             try {
             if (inputStream != null) {
                 inputStream.close();
             }
             if (outputStream != null) {
                 outputStream.close();
             }
             } catch (IOException ioe) {
             ioe.printStackTrace();
             }
         }

         System.out.println("get " + remoteName + " -> " + localName + " ...");

      } catch (Exception e) {
         System.out.println(e.getMessage());
         throw(e);
      }
   }   

   
   public static void main(String[] args) {

   }
} 