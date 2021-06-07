/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import net.tourbook.srtm.download.DownloadETOPO;
import net.tourbook.srtm.download.DownloadGLOBE;
import net.tourbook.srtm.download.DownloadSRTM3;

public class ElevationFile {

   private FileChannel _fileChannel;
   private ShortBuffer _shortBuffer;

   private boolean     _isFileAvailable  = false;
   private boolean     _isLocalFileError = false;

   public ElevationFile(final String localFilePath, final String localFilePathUnzipped, final int elevationTyp) throws Exception {

      switch (elevationTyp) {
      case ElevationType.ETOPO:
         initETOPO(localFilePath);
         break;
      case ElevationType.GLOBE:
         initGLOBE(localFilePath);
         break;
      case ElevationType.SRTM3:
         initSRTM3(localFilePath, localFilePathUnzipped);
         break;
      case ElevationType.SRTM1:
         initSRTM1(localFilePath);
         break;
      }
   }

   public void close() {

      try {
         if (_fileChannel != null) {
            _fileChannel.close();
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public short get(final int index) {

      if (!_isFileAvailable) {
         return (-32767);
      }
      return _shortBuffer.get(index);
   }

   private void handleError(final String fileName, final Exception e) {

      System.out.println(this.getClass().getCanonicalName() + " - handleError: " + fileName + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$

      if (e instanceof FileNotFoundException) {
         // done
      } else {
         e.printStackTrace();
      }

      _isFileAvailable = false;
      // dont return exception
   }

   private void initETOPO(final String fileName) throws Exception {

      try {
         open(fileName);
      } catch (final FileNotFoundException e1) {
         try {
            // download file
            final String localName = fileName;
            final String remoteName = localName.substring(localName.lastIndexOf(File.separator) + 1);
            DownloadETOPO.get(remoteName, localName);
            open(fileName);

         } catch (final Exception e2) {
            handleError(fileName, e2);
         }
      } catch (final Exception e1) { // other Error
         handleError(fileName, e1);
      }
   }

   private void initGLOBE(final String fileName) throws Exception {

      try {
         open(fileName);
      } catch (final FileNotFoundException e1) {
         try {
            // download gzip-File <fileName>.gz and unzip
            final String localZipName = fileName + ".gz"; //$NON-NLS-1$
            final String remoteFileName = localZipName.substring(localZipName.lastIndexOf(File.separator) + 1);
            DownloadGLOBE.get(remoteFileName, localZipName);
            FileZip.gunzip(localZipName);

            // delete zip archive file, this is not needed any more
            final File zipArchive = new File(localZipName);
            zipArchive.delete();

            open(fileName);

         } catch (final Exception e2) {
            handleError(fileName, e2);
         }
      } catch (final Exception e1) { // other Error
         handleError(fileName, e1);
      }
   }

   private void initSRTM1(final String fileName) throws Exception {

      // currently no automatically download realized
      try {
         open(fileName);
      } catch (final Exception e) {
         handleError(fileName, e);
      }
   }

   private void initSRTM3(final String localFilePath, final String localFilePathUnzipped) throws Exception {

      if (_isLocalFileError) {
         return;
      }

      try {
         open(localFilePathUnzipped);
      } catch (final FileNotFoundException e1) {
         try {

            //  download zip-File <fileName>.zip and unzip
            final String localZipName = localFilePath + ".zip"; //$NON-NLS-1$

            // check if local zip file exists with a size == 0
            final File localFile = new File(localZipName);
            if (localFile.exists() && localFile.length() == 0) {

               _isLocalFileError = true;

               /*
                * This case occures when an internet connection do not exists. Delete file that
                * it is downloaded when an internet connection is available and the application
                * is restarted
                */
               localFile.delete();

               throw new Exception(this.getClass().getCanonicalName() + " - local file is empty: " + localFile.getAbsolutePath()); //$NON-NLS-1$
            }

            final String remoteFileName = localZipName.substring(localZipName.lastIndexOf(File.separator) + 1);
            new DownloadSRTM3().get(remoteFileName, localZipName);
            FileZip.unzip(localZipName);

            // delete zip archive file, this is not needed any more
            final File zipArchive = new File(localZipName);
            zipArchive.delete();

            open(localFilePathUnzipped);

         } catch (final Exception e2) {
            handleError(localFilePath, e2);
         }
      } catch (final Exception e1) { // other Error
         handleError(localFilePathUnzipped, e1);
      }
   }

   @SuppressWarnings("resource")
   private void open(final String fileName) throws Exception {

      try {

         _fileChannel = new FileInputStream(new File(fileName)).getChannel();
         _shortBuffer = _fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, _fileChannel.size()).asShortBuffer();

      } catch (final Exception e) {
         System.out.println(this.getClass().getCanonicalName() + " - " + e.getMessage()); //$NON-NLS-1$
         throw (e);
      }

      System.out.println(this.getClass().getCanonicalName() + " - open " + fileName); //$NON-NLS-1$

      _isFileAvailable = true;
   }
}
