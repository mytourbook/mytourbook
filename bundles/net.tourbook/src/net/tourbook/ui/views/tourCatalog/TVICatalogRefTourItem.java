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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 * Contains a reference tours item
 */
public class TVICatalogRefTourItem extends TVICatalogTourItem {

   String label;
   long   refId;

   float  yearMapMinValue = Float.MIN_VALUE;
   float  yearMapMaxValue;

   int    tourCounter;

   public TVICatalogRefTourItem(final TVICatalogRootItem parentItem) {

      setParentItem(parentItem);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TVICatalogRefTourItem)) {
         return false;
      }
      final TVICatalogRefTourItem other = (TVICatalogRefTourItem) obj;
      if (refId != other.refId) {
         return false;
      }
      return true;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      /**
       * Derby does not support expression in "GROUP BY" statements, this is a workaround found
       * here: http://mail-archives.apache.org/mod_mbox/db-derby-dev/200605.mbox/%3C7415300
       * .1147889647479.JavaMail.jira@brutus%3E <br>
       * <code>
       *    String subSQLString = "(SELECT YEAR(tourDate)\n"
       *       + ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + "\n")
       *       + (" WHERE "
       *             + TourDatabase.TABLE_TOUR_REFERENCE
       *             + "_generatedId="
       *             + refId + "\n")
       *       + ")";
       *
       *    String sqlString = "SELECT years FROM \n"
       *       + subSQLString
       *       + (" REFYEARS(years) GROUP BY years");
       * </code>
       */

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                       //$NON-NLS-1$

            + " StartYear," + NL //                                  //$NON-NLS-1$
            + " SUM(1)" + NL //                                      //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_COMPARED + NL //    //$NON-NLS-1$
            + " WHERE refTourId=?" + NL //                           //$NON-NLS-1$
            + " GROUP BY startYear" + NL //                          //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, refId);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final TVICatalogYearItem yearItem = new TVICatalogYearItem(this);
            children.add(yearItem);

            yearItem.refId = refId;
            yearItem.year = result.getInt(1);
            yearItem.tourCounter = result.getInt(2);

            tourCounter = result.getInt(2);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (refId ^ (refId >>> 32));
      return result;
   }

   void remove() {

      final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();

      // remove all children
      if (unfetchedChildren != null) {
         unfetchedChildren.clear();
      }

      // remove this ref item from the parent item
      final ArrayList<TreeViewerItem> unfetchedParentChildren = getParentItem().getUnfetchedChildren();
      if (unfetchedParentChildren != null) {
         unfetchedParentChildren.remove(this);
      }
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVICatalogRefTourItem" + NL //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

            + "label             = " + label + NL //$NON-NLS-1$
            + "refId             = " + refId + NL //$NON-NLS-1$
            + "yearMapMinValue   = " + yearMapMinValue + NL //$NON-NLS-1$
            + "yearMapMaxValue   = " + yearMapMaxValue + NL //$NON-NLS-1$
            + "tourCounter       = " + tourCounter + NL //$NON-NLS-1$

            + "]" + NL //$NON-NLS-1$

      ;
   }

}
