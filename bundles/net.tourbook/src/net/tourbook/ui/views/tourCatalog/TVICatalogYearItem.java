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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;
import java.util.Objects;

import net.tourbook.common.util.TreeViewerItem;

/**
 * TTI (TreeViewerItem) is used in the tree viewer {@link TourCatalogView}, it contains tree items
 * for reference tours
 */
public class TVICatalogYearItem extends TVICatalogItem {

   long refId;
   int  year;

   /**
    * Number of tours
    */
   int  numTours;

   /**
    * @param parentItem
    * @param refId
    * @param year
    */
   public TVICatalogYearItem(final TreeViewerItem parentItem) {
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

      final TVICatalogYearItem other = (TVICatalogYearItem) obj;

      return refId == other.refId && year == other.year;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final TreeViewerItem parentItem = getParentItem();
      if (parentItem instanceof TVICatalogRefTourItem) {

         ((TVICatalogRefTourItem) parentItem).fetchComparedTours(this, children, year);
      }
   }

   TVICatalogRefTourItem getRefItem() {
      return (TVICatalogRefTourItem) getParentItem();
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
