/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionSetTourStructure_All extends Action implements IMenuCreator {

   private Menu _menu;

   private class ActionSetPartStructure extends Action {

      private short  __expandType;
      private String __expandName;

      private ActionSetPartStructure(final short expandType, final String expandName) {

         super(expandName, AS_CHECK_BOX);

         __expandType = expandType;
         __expandName = expandName;
      }

      @Override
      public void run() {

         // check if the tour editor contains a modified tour
         if (TourManager.isTourEditorModified()) {
            return;
         }

         if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),

               Messages.Equipment_Action_SetTourStructure_All_Dialog_Title,
               Messages.Equipment_Action_SetTourStructure_All_Dialog_Message.formatted(__expandName)) == false) {

            return;
         }

         final Runnable runnable = new Runnable() {

            private boolean _isModified;

            @Override
            public void run() {

               final EntityManager em = TourDatabase.getInstance().getEntityManager();
               try {

                  /*
                   * Update all parts which have not the current expand type
                   */

                  final Map<Long, Equipment> allEquipment = EquipmentManager.getAllEquipment_ByID();

                  for (final Equipment equipment : allEquipment.values()) {

                     final Set<EquipmentPart> allParts = equipment.getParts();

                     boolean isPartModified = false;

                     for (final EquipmentPart part : allParts) {

                        if (part.getExpandType() != __expandType) {

                           // set new expand type

                           part.setExpandType(__expandType);

                           isPartModified = true;
                           _isModified = true;
                        }
                     }

                     if (isPartModified == false) {

                        // check collated equipment

                        if (equipment.isCollate()) {

                           if (equipment.getExpandType() != __expandType) {

                              equipment.setExpandType(__expandType);

                              isPartModified = true;
                              _isModified = true;
                           }
                        }
                     }

                     if (isPartModified) {
                        TourDatabase.saveEntity(equipment, equipment.getEquipmentId(), Equipment.class);
                     }
                  }

               } catch (final Exception e) {

                  StatusUtil.log(e);

               } finally {

                  em.close();
               }

               if (_isModified) {
                  EquipmentManager.clearAllEquipmentResourcesAndFireModifyEvent();
               }
            }
         };

         BusyIndicator.showWhile(Display.getCurrent(), runnable);
      }
   }

   public ActionSetTourStructure_All() {

      super(Messages.Equipment_Action_SetTourStructure_All, AS_DROP_DOWN_MENU);

      setMenuCreator(this);
   }

   private void addActionToMenu(final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(_menu, -1);
   }

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
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

            final Menu menu = (Menu) e.widget;

            // dispose old items
            for (final MenuItem menuItem : menu.getItems()) {
               menuItem.dispose();
            }

            /*
             * Create all expand types
             */
            int typeIndex = 0;
            for (final short expandType : EquipmentManager.EXPAND_TYPES) {

               final ActionSetPartStructure actionTagStructure = new ActionSetPartStructure(
                     expandType,
                     EquipmentManager.EXPAND_TYPE_NAMES[typeIndex++]);

               addActionToMenu(actionTagStructure);
            }
         }
      });

      return _menu;
   }

}
