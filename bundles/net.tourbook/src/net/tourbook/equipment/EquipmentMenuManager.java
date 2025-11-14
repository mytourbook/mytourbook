/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;

public class EquipmentMenuManager {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private ITourProvider                 _tourProvider;

   private ActionAddEquipment_SubMenu    _actionAddEquipment;

   public EquipmentMenuManager(final ITourProvider tourProvider) {

      _tourProvider = tourProvider;

      createActions();

   }

   private void createActions() {

      _actionAddEquipment = new ActionAddEquipment_SubMenu("&Add Equipment");
   }

   /**
    * Add all tour equipment actions
    *
    * @param menuMgr
    */
   public void fillEquipmentMenu(final IMenuManager menuMgr) {

      menuMgr.add(new Separator());
      {
         menuMgr.add(_actionAddEquipment);
//         menuMgr.add(_actionRemoveTag);
      }
   }
}
