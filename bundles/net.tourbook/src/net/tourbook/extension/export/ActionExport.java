/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.extension.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.ui.SubMenu;
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
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

/**
 * Submenu for exporting tours
 */
public class ActionExport extends SubMenu {

   private List<ExportTourExtension>    _exportExtensionPoints;

   private List<ActionExportTour>       _exportTourActions;
   private List<ActionContributionItem> _exportTourContributionItems;

   private final ITourProvider          _tourProvider;

   private int                          _tourStartIndex = -1;
   private int                          _tourEndIndex   = -1;

   private class ActionExportTour extends Action {

      private final ExportTourExtension _exportTourExtension;

      public ActionExportTour(final ExportTourExtension exportTourExtension) {

         super(exportTourExtension.getVisibleName());
         setImageDescriptor(exportTourExtension.getImageDescriptor());

         _exportTourExtension = exportTourExtension;
      }

      public ExportTourExtension getExportTourExtension() {
         return _exportTourExtension;
      }

      @Override
      public void run() {

         final ArrayList<TourData> selectedTours;

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

         getExportTourExtension().exportTours(selectedTours, _tourStartIndex, _tourEndIndex);
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
   public ActionExport(final ITourProvider tourProvider) {

      super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      setText(Messages.action_export_tour);

      getExtensionPoints();
      createActions();
   }

   private void createActions() {

      if (_exportTourActions != null) {
         return;
      }

      _exportTourActions = new ArrayList<>();

      // create action for each extension point
      _exportExtensionPoints.forEach(exportTourExtension -> _exportTourActions.add(
            new ActionExportTour(exportTourExtension)));

      // create a menu item for each extension point as the MT extension point
      // needs to be at the end
      _exportTourContributionItems = new ArrayList<>();

      final Optional<ActionExportTour> mtExtension = _exportTourActions.stream().filter(extension -> extension.getExportTourExtension()
            .getFileExtension().equals(
                  "mt")).findFirst(); //$NON-NLS-1$
      ActionExportTour mtActionExportTour = null;
      if (mtExtension.isPresent()) {

         mtActionExportTour = mtExtension.get();
         _exportTourActions.remove(mtActionExportTour);
      }

      final List<ActionExportTour> sortedExportTourActions = _exportTourActions.stream()
            .sorted((o1, o2) -> o1.getExportTourExtension().getVisibleName().compareTo(o2.getExportTourExtension().getVisibleName()))
            .collect(Collectors.toList());
      sortedExportTourActions.forEach(action -> _exportTourContributionItems.add(new ActionContributionItem(action)));
      if (mtActionExportTour != null) {

         _exportTourContributionItems.add(new ActionContributionItem(mtActionExportTour));
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      for (final ActionContributionItem _exportTourContributionItem : _exportTourContributionItems) {

         if (((ActionExportTour) _exportTourContributionItem.getAction()).getExportTourExtension().getFileExtension().equals("mt")) { //$NON-NLS-1$

            (new Separator()).fill(menu, -1);
         }

         _exportTourContributionItem.fill(menu, -1);
      }
   }

   /**
    * Read extension points {@link TourbookPlugin#EXT_POINT_EXPORT_TOUR}
    */
   private List<ExportTourExtension> getExtensionPoints() {

      if (_exportExtensionPoints != null) {
         return _exportExtensionPoints;
      }

      _exportExtensionPoints = new ArrayList<>();

      final IExtensionPoint extPoint = Platform.getExtensionRegistry()
            .getExtensionPoint(
                  TourbookPlugin.PLUGIN_ID,
                  TourbookPlugin.EXT_POINT_EXPORT_TOUR);

      if (extPoint == null) {
         return _exportExtensionPoints;
      }

      for (final IExtension extension : extPoint.getExtensions()) {

         for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

            if (configElement.getName().equalsIgnoreCase("export") == false) { //$NON-NLS-1$
               continue;
            }
            try {
               final Object object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
               if (object instanceof ExportTourExtension) {

                  final ExportTourExtension exportTourItem = (ExportTourExtension) object;

                  exportTourItem.setExportId(configElement.getAttribute("id")); //$NON-NLS-1$
                  exportTourItem.setVisibleName(configElement.getAttribute("name")); //$NON-NLS-1$
                  exportTourItem.setFileExtension(configElement.getAttribute("fileextension")); //$NON-NLS-1$

                  _exportExtensionPoints.add(exportTourItem);
               }
            } catch (final CoreException e) {
               e.printStackTrace();
            }
         }
      }

      return _exportExtensionPoints;
   }

   public void setNumberOfTours(final int numTours) {

      setText(Messages.action_export_tour + String.format(" (%d)", numTours)); //$NON-NLS-1$
   }

   public void setTourRange(final int tourStartIndex, final int tourEndIndex) {

      _tourStartIndex = tourStartIndex;
      _tourEndIndex = tourEndIndex;
   }

}
