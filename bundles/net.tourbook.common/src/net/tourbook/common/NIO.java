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
package net.tourbook.common;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.core.runtime.FileLocator;

/**
 * Tools for the java.nio package.
 */
public class NIO {

   private static final String  FILE_PROTOCOL            = "file:";                              //$NON-NLS-1$

   public static final String   DEVICE_FOLDER_NAME_START = "[";                                  //$NON-NLS-1$

   private final static Pattern DRIVE_LETTER_PATTERN     = Pattern.compile("\\s*\\(([^(]*)\\)"); //$NON-NLS-1$

   /** Extracts <code>W530</code> from <code>[W530]\temp\other</code> */
   private final static Pattern DEVICE_NAME_PATTERN      = Pattern.compile("\\s*\\[([^]]*)");    //$NON-NLS-1$

   /** <code>([\\].*)</code> */
   private final static Pattern DEVICE_NAME_PATH_PATTERN = Pattern.compile("([\\\\].*)");        //$NON-NLS-1$

   /**
    * Replace device name with drive letter, e.g. [MEDIA]\CACHE -> D:\CACHE. This do not validate if
    * the path exists.
    *
    * @param folder
    * @return Returns the OS path or <code>null</code> when the device name cannot be converted into
    *         a drive letter.
    */
   public static String convertToOSPath(final String folder) {

      if (folder == null) {
         return null;
      }

      String osPath = null;

      if (isDeviceNameFolder(folder)) {

         // replace device name with drive letter, [MEDIA]\CACHE ->  D:\CACHE

         final String deviceName = parseDeviceName(folder);

         final Iterable<FileStore> fileStores = getFileStores();

         for (final FileStore store : fileStores) {

            final String storeName = store.name();

            if (storeName.equals(deviceName)) {

               final String driveLetter = parseDriveLetter(store);
               final String namedPath = parseDeviceNamePath(folder);

               osPath = driveLetter + namedPath;

               break;
            }
         }
      } else if (isTourBookFileSystem(folder)) {
         final TourbookFileSystem tourbookFileSystem = FileSystemManager.getTourbookFileSystem(folder);

         osPath = folder.replace(tourbookFileSystem.getDisplayId(), tourbookFileSystem.getId());

         return osPath;
      } else {

         // OS path is contained in the folder path

         osPath = folder;
      }

      return osPath;
   }

   /**
    * Convert bundle file path to absolute file path
    *
    * @param bundleUrl
    *           e.g.
    *           bundleresource://70.fwk1710675314/importdata/sporttracks/fitlogex/files/TimothyLake.fitlogEx
    * @return
    * @throws IOException
    */
   public static String getAbsolutePathFromBundleUrl(final URL bundleUrl) throws IOException {

      // file:/C:/DAT/MT/mytourbook/bundles/net.tourbook.tests/bin/importdata/sporttracks/fitlogex/files/TimothyLake.fitlogEx
      final URL fileUrl = FileLocator.toFileURL(bundleUrl);

      // file:/C:/DAT/MT/mytourbook/bundles/net.tourbook.tests/bin/importdata/sporttracks/fitlogex/files/TimothyLake.fitlogEx
      final String fileExternalForm = fileUrl.toExternalForm();

      // /C:/DAT/MT/mytourbook/bundles/net.tourbook.tests/bin/importdata/sporttracks/fitlogex/files/TimothyLake.fitlogEx
      String filename_WithoutFileProtocol = fileExternalForm.substring(FILE_PROTOCOL.length());

      /**
       * Fix java.nio.file.InvalidPathException: Illegal char <:> at index 2:
       * <p>
       * /C:/DAT/MT/mytourbook/bundles/net.tourbook.tests/bin/importdata/sporttracks/fitlogex/files/ParkCity.fitlogEx
       */
      if (UI.IS_WIN && filename_WithoutFileProtocol.startsWith(UI.SLASH)) {
         filename_WithoutFileProtocol = filename_WithoutFileProtocol.substring(1);
      }

      return filename_WithoutFileProtocol;
   }

   /**
    * Returns the {@link Path} for a given folder depending on
    * the folder's file system (Local drive, TourBookFileSystem such as Dropbox...)
    * Note : The file system is guessed from the folder name.
    *
    * @param folderName
    *           A given folder name
    * @return Returns the {@link Path} of the folder
    */
   public static Path getDeviceFolderPath(final String folderName) {

      return NIO.isTourBookFileSystem(folderName) ? FileSystemManager.getfolderPath(folderName)
            : Paths.get(folderName);
   }

   public static Iterable<FileStore> getFileStores() {

//		final long start = System.nanoTime();
//
      final Iterable<FileStore> systemFileStore = FileSystems.getDefault().getFileStores();
//
//    System.out.println((UI.timeStampNano() + " " + NIO.class.getName() + " \t")
//          + (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//    // TODO remove SYSTEM.OUT.PRINTLN

      final ArrayList<FileStore> fileStores = new ArrayList<>();

      Iterator<FileStore> fileStoresIterator = systemFileStore.iterator();
      while (fileStoresIterator.hasNext()) {
         fileStores.add(fileStoresIterator.next());
      }

      final List<TourbookFileSystem> fileSystems = FileSystemManager.getFileSystemsList();

      for (final TourbookFileSystem fileSystem : fileSystems) {

         final Iterable<FileStore> fileSystemStore = fileSystem.getFileStore();
         if (fileSystemStore != null) {
            fileStoresIterator = fileSystemStore.iterator();
            while (fileStoresIterator.hasNext()) {
               fileStores.add(fileStoresIterator.next());
            }
         }
      }

      return fileStores;
   }

   /**
    * @param fileName
    * @return Returns a path or <code>null</code> when an exception occurs.
    */
   public static Path getPath(final String fileName) {

      if (fileName == null) {
         return null;
      }

      try {

         return Paths.get(fileName);

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * @param folderName
    *           A given folder name
    * @return Returns <code>true</code> when the folder name starts with
    *         {@value #DEVICE_FOLDER_NAME_START}.
    */
   public static boolean isDeviceNameFolder(final String folderName) {

      if (folderName == null) {
         return false;
      }

      return folderName.startsWith(DEVICE_FOLDER_NAME_START);
   }

   /**
    * Gives an indication whether a given folder name is a
    * {@link TourbookFileSystem}
    *
    * @param folderName
    *           A given folder name
    * @return Returns true when the folder name starts with
    *         {@link TourBookFileSystem#getId()}.
    */
   public static boolean isTourBookFileSystem(final String folderName) {

      return FileSystemManager.getTourbookFileSystem(folderName) != null;
   }

   private static String parseDeviceName(final String fullName) {

      final Matcher matcher = DEVICE_NAME_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private static String parseDeviceNamePath(final String fullName) {

      final Matcher matcher = DEVICE_NAME_PATH_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   public static String parseDriveLetter(final FileStore store) {

      final String fullName = store.toString();

      final Matcher matcher = DRIVE_LETTER_PATTERN.matcher(fullName);

      while (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }
}
