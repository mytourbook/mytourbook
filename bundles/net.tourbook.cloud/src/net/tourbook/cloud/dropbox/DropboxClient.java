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

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class DropboxClient {

   private static DbxClientV2      _dropboxClient;
   private static DbxRequestConfig _requestConfig;
   final static IPreferenceStore   _prefStore = CommonActivator.getPrefStore();
   private static String           _accessToken;

   static {
      final IPropertyChangeListener prefChangeListenerCommon = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            if (event.getProperty().equals(ICommonPreferences.DROPBOX_ACCESSTOKEN)) {

               // Re create the Dropbox client
               createDefaultDropboxClient();
            }
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(prefChangeListenerCommon);

      createDefaultDropboxClient();
   }

   /**
    * Downloads a remote Dropbox file to a local temporary location
    *
    * @param dropboxFilefilePath
    *           The Dropbox path of the file
    * @return The local path of the downloaded file
    */
   public static Path CopyLocally(final String dropboxFilefilePath) {

      if (StringUtils.isNullOrEmpty(dropboxFilefilePath)) {
         return null;
      }

      final String fileName = Paths.get(dropboxFilefilePath).getFileName().toString();
      final Path filePath = Paths.get(FileUtils.getTempDirectoryPath(), fileName);

      //Downloading the file from Dropbox to the local disk
      try (OutputStream outputStream = new FileOutputStream(filePath.toString())) {

         _dropboxClient.files().download(dropboxFilefilePath).download(outputStream);

         return filePath;
      } catch (final DbxException | IOException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * Creates a Dropbox client with the access token from the preferences
    */
   private static void createDefaultDropboxClient() {

      _accessToken = _prefStore.getString(ICommonPreferences.DROPBOX_ACCESSTOKEN);

      _dropboxClient = createDropboxClient(_accessToken);
   }

   /**
    * Creates a Dropbox client with a given access token.
    * This will happen, for example, when a user has just retrieved an access token
    * but has not saved it yet into the preferences but wants to access the Dropbox account already.
    *
    * @param accessToken
    * @return
    */
   private static DbxClientV2 createDropboxClient(final String accessToken) {

      //Getting the current version of MyTourbook
      final Version version = FrameworkUtil.getBundle(DropboxClient.class).getVersion();

      _requestConfig = DbxRequestConfig.newBuilder("mytourbook/" + version.toString().replace(".qualifier", UI.EMPTY_STRING)).build(); //$NON-NLS-1$ //$NON-NLS-2$

      return new DbxClientV2(_requestConfig, accessToken);
   }

   /**
    * Gets the default Dropbox client if an access token was not provided.
    * Otherwise, creates a temporary Dropbox client.
    *
    * @param accessToken
    * @return
    */
   public static DbxClientV2 getDefault(final String accessToken) {

      if (StringUtils.isNullOrEmpty(accessToken)) {
         return _dropboxClient;
      }

      return createDropboxClient(accessToken);
   }
}
