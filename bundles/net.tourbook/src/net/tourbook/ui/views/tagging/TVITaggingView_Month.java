/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.TreeViewer;

public class TVITaggingView_Month extends TVITaggingView_Item {

   private final TVITaggingView_Year _yearItem;

   private final int                 _year;
   private final int                 _month;

   public TVITaggingView_Month(final TVITaggingView_Year parentItem,
                               final int dbYear,
                               final int dbMonth,
                               final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);

      _yearItem = parentItem;
      _year = dbYear;
      _month = dbMonth;
   }

   /**
    * Compare two instances of {@link TVITaggingView_Month}
    *
    * @param otherMonthItem
    *
    * @return
    */
   public int compareTo(final TVITaggingView_Month otherMonthItem) {

      if (this == otherMonthItem) {
         return 0;
      }

      if (_year < otherMonthItem._year) {

         return -1;

      } else if (_year > otherMonthItem._year) {

         return 1;

      } else {

         // same year, check month

         if (_month == otherMonthItem._month) {
            return 0;
         } else if (_month < otherMonthItem._month) {
            return -1;
         } else {
            return 1;
         }
      }
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

      final TVITaggingView_Month other = (TVITaggingView_Month) obj;

      if (_month != other._month) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_yearItem == null) {

         if (other._yearItem != null) {
            return false;
         }

      } else if (!_yearItem.equals(other._yearItem)) {

         return false;
      }

      return true;
   }

   @Override
   protected void fetchChildren() {

      TagLoader.loadValues(this, TagLoaderID.MONTH__TOURS);
   }

   public int getMonth() {
      return _month;
   }

   public TVITaggingView_Year getYearItem() {
      return _yearItem;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + _month;
      result = prime * result + _year;
      result = prime * result + ((_yearItem == null) ? 0 : _yearItem.hashCode());

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Month " + System.identityHashCode(this) + NL //     //$NON-NLS-1$

            + "  _year         = " + _year + NL //                            //$NON-NLS-1$
            + "  _month        = " + _month + NL //                           //$NON-NLS-1$
            + "  _yearItem     = " + _yearItem + NL //                        //$NON-NLS-1$

            + "  numTours          = " + numTours + NL //                     //$NON-NLS-1$

      ;
   }
}
