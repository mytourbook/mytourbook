/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import java.util.Objects;

import net.tourbook.common.util.TreeViewerItem;

/**
 * TTI (TreeViewerItem) is used in the tree viewer {@link ReferenceTourView}, it contains tree items
 * for reference tours
 */
public class TVIRefTour_YearItem extends TVIRefTour_Item {

   long refId;
   int  year;

   /**
    * Number of tours
    */
   int  numTours;

   /**
    * @param parentItem
    */
   public TVIRefTour_YearItem(final TreeViewerItem parentItem) {
      this.setParentItem(parentItem);
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final TVIRefTour_YearItem other = (TVIRefTour_YearItem) obj;

      return refId == other.refId && year == other.year;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final TreeViewerItem parentItem = getParentItem();
      if (parentItem instanceof TVIRefTour_RefTourItem) {

         ((TVIRefTour_RefTourItem) parentItem).fetchChildren_WithoutSubCategories(this, children, year);
      }
   }

   TVIRefTour_RefTourItem getRefItem() {
      return (TVIRefTour_RefTourItem) getParentItem();
   }

   @Override
   public int hashCode() {

      return Objects.hash(refId, year);
   }

   void remove() {

      // remove all children
      getUnfetchedChildren().clear();

      // remove this tour item from the parent
      getParentItem().getUnfetchedChildren().remove(this);
   }

}
