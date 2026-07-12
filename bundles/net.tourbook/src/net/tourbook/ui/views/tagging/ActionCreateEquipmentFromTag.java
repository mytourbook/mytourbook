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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
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

   private class ActionEquipmentWithParts extends Action {

      private final Equipment __equipment;

      public ActionEquipmentWithParts(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         final Image eqImage = EquipmentManager.getEquipmentImage(equipment);

         if (eqImage != null) {
            setImageDescriptor(ImageDescriptor.createFromImage(eqImage));
         }

         __equipment = equipment;
      }

      @Override
      public void run() {

         onCreateEquipmentPart(__equipment);
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

   /**
    * Tag and equipment expand types are differently, this has historical reasons
    *
    * @param tourTag
    *
    * @return
    */
   private int convertExpandType(final TourTag tourTag) {

      /**
       * When a part is expanded in the equipment viewer, the tours can be displayed in different
       * structures
       * <p>
       * <li>0 ... EXPAND_TYPE_FLAT</li>
       * <li>1 ... EXPAND_TYPE_YEAR_TOUR</li>
       * <li>2 ... EXPAND_TYPE_YEAR_MONTH_TOUR</li>
       */
//      private int     expandType       = EquipmentManager.EXPAND_TYPE_FLAT;

      /**
       * When a tag is expanded in the tag tree viewer, the tours can be displayed in different
       * structures
       * <p>
       * <li>1 ... {@link #EXPAND_TYPE__TAG_TOURS}</li>
       * <li>2 ... {@link #EXPAND_TYPE__TAG_YEAR_TOURS}</li>
       * <li>0 ... {@link #EXPAND_TYPE__TAG_YEAR_MONTH_TOURS}</li>
       */
//      private int     expandType = EXPAND_TYPE__TAG_TOURS;

// SET_FORMATTING_OFF

      switch (tourTag.getExpandType()) {

      //                                              2 -> 1
      case TourTag.EXPAND_TYPE__TAG_YEAR_TOURS:          return EquipmentManager.EXPAND_TYPE_YEAR_TOUR;

      //                                              0 -> 2
      case TourTag.EXPAND_TYPE__TAG_YEAR_MONTH_TOURS:    return EquipmentManager.EXPAND_TYPE_YEAR_MONTH_TOUR;

      //                                              1 -> 0
      case TourTag.EXPAND_TYPE__TAG_TOURS:
      default:                                           return EquipmentManager.EXPAND_TYPE_FLAT;
      }

// SET_FORMATTING_ON
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

         final ActionEquipmentWithParts equipmentAction = new ActionEquipmentWithParts(availableEquipment);

         addActionToMenu(menu, equipmentAction);
      }
   }

   private void onCreateEquipment() {

      final IStructuredSelection structuredSelection = _taggingView.getViewer().getStructuredSelection();

      boolean isEquipmentCreated = false;

      for (final Object selection : structuredSelection) {

         if (selection instanceof final TVITaggingView_Tag tagItem) {

            final TourTag tourTag = tagItem.getTourTag();

            final LocalDateTime now = LocalDateTime.now();
            final String collateID = "From tag - %s".formatted(TimeTools.Formatter_DateTime_SM.format(now));

            // convert expand type
            final int eqExpandType = convertExpandType(tourTag);

            final Equipment newEquipment = new Equipment();

// SET_FORMATTING_OFF

            newEquipment.setBrand            (tourTag.getTagName());
            newEquipment.setDescription      (tourTag.getNotes());
            newEquipment.setImageFilePath    (tourTag.getImageFilePath());

            newEquipment.setModel            (collateID);

            newEquipment.setIsCollate        (true);
            newEquipment.setCollateID        (collateID);

            newEquipment.setExpandType       (eqExpandType);

// SET_FORMATTING_ON

            // save new equipment
            final Equipment savedEquipment = TourDatabase.saveEntity(newEquipment, newEquipment.getEquipmentId(), Equipment.class);

            // set equipment into all tours with the same tag
            final long firstUseDate = EquipmentManager.setEquipmentFromTag(savedEquipment, tourTag);

            // set equipment first use date which is the oldest tour which contained the tag
            savedEquipment.setDateUsed(firstUseDate);
            savedEquipment.setDateBuilt(firstUseDate);

            // resave equipment
            TourDatabase.saveEntity(savedEquipment, savedEquipment.getEquipmentId(), Equipment.class);

            // update equipment from/until collate dates
            final Set<String> allModifiedCollateIDs = new HashSet<>(Arrays.asList(collateID));
            EquipmentManager.updateUntilDate_Equipment(allModifiedCollateIDs);

            isEquipmentCreated = true;
         }
      }

      if (isEquipmentCreated) {
         updateUI();
      }
   }

   private void onCreateEquipmentPart(final Equipment equipment) {

      final IStructuredSelection structuredSelection = _taggingView.getViewer().getStructuredSelection();

      boolean isPartCreated = false;

      for (final Object selection : structuredSelection) {

         if (selection instanceof final TVITaggingView_Tag tagItem) {

            final TourTag tourTag = tagItem.getTourTag();

            final LocalDateTime now = LocalDateTime.now();
            final String collateID = "From tag - %s".formatted(TimeTools.Formatter_DateTime_SM.format(now));

            // convert expand type
            final int partExpandType = convertExpandType(tourTag);

            final EquipmentPart newPart = new EquipmentPart();

// SET_FORMATTING_OFF

            newPart.setEquipment       (equipment);

            newPart.setBrand           (tourTag.getTagName());
            newPart.setDescription     (tourTag.getNotes());
            newPart.setImageFilePath   (tourTag.getImageFilePath());

            newPart.setModel           (collateID);

            newPart.setIsCollate       (true);
            newPart.setPartCollateID   (collateID);
            newPart.setCollateBetween  (EquipmentPart.COLLATED_WITH_NEXT);

            newPart.setExpandType      ((short) partExpandType);

// SET_FORMATTING_ON

            // save new part
            final EquipmentPart savedPart = TourDatabase.saveEntity(newPart, newPart.getPartId(), EquipmentPart.class);

            // set equipment into all tours with the same tag
            final long firstUseDate = EquipmentManager.setEquipmentFromTag(equipment, tourTag);

            // set equipment first use date which is the oldest tour which contained the tag
            savedPart.setDateUsed(firstUseDate);
            savedPart.setDateBuilt(firstUseDate);

            // resave part
            TourDatabase.saveEntity(savedPart, savedPart.getPartId(), EquipmentPart.class);

            /*
             * Add part to the equipment to correctly update the from/until dates, but this
             * equipment is not saved, it is reloaded when the UI is updated
             */
            equipment.getParts().add(savedPart);

            final HashSet<String> allCollateIDs = new HashSet<>(Arrays.asList(collateID));

            // update part from/until collate dates
            EquipmentManager.updateUntilDate_Parts(equipment, allCollateIDs, savedPart.getCollateBetween());

            isPartCreated = true;
         }
      }

      if (isPartCreated) {
         updateUI();
      }
   }

   /**
    * Clear caches and update UI
    */
   private void updateUI() {

      // remove old equipment from cached tours
      EquipmentManager.clearCachedValues();

      // this MUST be called after clearCachedValues()
      EquipmentMenuManager.updateRecentEquipment();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
   }
}
