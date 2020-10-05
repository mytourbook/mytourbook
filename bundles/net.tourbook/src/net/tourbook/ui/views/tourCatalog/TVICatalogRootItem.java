/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

public class TVICatalogRootItem extends TVICatalogItem {

   private ITourViewer _tourViewer;

   public TVICatalogRootItem(final ITourViewer tourViewer) {
      super();
      _tourViewer = tourViewer;
   }

   @Override
   protected void fetchChildren() {

      /*
       * set the children for the root item, these are reference tours
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final String sql = UI.EMPTY_STRING
            //
            + "SELECT" //$NON-NLS-1$

            + " label," //$NON-NLS-1$
            + " refId," //$NON-NLS-1$
            + " TourData_tourId," //$NON-NLS-1$

            // get number of compared tours
            + "(	select sum(1)" //$NON-NLS-1$
            + ("		from " + TourDatabase.TABLE_TOUR_COMPARED) //$NON-NLS-1$
            + ("		where " + TourDatabase.TABLE_TOUR_COMPARED + ".reftourid=" + TourDatabase.TABLE_TOUR_REFERENCE + ".refid") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + UI.SYMBOL_BRACKET_RIGHT

            + (" FROM " + TourDatabase.TABLE_TOUR_REFERENCE) //$NON-NLS-1$
            + (" ORDER BY label"); //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final TVICatalogRefTourItem refItem = new TVICatalogRefTourItem(this);
            children.add(refItem);

            refItem.label = result.getString(1);
            refItem.refId = result.getLong(2);
            refItem.setTourId(result.getLong(3));
            refItem.tourCounter = result.getInt(4);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   public ITourViewer getRootTourViewer() {
      return _tourViewer;
   }
}
