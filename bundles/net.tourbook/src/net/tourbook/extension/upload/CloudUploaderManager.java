/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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
package net.tourbook.extension.upload;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudUploaderManager {

   private static List<TourbookCloudUploader> _cloudUploadersList;

   public static List<String> getCloudUploaderIds() {

      final ArrayList<String> cloudUploaderIds = new ArrayList<>();

      getCloudUploaderList();

      _cloudUploadersList.forEach(cloudUploader -> cloudUploaderIds.add(cloudUploader.getId()));

      return cloudUploaderIds;
   }

   public static List<TourbookCloudUploader> getCloudUploaderList() {

      if (_cloudUploadersList == null) {
         _cloudUploadersList = readCloudUploadersExtensions("cloudUploader"); //$NON-NLS-1$
      }

      return _cloudUploadersList;
   }

   /**
    * Reads and collects all the extensions that implement {@link TourbookCloudUploader}.
    *
    * @param extensionPointName
    *           The extension point name
    * @return The list of {@link TourbookCloudUploader}.
    */
   private static List<TourbookCloudUploader> readCloudUploadersExtensions(final String extensionPointName) {

      final List<TourbookCloudUploader> cloudUploadersList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint == null) {
         return cloudUploadersList;
      }

      for (final IExtension extension : extPoint.getExtensions()) {

         for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

            if (configElement.getName().equalsIgnoreCase(extensionPointName)) {

               Object object;
               try {

                  object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                  if (object instanceof TourbookCloudUploader) {
                     cloudUploadersList.add((TourbookCloudUploader) object);
                  }

               } catch (final CoreException e) {
                  e.printStackTrace();
               }
            }
         }
      }

      return cloudUploadersList;
   }
}
