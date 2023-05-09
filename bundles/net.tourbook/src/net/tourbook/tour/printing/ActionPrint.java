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
package net.tourbook.tour.printing;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.Menu;

public class ActionPrint extends SubMenu {

   private static List<PrintTourExtension> _printExtensionPoints;

   private Menu                            _menu;
   private List<ActionPrintTour>           _printTourActions;

   private ITourProvider                   _tourProvider;

   private int                             _tourStartIndex = -1;
   private int                             _tourEndIndex   = -1;

   private class ActionPrintTour extends Action {

      private PrintTourExtension _printTourExtension;

      public ActionPrintTour(final PrintTourExtension printTourExtension) {

         super(printTourExtension.getVisibleName());
         setImageDescriptor(printTourExtension.getImageDescriptor());
         _printTourExtension = printTourExtension;
      }

      @SuppressWarnings("unused")
      ActionPrintTour(final String visibleName, final String fileExtension) {}

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

         _printTourExtension.printTours(selectedTours, _tourStartIndex, _tourEndIndex);
      }

   }

   /**
    * @param tourProvider
    * @param isAddMode
    * @param isSaveTour
    *           when <code>true</code> the tour will be saved and a
    *           {@link TourManager#TOUR_CHANGED} event is fired, otherwise the {@link TourData}
    *           from the tour provider is only updated
    */
   public ActionPrint(final ITourProvider tourProvider) {

      super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      setText(Messages.action_print_tour);

      getExtensionPoints();
      createActions();
   }

   private void addActionToMenu(final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(_menu, -1);
   }

   private void createActions() {

      if (_printTourActions != null) {
         return;
      }

      _printTourActions = new ArrayList<>();

      // create action for each extension point
      for (final PrintTourExtension printTourExtension : _printExtensionPoints) {
         _printTourActions.add(new ActionPrintTour(printTourExtension));
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      _menu = menu;

      _printTourActions.forEach(this::addActionToMenu);
   }

   /**
    * read extension points {@link TourbookPlugin#EXT_POINT_PRINT_TOUR}
    */
   private List<PrintTourExtension> getExtensionPoints() {

      if (_printExtensionPoints != null) {
         return _printExtensionPoints;
      }

      _printExtensionPoints = new ArrayList<>();

      final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(TourbookPlugin.PLUGIN_ID,
            TourbookPlugin.EXT_POINT_PRINT_TOUR);

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {
            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase("print")) { //$NON-NLS-1$
                  try {
                     final Object object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
                     if (object instanceof PrintTourExtension) {

                        final PrintTourExtension printTourItem = (PrintTourExtension) object;

                        printTourItem.setPrintId(configElement.getAttribute("id")); //$NON-NLS-1$
                        printTourItem.setVisibleName(configElement.getAttribute("name")); //$NON-NLS-1$

                        _printExtensionPoints.add(printTourItem);
                     }
                  } catch (final CoreException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return _printExtensionPoints;
   }

   public void setTourRange(final int tourStartIndex, final int tourEndIndex) {

      _tourStartIndex = tourStartIndex;
      _tourEndIndex = tourEndIndex;
   }
}
