/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment_Month extends TVIEquipmentView_Item {

   private final TVIEquipmentView_Equipment_Year _yearItem;

   private TVIEquipmentView_Equipment            _equipmentItem;

   private final int                             _year;
   private final int                             _month;

   public TVIEquipmentView_Equipment_Month(final TVIEquipmentView_Equipment_Year parentItem,
                                           final TVIEquipmentView_Equipment partItem,
                                           final int dbYear,
                                           final int dbMonth,
                                           final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);
      _equipmentItem = partItem;

      _yearItem = parentItem;
      _year = dbYear;
      _month = dbMonth;
   }

   /**
    * Compare two instances of {@link TVIEquipmentView_Equipment_Month}
    *
    * @param otherMonthItem
    *
    * @return
    */
   public int compareTo(final TVIEquipmentView_Equipment_Month otherMonthItem) {

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

      final TVIEquipmentView_Equipment_Month other = (TVIEquipmentView_Equipment_Month) obj;

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

      loadChildren_Tours();
   }

   public int getMonth() {
      return _month;
   }

   public TVIEquipmentView_Equipment getPartItem() {
      return _equipmentItem;
   }

   public TVIEquipmentView_Equipment_Year getYearItem() {
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
               + "   AND TourData.tourstarttime >= part.dateFrom" + NL //                          //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                         //$NON-NLS-1$
               + "   AND TourData.StartYear = ?" + NL //                                           //$NON-NLS-1$
               + "   AND TourData.StartMonth = ?" + NL //                                          //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "WHERE part.isCollate = TRUE" + NL //                                             //$NON-NLS-1$
               + "   AND part.partID = ?" + NL //                                                  //$NON-NLS-1$

               + "ORDER BY TourData.tourstarttime" + NL //                                         //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         // parameter: 1
         statement.setLong(1, _year);
         statement.setLong(2, _month);

         // parameter: 3
         final int nextIndex = sqlFilter.setParameters(statement, 3);

         // parameter: next
         statement.setLong(nextIndex, _yearItem.getEquipmentId());

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

            + "TVITagView_Month " + System.identityHashCode(this) + NL //     //$NON-NLS-1$

            + "[" + NL //                                                     //$NON-NLS-1$

            + "  _year         = " + _year + NL //                            //$NON-NLS-1$
            + "  _month        = " + _month + NL //                           //$NON-NLS-1$
            + "  _yearItem     = " + _yearItem + NL //                        //$NON-NLS-1$

            + "  numTours          = " + numTours + NL //                     //$NON-NLS-1$
//            + "  numTags_NoTours   = " + numTags_NoTours + NL //              //$NON-NLS-1$

            + "]" + NL //                                                     //$NON-NLS-1$
      ;
   }
}
