/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.action.ActionComputeCadenceZonesTimes;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionComputeElevationGain;
import net.tourbook.ui.action.ActionMultiplyCaloriesBy1000;
import net.tourbook.ui.action.ActionRetrieveWeatherData;
import net.tourbook.ui.action.ActionSetAltitudeValuesFromSRTM;
import net.tourbook.ui.action.ActionSetTimeZone;
import net.tourbook.ui.action.Action_SetCadence_SubMenu;
import net.tourbook.ui.action.Action_Weather_SubMenu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class Action_AdjustTourValues_SubMenu extends Action implements IMenuCreator {

   private ITourProvider2                             _tourProvider;
   private ITourProviderByID                          _tourProviderById;

   private Action_SetCadence_SubMenu                  _action_SetCadence_SubMenu;

   private ActionComputeCadenceZonesTimes             _action_ComputeCadenceZonesTimes;
   private ActionComputeDistanceValuesFromGeoposition _action_ComputeDistanceValuesFromGeoposition;
   private ActionComputeElevationGain                 _action_ComputeElevationGain;
   private ActionMultiplyCaloriesBy1000               _action_MultiplyCaloriesBy1000;
   private ActionSetAltitudeValuesFromSRTM            _action_SetAltitudeFromSRTM;
   private ActionSetTimeZone                          _action_SetTimeZone;
   private Action_Weather_SubMenu                     _action_Weather_SubMenu;

   /*
    * UI controls
    */
   private Menu _menu;

   public Action_AdjustTourValues_SubMenu(final ITourProvider2 tourProvider, final ITourProviderByID tourProviderById) {

      super(Messages.Tour_Action_AdjustTourValues, AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourProvider = tourProvider;
      _tourProviderById = tourProviderById;

      _action_SetCadence_SubMenu = new Action_SetCadence_SubMenu(_tourProvider);
      _action_Weather_SubMenu = new Action_Weather_SubMenu(_tourProvider);

      _action_ComputeCadenceZonesTimes = new ActionComputeCadenceZonesTimes(_tourProvider);
      _action_ComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(_tourProvider);
      _action_ComputeElevationGain = new ActionComputeElevationGain(_tourProviderById);
      _action_MultiplyCaloriesBy1000 = new ActionMultiplyCaloriesBy1000(_tourProvider);
      _action_SetAltitudeFromSRTM = new ActionSetAltitudeValuesFromSRTM(_tourProvider);
      _action_SetTimeZone = new ActionSetTimeZone(_tourProvider);
   }

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_ComputeDistanceValuesFromGeoposition).fill(menu, -1);
      new ActionContributionItem(_action_ComputeElevationGain).fill(menu, -1);
      new ActionContributionItem(_action_ComputeCadenceZonesTimes).fill(menu, -1);
      new ActionContributionItem(_action_MultiplyCaloriesBy1000).fill(menu, -1);
      new ActionContributionItem(_action_SetAltitudeFromSRTM).fill(menu, -1);
      new ActionContributionItem(_action_SetCadence_SubMenu).fill(menu, -1);
      new ActionContributionItem(_action_SetTimeZone).fill(menu, -1);
      new ActionContributionItem(_action_Weather_SubMenu).fill(menu, -1);
   }

   public ActionRetrieveWeatherData getActionRetrieveWeatherData() {
      return _action_Weather_SubMenu.getActionRetrieveWeatherData();
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
      _menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuShown(final MenuEvent e) {

            // dispose old menu items
            for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
               menuItem.dispose();
            }

            fillMenu(_menu);
         }
      });

      return _menu;
   }

}
