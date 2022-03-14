/*******************************************************************************
 * Copyright (C) 2020, 2022 Frédéric Bard
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
import java.util.List;
import java.util.Objects;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class SubMenu_SetWeatherConditions extends Action implements IMenuCreator {

   private Menu                             _menu;

   private final ITourProvider2             _tourProvider;

   private List<ActionSetWeatherConditions> _actionsSetWeatherConditions = new ArrayList<>();

   private class ActionSetWeatherConditions extends Action {

      private String _weatherId;

      public ActionSetWeatherConditions(final String text, final String weatherId) {

         super(text, AS_PUSH_BUTTON);

         _weatherId = weatherId;
         setImageDescriptor(UI.IMAGE_REGISTRY.getDescriptor(_weatherId));
      }

      @Override
      public void run() {
         setWeatherConditions(_weatherId);
      }
   }

   public SubMenu_SetWeatherConditions(final ITourProvider2 tourProvider) {

      super(Messages.Tour_Action_SetWeatherConditions, AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourProvider = tourProvider;

      for (int index = 0; index < IWeather.cloudText.length; ++index) {

         _actionsSetWeatherConditions.add(new ActionSetWeatherConditions(IWeather.cloudText[index],
               IWeather.cloudIcon[index]));
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

   private void fillMenu(final Menu menu) {

      for (final ActionSetWeatherConditions actionSetWeatherConditions : _actionsSetWeatherConditions) {
         new ActionContributionItem(actionSetWeatherConditions).fill(menu, -1);
      }
   }

   @Override
   public Menu getMenu(final Control arg0) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();

      _menu = new Menu(parent);

      // Add listener to re-populate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> {

         // dispose old menu items
         for (final MenuItem menuItem : ((Menu) menuEvent.widget).getItems()) {
            menuItem.dispose();
         }

         fillMenu(_menu);
      }));

      return _menu;
   }

   public void setWeatherConditions(final String weatherDescription) {

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

         if (Objects.equals(tourData.getWeather_Clouds(), weatherDescription)) {
            continue;
         }

         // Weather description is not the same

         tourData.setWeather_Clouds(weatherDescription);

         modifiedTours.add(tourData);
      }

      if (modifiedTours.size() > 0) {
         TourManager.saveModifiedTours(modifiedTours);
      }

   }
}
