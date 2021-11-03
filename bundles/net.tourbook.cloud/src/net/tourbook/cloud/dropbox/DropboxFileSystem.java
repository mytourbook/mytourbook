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
package net.tourbook.cloud.dropbox;

import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.CloudImages;
import net.tourbook.cloud.Preferences;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class DropboxFileSystem extends TourbookFileSystem {

   private java.nio.file.FileSystem _dropboxFileSystem;
   private IPreferenceStore         _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener  _prefChangeListenerCommon;

   public DropboxFileSystem() {

      super("Dropbox"); //$NON-NLS-1$

      createDropboxFileSystem();

      _prefChangeListenerCommon = event -> {

         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

            closeDropboxFileSystem();

            // Re create the Dropbox file system
            createDropboxFileSystem();
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListenerCommon);
   }

   @Override
   protected void close() {

      _prefStore.removePropertyChangeListener(_prefChangeListenerCommon);
      closeDropboxFileSystem();
   }

   /**
    * Closes the Dropbox Java 8 FileSystem.
    * This function will be called whenever the user
    * stops the folder watch or quits MyTourbook.
    */
   public void closeDropboxFileSystem() {

      if (_dropboxFileSystem == null) {
         return;
      }

      try {
         _dropboxFileSystem.close();
         _dropboxFileSystem = null;
      } catch (final IOException e) {
         StatusUtil.log(e);
      }
   }

   @Override
   protected File copyFileLocally(final String dropboxFilePath) {

      final Path localFilePath = DropboxClient.CopyLocally(dropboxFilePath);

      return localFilePath != null ? localFilePath.toFile() : null;
   }

   /**
    * Creates a Java 7 FileSystem over DropBox using the library
    * https://github.com/FJBDev/java7-fs-dropbox
    *
    * @return True if the file system was created successfully, false otherwise.
    */
   public boolean createDropboxFileSystem() {

      boolean result = false;

      final String accessToken = DropboxClient.getValidTokens();
      if (StringUtils.isNullOrEmpty(accessToken)) {
         _dropboxFileSystem = null;
         return result;
      }

      final URI uri = URI.create("dropbox://root"); //$NON-NLS-1$
      final Map<String, String> properties = new HashMap<>();
      properties.put("accessToken", accessToken); //$NON-NLS-1$

      final FileSystemProvider provider = new DropBoxFileSystemProvider(new DropBoxFileSystemRepository());

      try {
         _dropboxFileSystem = provider.newFileSystem(uri, properties);

         result = true;
      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      return result;
   }

   /**
    * Retrieves the Dropbox {@link FileStore}.
    * Creates it if necessary.
    *
    * @return A list of Dropbox {@link FileStore}
    */
   @Override
   public Iterable<FileStore> getFileStore() {

      if (_dropboxFileSystem != null || createDropboxFileSystem()) {
         return _dropboxFileSystem.getFileStores();
      }

      return null;
   }

   /**
    * Retrieves the Dropbox {@link FileSystem}.
    *
    * @return
    */
   @Override
   protected FileSystem getFileSystem() {
      return _dropboxFileSystem;
   }

   @Override
   public ImageDescriptor getFileSystemImageDescriptor() {
      return Activator.getImageDescriptor(CloudImages.Cloud_Dropbox_Logo);
   }

   /**
    * Get the Dropbox {@link Path} of a given filename
    *
    * @param fileName
    * @return
    */
   @Override
   protected Path getfolderPath(final String folderName) {
      if (_dropboxFileSystem == null) {
         return null;
      }

      //We remove the "Dropbox" string from the folderName
      final String dropboxFilePath = folderName.substring(getId().length());
      return _dropboxFileSystem.getPath(dropboxFilePath);
   }

   @Override
   public String getPreferencePageId() {
      return PrefPageDropbox.ID;
   }

   /**
    * When the user clicks on the "Choose Folder" button, a dialog is opened
    * so that the user can choose which folder will be used when using their Dropbox
    * account as a device to watch.
    */
   @Override
   public String selectFileSystemFolder(final Shell shell, final String workingDirectory) {

      final DialogDropboxFolderBrowser[] dropboxFolderChooser = new DialogDropboxFolderBrowser[1];
      final int[] folderChooserResult = new int[1];
      BusyIndicator.showWhile(Display.getCurrent(), () -> {
         final String accessToken = DropboxClient.getValidTokens();

         dropboxFolderChooser[0] = new DialogDropboxFolderBrowser(Display.getCurrent().getActiveShell(),
               accessToken,
               workingDirectory);
         folderChooserResult[0] = dropboxFolderChooser[0].open();
      });

      if (folderChooserResult[0] == Window.OK) {

         final String selectedFolder = dropboxFolderChooser[0].getSelectedFolder();
         if (StringUtils.hasContent(selectedFolder)) {
            FILE_SYSTEM_FOLDER = selectedFolder;

            return getDisplayId() + selectedFolder;
         }
      }

      return UI.EMPTY_STRING;
   }
}
