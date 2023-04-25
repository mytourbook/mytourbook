/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.ui.action;

import static org.eclipse.swt.events.MenuListener.menuShownAdapter;

import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

/**
 * Set person for a tour
 */
public class ActionSetPausesType extends Action implements IMenuCreator {

   private Menu          _menu;

   private ITourProvider _tourProvider;
   private int[]         _tourPausesIndices;

   private class ActionSetPausesType2 extends Action {

      boolean _isAutoPause;

      public ActionSetPausesType2(final String text, final boolean isAutoPause) {

         super(text, AS_PUSH_BUTTON);

         _isAutoPause = isAutoPause;
      }

      @Override
      public void run() {
         setPausesType(_isAutoPause);
      }
   }

   public ActionSetPausesType(final ITourProvider tourProvider) {

      //todo fb
      //todo fb: Grey out the pause type that is already selected
      super("Set pauses type", AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourProvider = tourProvider;
   }
   private void addActionToMenu(final Action action, final Menu menu) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   @Override
   public void dispose() {
      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      addActionToMenu(new ActionSetPausesType2("Manual", false), menu);
      addActionToMenu(new ActionSetPausesType2("Automatic", true), menu);
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();
      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> {

         // dispose old menu items
         for (final MenuItem menuItem : ((Menu) menuEvent.widget).getItems()) {
                  menuItem.dispose();
               }

               fillMenu(_menu);
            }));

      return _menu;
   }

   public void setPausesType(final boolean isAutoPause) {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      final ArrayList<TourData> modifiedTours = new ArrayList<>();

      final Shell shell = Display.getCurrent().getActiveShell();
      if (selectedTours == null || selectedTours.isEmpty()) {

         // a tour is not selected
         MessageDialog.openInformation(
               shell,
               Messages.Dialog_SetWeatherDescription_Dialog_Title,
               Messages.UI_Label_TourIsNotSelected);

         return;
      }

      for (final TourData tourData : selectedTours) {

         long[] pausedTime_Data = tourData.getPausedTime_Data();

         if (pausedTime_Data == null) {

            final long[] pausedTime_Start = tourData.getPausedTime_Start();

            pausedTime_Data = new long[pausedTime_Start.length];
            Arrays.setAll(pausedTime_Data, i -> 1);
         }

         pausedTime_Data[_tourPausesIndices[0]] = isAutoPause ? 1 : 0;

         tourData.setPausedTime_Data(pausedTime_Data);

         modifiedTours.add(tourData);
      }

      if (modifiedTours.size() > 0) {
         TourManager.saveModifiedTours(modifiedTours);
      }

   }

   public void setTourPauses(final int[] selectedIndices) {

      _tourPausesIndices = selectedIndices;
   }

}
