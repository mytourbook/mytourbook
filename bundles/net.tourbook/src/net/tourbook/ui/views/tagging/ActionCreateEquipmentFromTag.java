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
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.time.TimeTools;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.equipment.EquipmentMenuManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Submenu for exporting tours
 */
public class ActionCreateEquipmentFromTag extends Action {

   private TaggingView _taggingView;

   /**
    * @param taggingView
    */
   public ActionCreateEquipmentFromTag(final TaggingView taggingView) {

      super(Messages.Action_Tag_CreateEquipmentFromTag, AS_PUSH_BUTTON);

      _taggingView = taggingView;
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

   private void createEquipment() {

      final IStructuredSelection structuredSelection = _taggingView.getViewer().getStructuredSelection();

      boolean isEquipmentCreated = false;

      for (final Object selection : structuredSelection) {

         if (selection instanceof final TVITaggingView_Tag tagItem) {

            final TourTag tourTag = tagItem.getTourTag();

            final LocalDateTime now = LocalDateTime.now();
            final String collateID = Messages.Equipment_Info_FromTag.formatted(TimeTools.Formatter_DateTime_SM.format(now));

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

   @Override
   public void run() {

      createEquipment();
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
