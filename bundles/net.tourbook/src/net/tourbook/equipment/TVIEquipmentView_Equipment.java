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
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment extends TVIEquipmentView_Item {

   private Equipment _equipment;

   private long      _equipmentID;

   private boolean   _isMonthCategory;

   public TVIEquipmentView_Equipment(final TreeViewer equipViewer, final Equipment equipment) {

      super(equipViewer);

      _equipment = equipment;
      _equipmentID = equipment.getEquipmentId();

      firstColumn = equipment.getName();

      type = equipment.getType();
      dateFrom = equipment.getDateFrom_Local();

      price = equipment.getPrice();
      priceUnit = equipment.getPriceUnit();

      if (UI.IS_SCRAMBLE_DATA) {
         firstColumn = UI.scrambleText(firstColumn);
      }

      _isMonthCategory = equipment.getExpandType() == EquipmentManager.EXPAND_TYPE_YEAR_MONTH_TOUR;
   }

   @Override
   protected void fetchChildren() {

      loadChildren_Parts();

      /*
       * Load equipment children which are collated
       */
      if (_equipment.isCollate()) {

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
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   public long getEquipmentID() {
      return _equipmentID;
   }

   int getExpandType() {
      return _equipment.getExpandType();
   }

   private void loadChildren_Parts() {

      final Set<EquipmentPart> allParts = _equipment.getParts();

      final ArrayList<TreeViewerItem> allPartItems = new ArrayList<>();

      for (final EquipmentPart part : allParts) {

         long durationMS = part.getDuration();
         String durationLastText = UI.EMPTY_STRING;

         if (part.getDateUntil() == TimeTools.MAX_TIME_IN_EPOCH_MILLI) {

            // this is the last collated part

            durationMS = TimeTools.nowInMilliseconds() - part.getDateFrom();
            durationLastText = "Until now : ";
         }

         final TVIEquipmentView_Part partItem = new TVIEquipmentView_Part(this, part, getEquipmentViewer());

// SET_FORMATTING_OFF

         partItem.firstColumn          = part.getName();

         partItem.type                 = part.getType();
         partItem.dateFrom             = part.getDateFrom_Local();

         partItem.price                = part.getPrice();
         partItem.priceUnit            = part.getPriceUnit();

         partItem.usageDuration        = durationMS;
         partItem.usageDurationLast    = durationLastText;

// SET_FORMATTING_ON

         allPartItems.add(partItem);

         loadSummarizedValues_Part(partItem);
      }

      setChildren(allPartItems);
   }

   /**
    * Get all tours for this part
    */
   private void loadChildren_Tours() {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         /*
          * Load: Equipment, Tour
          */
         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM equipment AS equipment" + NL //                                          //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "  ON j_td_eq.equipment_equipmentid = equipment.EQUIPMENTID" + NL //           //$NON-NLS-1$

               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "  ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                       //$NON-NLS-1$
               + "  AND TourData.tourstarttime >= equipment.dateFrom" + NL //                   //$NON-NLS-1$
               + "  AND TourData.tourstarttime <  equipment.dateUntil" + NL //                  //$NON-NLS-1$

               // get tag id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" // //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                    //$NON-NLS-1$

               // get marker id's
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //               //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                       //$NON-NLS-1$

               + "WHERE equipment.isCollate = TRUE" + NL //                                     //$NON-NLS-1$
               + "   AND equipment.equipmentID = ?" + NL //                                     //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "ORDER BY TourData.TOURSTARTTIME" + NL //                                      //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, _equipmentID);
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

               // get tags from left join
               if (dbTagId instanceof final Long tagId) {
                  allTagIDs.add(tagId);
               }

               // get markers from left join
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

               + "FROM equipment AS equipment" + NL //                                          //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = equipment.EQUIPMENTID" + NL //          //$NON-NLS-1$

               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "   ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= equipment.dateFrom" + NL //                  //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  equipment.dateUntil" + NL //                 //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "WHERE equipment.iscollate = TRUE" + NL //                                     //$NON-NLS-1$
               + "   AND equipment.equipmentID = ?" + NL //                                     //$NON-NLS-1$

               + "GROUP BY TourData.STARTYEAR" + NL //                                          //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         final int nextIndex = sqlFilter.setParameters(statement, 1);
         statement.setLong(nextIndex, _equipmentID);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int year = result.getInt(1);
            final long numTours = result.getLong(2);

            final TVIEquipmentView_Equipment_Year yearItem = new TVIEquipmentView_Equipment_Year(

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

            + "TVIEquipmentView_Equipment" + NL //       //$NON-NLS-1$

            + " _equipment = " + _equipment + NL //      //$NON-NLS-1$
      ;
   }

}
