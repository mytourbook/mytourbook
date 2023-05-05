/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
import java.util.Optional;

import net.tourbook.common.util.StringUtils;

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

      _fileSystemsList.forEach(TourbookFileSystem::close);
   }

   /**
    * Copy a file from a {@link TourBookFileSystem} to the user's
    * local file system.
    *
    * @param absolutefilePath
    * @return
    */
   public static File CopyLocally(final String absolutefilePath) {

      final TourbookFileSystem tourbookFileSystem = getTourbookFileSystem(absolutefilePath);

      if (tourbookFileSystem == null) {
         return null;
      }

      final String filePath = absolutefilePath.substring(tourbookFileSystem.getId().length());

      return tourbookFileSystem.copyFileLocally(filePath);
   }

   /**
    * Returns the {@link FileSystem}, if found, for a given device folder.
    *
    * @param deviceFolder
    * @return
    */
   public static FileSystem getFileSystem(final String deviceFolder) {

      final TourbookFileSystem tourbookFileSystem = getTourbookFileSystem(deviceFolder);

      if (tourbookFileSystem != null) {
         return tourbookFileSystem.getFileSystem();
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

      final TourbookFileSystem tourbookFileSystem = getTourbookFileSystem(deviceFolderName);

      if (tourbookFileSystem != null) {
         return tourbookFileSystem.getId();
      }

      return UI.EMPTY_STRING;
   }

   /**
    * Collects all the identifiers of the available file systems
    *
    * @return Returns a list of {@link String}
    */
   public static List<String> getFileSystemsIds() {

      final ArrayList<String> fileSystemsIds = new ArrayList<>();

      getFileSystemsList();

      _fileSystemsList.forEach(tfs -> fileSystemsIds.add(tfs.getId()));

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

      final TourbookFileSystem tourbookFileSystem = getTourbookFileSystem(folderName);

      if (tourbookFileSystem != null) {
         return tourbookFileSystem.getfolderPath(folderName);
      }

      return null;
   }

   /**
    * Returns the {@link TourbookFileSystem}, if found, for a given device folder.
    *
    * @param deviceFolder
    * @return
    */
   public static TourbookFileSystem getTourbookFileSystem(final String deviceFolder) {

      if (StringUtils.isNullOrEmpty(deviceFolder)) {
         return null;
      }

      getFileSystemsList();

      final Optional<TourbookFileSystem> fileSystemSearchResult = _fileSystemsList.stream()
            .filter(tfs -> deviceFolder.toLowerCase().startsWith(tfs.getId().toLowerCase()) ||
                  deviceFolder.toLowerCase().startsWith(tfs.getDisplayId().toLowerCase()))
            .findAny();

      if (fileSystemSearchResult.isPresent()) {
         return fileSystemSearchResult.get();
      }

      return null;
   }

   /**
    * Determines, for a given file path, if it belongs to a {@link TourbookFileSystem}
    *
    * @param absoluteFilePath
    *           A given absolute file path
    * @return True if the file comes from a {@link TourbookFileSystem}, false otherwise.
    */
   public static boolean isFileFromTourBookFileSystem(final String absoluteFilePath) {

      getFileSystemsList();

      return _fileSystemsList.stream()
            .anyMatch(tfs -> absoluteFilePath.toLowerCase().startsWith(tfs.getId().toLowerCase()));
   }

   /**
    * Read and collects all the extensions that implement {@link TourbookFileSystem}.
    *
    * @param extensionPointName
    *           The extension point name
    * @return The list of {@link TourbookFileSystem}.
    */
   private static ArrayList<TourbookFileSystem> readFileSystemsExtensions(final String extensionPointName) {

      final ArrayList<TourbookFileSystem> fileSystemsList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint == null) {
         return fileSystemsList;
      }

      for (final IExtension extension : extPoint.getExtensions()) {

         for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

            if (configElement.getName().equalsIgnoreCase(extensionPointName)) {

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

      return fileSystemsList;
   }
}
