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
package net.tourbook.ui.views.tourBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.database.TourDatabase;
import net.tourbook.equipment.EquipmentPartFilter;
import net.tourbook.ui.AppFilter;

public class TVITourBookYearCategorized extends TVITourBookItem {

   private TourBookViewLayout _viewLayout;

   public TVITourBookYearCategorized(final TourBookView view,
                                     final TVITourBookItem parentItem,
                                     final TourBookViewLayout itemType) {

      super(view);

      _viewLayout = itemType;

      setParentItem(parentItem);
   }

   /**
    * Fetch all tour data within a month/week category
    */
   @Override
   protected void fetchChildren() {

      /*
       * Set the children for the yearSub (month,week,...) item, these are tour items
       */
      String sumYear = UI.EMPTY_STRING;
      String sumYearSub = UI.EMPTY_STRING;

      if (_viewLayout == TourBookViewLayout.CATEGORY_WEEK) {

         // categorize by week

         sumYear = "startWeekYear"; //$NON-NLS-1$
         sumYearSub = "startWeek"; //$NON-NLS-1$

      } else {

         // categorize by month (default)

         sumYear = "startYear"; //$NON-NLS-1$
         sumYearSub = "startMonth"; //$NON-NLS-1$
      }

      final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);
      final SQLData partFilter = new EquipmentPartFilter().getSqlData();

      final String sql = NL

            + "SELECT" + NL //                                                                        //$NON-NLS-1$

            + TVITourBookItem.getSQL_ALL_TOUR_FIELDS("TourData", 3) + "," + NL //                     //$NON-NLS-1$ //$NON-NLS-2$

            + "	jTdataTtag.TourTag_tagId," + NL //                                                  //$NON-NLS-1$
            + "	Tmarker.markerId," + NL //                                                          //$NON-NLS-1$
            + "	TNutritionProduct.productId," + NL //                                               //$NON-NLS-1$
            + "	jTdataTequipment.Equipment_EquipmentID " + NL //                                    //$NON-NLS-1$

            + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                          //$NON-NLS-1$

            + partFilter.getSqlString()

            // get tag IDs
            + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" //             //$NON-NLS-1$
            + " ON tourID = jTdataTtag.TourData_tourId" + NL //                                       //$NON-NLS-1$

            // get marker IDs
            + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " Tmarker" //                           //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = Tmarker.TourData_tourId" + NL //                                 //$NON-NLS-1$

            // get nutrition product IDs
            + "LEFT JOIN " + TourDatabase.TABLE_TOUR_NUTRITION_PRODUCT + " TNutritionProduct" //      //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = TNutritionProduct.TourData_tourId" + NL //                       //$NON-NLS-1$

            // get equipment IDs
            + " LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " jTdataTequipment" //    //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = jTdataTequipment.TourData_tourId" + NL //                        //$NON-NLS-1$

            + "WHERE  " + sumYear + " = ?" + NL //                                                    //$NON-NLS-1$ //$NON-NLS-2$
            + "   AND " + sumYearSub + " = ?" + NL //                                                 //$NON-NLS-1$ //$NON-NLS-2$

            + appFilter.getWhereClause()

            + "ORDER BY TourStartTime" + NL; //                                                       //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = partFilter.setParameters(prepStmt, nextIndex);

         prepStmt.setInt(nextIndex++, tourYear);
         prepStmt.setInt(nextIndex++, tourYearSub);

         nextIndex = appFilter.setParameters(prepStmt, nextIndex);

         fetchTourItems(prepStmt);

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }
   }

   public TourBookViewLayout getCategory() {
      return _viewLayout;
   }

   @Override
   public String toString() {

      boolean isShortInfo = true;
      isShortInfo = isShortInfo == true;

      if (isShortInfo) {

         return "TVITourBookYearCategorized  tourYearSub = " + tourYearSub; //$NON-NLS-1$

      } else {

         return NL

               + "TVITourBookYearCategorized" + NL //             //$NON-NLS-1$

               + "[" + NL //                                      //$NON-NLS-1$

               + "tourYear    = " + tourYear + NL //              //$NON-NLS-1$
               + "tourYearSub = " + tourYearSub + NL //           //$NON-NLS-1$
               + "_viewLayout = " + _viewLayout + NL //           //$NON-NLS-1$

               + "]" + NL //                                      //$NON-NLS-1$
         ;
      }
   }
}
