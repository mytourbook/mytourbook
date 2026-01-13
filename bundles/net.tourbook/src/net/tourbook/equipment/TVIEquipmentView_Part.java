/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Part extends TVIEquipmentView_Item {

   private Equipment     _equipment;
   private EquipmentPart _part;

   private long          _partID;
   private boolean       _isMonthCategory;

   public TVIEquipmentView_Part(final TVIEquipmentView_Equipment parentItem,
                                final EquipmentPart equipmentPart,
                                final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);

      _equipment = parentItem.getEquipment();

      _part = equipmentPart;
      _partID = equipmentPart.getPartId();

      _isMonthCategory = equipmentPart.getExpandType() == EquipmentManager.EXPAND_TYPE_YEAR_MONTH_TOUR;
   }

   @Override
   protected void fetchChildren() {

      switch (getExpandType()) {

      case EquipmentManager.EXPAND_TYPE_FLAT:
         loadChildren_Tours();
         break;

      case EquipmentManager.EXPAND_TYPE_YEAR_TOUR:
         loadChildren_Years(false);
         break;

      case EquipmentManager.EXPAND_TYPE_YEAR_MONTH_TOUR:
         loadChildren_Years(true);
         break;

      default:
         break;
      }
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   int getExpandType() {
      return _part.getExpandType();
   }

   public EquipmentPart getPart() {
      return _part;
   }

   public long getPartID() {
      return _partID;
   }

   /**
    * Get all tours for this part
    */
   private void loadChildren_Tours() {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         /*
          * Load: Part, Tour
          */
         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM equipmentpart part" + NL //                                              //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               // The alias "TourData" is needed that the tour filter is working
               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "   ON TourData.tourID = j_td_eq.tourdata_tourID" + NL //                      //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$

               // get tag id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" // //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                    //$NON-NLS-1$

               // get marker id's
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //               //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                       //$NON-NLS-1$

               + "WHERE part.isCollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "   AND part.partID = ?" + NL //                                               //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "ORDER BY TourData.tourstarttime" + NL //                                      //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, _partID);
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();

         long prevTourId = -1;
         Set<Long> allTagIDs = null;
         Set<Long> allMarkerIDs = null;

         while (result.next()) {

// SET_FORMATTING_OFF

            final long dbTourId     = result.getLong(1);
            final Object dbTagId    = result.getObject(6);
            final Object dbMarkerId = result.getObject(7);

// SET_FORMATTING_ON

            if (dbTourId == prevTourId) {

               // additional resultsets for the same tour

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

               final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(this, this, getEquipmentViewer());

               allTourItems.add(tourItem);

               tourItem.readColumnValues_Tour(result);

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
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

         net.tourbook.ui.UI.showSQLException(e);
      }

      setChildren(allTourItems);
   }

   /**
    * Get all years for this part
    *
    * @param isMonth
    */
   private void loadChildren_Years(final boolean isMonth) {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + "   TourData.STARTYEAR," + NL //                                               //$NON-NLS-1$
               + "   COUNT(*) AS num_Tours," + NL //                                            //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_SUM_COLUMNS_SUMMARIZED

               + "FROM equipmentpart AS part" + NL //                                           //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "   ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + "WHERE part.iscollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "   AND part.partid = ?" + NL //                                               //$NON-NLS-1$

               + "GROUP BY TourData.STARTYEAR" + NL //                                          //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         final int nextIndex = sqlFilter.setParameters(statement, 1);
         statement.setLong(nextIndex, _partID);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int year = result.getInt(1);
            final long numTours = result.getLong(2);

            final TVIEquipmentView_Part_Year yearItem = new TVIEquipmentView_Part_Year(

                  this,
                  year,
                  _isMonthCategory,
                  getEquipmentViewer());

            allTourItems.add(yearItem);

            yearItem.numTours_IsCollated = numTours;

            yearItem.firstColumn = Integer.toString(year);

            yearItem.readCommonValues(result, 3);

            if (UI.IS_SCRAMBLE_DATA) {
               yearItem.firstColumn = UI.scrambleText(yearItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);
      }

      setChildren(allTourItems);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Part" + NL

            + " _part   = " + _part + NL

      ;
   }

}
