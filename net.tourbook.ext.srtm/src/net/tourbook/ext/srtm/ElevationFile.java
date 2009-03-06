/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
/**
 * @author Alfred Barten
 */
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
            // download file
            String localName = fileName; 
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadETOPO.get(remoteName, localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // other Error
         handleError(fileName, e1);
      }
   }
   
   private void initGLOBE(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (FileNotFoundException e1) {
         try {
            // download gzip-File <fileName>.gz and unzip
            String localName = fileName+".gz";  //$NON-NLS-1$
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadGLOBE.get(remoteName, localName);
            FileZip.gunzip(localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // other Error
         handleError(fileName, e1);
      }
   }
   
   private void initSRTM3(String fileName) throws Exception {
      
      try {
         open(fileName);        
      } catch (FileNotFoundException e1) {
         try {
            //  download zip-File <fileName>.zip and unzip
            String localName = fileName+".zip";  //$NON-NLS-1$
            String remoteName = localName.substring(localName.lastIndexOf(File.separator)+1);
            DownloadSRTM3.get(remoteName, localName);
            FileZip.unzip(localName);
            open(fileName);
            
         } catch (Exception e2) {
            handleError(fileName, e2);            
         }
      } catch (Exception e1) { // other Error
         handleError(fileName, e1);
      }
   }
   
   private void initSRTM1(String fileName) throws Exception {
      
	   // currently no automatically download realized
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
      System.out.println("open " + fileName); //$NON-NLS-1$
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

   private void handleError(String fileName, Exception e) { 
      System.out.println("handleError: " + fileName + ": "+ e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
      // e.printStackTrace();
      exists = false;
      // dont return exception      
   }
   
   public static void main(String[] args) throws Exception {

   }
}
