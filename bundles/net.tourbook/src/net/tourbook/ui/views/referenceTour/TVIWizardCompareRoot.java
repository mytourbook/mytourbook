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

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVIWizardCompareRoot extends TVIWizardCompareItem {

   public TVIWizardCompareRoot(final boolean isUseAppFilter) {

      this.isUseAppFilter = isUseAppFilter;
   }

   @Override
   protected void fetchChildren() {

      /*
       * Set the children for the root item, these are year items
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // use fast app filter
         final SQLFilter appFilter = new SQLFilter(SQLFilter.ONLY_FAST_APP_FILTERS);

         String sqlWhere = UI.EMPTY_STRING;

         if (isUseAppFilter) {
            sqlWhere = " WHERE 1=1 " + appFilter.getWhereClause() + NL;//      //$NON-NLS-1$
         }

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                 //$NON-NLS-1$

               + " startYear " + NL //                            //$NON-NLS-1$

               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //  //$NON-NLS-1$
               + sqlWhere
               + " GROUP BY startYear" + NL //                    //$NON-NLS-1$
               + " ORDER BY startYear" + NL //                    //$NON-NLS-1$
         ;

         final PreparedStatement stmt = conn.prepareStatement(sql);

         // app filter parameters
         if (isUseAppFilter) {
            appFilter.setParameters(stmt, 1);
         }

         final ResultSet result = stmt.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);

            final TVIWizardCompareYear yearItem = new TVIWizardCompareYear(this, isUseAppFilter);
            children.add(yearItem);

            yearItem.treeColumn = Integer.toString(dbYear);
            yearItem.tourYear = dbYear;
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }
}
