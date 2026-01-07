/*******************************************************************************
 * Copyright (C) 2025, 2025 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment extends TVIEquipmentView_Item {

   private Equipment _equipment;

   private long      _equipmentID;

   public TVIEquipmentView_Equipment(final TreeViewer equipViewer, final Equipment equipment) {

      super(equipViewer);

      _equipment = equipment;
      _equipmentID = equipment.getEquipmentId();

      firstColumn = equipment.getName();

      type = equipment.getType();
      date = equipment.getDate_Local();

      price = equipment.getPrice();
      priceUnit = equipment.getPriceUnit();

      if (UI.IS_SCRAMBLE_DATA) {
         firstColumn = UI.scrambleText(firstColumn);
      }
   }

   @Override
   protected void fetchChildren() {

      setChildren(readChildren());
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   public long getEquipmentID() {
      return _equipmentID;
   }

   private ArrayList<TreeViewerItem> readChildren() {

      final Set<EquipmentPart> allParts = _equipment.getParts();

      final ArrayList<TreeViewerItem> allPartItems = new ArrayList<>();

      for (final EquipmentPart part : allParts) {

         long durationMS = part.getDuration();
         String durationLastText = UI.EMPTY_STRING;

         if (part.getDateUntil() == TimeTools.MAX_TIME_IN_EPOCH_MILLI) {

            // this is the last collated part

            durationMS = TimeTools.nowInMilliseconds() - part.getDate();
            durationLastText = "Until now : ";
         }

         final TVIEquipmentView_Part partItem = new TVIEquipmentView_Part(this, part, getEquipmentViewer());

// SET_FORMATTING_OFF

         partItem.firstColumn          = part.getName();

         partItem.type                 = part.getType();
         partItem.date                 = part.getDate_Local();

         partItem.price                = part.getPrice();
         partItem.priceUnit            = part.getPriceUnit();

         partItem.usageDuration        = durationMS;
         partItem.usageDurationLast    = durationLastText;

// SET_FORMATTING_ON

         allPartItems.add(partItem);

         loadSummarizedValues_Part(partItem);
      }

      return allPartItems;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Equipment" + NL //       //$NON-NLS-1$

            + " _equipment = " + _equipment + NL //      //$NON-NLS-1$
      ;
   }

}
