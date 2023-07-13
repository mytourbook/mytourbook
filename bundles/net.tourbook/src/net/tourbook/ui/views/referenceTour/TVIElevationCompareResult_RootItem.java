/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;

/**
 * Root item for compare results, the children are reference tours
 */
public class TVIElevationCompareResult_RootItem extends TVIElevationCompareResult_Item {

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final ArrayList<RefTourItem> allSelectedRefTourItems = ElevationCompareManager.getComparedReferenceTours();

      if (allSelectedRefTourItems == null) {
         return;
      }

      for (final RefTourItem refTourItem : allSelectedRefTourItems) {

         children.add(new TVIElevationCompareResult_ReferenceTour(
               this,
               refTourItem.label,
               refTourItem,
               refTourItem.tourId));
      }
   }

}
