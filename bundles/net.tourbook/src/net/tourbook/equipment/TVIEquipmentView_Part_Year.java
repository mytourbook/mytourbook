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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilter_WithExists;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Part_Year extends TVIEquipmentView_Item {

   private TVIEquipmentView_Part _partItem;

   /**
    * Year category
    */
   private final int             _year;

   /**
    * Is <code>true</code> when the children of this year item contains month items<br>
    * Is <code>false</code> when the children of this year item contains tour items
    */
   private boolean               _isMonthCategory;

   public TVIEquipmentView_Part_Year(final TVIEquipmentView_Part partItem,
                                     final int year,
                                     final boolean isMonth,
                                     final TreeViewer treeViewer,
                                     final boolean isShowTours) {

      super(treeViewer, isShowTours);

      setParentItem(partItem);

      _partItem = partItem;
      _year = year;
      _isMonthCategory = isMonth;
   }

   /**
    * Compare two instances of {@link TVIEquipmentView_Part_Year}
    *
    * @param otherYearItem
    *
    * @return
    */
   public int compareTo(final TVIEquipmentView_Part_Year otherYearItem) {

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

      final TVIEquipmentView_Part_Year other = (TVIEquipmentView_Part_Year) obj;

      if (_isMonthCategory != other._isMonthCategory) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_partItem == null) {
         if (other._partItem != null) {
            return false;
         }
      } else if (!_partItem.equals(other._partItem)) {
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

   public long getPartId() {
      return _partItem.getPartID();
   }

   public TVIEquipmentView_Part getPartItem() {
      return _partItem;
   }

   public int getYear() {
      return _year;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (_isMonthCategory ? 1231 : 1237);
      result = prime * result + ((_partItem == null) ? 0 : _partItem.hashCode());
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

         final SQLFilter appFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS_NO_TAG);
         final TourTagFilter_WithExists tagFilter = new TourTagFilter_WithExists();

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
               + "   AND TourData.tourstarttime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$
               + "   AND TourData.StartYear = ?" + NL //                                        //$NON-NLS-1$

               + appFilter.getWhereClause()
               + tagFilter.getSql()

               + "WHERE part.iscollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "   AND part.partid = ?" + NL //                                               //$NON-NLS-1$

               + "GROUP BY "
               + "   TourData.StartYear," + NL //                                               //$NON-NLS-1$
               + "   TourData.StartMonth" + NL //                                               //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         statement.setLong(nextIndex++, _year);

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = tagFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, _partItem.getPartID());

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int dbYear = result.getInt(1);
            final int dbMonth = result.getInt(2);
            final long numTours = result.getLong(3);

            final TVIEquipmentView_Part_Month monthItem = new TVIEquipmentView_Part_Month(

                  this,
                  _partItem,
                  dbYear,
                  dbMonth,
                  getEquipmentViewer(),
                  isShowTours());

            allTourItems.add(monthItem);

            monthItem.numTours_IsCollated = numTours;

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

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter appFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS_NO_TAG);
         final TourTagFilter_WithExists tagFilter = new TourTagFilter_WithExists();

         /*
          * Load: Part, Year, Tour
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

               + appFilter.getWhereClause()
               + tagFilter.getSql()

               // get all equipment id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                             //$NON-NLS-1$

               // get tag id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" // //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                    //$NON-NLS-1$

               // get marker id's
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //               //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                       //$NON-NLS-1$

               + "WHERE part.isCollate = TRUE" + NL //                                             //$NON-NLS-1$
               + "   AND part.partID = ?" + NL //                                                  //$NON-NLS-1$

               + "ORDER BY TourData.tourstarttime" + NL //                                         //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         statement.setLong(nextIndex++, _year);

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = tagFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, _partItem.getPartID());

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

               final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(this, _partItem, getEquipmentViewer(), isShowTours());

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

            + "TVITagView_Year" + System.identityHashCode(this) + NL //    //$NON-NLS-1$

            + "  _year        = " + _year + NL //                          //$NON-NLS-1$
            + "  _isMonth     = " + _isMonthCategory + NL //               //$NON-NLS-1$

            + NL
            + "  numTours_IsCollated = " + numTours_IsCollated + NL //     //$NON-NLS-1$
      ;
   }
}
