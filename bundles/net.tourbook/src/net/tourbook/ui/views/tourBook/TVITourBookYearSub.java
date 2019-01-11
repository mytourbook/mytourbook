/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITourBookYearSub extends TVITourBookItem {

   private YearSubCategory _category;

   public TVITourBookYearSub(final TourBookView view,
                             final TVITourBookItem parentItem,
                             final YearSubCategory itemType) {

      super(view);

      _category = itemType;

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      /*
       * set the children for the yearSub (month,week,...) item, these are tour items
       */
      String sumYear = UI.EMPTY_STRING;
      String sumYearSub = UI.EMPTY_STRING;

      if (_category == YearSubCategory.WEEK) {
         sumYear = "startWeekYear"; //$NON-NLS-1$
         sumYearSub = "startWeek"; //$NON-NLS-1$
      } else { // default to month
         sumYear = "startYear"; //$NON-NLS-1$
         sumYearSub = "startMonth"; //$NON-NLS-1$
      }

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final String sqlString = UI.NEW_LINE
            //
            + "SELECT " //                        //$NON-NLS-1$
            //
            + "startYear, " //                              1   //$NON-NLS-1$
            + "startMonth, " //                             2   //$NON-NLS-1$
            + "startDay, " //                               3   //$NON-NLS-1$
            + "tourDistance, " //                           4   //$NON-NLS-1$
            + "tourRecordingTime, " //                      5   //$NON-NLS-1$
            + "tourDrivingTime, " //                        6   //$NON-NLS-1$
            + "tourAltUp, " //                              7   //$NON-NLS-1$
            + "tourAltDown, " //                            8   //$NON-NLS-1$
            + "startDistance, " //                          9   //$NON-NLS-1$
            + "tourID, " //                                 10   //$NON-NLS-1$
            + "tourType_typeId, " //                        11   //$NON-NLS-1$
            + "tourTitle, " //                              12   //$NON-NLS-1$
            + "deviceTimeInterval, " //                     13   //$NON-NLS-1$
            + "maxSpeed, " //                               14   //$NON-NLS-1$
            + "maxAltitude, " //                            15   //$NON-NLS-1$
            + "maxPulse, " //                               16   //$NON-NLS-1$
            + "avgPulse, " //                               17   //$NON-NLS-1$
            + "avgCadence, " //                             18   //$NON-NLS-1$
            + "(DOUBLE(avgTemperature) / temperatureScale), " // 19   //$NON-NLS-1$
            + "jTdataTtag.TourTag_tagId, "//                20   //$NON-NLS-1$
            + "Tmarker.markerId, "//                        21   //$NON-NLS-1$

            + "TourStartTime, " //                          22   //$NON-NLS-1$
            + "TimeZoneId, " //                             23   //$NON-NLS-1$

            + "startWeek, " //                              24   //$NON-NLS-1$
            + "startWeekYear, " //                          25   //$NON-NLS-1$
            //
            + "weatherWindDir, " //                         26  //$NON-NLS-1$
            + "weatherWindSpd, " //                         27  //$NON-NLS-1$
            + "weatherClouds, " //                          28  //$NON-NLS-1$
            //
            + "restPulse, " //                              29  //$NON-NLS-1$
            + "calories, " //                               30   //$NON-NLS-1$
            //
            + "tourPerson_personId, " //                    31   //$NON-NLS-1$
            //
            + "numberOfTimeSlices, " //                     32   //$NON-NLS-1$
            + "numberOfPhotos, " //                         33   //$NON-NLS-1$
            + "dpTolerance, " //                            34   //$NON-NLS-1$
            //
            + "frontShiftCount, " //                        35   //$NON-NLS-1$
            + "rearShiftCount," //                          36   //$NON-NLS-1$
            //
            // ---------- POWER -------------
            //
            + "power_Avg," //                               37   //$NON-NLS-1$
            + "power_Max, " //                              38   //$NON-NLS-1$
            + "power_Normalized, " //                       39   //$NON-NLS-1$
            + "power_FTP, " //                              40   //$NON-NLS-1$

            + "power_TotalWork, " //                        41   //$NON-NLS-1$
            + "power_TrainingStressScore, " //              42   //$NON-NLS-1$
            + "power_IntensityFactor, " //                  43   //$NON-NLS-1$

            + "power_PedalLeftRightBalance, " //            44   //$NON-NLS-1$
            + "power_AvgLeftTorqueEffectiveness, " //       45   //$NON-NLS-1$
            + "power_AvgRightTorqueEffectiveness, " //      46   //$NON-NLS-1$
            + "power_AvgLeftPedalSmoothness, " //           47   //$NON-NLS-1$
            + "power_AvgRightPedalSmoothness, " //          48   //$NON-NLS-1$

            + "bikerWeight, " //                            49   //$NON-NLS-1$
            //
            // ---------- IMPORT -------------
            //
            + "tourImportFileName, " //                     50   //$NON-NLS-1$
            + "tourImportFilePath, " //                     51   //$NON-NLS-1$
            + "devicePluginName, " //                       52   //$NON-NLS-1$
            + "deviceFirmwareVersion, " //                  53   //$NON-NLS-1$

            + "cadenceMultiplier, " //                      54   //$NON-NLS-1$

            //
            // ---------- RUNNING DYNAMICS -------------
            //
            + "runDyn_StanceTime_Min, " //                  55   //$NON-NLS-1$
            + "runDyn_StanceTime_Max, " //                  56   //$NON-NLS-1$
            + "runDyn_StanceTime_Avg, " //                  57   //$NON-NLS-1$

            + "runDyn_StanceTimeBalance_Min, " //           58   //$NON-NLS-1$
            + "runDyn_StanceTimeBalance_Max, " //           59   //$NON-NLS-1$
            + "runDyn_StanceTimeBalance_Avg, " //           60   //$NON-NLS-1$

            + "runDyn_StepLength_Min, " //                  61   //$NON-NLS-1$
            + "runDyn_StepLength_Max, " //                  62   //$NON-NLS-1$
            + "runDyn_StepLength_Avg, " //                  63   //$NON-NLS-1$

            + "runDyn_VerticalOscillation_Min, " //         64   //$NON-NLS-1$
            + "runDyn_VerticalOscillation_Max, " //         65   //$NON-NLS-1$
            + "runDyn_VerticalOscillation_Avg, " //         66   //$NON-NLS-1$

            + "runDyn_VerticalRatio_Min, " //               67   //$NON-NLS-1$
            + "runDyn_VerticalRatio_Max, " //               68   //$NON-NLS-1$
            + "runDyn_VerticalRatio_Avg, " //               69   //$NON-NLS-1$

            //
            // ---------- SURFING -------------
            //
            + "surfing_NumberOfEvents, " //                 70   //$NON-NLS-1$
            + "surfing_MinSpeed_StartStop, " //             71   //$NON-NLS-1$
            + "surfing_MinSpeed_Surfing, " //               72   //$NON-NLS-1$
            + "surfing_MinTimeDuration, " //                73   //$NON-NLS-1$
            + "surfing_IsMinDistance, " //                  74   //$NON-NLS-1$
            + "surfing_MinDistance" //                      75   //$NON-NLS-1$

            + UI.NEW_LINE

            + (" FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + UI.NEW_LINE) //         //$NON-NLS-1$ //$NON-NLS-2$

            // get tag id's
            + (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
            + (" ON TourData.tourId = jTdataTtag.TourData_tourId") //$NON-NLS-1$

            // get marker id's
            + (" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_MARKER + " Tmarker") //$NON-NLS-1$ //$NON-NLS-2$
            + (" ON TourData.tourId = Tmarker.TourData_tourId\n") //$NON-NLS-1$

            + (" WHERE " + sumYear + " = ?\n")//            //$NON-NLS-1$ //$NON-NLS-2$
            + (" AND " + sumYearSub + " = ?\n")//               //$NON-NLS-1$ //$NON-NLS-2$
            + sqlFilter.getWhereClause()

            + " ORDER BY TourStartTime\n"; //$NON-NLS-1$

      try {

         final Connection conn = TourDatabase.getInstance().getConnection();

//         TourDatabase.enableRuntimeStatistics(conn);

         final PreparedStatement statement = conn.prepareStatement(sqlString);
         statement.setInt(1, tourYear);
         statement.setInt(2, tourYearSub);
         sqlFilter.setParameters(statement, 3);

         long prevTourId = -1;
         HashSet<Long> tagIds = null;
         HashSet<Long> markerIds = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long resultTourId = result.getLong(10);

            final Object resultTagId = result.getObject(20);
            final Object resultMarkerId = result.getObject(21);

            if (resultTourId == prevTourId) {

               // additional result set's for the same tour

               // get tags from outer join
               if (resultTagId instanceof Long) {
                  tagIds.add((Long) resultTagId);
               }

               // get markers from outer join
               if (resultMarkerId instanceof Long) {
                  markerIds.add((Long) resultMarkerId);
               }

            } else {

               // first resultset for a new tour

               final TVITourBookTour tourItem = new TVITourBookTour(tourBookView, this);
               children.add(tourItem);

               tourItem.tourId = resultTourId;

// SET_FORMATTING_OFF

               final int dbYear                 = result.getInt(1);
               final int dbMonth                = result.getInt(2);
               final int dbDay                  = result.getInt(3);
               final int dbWeek                 = result.getInt(24);

               tourItem.treeColumn              = Integer.toString(dbDay);
               tourItem.tourYear                = dbYear;
               tourItem.tourYearSub             = tourYearSub;
               tourItem.tourMonth               = dbMonth;
               tourItem.tourDay                 = dbDay;
               tourItem.tourWeek                = dbWeek;

               final long dbDistance            = tourItem.colDistance = result.getLong(4);
               tourItem.colRecordingTime        = result.getLong(5);
               final long dbDrivingTime         = tourItem.colDrivingTime = result.getLong(6);
               tourItem.colAltitudeUp           = result.getLong(7);
               tourItem.colAltitudeDown         = result.getLong(8);

               tourItem.colStartDistance        = result.getLong(9);
               final Object tourTypeId          = result.getObject(11);
               tourItem.colTourTitle            = result.getString(12);
               tourItem.colTimeInterval         = result.getShort(13);

               tourItem.colMaxSpeed             = result.getFloat(14);
               tourItem.colMaxAltitude          = result.getLong(15);
               tourItem.colMaxPulse             = result.getLong(16);
               tourItem.colAvgPulse             = result.getFloat(17);
               final float dbAvgCadence         = result.getFloat(18);
               tourItem.colAvgTemperature       = result.getFloat(19);

               final long dbTourStartTime       = result.getLong(22);
               final String dbTimeZoneId        = result.getString(23);

               tourItem.colWeekNo               = result.getInt(24);
               tourItem.colWeekYear             = result.getInt(25);

               tourItem.colWindDir              = result.getInt(26);
               tourItem.colWindSpd              = result.getInt(27);
               tourItem.colClouds               = result.getString(28);
               tourItem.colRestPulse            = result.getInt(29);

               tourItem.colCalories             = result.getInt(30);
               tourItem.colPersonId             = result.getLong(31);

               tourItem.colNumberOfTimeSlices   = result.getInt(32);
               tourItem.colNumberOfPhotos       = result.getInt(33);
               tourItem.colDPTolerance          = result.getInt(34);

               tourItem.colFrontShiftCount      = result.getInt(35);
               tourItem.colRearShiftCount       = result.getInt(36);

               // ----------------- POWER ------------------

               final float dbAvgPower                          = result.getFloat(37);

               tourItem.colPower_Avg = dbAvgPower;
               tourItem.colPower_Max                           = result.getInt(38);
               tourItem.colPower_Normalized                    = result.getInt(39);
               tourItem.colPower_FTP                           = result.getInt(40);

               tourItem.colPower_TotalWork                     = result.getLong(41);
               tourItem.colPower_TrainingStressScore           = result.getFloat(42);
               tourItem.colPower_IntensityFactor               = result.getFloat(43);

               tourItem.colPower_PedalLeftRightBalance         = result.getInt(44);
               tourItem.colPower_AvgLeftTorqueEffectiveness    = result.getFloat(45);
               tourItem.colPower_AvgRightTorqueEffectiveness   = result.getFloat(46);
               tourItem.colPower_AvgLeftPedalSmoothness        = result.getFloat(47);
               tourItem.colPower_AvgRightPedalSmoothness       = result.getFloat(48);

               final float dbBodyWeight                        = result.getFloat(49);

               // --------------------- IMPORT ------------------

               tourItem.col_ImportFileName                  = result.getString(50);
               tourItem.col_ImportFilePath                  = result.getString(51);

               String dbDeviceName                          = result.getString(52);
               String dbFirmwareVersion                     = result.getString(53);

               // -----------------------------------------------

               final float dbCadenceMultiplier = result.getFloat(54);

               // ---------- RUNNING DYNAMICS -------------

               tourItem.colRunDyn_StanceTime_Min            = result.getInt(55);
               tourItem.colRunDyn_StanceTime_Max            = result.getInt(56);
               tourItem.colRunDyn_StanceTime_Avg            = result.getFloat(57);

               tourItem.colRunDyn_StanceTimeBalance_Min     = result.getInt(58)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_StanceTimeBalance_Max     = result.getInt(59)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_StanceTimeBalance_Avg     = result.getFloat(60)   / TourData.RUN_DYN_DATA_MULTIPLIER;

               tourItem.colRunDyn_StepLength_Min            = result.getInt(61);
               tourItem.colRunDyn_StepLength_Max            = result.getInt(62);
               tourItem.colRunDyn_StepLength_Avg            = result.getFloat(63);

               tourItem.colRunDyn_VerticalOscillation_Min   = result.getInt(64)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_VerticalOscillation_Max   = result.getInt(65)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_VerticalOscillation_Avg   = result.getFloat(66)   / TourData.RUN_DYN_DATA_MULTIPLIER;

               tourItem.colRunDyn_VerticalRatio_Min         = result.getInt(67)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_VerticalRatio_Max         = result.getInt(68)     / TourData.RUN_DYN_DATA_MULTIPLIER;
               tourItem.colRunDyn_VerticalRatio_Avg         = result.getFloat(69)   / TourData.RUN_DYN_DATA_MULTIPLIER;

               // ---------- SURFING -------------

               tourItem.col_Surfing_NumberOfEvents          = result.getShort(70);
               tourItem.col_Surfing_MinSpeed_StartStop      = result.getShort(71);
               tourItem.col_Surfing_MinSpeed_Surfing        = result.getShort(72);
               tourItem.col_Surfing_MinTimeDuration         = result.getShort(73);

               tourItem.col_Surfing_IsMinDistance           = result.getBoolean(74);
               tourItem.col_Surfing_MinDistance             = result.getShort(75);

// SET_FORMATTING_ON

               // -----------------------------------------------

               tourItem.colBodyWeight = dbBodyWeight;
               tourItem.colPower_PowerToWeight = dbBodyWeight == 0 ? 0 : dbAvgPower / dbBodyWeight;

               tourItem.colAvgCadence = dbAvgCadence * dbCadenceMultiplier;
               tourItem.colCadenceMultiplier = dbCadenceMultiplier;

               // -----------------------------------------------

               dbDeviceName = dbDeviceName == null ? UI.EMPTY_STRING : dbDeviceName;
               dbFirmwareVersion = dbFirmwareVersion == null ? UI.EMPTY_STRING : dbFirmwareVersion;

               final String deviceName = dbFirmwareVersion.length() == 0//
                     ? dbDeviceName
                     : dbDeviceName
                           + UI.SPACE
                           + UI.SYMBOL_BRACKET_LEFT
                           + dbFirmwareVersion
                           + UI.SYMBOL_BRACKET_RIGHT;

               tourItem.col_DeviceName = deviceName;

               // -----------------------------------------------

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbTourStartTime, dbTimeZoneId);

               tourItem.colTourDateTime = tourDateTime;
               tourItem.colTimeZoneId = dbTimeZoneId;
               tourItem.colWeekDay = tourDateTime.weekDay;

               tourItem.tourTypeId = (tourTypeId == null //
                     ? TourDatabase.ENTITY_IS_NOT_SAVED
                     : (Long) tourTypeId);

               // compute average speed/pace, prevent divide by 0
               tourItem.colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
               tourItem.colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 1000 / dbDistance;

               tourItem.colPausedTime = tourItem.colRecordingTime - tourItem.colDrivingTime;

               // get first tag id
               if (resultTagId instanceof Long) {

                  tagIds = new HashSet<>();
                  tagIds.add((Long) resultTagId);

                  tourItem.setTagIds(tagIds);
               }

               // get first marker id
               if (resultMarkerId instanceof Long) {

                  markerIds = new HashSet<>();
                  markerIds.add((Long) resultMarkerId);

                  tourItem.setMarkerIds(markerIds);
               }
            }

            prevTourId = resultTourId;
         }

//       TourDatabase.disableRuntimeStatistic(conn);

         conn.close();

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   public YearSubCategory getCategory() {
      return _category;
   }

}
