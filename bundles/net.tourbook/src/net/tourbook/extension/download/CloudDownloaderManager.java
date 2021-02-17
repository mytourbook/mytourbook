/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.extension.download;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudDownloaderManager {

   private static List<TourbookCloudDownloader> _cloudDownloadersList;

   public static List<String> getCloudDownloaderIds() {

      final ArrayList<String> cloudDownloaderIds = new ArrayList<>();

      getCloudDownloaderList();

      _cloudDownloadersList.forEach(cd -> cloudDownloaderIds.add(cd.getId()));

      return cloudDownloaderIds;
   }

   public static List<TourbookCloudDownloader> getCloudDownloaderList() {

      if (_cloudDownloadersList == null) {
         _cloudDownloadersList = readCloudDownloadersExtensions("cloudDownloader"); //$NON-NLS-1$
      }

      return _cloudDownloadersList;
   }


   /**
    * Read and collects all the extensions that implement {@link TourbookCloudDownloader}.
    *
    * @param extensionPointName
    *           The extension point name
    * @return The list of {@link TourbookCloudDownloader}.
    */
   private static ArrayList<TourbookCloudDownloader> readCloudDownloadersExtensions(final String extensionPointName) {

      final ArrayList<TourbookCloudDownloader> cloudDownloadersList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {

            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase(extensionPointName)) {

                  Object object;
                  try {

                     object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                     if (object instanceof TourbookCloudDownloader) {
                        cloudDownloadersList.add((TourbookCloudDownloader) object);
                     }

                  } catch (final CoreException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return cloudDownloadersList;
   }
}
