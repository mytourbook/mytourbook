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

import net.tourbook.common.util.TreeViewerItem;

import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIEquipmentView_Item extends TreeViewerItem {

   private TreeViewer _equipmentViewer;

   /**
    * Content which is displayed in the first tree column
    */
   String             firstColumn;

   long               numTours;
   int                numTags_NoTours;

   /*
    * These are common values for equipment, part and service
    */
   float  price;
   String priceUnit;

   public TVIEquipmentView_Item(final TreeViewer equipmentViewer) {

      _equipmentViewer = equipmentViewer;
   }

   /**
    * @return Each equipment viewer item has access to its viewer
    */
   public TreeViewer getEquipmentViewer() {

      return _equipmentViewer;
   }

}
