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
import java.util.List;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Part extends TVIEquipmentView_Item {

   private Equipment     _equipment;
   private EquipmentPart _part;

   private long          _partID;
   private boolean       _isMonthCategory;

   public TVIEquipmentView_Part(final TVIEquipmentView_Equipment parentItem,
                                final EquipmentPart equipmentPart,
                                final TreeViewer treeViewer,
                                final EquipmentViewerType equipmentType) {

      super(treeViewer, equipmentType);

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

   @Override
   public boolean hasChildren() {

      final List<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();
      final boolean is0UnfetchedChildren = unfetchedChildren != null && unfetchedChildren.size() == 0;

      final EquipmentViewerType viewerType = getViewerType();

      if (viewerType == EquipmentViewerType.IS_EQUIPMENT_FILTER) {

         if (_part.isCollate()) {

            // hide the expand icon in the view

            return false;

         } else {

            if (is0UnfetchedChildren) {

               // hide the expand icon in the view

               return false;
            }
         }

      } else if (viewerType == EquipmentViewerType.IS_EQUIPMENT_VIEWER && _part.isCollate()) {

         if (is0UnfetchedChildren) {

            // hide the expand icon in the view

            return false;
         }
      }

      return true;
   }

   /**
    * Get all tours for this part
    */
   private void loadChildren_Tours() {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();
         final SQLData partFilter = new EquipmentPartFilter().getSqlData();

         /*
          * Load: Part, Tours
          */
         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                            //$NON-NLS-1$
               + NL
               + "---------------" + NL //                                                               //$NON-NLS-1$
               + "-- part - tours" + NL //                                                               //$NON-NLS-1$
               + "---------------" + NL //                                                               //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                        //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM " + TourDatabase.TABLE_EQUIPMENT_PART + " AS part" + NL //                        //$NON-NLS-1$ //$NON-NLS-2$

               + "JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_td_eq" + NL //           //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //              //$NON-NLS-1$

               // The alias "TourData" is needed that the tour filter is working
               + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" + NL //                         //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON TourData.tourID = j_td_eq.tourdata_tourID" + NL //                               //$NON-NLS-1$
               + "   AND TourData.TourStartTime >= part.dateFrom" + NL //                                //$NON-NLS-1$
               + "   AND TourData.TourStartTime <  part.dateUntil" + NL //                               //$NON-NLS-1$

               + partFilter.getSqlString()

               // get all equipment id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                               //$NON-NLS-1$

               // get all tag id's
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" //          //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                             //$NON-NLS-1$

               // get all marker id's
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                        //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                                //$NON-NLS-1$

               + "WHERE part.isCollate = TRUE" + NL //                                                   //$NON-NLS-1$
               + "  AND part.partID    = ?" + NL //                                                      //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "ORDER BY TourData.TourStartTime" + NL //                                               //$NON-NLS-1$

               + NL
               + "--;" + NL //                                                                           //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = partFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, _partID);

         nextIndex = appFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         long prevTourId = -1;
         Set<Long> allEquipmentIDs = null;
         Set<Long> allTagIDs = null;
         Set<Long> allMarkerIDs = null;

         while (result.next()) {

// SET_FORMATTING_OFF

            final long dbTourId        = result.getLong(1);
            final Object dbTagId       = result.getObject(6);
            final Object dbMarkerId    = result.getObject(7);
            final Object dbEquipmentID = result.getObject(8);

// SET_FORMATTING_ON

            if (dbTourId == prevTourId) {

               // additional resultsets for the same tour

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

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

               final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(

                     this,
                     this,
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
               if (dbTagId instanceof final Long tagID) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add(tagID);

                  tourItem.setTagIds(allTagIDs);
               }

               // get first marker id
               if (dbMarkerId instanceof final Long markerID) {

                  allMarkerIDs = new HashSet<>();
                  allMarkerIDs.add(markerID);

                  tourItem.setMarkerIds(allMarkerIDs);
               }
            }

            prevTourId = dbTourId;
         }
      } catch (final SQLException e) {

         SQL.showException(e, sql);
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

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();
         final SQLData partFilter = new EquipmentPartFilter().getSqlData();

         /*
          * Load: Part, Years
          */
         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                   //$NON-NLS-1$
               + NL
               + "---------------" + NL //                                                      //$NON-NLS-1$
               + "-- part - years" + NL //                                                      //$NON-NLS-1$
               + "---------------" + NL //                                                      //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + "   tdFields.StartYear," + NL //                                               //$NON-NLS-1$
               + "   COUNT(*) AS Num_Tours," + NL //                                            //$NON-NLS-1$

               + getSQL_SUM_COLUMNS("tdFields", 3) //                                           //$NON-NLS-1$

               + "FROM " + NL //                                                                //$NON-NLS-1$

               + "(" + NL //                                                                    //$NON-NLS-1$

               // Get distinct tours that match the criteria (parts 1 or 6 active at tour start)
               + "   SELECT DISTINCT" + NL //                                                   //$NON-NLS-1$

               + "      TourData.TourID," + NL //                                               //$NON-NLS-1$
               + "      TourData.StartYear," + NL //                                            //$NON-NLS-1$

               + getSQL_SUM_TOUR_COLUMNS("TourData", 6) //                                      //$NON-NLS-1$

               + "   FROM " + TourDatabase.TABLE_EQUIPMENT_PART + " AS part" + NL //            //$NON-NLS-1$ //$NON-NLS-2$

               + "JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_td_eq" + NL //  //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" + NL //                //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "   AND TourData.TourStartTime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "   AND TourData.TourStartTime <  part.dateUntil" + NL //                      //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter.getSqlString()

               + "	WHERE part.IsCollate = TRUE" + NL //                                       //$NON-NLS-1$
               + "     AND part.PartId = ?" + NL //                                             //$NON-NLS-1$

               + ") AS tdFields" + NL //                                                        //$NON-NLS-1$

               + "GROUP BY StartYear" + NL //                                                   //$NON-NLS-1$
               + "ORDER BY StartYear" + NL //                                                   //$NON-NLS-1$

               + NL
               + "--;" + NL //                                                                  //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, _partID);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int year = result.getInt(1);
            final long numTours = result.getLong(2);

            final TVIEquipmentView_Part_Year yearItem = new TVIEquipmentView_Part_Year(

                  this,
                  year,
                  _isMonthCategory,
                  getEquipmentViewer(),
                  getViewerType());

            allTourItems.add(yearItem);

            yearItem.numTours_IsCollated = numTours;

            yearItem.firstColumn = Integer.toString(year);

            yearItem.readCommonValues(result, 3);

            if (UI.IS_SCRAMBLE_DATA) {
               yearItem.firstColumn = UI.scrambleText(yearItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }

      setChildren(allTourItems);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Part" + NL //$NON-NLS-1$

            + " _part   = " + _part + NL //$NON-NLS-1$

      ;
   }

}
