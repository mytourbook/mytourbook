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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class TVIWizardCompareMonth extends TVIWizardCompareItem {

   int tourYear;
   int tourMonth;

   TVIWizardCompareMonth(final TVIWizardCompareItem parentItem, final boolean isUseAppFilter) {

      setParentItem(parentItem);

      this.isUseAppFilter = isUseAppFilter;
   }

   @Override
   protected void fetchChildren() {

      /*
       * Set the children for the month item, these are tour items
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      // use fast app filter
      final SQLFilter appFilter = new SQLFilter(SQLFilter.ONLY_FAST_APP_FILTERS);

      String sqlWhere = UI.EMPTY_STRING;

      if (isUseAppFilter) {
         sqlWhere = UI.SPACE + appFilter.getWhereClause() + NL; //
      }

      final String sql = UI.EMPTY_STRING + NL

            + "SELECT" + NL //                                       //$NON-NLS-1$

            + " tourId," + NL //                                  1  //$NON-NLS-1$
            + " startYear," + NL //                               2  //$NON-NLS-1$
            + " startMonth," + NL //                              3  //$NON-NLS-1$
            + " startDay," + NL //                                4  //$NON-NLS-1$
            + " tourType_typeId," + NL //                         5  //$NON-NLS-1$
            + " tourDistance," + NL //                            6  //$NON-NLS-1$
            + " tourDeviceTime_Elapsed," + NL //                  7  //$NON-NLS-1$
            + " tourAltUp" + NL //                                8  //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //        //$NON-NLS-1$

            + " WHERE startYear=? AND startMonth=?" + NL //          //$NON-NLS-1$
            + sqlWhere

            + " ORDER BY startDay, startHour, startMinute" + NL //   //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement stmt = conn.prepareStatement(sql);
         stmt.setInt(1, tourYear);
         stmt.setInt(2, tourMonth);

         // app filter parameters
         if (isUseAppFilter) {
            appFilter.setParameters(stmt, 3);
         }

         final ResultSet result = stmt.executeQuery();
         while (result.next()) {

            // new tour is in the resultset
            final TVIWizardCompareTour tourItem = new TVIWizardCompareTour(this);

            children.add(tourItem);

            tourItem.tourId = result.getLong(1);

            final int dbYear = result.getInt(2);
            final int dbMonth = result.getInt(3);
            final int dbDay = result.getInt(4);
            tourItem.treeColumn = Integer.toString(dbDay);

            tourItem.tourYear = dbYear;
            tourItem.tourMonth = dbMonth;
            tourItem.tourDay = dbDay;

            final Object tourTypeId = result.getObject(5);

            tourItem.tourTypeId = (tourTypeId == null
                  ? TourDatabase.ENTITY_IS_NOT_SAVED
                  : (Long) tourTypeId);

            tourItem.colDistance = result.getLong(6);
            tourItem.colElapsedTime = result.getLong(7);
            tourItem.colAltitudeUp = result.getLong(8);
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   @Override
   public String toString() {

      return "TVIWizardCompareMonth\n[\n" //$NON-NLS-1$

            + "tourYear=" + tourYear + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "tourMonth=" + tourMonth + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "]\n"; //$NON-NLS-1$
   }
}
