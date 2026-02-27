/*******************************************************************************
 * Copyright (C) 2015, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.collateTours;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

public class TVICollatedTour_Event extends TVICollatedTour {

   private static final char NL = UI.NEW_LINE;

   long                      tourId;

   ZonedDateTime             eventStart;
   ZonedDateTime             eventEnd;

   boolean                   isFirstEvent;
   boolean                   isLastEvent;

   TVICollatedTour_Event(final CollatedToursView view, final TVICollatedTour parentItem) {

      super(view);

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> allChildren = new ArrayList<>();
      setChildren(allChildren);

      final AppFilter appFilter = new AppFilter();

      final String sql = UI.EMPTY_STRING

            + "--" + NL //                                                    //$NON-NLS-1$
            + NL
            + "--------------------------" + NL //                            //$NON-NLS-1$
            + "-- collated - tour details" + NL //                            //$NON-NLS-1$
            + "--------------------------" + NL //                            //$NON-NLS-1$
            + NL

            + "SELECT" + NL //                                                //$NON-NLS-1$

            + "   tourID," + NL //                                         1  //$NON-NLS-1$
            + "   tourPerson_personId," + NL //                            2  //$NON-NLS-1$

            + "   tourType_typeId," + NL //                                3  //$NON-NLS-1$
            + "   jTdataTtag.TourTag_tagId," + NL //                       4  //$NON-NLS-1$
            + "   Tmarker.markerId," + NL //                               5  //$NON-NLS-1$
            + "   jTdataEq.Equipment_EquipmentID," + NL //                 6  //$NON-NLS-1$

            + "   TourStartTime," + NL //                                  7  //$NON-NLS-1$
            + "   tourDistance," + NL //                                   8  //$NON-NLS-1$
            + "   TourDeviceTime_Elapsed," + NL //                         9  //$NON-NLS-1$
            + "   TourComputedTime_Moving," + NL //                        10 //$NON-NLS-1$
            + "   tourAltUp," + NL //                                      11 //$NON-NLS-1$
            + "   tourAltDown," + NL //                                    12 //$NON-NLS-1$
            + "   startDistance," + NL //                                  13 //$NON-NLS-1$
            + "   tourTitle," + NL //                                      14 //$NON-NLS-1$
            + "   deviceTimeInterval," + NL //                             15 //$NON-NLS-1$
            + "   maxSpeed," + NL //                                       16 //$NON-NLS-1$
            + "   maxAltitude," + NL //                                    17 //$NON-NLS-1$
            + "   maxPulse," + NL //                                       18 //$NON-NLS-1$
            + "   avgPulse," + NL //                                       19 //$NON-NLS-1$
            + "   avgCadence," + NL //                                     20 //$NON-NLS-1$
            + "   (DOUBLE(weather_Temperature_Average_Device) / temperatureScale)," + NL //     21 //$NON-NLS-1$
            + "   startWeek," + NL //                                      22 //$NON-NLS-1$
            + "   startWeekYear," + NL //                                  23 //$NON-NLS-1$

            + "   weather_Wind_Direction," + NL //                         24 //$NON-NLS-1$
            + "   weather_Wind_Speed," + NL //                             25 //$NON-NLS-1$
            + "   weather_Clouds," + NL //                                 26 //$NON-NLS-1$

            + "   restPulse," + NL //                                      27 //$NON-NLS-1$
            + "   calories," + NL //                                       28 //$NON-NLS-1$

            + "   numberOfTimeSlices," + NL //                             29 //$NON-NLS-1$
            + "   numberOfPhotos," + NL //                                 30 //$NON-NLS-1$
            + "   dpTolerance," + NL //                                    31 //$NON-NLS-1$

            + "   frontShiftCount," + NL //                                32 //$NON-NLS-1$
            + "   rearShiftCount," + NL //                                 33 //$NON-NLS-1$

            + "   TimeZoneId" + NL //                                      34 //$NON-NLS-1$

            + "FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //    //$NON-NLS-1$ //$NON-NLS-2$

            // get all equipment id's
            + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" // //$NON-NLS-1$ //$NON-NLS-2$
            + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                      //$NON-NLS-1$

            // get tag id's
            + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" //    //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                     //$NON-NLS-1$

            // get marker id's
            + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " Tmarker" //                  //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = Tmarker.TourData_tourId" + NL //                        //$NON-NLS-1$

            + "WHERE TourStartTime >= ? AND TourStartTime < ?" + NL //                       //$NON-NLS-1$
            + appFilter.getWhereClause()

            + "ORDER BY TourStartTime" + NL //                                               //$NON-NLS-1$

            + NL;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, eventStart.toInstant().toEpochMilli());
         statement.setLong(2, eventEnd.toInstant().toEpochMilli());

         appFilter.setParameters(statement, 3);

         long prevTourId = -1;

         HashSet<Long> allEquipmentIDs = null;
         HashSet<Long> allMarkerIDs = null;
         HashSet<Long> allTagIDs = null;

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long dbTourId = result.getLong(1);

            final Object dbTagId = result.getObject(4);
            final Object dbMarkerId = result.getObject(5);
            final Object dbEquipmentId = result.getObject(6);

            if (dbTourId == prevTourId) {

               // additional result sets for the same tour

               // get markers from outer join
               if (dbMarkerId instanceof Long) {
                  allMarkerIDs.add((Long) dbMarkerId);
               }

               // get tags from outer join
               if (dbTagId instanceof Long) {
                  allTagIDs.add((Long) dbTagId);
               }

               // get equipment from outer join
               if (dbEquipmentId instanceof Long) {
                  allEquipmentIDs.add((Long) dbEquipmentId);
               }

            } else {

               // first resultset for a new tour

               final TVICollatedTour_Tour tourItem = new TVICollatedTour_Tour(collateToursView, this);

               allChildren.add(tourItem);

// SET_FORMATTING_OFF

               tourItem.tourId                     = dbTourId;
               tourItem.colPersonId                = result.getLong(2);

               final Object tourTypeId             = result.getObject(3);

               // 4: tag id
               // 5: marker id
               // 5: equipment id


               final long dbTourStartTime          = result.getLong(7);
               final long dbDistance               = tourItem.colDistance = result.getLong(8);
               tourItem.colElapsedTime             = result.getLong(9);
               final long dbMovingTime             = tourItem.colMovingTime = result.getLong(10);
               tourItem.colAltitudeUp              = result.getLong(11);
               tourItem.colAltitudeDown            = result.getLong(12);

               tourItem.colStartDistance           = result.getLong(13);
               tourItem.colTourTitle               = result.getString(14);
               tourItem.colTimeInterval            = result.getShort(15);

               tourItem.colMaxSpeed                = result.getFloat(16);
               tourItem.colMaxAltitude             = result.getLong(17);
               tourItem.colMaxPulse                = result.getLong(18);
               tourItem.colAvgPulse                = result.getFloat(19);
               tourItem.colAvgCadence              = result.getFloat(20);
               tourItem.colAvgTemperature_Device   = result.getFloat(21);

               tourItem.colWeekNo                  = result.getInt(22);
               tourItem.colWeekYear                = result.getInt(23);

               tourItem.colWindDir                 = result.getInt(24);
               tourItem.colWindSpd                 = result.getInt(25);
               tourItem.colClouds                  = result.getString(26);

               tourItem.colRestPulse               = result.getInt(27);
               tourItem.colCalories                = result.getInt(28);

               tourItem.colNumberOfTimeSlices      = result.getInt(29);
               tourItem.colNumberOfPhotos          = result.getInt(30);
               tourItem.colDPTolerance             = result.getInt(31);

               tourItem.colFrontShiftCount         = result.getInt(32);
               tourItem.colRearShiftCount          = result.getInt(33);

               final String dbTimeZoneId           = result.getString(34);

// SET_FORMATTING_ON

               // -----------------------------------------------

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbTourStartTime, dbTimeZoneId);

               tourItem.colTourStartTime = dbTourStartTime;

               tourItem.colWeekDay = tourDateTime.weekDay;

               tourItem.tourTypeId = (tourTypeId == null
                     ? TourDatabase.ENTITY_IS_NOT_SAVED
                     : (Long) tourTypeId);

               // compute average speed/pace, prevent divide by 0
               tourItem.colAvgSpeed = dbMovingTime == 0 ? 0 : 3.6f * dbDistance / dbMovingTime;
               tourItem.colAvgPace = dbDistance == 0 ? 0 : dbMovingTime * 1000f / dbDistance;

               tourItem.colPausedTime = tourItem.colElapsedTime - tourItem.colRecordedTime;
               tourItem.colBreakTime = tourItem.colElapsedTime - tourItem.colMovingTime;

               // get first marker id
               if (dbMarkerId instanceof Long) {

                  allMarkerIDs = new HashSet<>();
                  allMarkerIDs.add((Long) dbMarkerId);

                  tourItem.setMarkerIds(allMarkerIDs);
               }

               // get first tag id
               if (dbTagId instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagId);

                  tourItem.setTagIds(allTagIDs);
               }

               // get first equipment id
               if (dbEquipmentId instanceof Long) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add((Long) dbEquipmentId);

                  tourItem.setEquipmentIds(allEquipmentIDs);
               }
            }

            prevTourId = dbTourId;
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   @Override
   public Long getTourId() {
      return tourId;
   }

   @Override
   public boolean hasChildren() {

      if (eventEnd == null) {

         // this occures when the collation task is canceled by the user

         return false;
      }

      return colCounter > 0;
   }

   @Override
   public String toString() {
      return "\nTVICollatedTour_Event\n"// //$NON-NLS-1$
            + ("eventStart=" + eventStart + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("eventEnd=" + eventEnd + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
//            + ("eventStartText=" + eventStartText)
            + "\n"; //$NON-NLS-1$
   }

}
