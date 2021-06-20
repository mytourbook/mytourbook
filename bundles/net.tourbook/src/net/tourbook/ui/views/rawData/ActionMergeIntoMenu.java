/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionMergeIntoMenu extends Action implements IMenuCreator {

   private RawDataView fRawDataView;

   private Menu        fMenu;

   public class ActionNotAvailable extends Action {
      public ActionNotAvailable() {
         setText(Messages.import_data_action_assignment_is_not_available);
         setEnabled(false);
      }
   }

   public ActionMergeIntoMenu(final RawDataView rawDataView) {

      super(UI.EMPTY_STRING, AS_DROP_DOWN_MENU);
      setMenuCreator(this);

      fRawDataView = rawDataView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Saved_MergedTour));
      setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Saved_MergedTour_Disabled));
   }

   private void addActionToMenu(final Action action, final Menu menu) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   @Override
   public void dispose() {
      if (fMenu != null) {
         fMenu.dispose();
         fMenu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      // check selected tour
      final ArrayList<TourData> selectedFromTour = fRawDataView.getAllSelectedTours();
      if (selectedFromTour == null || selectedFromTour.size() != 1) {
         addActionToMenu(new ActionNotAvailable(), menu);
         return;
      }

      final TourData mergeFromTour = selectedFromTour.get(0);
      final Collection<TourData> importedTours = RawDataManager.getInstance().getImportedTours().values();

      final ArrayList<TourData> sortedTours = new ArrayList<>(importedTours);
      Collections.sort(sortedTours);
      int menuItems = 0;

      // get all tours which can be assigned, these will be tours which have not been assigned yet
      for (final TourData selectedTour : sortedTours) {

         // add tours which are already saved
         if (selectedTour.getTourPerson() != null
               //
               // check if this is the selected tour
               && mergeFromTour != selectedTour
               //
               // check if the merge tour id is set
//					&& selectedTour.getMergeTargetTourId() == null
               //
               // check if time series are available
               && selectedTour.timeSerie != null
               && selectedTour.timeSerie.length != 0
               && mergeFromTour.timeSerie != null
               && mergeFromTour.timeSerie.length != 0) {

            final ActionMergeInto actionMerge = new ActionMergeInto(mergeFromTour, selectedTour, fRawDataView);
            addActionToMenu(actionMerge, menu);
            menuItems++;
         }
      }

      // it's possible that a merge action is not available
      if (menuItems == 0) {
         addActionToMenu(new ActionNotAvailable(), menu);
      }
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();
      fMenu = new Menu(parent);

      // Add listener to repopulate the menu each time
      fMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuShown(final MenuEvent e) {

            // dispose old menu items
            for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
               menuItem.dispose();
            }

            fillMenu(fMenu);
         }
      });

      return fMenu;
   }

}
