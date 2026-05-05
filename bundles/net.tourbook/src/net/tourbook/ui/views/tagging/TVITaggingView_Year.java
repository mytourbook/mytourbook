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

public class TVITaggingView_Year extends TVITaggingView_Item {

   private final int          _year;

   private TVITaggingView_Tag _tagItem;

   /**
    * <code>true</code> when the children of this year item contains month items<br>
    * <code>false</code> when the children of this year item contains tour items
    */
   private boolean            _isMonth;

   public TVITaggingView_Year(final TVITaggingView_Tag parentItem,
                              final int year,
                              final boolean isMonth,
                              final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);

      _tagItem = parentItem;
      _year = year;
      _isMonth = isMonth;
   }

   /**
    * Compare two instances of {@link TVITaggingView_Year}
    *
    * @param otherYearItem
    *
    * @return
    */
   public int compareTo(final TVITaggingView_Year otherYearItem) {

      if (this == otherYearItem) {
         return 0;
      }

      if (_year == otherYearItem._year) {
         return 0;
      } else if (_year < otherYearItem._year) {
         return -1;
      } else {
         return 1;
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

      final TVITaggingView_Year other = (TVITaggingView_Year) obj;

      if (_isMonth != other._isMonth) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_tagItem == null) {
         if (other._tagItem != null) {
            return false;
         }
      } else if (!_tagItem.equals(other._tagItem)) {
         return false;
      }

      return true;
   }

   @Override
   protected void fetchChildren() {

      if (_isMonth) {

         TagLoader.loadValues(this, TagLoaderID.YEAR__MONTHS);

      } else {

         updateNumLoadedItems_Increment();

         TagLoader.loadValues(this, TagLoaderID.YEAR__TOURS);
      }
   }

   public long getTagId() {
      return _tagItem.getTagId();
   }

   public TVITaggingView_Tag getTagItem() {
      return _tagItem;
   }

   public int getYear() {
      return _year;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (_isMonth ? 1231 : 1237);
      result = prime * result + ((_tagItem == null) ? 0 : _tagItem.hashCode());
      result = prime * result + _year;
      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Year " + System.identityHashCode(this) + NL //   //$NON-NLS-1$

            + "  _year        = " + _year + NL //                          //$NON-NLS-1$
            + "  _isMonth     = " + _isMonth + NL //                       //$NON-NLS-1$

            + NL
            + "  numTours          = " + numTours + NL //                  //$NON-NLS-1$
      ;
   }

}
