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
package net.tourbook.map2.action;

import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.preferences.PrefPage_Map2_Providers;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ActionManageMapProviders extends Action {

   private Map2View             _map2View;

   private AnimatedToolTipShell _openedTooltip;

   public ActionManageMapProviders(final Map2View map2View) {

      super(Messages.Map_Action_ManageMapProviders, AS_PUSH_BUTTON);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Options));

      _map2View = map2View;
   }

   /**
    * This tooltip will be closed when the pref dialog is opened.
    *
    * @param openedTooltip
    */
   public void closeThisTooltip(final AnimatedToolTipShell openedTooltip) {

      _openedTooltip = openedTooltip;
   }

   @Override
   public void run() {

      // set the currently displayed map provider so that this mp will be selected in the pref page
      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

      prefStore.setValue(//
            IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER,
            _map2View.getMap().getMapProvider().getId());

      PreferencesUtil.createPreferenceDialogOn(
            Display.getCurrent().getActiveShell(),
            PrefPage_Map2_Providers.ID,
            null,
            null).open();
   }

}
