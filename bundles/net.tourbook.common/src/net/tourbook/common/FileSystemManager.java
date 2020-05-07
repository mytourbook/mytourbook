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

import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class FileSystemManager {

   private static List<FileSystem> _fileSystemsList;

   /**
    * Read file stores that can be used as file systems.
    *
    * @return Returns a list of {@link FileSystem}
    */
   @SuppressWarnings("unchecked")
   public static List<FileSystem> getFileSystemsList() {

      if (_fileSystemsList == null) {

         _fileSystemsList = readFileSystemsExtensions("cloudFileSystem");//TourbookPlugin.EXT_POINT_DEVICE_DATA_READER);
      }

      return _fileSystemsList;
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   private static ArrayList readFileSystemsExtensions(final String extensionPointName) {

      final ArrayList fileSystemsList = new ArrayList();

      final IExtensionPoint extPoint = Platform.getExtensionRegistry()//
            .getExtensionPoint("net.tourbook", extensionPointName);

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {

            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase("cloud")) { //$NON-NLS-1$

                  Object object;
                  try {

                     object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                     /*
                      * if (object instanceof TourbookDevice) {
                      * final TourbookDevice device = (TourbookDevice) object;
                      * device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
                      * device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
                      * final String extensionSortPriority = configElement
                      * .getAttribute("extensionSortPriority"); //$NON-NLS-1$
                      * if (extensionSortPriority != null) {
                      * try {
                      * device.extensionSortPriority = Integer.parseInt(extensionSortPriority);
                      * } catch (final Exception e) {
                      * // do nothing
                      * }
                      * }
                      * fileSystemsList.add(device);
                      * }
                      * if (object instanceof ExternalDevice) {
                      * final ExternalDevice device = (ExternalDevice) object;
                      * device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
                      * device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
                      * fileSystemsList.add(device);
                      * }
                      */

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
