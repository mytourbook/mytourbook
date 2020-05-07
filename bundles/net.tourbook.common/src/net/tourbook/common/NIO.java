/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Tools for the java.nio package.
 */
public class NIO {

   public static final String    DEVICE_FOLDER_NAME_START    = "[";                                  //$NON-NLS-1$
   public static final String    VIRTUAL_DROPBOX_FOLDER_NAME = "dropbox";                            //$NON-NLS-1$

   private final static Pattern  DRIVE_LETTER_PATTERN        = Pattern.compile("\\s*\\(([^(]*)\\)"); //$NON-NLS-1$
   /** Extracts <code>W530</code> from <code>[W530]\temp\other</code> */
   private final static Pattern  DEVICE_NAME_PATTERN         = Pattern.compile("\\s*\\[([^]]*)");    //$NON-NLS-1$

   /** <code>([\\].*)</code> */
   private final static Pattern  DEVICE_NAME_PATH_PATTERN    = Pattern.compile("([\\\\].*)");        //$NON-NLS-1$

   private static FileSystem     _dropboxFileSystem;

   final static IPreferenceStore _prefStore                  = CommonActivator.getPrefStore();

   static {

      final IPropertyChangeListener prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            if (event.getProperty().equals(ICommonPreferences.DROPBOX_ACCESSTOKEN)) {

               // Re create the Dropbox file system
               createDropboxFileSystem();
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(prefChangeListenerCommon);
   }

   /**
    * Closes the Dropbox Java 7 FileSystem.
    * This function will be called whenever the user
    * stops the folder watch or quits MyTourbook.
    */
   public static void closeDropboxFileSystem() {

      if (_dropboxFileSystem != null) {
         try {
            _dropboxFileSystem.close();
            _dropboxFileSystem = null;
         } catch (final IOException e) {
            StatusUtil.log(e);
         }
      }
   }

   /**
    * Replace device name with drive letter, e.g. [MEDIA]\CACHE -> D:\CACHE. This do not validate if
    * the path exists.
    *
    * @param folder
    * @return Returns the os path or <code>null</code> when the device name cannot be converted into
    *         a driveletter.
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
      } else {

         // OS path is contained in the folder path

         osPath = folder;
      }

      return osPath;
   }

   /**
    * Creates a Java 7 FileSystem over DropBox using the library
    * https://github.com/FJBDev/java7-fs-dropbox
    *
    * @return True if the file system was created successfully, false otherwise.
    */
   public static boolean createDropboxFileSystem() {

      boolean result = false;

      final URI uri = URI.create("dropbox://root"); //$NON-NLS-1$
      final Map<String, String> env = new HashMap<>();

      final String accessToken = _prefStore.getString(ICommonPreferences.DROPBOX_ACCESSTOKEN);
      if (StringUtils.isNullOrEmpty(accessToken)) {
         _dropboxFileSystem = null;
         return result;
      }

      env.put("accessToken", accessToken); //$NON-NLS-1$

      final FileSystemProvider provider = new DropBoxFileSystemProvider(new DropBoxFileSystemRepository());

      try {
         _dropboxFileSystem = provider.newFileSystem(uri, env);

         result = true;
      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      return result;
   }

   /**
    * Returns the {@link Path} for a given folder depending on
    * the folder's file system (Local drive, Dropbox)
    * Note : The file system is guessed from the folder name.
    *
    * @param folderName
    *           A given folder name
    * @return Returns the {@link Path} of the folder
    */
   public static Path getDeviceFolderPath(final String folderName) {

      return NIO.isDropboxDevice(folderName) ? getDropboxFilePath(UI.EMPTY_STRING) : Paths.get(folderName);
   }

   /**
    * Get the Dropbox {@link Path} of a given filename
    *
    * @param fileName
    * @return
    */
   private static Path getDropboxFilePath(final String fileName) {
      if (_dropboxFileSystem == null) {
         return null;
      }

      final String dropboxFilePath = _prefStore.getString(ICommonPreferences.DROPBOX_FOLDER) + fileName;
      return _dropboxFileSystem.getPath(dropboxFilePath);
   }

   /**
    * Retrieves the Dropbox {@link FileStore}.
    * Creates it if necessary.
    *
    * @return A list of Dropbox {@link FileStore}
    */
   private static Iterable<FileStore> getDropboxFileStores() {
      if (_dropboxFileSystem != null) {
         return _dropboxFileSystem.getFileStores();
      } else {
         if (createDropboxFileSystem()) {
            return _dropboxFileSystem.getFileStores();
         }
      }

      return null;
   }

   /**
    * Retrieves the Dropbox {@link FileSystem}.
    *
    * @return
    */
   public static FileSystem getDropboxFileSystem() {
      return _dropboxFileSystem;
   }

   public static Iterable<FileStore> getFileStores() {

//		final long start = System.nanoTime();
//
      final Iterable<FileStore> systemFileStore = FileSystems.getDefault().getFileStores();

      final ArrayList<FileStore> fileStores = new ArrayList<>();

      Iterator<FileStore> fileStoresIterator = systemFileStore.iterator();
      while (fileStoresIterator.hasNext()) {
         fileStores.add(fileStoresIterator.next());
      }

      final Iterable<FileStore> dropboxFileStore = getDropboxFileStores();

      if (dropboxFileStore != null) {
         fileStoresIterator = dropboxFileStore.iterator();
         while (fileStoresIterator.hasNext()) {
            fileStores.add(fileStoresIterator.next());
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
    * Gives an indication whether a given folder name is a virtual Dropbox
    * file system
    *
    * @param folderName
    *           A given folder name
    * @return Returns true when the folder name is equal to
    *         {@value #VIRTUAL_DROPBOX_FOLDER_NAME}.
    */
   public static boolean isDropboxDevice(final String folderName) {
      if (folderName == null) {
         return false;
      }

      return folderName.equalsIgnoreCase(VIRTUAL_DROPBOX_FOLDER_NAME);
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
