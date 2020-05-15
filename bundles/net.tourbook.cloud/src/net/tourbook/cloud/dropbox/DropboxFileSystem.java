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
package net.tourbook.cloud.dropbox;

import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.ICloudPreferences;
import net.tourbook.common.IFileSystem;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class DropboxFileSystem implements IFileSystem {

   private java.nio.file.FileSystem _dropboxFileSystem;

   private IPreferenceStore         _prefStore = Activator.getDefault().getPreferenceStore();

   public DropboxFileSystem() {

      createDropboxFileSystem();

      final IPropertyChangeListener prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            if (event.getProperty().equals(ICloudPreferences.DROPBOX_ACCESSTOKEN)) {

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
   public void closeDropboxFileSystem() {

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
    * Creates a Java 7 FileSystem over DropBox using the library
    * https://github.com/FJBDev/java7-fs-dropbox
    *
    * @return True if the file system was created successfully, false otherwise.
    */
   public boolean createDropboxFileSystem() {

      boolean result = false;

      final URI uri = URI.create("dropbox://root"); //$NON-NLS-1$
      final Map<String, String> env = new HashMap<>();

      final String accessToken = _prefStore.getString(ICloudPreferences.DROPBOX_ACCESSTOKEN);
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
         //TODO FB  StatusUtil.log(e);
      }

      return result;
   }

   /**
    * Get the Dropbox {@link Path} of a given filename
    *
    * @param fileName
    * @return
    */
   private Path getDropboxFilePath(final String fileName) {
      if (_dropboxFileSystem == null) {
         return null;
      }

      final String dropboxFilePath = _prefStore.getString(ICloudPreferences.DROPBOX_FOLDER) + fileName;
      return _dropboxFileSystem.getPath(dropboxFilePath);
   }

   /**
    * Retrieves the Dropbox {@link FileStore}.
    * Creates it if necessary.
    *
    * @return A list of Dropbox {@link FileStore}
    */
   private Iterable<FileStore> getDropboxFileStores() {
      if (_dropboxFileSystem != null) {
         return _dropboxFileSystem.getFileStores();
      } else {
         if (createDropboxFileSystem()) {
            return _dropboxFileSystem.getFileStores();
         }
      }

      return null;
   }

   @Override
   public String getFileSystemId() {
      return "32942983";
   }

   @Override
   public String getFileSystemName() {
      return "Dropbox";
   }
}
