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
import java.util.HashSet;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment_Month extends TVIEquipmentView_Item {

   private final TVIEquipmentView_Equipment_Year _yearItem;

   private TVIEquipmentView_Equipment            _equipmentItem;

   private final int                             _year;
   private final int                             _month;

   public TVIEquipmentView_Equipment_Month(final TVIEquipmentView_Equipment_Year parentItem,
                                           final TVIEquipmentView_Equipment equipmentItem,
                                           final int dbYear,
                                           final int dbMonth,
                                           final TreeViewer treeViewer,
                                           final EquipmentViewerType equipmentType) {

      super(treeViewer, equipmentType);

      setParentItem(parentItem);
      _equipmentItem = equipmentItem;

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

   public TVIEquipmentView_Equipment getEquipmentItem() {
      return _equipmentItem;
   }

   public int getMonth() {
      return _month;
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

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();

         /*
          * Load: Equipment, Year, Month, Tour
          */
         final String sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "----------------------------" + NL //                                            //$NON-NLS-1$
               + "-- equipment - month - tours" + NL //                                            //$NON-NLS-1$
               + "----------------------------" + NL //                                            //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM " + TourDatabase.TABLE_EQUIPMENT + " AS equipment" + NL //                  //$NON-NLS-1$ //$NON-NLS-2$

               + "JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_td_eq" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON j_td_eq.equipment_equipmentid = equipment.equipmentid" + NL //             //$NON-NLS-1$

               + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON TourData.tourID = j_td_eq.tourdata_tourID" + NL //                         //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= equipment.dateFrom" + NL //                     //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  equipment.dateUntil" + NL //                    //$NON-NLS-1$
               + "   AND TourData.StartYear = ?" + NL //                                           //$NON-NLS-1$
               + "   AND TourData.StartMonth = ?" + NL //                                          //$NON-NLS-1$

               + appFilter.getWhereClause()

               // get all equipment id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                             //$NON-NLS-1$

               // get tag id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" //    //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                       //$NON-NLS-1$

               // get marker id's
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                  //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                          //$NON-NLS-1$

               + "WHERE equipment.isCollate = TRUE" + NL //                                        //$NON-NLS-1$
               + "   AND equipment.equipmentID = ?" + NL //                                        //$NON-NLS-1$

               + "ORDER BY TourData.tourstarttime" + NL //                                         //$NON-NLS-1$

               + NL;

         final PreparedStatement statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         statement.setLong(nextIndex++, _year);
         statement.setLong(nextIndex++, _month);

         nextIndex = appFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, _yearItem.getEquipmentId());

         final ResultSet result = statement.executeQuery();

         long prevTourId = -1;
         Set<Long> allEquipmentIDs = null;
         Set<Long> allTagIDs = null;
         Set<Long> allMarkerIDs = null;

         while (result.next()) {

// SET_FORMATTING_OFF

            final long dbTourId     = result.getLong(1);
            final Object dbTagId    = result.getObject(6);
            final Object dbMarkerId = result.getObject(7);
            final Object dbEquipmentID = result.getObject(8);

// SET_FORMATTING_ON

            if (dbTourId == prevTourId) {

               // additional resultsets for the same tour

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

               // get tags from outer join
               if (dbTagId instanceof final Long tagId) {
                  allTagIDs.add(tagId);
               }

               // get markers from outer join
               if (dbMarkerId instanceof final Long markerId) {
                  allMarkerIDs.add(markerId);
               }

            } else {

               // first resultset for a new tour

               final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(

                     this,
                     _equipmentItem,
                     getEquipmentViewer(),
                     getViewerType());

               allTourItems.add(tourItem);

               tourItem.readColumnValues_Tour(result);

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }

               // get first equipment id
               if (dbEquipmentID instanceof final Long equipmentID) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add(equipmentID);

                  tourItem.setEquipmentIds(allEquipmentIDs);
               }

               // get first tag id
               if (dbTagId instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagId);

                  tourItem.setTagIds(allTagIDs);
               }

               // get first marker id
               if (dbMarkerId instanceof Long) {

                  allMarkerIDs = new HashSet<>();
                  allMarkerIDs.add((Long) dbMarkerId);

                  tourItem.setMarkerIds(allMarkerIDs);
               }
            }

            prevTourId = dbTourId;
         }

      } catch (final SQLException e) {

         UI.showSQLException(e);
      }

      setChildren(allTourItems);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Month " + System.identityHashCode(this) + NL //     //$NON-NLS-1$

            + "  _year         = " + _year + NL //                            //$NON-NLS-1$
            + "  _month        = " + _month + NL //                           //$NON-NLS-1$
            + "  _yearItem     = " + _yearItem + NL //                        //$NON-NLS-1$

            + "  numTours_IsCollated = " + numTours_IsCollated + NL //                         //$NON-NLS-1$
      ;
   }
}
