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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;

public class TVIRefTour_RootItem extends TVIRefTour_Item {

   private int _viewLayout;

   public TVIRefTour_RootItem() {

      super();

      _viewLayout = ElevationCompareManager.getReferenceTour_ViewLayout();
   }

   @Override
   protected void fetchChildren() {

      /*
       * Set the children for the root item, these are reference tours
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      setChildren(children);

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                                            //$NON-NLS-1$

            + " TourReference.label," + NL //                                          1  //$NON-NLS-1$
            + " TourReference.refId," + NL //                                          2  //$NON-NLS-1$
            + " TourReference.TourData_tourId," + NL //                                3  //$NON-NLS-1$

            + " TourData.hasGeoData," + NL //                                          4  //$NON-NLS-1$
            + " TourData.tourType_typeId," + NL //                                     5  //$NON-NLS-1$

            // get number of compared tours
            + "(" + NL //                                                              6  //$NON-NLS-1$
            + "   SELECT SUM(1)" + NL //                                                  //$NON-NLS-1$
            + "      FROM " + TourDatabase.TABLE_TOUR_COMPARED + NL //                    //$NON-NLS-1$
            + "      WHERE " + TourDatabase.TABLE_TOUR_COMPARED + ".reftourid=" + TourDatabase.TABLE_TOUR_REFERENCE + ".refid" + NL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + ")" + NL //                                                                 //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_REFERENCE + " TourReference" + NL //     //$NON-NLS-1$

            // get data for a tour
            + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData " + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourReference.TourData_tourId = TourData.tourId" + NL //               //$NON-NLS-1$

            + " ORDER BY label" + NL; //                                                  //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection();
            PreparedStatement statement = conn.prepareStatement(sql);) {

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final TVIRefTour_RefTourItem refItem = new TVIRefTour_RefTourItem(this, _viewLayout);

            children.add(refItem);

            /*
             * From TourReference
             */
            refItem.label = result.getString(1);
            refItem.refId = result.getLong(2);
            refItem.setTourId(result.getLong(3));

            /*
             * From TourData
             */
            refItem.hasGeoData = result.getBoolean(4);
            final Object tourTypeId = result.getObject(5);

            refItem.numTours = result.getInt(6);

            // tour type
            refItem.tourTypeId = tourTypeId == null
                  ? TourDatabase.ENTITY_IS_NOT_SAVED
                  : (Long) tourTypeId;

         }

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);
      }
   }
}
