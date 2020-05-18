/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * Class that manages virtual file systems.
 *
 * @author Frédéric Bard
 */
public class FileSystemManager {

   private static List<TourbookFileSystem> _fileSystemsList;

   public static void closeFileSystems() {
      if (_fileSystemsList == null) {
         return;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         tourbookFileSystem.close();
      }
   }

   /**
    * Copy a file from a {@link TourBookFileSystem} to the user's
    * local file system.
    *
    * @param absolutefilePath
    * @return
    */
   public static File CopyLocally(final String absolutefilePath) {

      if (_fileSystemsList == null) {
         return null;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {

         if (absolutefilePath.toLowerCase().startsWith(tourbookFileSystem.getId().toLowerCase())) {
            final String filePath =
                  absolutefilePath.substring(tourbookFileSystem.getId().length());
            return tourbookFileSystem.copyFileLocally(filePath);
         }
      }

      return null;
   }

   /**
    * Returns the {@link FileSystem}, if found, for a given device folder.
    *
    * @param deviceFolder
    * @return
    */
   public static FileSystem getFileSystem(final String deviceFolder) {

      if (_fileSystemsList == null) {
         return null;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         if (tourbookFileSystem.getId().equalsIgnoreCase(deviceFolder.toLowerCase())) {
            return tourbookFileSystem.getFileSystem();
         }
      }

      return null;
   }

   /**
    * Returns the id, if the {@link TourbookFileSystem} was found, for a given device folder name
    *
    * @param deviceFolderName
    * @return
    */
   public static String getFileSystemId(final String deviceFolderName) {
      String fileSystemsId = "";

      if (_fileSystemsList == null) {
         return fileSystemsId;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         if (tourbookFileSystem.getId().equalsIgnoreCase(deviceFolderName.toLowerCase())) {
            fileSystemsId = tourbookFileSystem.getId();
            return fileSystemsId;
         }
      }

      return fileSystemsId;
   }

   /**
    * Collects all the identifiers of the available file systems
    *
    * @return Returns a list of {@link String}
    */
   public static List<String> getFileSystemsIds() {

      final ArrayList<String> fileSystemsIds = new ArrayList<>();

      if (_fileSystemsList == null) {
         return fileSystemsIds;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         fileSystemsIds.add(tourbookFileSystem.getId());
      }

      return fileSystemsIds;
   }

   /**
    * Read file stores that can be used as file systems.
    *
    * @return Returns a list of {@link FileSystem}
    */
   public static List<TourbookFileSystem> getFileSystemsList() {

      if (_fileSystemsList == null) {
         _fileSystemsList = readFileSystemsExtensions("fileSystem"); //$NON-NLS-1$
      }

      return _fileSystemsList;
   }

   public static Path getfolderPath(final String folderName) {

      if (_fileSystemsList == null) {
         return null;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         if (tourbookFileSystem.getId().equalsIgnoreCase(folderName.toLowerCase())) {
            return tourbookFileSystem.getfolderPath(folderName);
         }
      }
      return null;
   }

   public static boolean isFileFromTourBookFileSystem(final String osFilePath) {

      if (_fileSystemsList == null) {
         return false;
      }

      for (final TourbookFileSystem tourbookFileSystem : _fileSystemsList) {
         if (osFilePath.toLowerCase().startsWith(tourbookFileSystem.getId().toLowerCase())) {
            return true;
         }
      }

      return false;
   }

   private static ArrayList<TourbookFileSystem> readFileSystemsExtensions(final String extensionPointName) {

      final ArrayList<TourbookFileSystem> fileSystemsList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {

            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase("fileSystem")) { //$NON-NLS-1$

                  Object object;
                  try {

                     object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                     if (object instanceof TourbookFileSystem) {
                        final TourbookFileSystem fileSystem = (TourbookFileSystem) object;
                        fileSystemsList.add(fileSystem);
                     }

                  } catch (final CoreException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return fileSystemsList;
   }
}
