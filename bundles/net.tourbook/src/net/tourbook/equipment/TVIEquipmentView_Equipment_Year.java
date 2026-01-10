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
package net.tourbook.equipment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment_Year extends TVIEquipmentView_Item {

   private TVIEquipmentView_Equipment _equipmentItem;

   /**
    * Year category
    */
   private final int                  _year;

   /**
    * Is <code>true</code> when the children of this year item contains month items<br>
    * Is <code>false</code> when the children of this year item contains tour items
    */
   private boolean                    _isMonthCategory;

   public TVIEquipmentView_Equipment_Year(final TVIEquipmentView_Equipment tviEquipmentView_Equipment,
                                          final int year,
                                          final boolean isMonth,
                                          final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(tviEquipmentView_Equipment);

      _equipmentItem = tviEquipmentView_Equipment;
      _year = year;
      _isMonthCategory = isMonth;
   }

   /**
    * Compare two instances of {@link TVIEquipmentView_Equipment_Year}
    *
    * @param otherYearItem
    *
    * @return
    */
   public int compareTo(final TVIEquipmentView_Equipment_Year otherYearItem) {

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

      final TVIEquipmentView_Equipment_Year other = (TVIEquipmentView_Equipment_Year) obj;

      if (_isMonthCategory != other._isMonthCategory) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_equipmentItem == null) {
         if (other._equipmentItem != null) {
            return false;
         }
      } else if (!_equipmentItem.equals(other._equipmentItem)) {
         return false;
      }

      return true;
   }

   @Override
   protected void fetchChildren() {

      if (_isMonthCategory) {

         loadChildren_Months();

      } else {

         loadChildren_Tours();
      }
   }

   public long getEquipmentId() {
      return _equipmentItem.getEquipmentID();
   }

   public TVIEquipmentView_Equipment getEquipmentItem() {
      return _equipmentItem;
   }

   public int getYear() {
      return _year;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (_isMonthCategory ? 1231 : 1237);
      result = prime * result + ((_equipmentItem == null) ? 0 : _equipmentItem.hashCode());
      result = prime * result + _year;
      return result;
   }

   /**
    * Get all months for this year
    *
    * @param isMonth
    */
   private void loadChildren_Months() {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + "   TourData.StartYear," + NL //                                            1  //$NON-NLS-1$
               + "   TourData.StartMonth," + NL //                                           2  //$NON-NLS-1$

               + "   COUNT(*) AS num_Tours," + NL //                                         3  //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_SUM_COLUMNS_SUMMARIZED //                         4

               + "FROM equipmentpart AS part" + NL //                                           //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "   ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= part.\"DATE\"" + NL //                       //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateuntil" + NL //                      //$NON-NLS-1$
               + "   AND TourData.StartYear = ?" + NL //                                        //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "WHERE part.iscollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "   AND part.partid = ?" + NL //                                               //$NON-NLS-1$

               + "GROUP BY "
               + "   TourData.StartYear," + NL //                                               //$NON-NLS-1$
               + "   TourData.StartMonth" + NL //                                               //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         // parameter: 1
         statement.setLong(1, _year);

         // parameter: 2
         final int nextIndex = sqlFilter.setParameters(statement, 2);

         // parameter: next
         statement.setLong(nextIndex, _equipmentItem.getEquipmentID());

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int dbYear = result.getInt(1);
            final int dbMonth = result.getInt(2);
            final long numTours = result.getLong(3);

            final TVIEquipmentView_Equipment_Month monthItem = new TVIEquipmentView_Equipment_Month(

                  this,
                  _equipmentItem,
                  dbYear,
                  dbMonth,
                  getEquipmentViewer());

            allTourItems.add(monthItem);

            monthItem.numTours = numTours;

            monthItem.firstColumn = LocalDate.of(dbYear, dbMonth, 1).format(TimeTools.Formatter_Month);

            monthItem.readCommonValues(result, 4);

            if (UI.IS_SCRAMBLE_DATA) {
               monthItem.firstColumn = UI.scrambleText(monthItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);
      }

      setChildren(allTourItems);
   }

   private void loadChildren_Tours() {

      final ArrayList<TreeViewerItem> allChildren = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         /*
          * Load: Equipment, Part, Year, Tour
          */
         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM equipmentpart AS part" + NL //                                              //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                      //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //        //$NON-NLS-1$

               + "JOIN tourdata AS TourData" + NL //                                               //$NON-NLS-1$
               + "   ON TourData.tourID = j_td_eq.tourdata_tourID" + NL //                         //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= part.\"DATE\"" + NL //                          //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                         //$NON-NLS-1$
               + "   AND TourData.StartYear = ?" + NL //                                           //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "WHERE part.isCollate = TRUE" + NL //                                             //$NON-NLS-1$
               + "   AND part.partID = ?" + NL //                                                  //$NON-NLS-1$

               + "ORDER BY TourData.tourstarttime" + NL //                                         //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         // 1
         statement.setLong(1, _year);

         // 2
         final int nextIndex = sqlFilter.setParameters(statement, 2);

         // next
         statement.setLong(nextIndex, _equipmentItem.getEquipmentID());

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(this, _equipmentItem, getEquipmentViewer());

            allChildren.add(tourItem);

            tourItem.readColumnValues_Tour(result);

            if (UI.IS_SCRAMBLE_DATA) {
               tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         UI.showSQLException(e);
      }

      setChildren(allChildren);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Year " + System.identityHashCode(this) + NL //   //$NON-NLS-1$

            + " [" + NL //                                                 //$NON-NLS-1$

            + "  _year        = " + _year + NL //                          //$NON-NLS-1$
            + "  _isMonth     = " + _isMonthCategory + NL //                       //$NON-NLS-1$

            + NL
            + "  numTours          = " + numTours + NL //                  //$NON-NLS-1$
//            + "  numTags_NoTours   = " + numTags_NoTours + NL //           //$NON-NLS-1$

//          + NL
//          + "_tagItem  = " + _tagItem + NL //                            //$NON-NLS-1$

            + "]" + NL //                                                  //$NON-NLS-1$
      ;
   }
}
