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
package net.tourbook.extension.upload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * Submenu for uploading tours
 */
public class ActionUpload extends Action implements IMenuCreator {

   private static List<TourbookCloudUploader> _tourbookCloudUploaders = new ArrayList<>();

   private static List<ActionUploadTour>      _uploadTourActions      = new ArrayList<>();
   private Menu                               _menu;

   private final ITourProvider                _tourProvider;

   private class ActionUploadTour extends Action {

      private final TourbookCloudUploader _tourbookCloudUploader;

      public ActionUploadTour(final TourbookCloudUploader tourbookCloudUploader) {

         super(tourbookCloudUploader.getName());

         _tourbookCloudUploader = tourbookCloudUploader;
      }

      public boolean isVendorReady() {
         return _tourbookCloudUploader.isReady();
      }

      @Override
      public void run() {

         final List<TourData> selectedTours;

         if (_tourProvider instanceof ITourProviderAll) {
            selectedTours = ((ITourProviderAll) _tourProvider).getAllSelectedTours();
         } else {
            selectedTours = _tourProvider.getSelectedTours();
         }

         if (selectedTours == null || selectedTours.isEmpty()) {
            return;
         }

         // sort by date/time
         Collections.sort(selectedTours);

         _tourbookCloudUploader.uploadTours(selectedTours);
      }

   }

   /**
    * @param tourProvider
    * @param isAddMode
    * @param isSaveTour
    *           when <code>true</code> the tour will be saved and a {@link TourManager#TOUR_CHANGED}
    *           event is fired, otherwise the {@link TourData} from the tour provider is only
    *           updated
    */
   public ActionUpload(final ITourProvider tourProvider) {

      super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      setText(Messages.action_upload_tour);
      setMenuCreator(this);

      getCloudUploaders();
      createActions();
   }

   /**
    * read extension points {@link TourbookPlugin#EXT_POINT_EXPORT_TOUR}
    */
   private static List<TourbookCloudUploader> getCloudUploaders() {

      if (_tourbookCloudUploaders.isEmpty()) {
         _tourbookCloudUploaders = readCloudUploaderExtensions("cloudUploader"); //$NON-NLS-1$
      }

      for (final ActionUploadTour actionUploadTour : _uploadTourActions) {
         actionUploadTour.setEnabled(actionUploadTour.isVendorReady());
      }

      return _tourbookCloudUploaders;
   }

   /**
    * Read and collects all the extensions that implement {@link TourbookFileSystem}.
    *
    * @param extensionPointName
    *           The extension point name
    * @return The list of {@link TourbookFileSystem}.
    */
   private static List<TourbookCloudUploader> readCloudUploaderExtensions(final String extensionPointName) {

      final List<TourbookCloudUploader> cloudUploadersList = new ArrayList<>();

      final IExtensionPoint extPoint = Platform
            .getExtensionRegistry()
            .getExtensionPoint("net.tourbook", extensionPointName); //$NON-NLS-1$

      if (extPoint == null) {
         return cloudUploadersList;
      }

      for (final IExtension extension : extPoint.getExtensions()) {

         for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

            if (configElement.getName().equalsIgnoreCase("cloudUploader")) { //$NON-NLS-1$

               Object object;
               try {

                  object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

                  if (object instanceof TourbookCloudUploader) {
                     final TourbookCloudUploader cloudUploader = (TourbookCloudUploader) object;
                     cloudUploadersList.add(cloudUploader);
                  }

               } catch (final CoreException e) {
                  e.printStackTrace();
               }
            }
         }
      }

      return cloudUploadersList;
   }

   private void addActionToMenu(final Action action) {
      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(_menu, -1);
   }

   private void createActions() {

      if (!_uploadTourActions.isEmpty()) {
         return;
      }

      for (final TourbookCloudUploader tourbookCloudUploader : _tourbookCloudUploaders) {

         _uploadTourActions.add(new ActionUploadTour(tourbookCloudUploader));
      }
   }

   @Override
   public void dispose() {
      if (_menu == null) {
         return;
      }

      _menu.dispose();
      _menu = null;
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();
      _menu = new Menu(parent);

      for (final ActionUploadTour action : _uploadTourActions) {
         addActionToMenu(action);
      }

      return _menu;
   }

   public boolean hasUploaders() {
      return getCloudUploaders().size() > 0;
   }

   public void setNumberOfTours(final int numTours) {

      setText(Messages.action_export_tour + String.format(" (%d)", numTours)); //$NON-NLS-1$
   }
}
