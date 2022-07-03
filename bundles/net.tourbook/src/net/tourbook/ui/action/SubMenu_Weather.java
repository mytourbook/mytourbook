/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 */
public class SubMenu_Weather extends Action implements IMenuCreator {

   private Menu                           _menu;

   private ActionAdjustTemperature        _action_AdjustTemperature;
   private ActionRetrieveWeatherData      _action_RetrieveWeatherData;
   private ActionComputeMinMaxTemperature _action_ComputeMinMaxTemperature;
   private SubMenu_SetWeatherConditions   _subMenu_SetWeatherConditions;

   private ITourProvider2                 _tourProvider;

   public SubMenu_Weather(final ITourProvider2 tourViewer) {

      super(Messages.Tour_Action_Weather, AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourProvider = tourViewer;

      _action_AdjustTemperature = new ActionAdjustTemperature(_tourProvider);
      _action_ComputeMinMaxTemperature = new ActionComputeMinMaxTemperature(_tourProvider);
      _action_RetrieveWeatherData = new ActionRetrieveWeatherData(_tourProvider);
      _subMenu_SetWeatherConditions = new SubMenu_SetWeatherConditions(_tourProvider);
   }

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_AdjustTemperature).fill(menu, -1);
      new ActionContributionItem(_action_ComputeMinMaxTemperature).fill(menu, -1);
      new ActionContributionItem(_action_RetrieveWeatherData).fill(menu, -1);
      new ActionContributionItem(_subMenu_SetWeatherConditions).fill(menu, -1);
   }

   public ActionRetrieveWeatherData getActionRetrieveWeatherData() {
      return _action_RetrieveWeatherData;
   }

   @Override
   public Menu getMenu(final Control parent) {
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

}
