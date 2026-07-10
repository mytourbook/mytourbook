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
package net.tourbook.ui.views.tagging;

import java.time.LocalDateTime;
import java.util.List;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.equipment.EquipmentMenuManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 * Submenu for exporting tours
 */
public class ActionCreateEquipmentFromTag extends SubMenu {

   private TaggingView               _taggingView;

   private ActionCreateEquipment     _actionCreateEquipment;
   private ActionCreateEquipmentPart _actionCreateEquipmentPart;

   private class ActionCreateEquipment extends Action {

      public ActionCreateEquipment() {

         super("&Equipment");
      }

      @Override
      public void run() {

         onCreateEquipment();
      }

   }

   private class ActionCreateEquipmentPart extends SubMenu {

      public ActionCreateEquipmentPart() {

         super("Equipment &Part for", AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {

      }

      @Override
      public void fillMenu(final Menu menu) {

         fillMenu_10_EquipmentActions(menu);
      }
   }

   private class ActionEquipment extends Action {

      private final Equipment __equipment;

      public ActionEquipment(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         final Image eqImage = EquipmentManager.getEquipmentImage(equipment);

         if (eqImage != null) {
            setImageDescriptor(ImageDescriptor.createFromImage(eqImage));
         }

         __equipment = equipment;
      }

      @Override
      public void run() {

      }
   }

   /**
    * @param taggingView
    * @param isAddMode
    * @param isSaveTour
    *           when <code>true</code> the tour will be saved and a {@link TourManager#TOUR_CHANGED}
    *           event is fired, otherwise the {@link TourData} from the tour provider is only
    *           updated
    */
   public ActionCreateEquipmentFromTag(final TaggingView taggingView) {

      super(UI.IS_NOT_INITIALIZED, AS_DROP_DOWN_MENU);

      _taggingView = taggingView;

      setText("&Create Equipment from Tag");

      createActions();
   }

   private void createActions() {

      _actionCreateEquipment = new ActionCreateEquipment();
      _actionCreateEquipmentPart = new ActionCreateEquipmentPart();
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      addActionToMenu(_actionCreateEquipment);
      addActionToMenu(_actionCreateEquipmentPart);
   }

   private void fillMenu_10_EquipmentActions(final Menu menu) {

      final List<Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_Name();

      // Preload the equipment images
      // Note that the hourglass is only displayed on Windows (it doesn't seem
      // to work on Linux)
      BusyIndicator.showWhile(Display.getCurrent(),
            () -> allAvailableEquipment
                  .forEach(equipment -> EquipmentManager.getEquipmentImage(equipment)));

      for (final Equipment availableEquipment : allAvailableEquipment) {

         if (availableEquipment.isRetired()) {

            // skip retired equipment https://github.com/mytourbook/mytourbook/issues/1660
            continue;
         }

         if (availableEquipment.canCollate()) {

            // parts can be created only for not collated equipment

            continue;
         }

         final ActionEquipment equipmentAction = new ActionEquipment(availableEquipment);

         addActionToMenu(menu, equipmentAction);
      }
   }

   private void onCreateEquipment() {

      final IStructuredSelection structuredSelection = _taggingView.getViewer().getStructuredSelection();

      for (final Object selection : structuredSelection) {

         if (selection instanceof final TVITaggingView_Tag tagItem) {

            final TourTag tourTag = tagItem.getTourTag();

            final LocalDateTime now = LocalDateTime.now();

            final Equipment equipment = new Equipment();

// SET_FORMATTING_OFF

            equipment.setBrand         (tourTag.getTagName());
            equipment.setDescription   (tourTag.getNotes());
            equipment.setImageFilePath (tourTag.getImageFilePath());

            equipment.setModel         ("From tag - " + TimeTools.Formatter_DateTime_SM.format(now));

// SET_FORMATTING_ON

            TourDatabase.saveEntity(equipment, equipment.getEquipmentId(), Equipment.class);

            /*
             * Clear caches and update UI
             */

            // remove old equipment from cached tours
            EquipmentManager.clearCachedValues();

            // this MUST be called after clearCachedValues()
            EquipmentMenuManager.updateRecentEquipment();

            TourManager.getInstance().clearTourDataCache();

            // fire modify event
            TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
         }
      }
   }
}
