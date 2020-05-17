/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

public abstract class TVITourBookItem extends TreeViewerItem implements ITourItem {

   static ZonedDateTime calendar8 = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   static final char    NL        = net.tourbook.common.UI.NEW_LINE;

   static final String  SQL_ALL_TOUR_FIELDS;
   static final String  SQL_SUM_COLUMNS;
   static final String  SQL_SUM_FIELDS;

   static {

      SQL_ALL_TOUR_FIELDS = NL

            + "startYear, " //                                    1     //$NON-NLS-1$
            + "startMonth, " //                                   2     //$NON-NLS-1$
            + "startDay, " //                                     3     //$NON-NLS-1$
            + "tourDistance, " //                                 4     //$NON-NLS-1$
            + "tourRecordingTime, " //                            5     //$NON-NLS-1$
            + "tourDrivingTime, " //                              6     //$NON-NLS-1$
            + "tourAltUp, " //                                    7     //$NON-NLS-1$
            + "tourAltDown, " //                                  8     //$NON-NLS-1$
            + "startDistance, " //                                9     //$NON-NLS-1$
            + "tourID, " //                                       10    //$NON-NLS-1$
            + "tourType_typeId, " //                              11    //$NON-NLS-1$
            + "tourTitle, " //                                    12    //$NON-NLS-1$
            + "deviceTimeInterval, " //                           13    //$NON-NLS-1$
            + "maxSpeed, " //                                     14    //$NON-NLS-1$
            + "maxAltitude, " //                                  15    //$NON-NLS-1$
            + "maxPulse, " //                                     16    //$NON-NLS-1$
            + "avgPulse, " //                                     17    //$NON-NLS-1$
            + "avgCadence, " //                                   18    //$NON-NLS-1$
            + "(DOUBLE(avgTemperature) / temperatureScale), " //  19    //$NON-NLS-1$
            + "jTdataTtag.TourTag_tagId, "//                      20    //$NON-NLS-1$
            + "Tmarker.markerId, "//                              21    //$NON-NLS-1$

            + "TourStartTime, " //                                22    //$NON-NLS-1$
            + "TimeZoneId, " //                                   23    //$NON-NLS-1$

            + "startWeek, " //                                    24    //$NON-NLS-1$
            + "startWeekYear, " //                                25    //$NON-NLS-1$
            //
            + "weatherWindDir, " //                               26    //$NON-NLS-1$
            + "weatherWindSpd, " //                               27    //$NON-NLS-1$
            + "weatherClouds, " //                                28    //$NON-NLS-1$
            //
            + "restPulse, " //                                    29    //$NON-NLS-1$
            + "calories, " //                                     30    //$NON-NLS-1$
            //
            + "tourPerson_personId, " //                          31    //$NON-NLS-1$
            //
            + "numberOfTimeSlices, " //                           32    //$NON-NLS-1$
            + "numberOfPhotos, " //                               33    //$NON-NLS-1$
            + "dpTolerance, " //                                  34    //$NON-NLS-1$
            //
            + "frontShiftCount, " //                              35    //$NON-NLS-1$
            + "rearShiftCount," //                                36    //$NON-NLS-1$
            //
            // ---------- POWER -------------
            //
            + "power_Avg," //                                     37    //$NON-NLS-1$
            + "power_Max, " //                                    38    //$NON-NLS-1$
            + "power_Normalized, " //                             39    //$NON-NLS-1$
            + "power_FTP, " //                                    40    //$NON-NLS-1$

            + "power_TotalWork, " //                              41    //$NON-NLS-1$
            + "power_TrainingStressScore, " //                    42    //$NON-NLS-1$
            + "power_IntensityFactor, " //                        43    //$NON-NLS-1$

            + "power_PedalLeftRightBalance, " //                  44    //$NON-NLS-1$
            + "power_AvgLeftTorqueEffectiveness, " //             45    //$NON-NLS-1$
            + "power_AvgRightTorqueEffectiveness, " //            46    //$NON-NLS-1$
            + "power_AvgLeftPedalSmoothness, " //                 47    //$NON-NLS-1$
            + "power_AvgRightPedalSmoothness, " //                48    //$NON-NLS-1$

            + "bikerWeight, " //                                  49    //$NON-NLS-1$
            //
            // ---------- IMPORT -------------
            //
            + "tourImportFileName, " //                           50    //$NON-NLS-1$
            + "tourImportFilePath, " //                           51    //$NON-NLS-1$
            + "devicePluginName, " //                             52    //$NON-NLS-1$
            + "deviceFirmwareVersion, " //                        53    //$NON-NLS-1$

            + "cadenceMultiplier, " //                            54    //$NON-NLS-1$

            //
            // ---------- RUNNING DYNAMICS -------------
            //
            + "runDyn_StanceTime_Min, " //                        55    //$NON-NLS-1$
            + "runDyn_StanceTime_Max, " //                        56    //$NON-NLS-1$
            + "runDyn_StanceTime_Avg, " //                        57    //$NON-NLS-1$

            + "runDyn_StanceTimeBalance_Min, " //                 58    //$NON-NLS-1$
            + "runDyn_StanceTimeBalance_Max, " //                 59    //$NON-NLS-1$
            + "runDyn_StanceTimeBalance_Avg, " //                 60    //$NON-NLS-1$

            + "runDyn_StepLength_Min, " //                        61    //$NON-NLS-1$
            + "runDyn_StepLength_Max, " //                        62    //$NON-NLS-1$
            + "runDyn_StepLength_Avg, " //                        63    //$NON-NLS-1$

            + "runDyn_VerticalOscillation_Min, " //               64    //$NON-NLS-1$
            + "runDyn_VerticalOscillation_Max, " //               65    //$NON-NLS-1$
            + "runDyn_VerticalOscillation_Avg, " //               66    //$NON-NLS-1$

            + "runDyn_VerticalRatio_Min, " //                     67    //$NON-NLS-1$
            + "runDyn_VerticalRatio_Max, " //                     68    //$NON-NLS-1$
            + "runDyn_VerticalRatio_Avg, " //                     69    //$NON-NLS-1$

            //
            // ---------- SURFING -------------
            //
            + "surfing_NumberOfEvents, " //                       70    //$NON-NLS-1$
            + "surfing_MinSpeed_StartStop, " //                   71    //$NON-NLS-1$
            + "surfing_MinSpeed_Surfing, " //                     72    //$NON-NLS-1$
            + "surfing_MinTimeDuration, " //                      73    //$NON-NLS-1$
            + "surfing_IsMinDistance, " //                        74    //$NON-NLS-1$
            + "surfing_MinDistance," //                           75    //$NON-NLS-1$

            //
            // ---------- TRAINING -------------
            //
            + "training_TrainingEffect_Aerob, " //                76    //$NON-NLS-1$
            + "training_TrainingEffect_Anaerob, " //              77    //$NON-NLS-1$
            + "training_TrainingPerformance, " //                 78    //$NON-NLS-1$

            // ---------- CADENCE ZONE -------------

            + "cadenceZone_SlowTime, " //                         79    //$NON-NLS-1$
            + "cadenceZone_FastTime, " //                         80    //$NON-NLS-1$
            + "cadenceZones_DelimiterValue, " //                  81    //$NON-NLS-1$

            // ---------- WEATHER -------------

            + "weather_Temperature_Min, " //                      82    //$NON-NLS-1$
            + "weather_Temperature_Max, " //                      83    //$NON-NLS-1$

            // ---------- TOUR START LOCATION -------------

            + "tourStartPlace, " //                               84    //$NON-NLS-1$
            + "tourEndPlace, " //                                 85    //$NON-NLS-1$

            // -------- AVERAGE ALTITUDE CHANGE -----------

            + "avgAltitudeChange " //                             86    //$NON-NLS-1$

      ;

      SQL_SUM_FIELDS = NL

            + "TourDistance,                " + NL //$NON-NLS-1$
            + "TourRecordingTime,           " + NL //$NON-NLS-1$
            + "TourDrivingTime,             " + NL //$NON-NLS-1$
            + "TourAltUp,                   " + NL //$NON-NLS-1$
            + "TourAltDown,                 " + NL //$NON-NLS-1$

            + "MaxAltitude,                 " + NL //$NON-NLS-1$
            + "MaxPulse,                    " + NL //$NON-NLS-1$
            + "MaxSpeed,                    " + NL //$NON-NLS-1$

            + "AvgCadence,                  " + NL //$NON-NLS-1$
            + "AvgPulse,                    " + NL //$NON-NLS-1$
            + "AvgTemperature,              " + NL //$NON-NLS-1$
            + "CadenceMultiplier,           " + NL //$NON-NLS-1$
            + "TemperatureScale,            " + NL //$NON-NLS-1$
            + "WeatherWindDir,              " + NL //$NON-NLS-1$
            + "WeatherWindSpd,              " + NL //$NON-NLS-1$

            + "Calories,                    " + NL //$NON-NLS-1$
            + "RestPulse,                   " + NL //$NON-NLS-1$

            + "Power_TotalWork,             " + NL //$NON-NLS-1$

            + "NumberOfTimeSlices,          " + NL //$NON-NLS-1$
            + "NumberOfPhotos,              " + NL //$NON-NLS-1$

            + "FrontShiftCount,             " + NL //$NON-NLS-1$
            + "RearShiftCount,              " + NL //$NON-NLS-1$

            + "surfing_NumberOfEvents,      " + NL //$NON-NLS-1$

            + "cadenceZone_SlowTime,        " + NL //$NON-NLS-1$
            + "cadenceZone_FastTime,        " + NL //$NON-NLS-1$
            + "cadenceZones_DelimiterValue, " + NL //$NON-NLS-1$

            + "weather_Temperature_Min,     " + NL //$NON-NLS-1$
            + "weather_Temperature_Max      " + NL //$NON-NLS-1$

      ;

      SQL_SUM_COLUMNS = NL

            + "SUM( CAST(TourDistance AS BIGINT)),          " + NL // 0   //$NON-NLS-1$
            + "SUM( CAST(TourRecordingTime AS BIGINT)),     " + NL // 1   //$NON-NLS-1$
            + "SUM( CAST(TourDrivingTime AS BIGINT)),       " + NL // 2   //$NON-NLS-1$
            + "SUM( CAST(TourAltUp AS BIGINT)),             " + NL // 3   //$NON-NLS-1$
            + "SUM( CAST(TourAltDown AS BIGINT)),           " + NL // 4   //$NON-NLS-1$
            + "SUM(1),                                      " + NL // 5   //$NON-NLS-1$
            //
            + "MAX(MaxSpeed),                               " + NL // 6   //$NON-NLS-1$
            + "MAX(MaxAltitude),                            " + NL // 7   //$NON-NLS-1$
            + "MAX(MaxPulse),                               " + NL // 8 //$NON-NLS-1$
            //
            + "AVG( CASE WHEN AvgPulse = 0         THEN NULL ELSE AvgPulse END         ), " + NL //                              9   //$NON-NLS-1$
            + "AVG( CASE WHEN AvgCadence = 0       THEN NULL ELSE DOUBLE(AvgCadence) * CadenceMultiplier END ),      " + NL //   10   //$NON-NLS-1$
            + "AVG( CASE WHEN AvgTemperature = 0   THEN NULL ELSE DOUBLE(AvgTemperature) / TemperatureScale END ),   " + NL //   11   //$NON-NLS-1$
            + "AVG( CASE WHEN WeatherWindDir = 0   THEN NULL ELSE WeatherWindDir END   ), " + NL //                              12   //$NON-NLS-1$
            + "AVG( CASE WHEN WeatherWindSpd = 0   THEN NULL ELSE WeatherWindSpd END   ), " + NL //                              13   //$NON-NLS-1$
            + "AVG( CASE WHEN RestPulse = 0        THEN NULL ELSE RestPulse END        ), " + NL //                              14   //$NON-NLS-1$
            //
            + "SUM( CAST(Calories AS BIGINT)),              " + NL // 15   //$NON-NLS-1$
            + "SUM( CAST(Power_TotalWork AS BIGINT)),       " + NL // 16   //$NON-NLS-1$

            + "SUM( CAST(NumberOfTimeSlices AS BIGINT)),    " + NL // 17   //$NON-NLS-1$
            + "SUM( CAST(NumberOfPhotos AS BIGINT)),        " + NL // 18   //$NON-NLS-1$
            //
            + "SUM( CAST(FrontShiftCount AS BIGINT)),       " + NL // 19   //$NON-NLS-1$
            + "SUM( CAST(RearShiftCount AS BIGINT)),        " + NL // 20   //$NON-NLS-1$

            + "SUM( CAST(Surfing_NumberOfEvents AS BIGINT))," + NL // 21   //$NON-NLS-1$

            + "SUM( CAST(cadenceZone_SlowTime AS BIGINT)),  " + NL // 22   //$NON-NLS-1$
            + "SUM( CAST(cadenceZone_FastTime AS BIGINT)),  " + NL // 23   //$NON-NLS-1$
            + "AVG( CASE WHEN cadenceZones_DelimiterValue = 0 THEN NULL ELSE cadenceZones_DelimiterValue END ), " + NL // 24  //$NON-NLS-1$

            + "MIN(CASE WHEN weather_Temperature_Min = 0 THEN NULL ELSE weather_Temperature_Min END), " + NL // 25            //$NON-NLS-1$
            + "MAX(CASE WHEN weather_Temperature_Max = 0 THEN NULL ELSE weather_Temperature_Max END)  " + NL // 26            //$NON-NLS-1$
      ;

   }

   TourBookView tourBookView;

   String       treeColumn;

   int          tourYear;

   /**
    * Month starts with 1 for January
    */
   int          tourMonth;
   int          tourWeek;
   int          tourYearSub;
   int          tourDay;

   /**
    * Contains the tour date time with time zone info when available
    */
   TourDateTime colTourDateTime;
   String       colTimeZoneId;

   String       colTourTitle;
   String       colTourLocation_Start;   // tourStartPlace
   String       colTourLocation_End;     // tourEndPlace

   long         colPersonId;             // tourPerson_personId

   long         colCounter;
   long         colCalories;
   long         colTourDistance;
   float        colBodyWeight;

   long         colTourRecordingTime;
   long         colTourDrivingTime;
   long         colPausedTime;

   long         colAltitudeUp;
   long         colAltitudeDown;
   float        colAltitude_AvgChange;

   float        colMaxSpeed;
   long         colMaxAltitude;
   long         colMaxPulse;

   float        colAvgSpeed;
   float        colAvgPace;
   float        colAvgPulse;
   float        colAvgCadence;

   float        colTemperature_Avg;
   float        colTemperature_Min;
   float        colTemperature_Max;

   int          colWindSpd;
   int          colWindDir;
   String       colClouds;
   int          colRestPulse;

   int          colWeekNo;
   String       colWeekDay;
   int          colWeekYear;

   long         colNumberOfTimeSlices;
   long         colNumberOfPhotos;

   int          colDPTolerance;

   long         colFrontShiftCount;
   long         colRearShiftCount;

   float        colCadenceMultiplier;

   String       colSlowVsFastCadence;
   int          colCadenceZonesDelimiter;

   // ----------- Running Dynamics ---------

   int   colRunDyn_StanceTime_Min;
   int   colRunDyn_StanceTime_Max;
   float colRunDyn_StanceTime_Avg;

   float colRunDyn_StanceTimeBalance_Min;
   float colRunDyn_StanceTimeBalance_Max;
   float colRunDyn_StanceTimeBalance_Avg;

   int   colRunDyn_StepLength_Min;
   int   colRunDyn_StepLength_Max;
   float colRunDyn_StepLength_Avg;

   float colRunDyn_VerticalOscillation_Min;
   float colRunDyn_VerticalOscillation_Max;
   float colRunDyn_VerticalOscillation_Avg;

   float colRunDyn_VerticalRatio_Min;
   float colRunDyn_VerticalRatio_Max;
   float colRunDyn_VerticalRatio_Avg;

   // ----------- POWER ---------

   float colPower_AvgLeftTorqueEffectiveness;
   float colPower_AvgRightTorqueEffectiveness;
   float colPower_AvgLeftPedalSmoothness;
   float colPower_AvgRightPedalSmoothness;
   int   colPower_PedalLeftRightBalance;

   float colPower_Avg;
   int   colPower_Max;
   int   colPower_Normalized;
   long  colPower_TotalWork;

   // ----------- TRAINING ---------

   int   colTraining_FTP;

   float colTraining_TrainingStressScore;
   float colTraining_IntensityFactor;
   float colTraining_PowerToWeight;

   float colTraining_TrainingEffect_Aerob;
   float colTraining_TrainingEffect_Anaerobic;
   float colTraining_TrainingPerformance;

   // ----------- SURFING ---------

   long    col_Surfing_NumberOfEvents;
   short   col_Surfing_MinSpeed_StartStop;
   short   col_Surfing_MinSpeed_Surfing;
   short   col_Surfing_MinTimeDuration;

   boolean col_Surfing_IsMinDistance;
   short   col_Surfing_MinDistance;

   // ----------- IMPORT ---------

   String col_ImportFileName;
   String col_ImportFilePath;
   String col_DeviceName;

   TVITourBookItem(final TourBookView view) {

      tourBookView = view;
   }

   void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

// SET_FORMATTING_OFF

      colTourDistance                  = result.getLong(startIndex + 0);

      colTourRecordingTime             = result.getLong(startIndex + 1);
      colTourDrivingTime               = result.getLong(startIndex + 2);

      colAltitudeUp                    = result.getLong(startIndex + 3);
      colAltitudeDown                  = result.getLong(startIndex + 4);

      // VERY IMPORTANT !
      // Note that we don't do an AVG(avgAltitudeChange) as it would return wrong results.
      // Indeed, we can't do an mean average as we need to do a distance-weighted average.
      colAltitude_AvgChange  = colTourDistance <= 0 ? 0 : (colAltitudeUp + colAltitudeDown) / (colTourDistance / 1000f);

      colCounter                       = result.getLong(startIndex + 5);

      colMaxSpeed                      = result.getFloat(startIndex + 6);

      // compute average speed/pace, prevent divide by 0
      colAvgSpeed                      = colTourDrivingTime == 0 ? 0 : 3.6f * colTourDistance / colTourDrivingTime;
      colAvgPace                       = colTourDistance == 0 ? 0 : colTourDrivingTime * 1000f / colTourDistance;

      colMaxAltitude                   = result.getLong(startIndex + 7);
      colMaxPulse                      = result.getLong(startIndex + 8);

      colAvgPulse                      = result.getFloat(startIndex + 9);
      colAvgCadence                    = result.getFloat(startIndex + 10);
      colTemperature_Avg               = result.getFloat(startIndex + 11);

      colWindDir                       = result.getInt(startIndex + 12);
      colWindSpd                       = result.getInt(startIndex + 13);
      colRestPulse                     = result.getInt(startIndex + 14);

      colCalories                      = result.getLong(startIndex + 15);
      colPower_TotalWork               = result.getLong(startIndex + 16);

      colNumberOfTimeSlices            = result.getLong(startIndex + 17);
      colNumberOfPhotos                = result.getLong(startIndex + 18);

      colFrontShiftCount               = result.getLong(startIndex + 19);
      colRearShiftCount                = result.getLong(startIndex + 20);

      col_Surfing_NumberOfEvents       = result.getLong(startIndex + 21);

      final int cadenceZone_SlowTime   = result.getInt(startIndex + 22);
      final int cadenceZone_FastTime   = result.getInt(startIndex + 23);
      colCadenceZonesDelimiter         = result.getInt(startIndex + 24);

      colTemperature_Min               = result.getFloat(startIndex + 25);
      colTemperature_Max               = result.getFloat(startIndex + 26);

// SET_FORMATTING_ON

      colPausedTime = colTourRecordingTime - colTourDrivingTime;

      colSlowVsFastCadence = TourManager.generateCadenceZones_TimePercentages(cadenceZone_SlowTime, cadenceZone_FastTime);
   }

   protected void fetchTourItems(final PreparedStatement statement) throws SQLException {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

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

            final long dbDistance            = tourItem.colTourDistance = result.getLong(4);
            tourItem.colTourRecordingTime    = result.getLong(5);
            final long dbDrivingTime         = tourItem.colTourDrivingTime = result.getLong(6);
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
            tourItem.colTemperature_Avg      = result.getFloat(19);

            final long dbTourStartTime       = result.getLong(22);
            final String dbTimeZoneId        = result.getString(23);

            tourItem.colWeekNo               = result.getInt(24);
            tourItem.colWeekYear             = result.getInt(25);

            tourItem.colWindDir              = result.getInt(26);
            tourItem.colWindSpd              = result.getInt(27);
            tourItem.colClouds               = result.getString(28);
            tourItem.colRestPulse            = result.getInt(29);

            tourItem.colCalories             = result.getLong(30);
            tourItem.colPersonId             = result.getLong(31);

            tourItem.colNumberOfTimeSlices   = result.getLong(32);
            tourItem.colNumberOfPhotos       = result.getLong(33);
            tourItem.colDPTolerance          = result.getInt(34);

            tourItem.colFrontShiftCount      = result.getLong(35);
            tourItem.colRearShiftCount       = result.getLong(36);

            // ----------------- POWER ------------------

            final float dbAvgPower                          = result.getFloat(37);

            tourItem.colPower_Avg = dbAvgPower;
            tourItem.colPower_Max                           = result.getInt(38);
            tourItem.colPower_Normalized                    = result.getInt(39);
            tourItem.colTraining_FTP                        = result.getInt(40);

            tourItem.colPower_TotalWork                     = result.getLong(41);
            tourItem.colTraining_TrainingStressScore        = result.getFloat(42);
            tourItem.colTraining_IntensityFactor            = result.getFloat(43);

            tourItem.colPower_PedalLeftRightBalance         = result.getInt(44);
            tourItem.colPower_AvgLeftTorqueEffectiveness    = result.getFloat(45);
            tourItem.colPower_AvgRightTorqueEffectiveness   = result.getFloat(46);
            tourItem.colPower_AvgLeftPedalSmoothness        = result.getFloat(47);
            tourItem.colPower_AvgRightPedalSmoothness       = result.getFloat(48);

            final float dbBodyWeight                        = result.getFloat(49);

            // --------------------- IMPORT ------------------

            tourItem.col_ImportFileName                     = result.getString(50);
            tourItem.col_ImportFilePath                     = result.getString(51);

            String dbDeviceName                             = result.getString(52);
            String dbFirmwareVersion                        = result.getString(53);

            // -----------------------------------------------

            final float dbCadenceMultiplier                 = result.getFloat(54);

            // ---------- RUNNING DYNAMICS -------------

            tourItem.colRunDyn_StanceTime_Min               = result.getInt(55);
            tourItem.colRunDyn_StanceTime_Max               = result.getInt(56);
            tourItem.colRunDyn_StanceTime_Avg               = result.getFloat(57);

            tourItem.colRunDyn_StanceTimeBalance_Min        = result.getInt(58)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_StanceTimeBalance_Max        = result.getInt(59)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_StanceTimeBalance_Avg        = result.getFloat(60)   / TourData.RUN_DYN_DATA_MULTIPLIER;

            tourItem.colRunDyn_StepLength_Min               = result.getInt(61);
            tourItem.colRunDyn_StepLength_Max               = result.getInt(62);
            tourItem.colRunDyn_StepLength_Avg               = result.getFloat(63);

            tourItem.colRunDyn_VerticalOscillation_Min      = result.getInt(64)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_VerticalOscillation_Max      = result.getInt(65)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_VerticalOscillation_Avg      = result.getFloat(66)   / TourData.RUN_DYN_DATA_MULTIPLIER;

            tourItem.colRunDyn_VerticalRatio_Min            = result.getInt(67)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_VerticalRatio_Max            = result.getInt(68)     / TourData.RUN_DYN_DATA_MULTIPLIER;
            tourItem.colRunDyn_VerticalRatio_Avg            = result.getFloat(69)   / TourData.RUN_DYN_DATA_MULTIPLIER;

            // ---------- SURFING -------------

            tourItem.col_Surfing_NumberOfEvents             = result.getLong(70);
            tourItem.col_Surfing_MinSpeed_StartStop         = result.getShort(71);
            tourItem.col_Surfing_MinSpeed_Surfing           = result.getShort(72);
            tourItem.col_Surfing_MinTimeDuration            = result.getShort(73);

            tourItem.col_Surfing_IsMinDistance              = result.getBoolean(74);
            tourItem.col_Surfing_MinDistance                = result.getShort(75);

            // ---------- TRAINING -------------

            tourItem.colTraining_TrainingEffect_Aerob       = result.getFloat(76);
            tourItem.colTraining_TrainingEffect_Anaerobic   = result.getFloat(77);
            tourItem.colTraining_TrainingPerformance        = result.getFloat(78);

            // ---------- CADENCE ZONE -------------

            final int cadenceZone_SlowTime                  = result.getInt(79);
            final int cadenceZone_FastTime                  = result.getInt(80);
            tourItem.colCadenceZonesDelimiter               = result.getInt(81);

            // ---------- WEATHER -------------

            tourItem.colTemperature_Min                     = result.getFloat(82);
            tourItem.colTemperature_Max                     = result.getFloat(83);

            // ---------- TOUR START LOCATION -------------

            tourItem.colTourLocation_Start                  = result.getString(84);
            tourItem.colTourLocation_End                    = result.getString(85);

            // -------- AVERAGE ALTITUDE CHANGE -----------

            tourItem.colAltitude_AvgChange                  = result.getLong(86);

// SET_FORMATTING_ON

            // -----------------------------------------------

            tourItem.colBodyWeight = dbBodyWeight;
            tourItem.colTraining_PowerToWeight = dbBodyWeight == 0 ? 0 : dbAvgPower / dbBodyWeight;

            tourItem.colAvgCadence = dbAvgCadence * dbCadenceMultiplier;
            tourItem.colCadenceMultiplier = dbCadenceMultiplier;

            tourItem.colSlowVsFastCadence = TourManager.generateCadenceZones_TimePercentages(cadenceZone_SlowTime, cadenceZone_FastTime);

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
            tourItem.colDateTimeText = TimeTools.Formatter_Date_S.format(tourDateTime.tourZonedDateTime);
            tourItem.colTimeZoneId = dbTimeZoneId;
            tourItem.colWeekDay = tourDateTime.weekDay;

            tourItem.tourTypeId = (tourTypeId == null //
                  ? TourDatabase.ENTITY_IS_NOT_SAVED
                  : (Long) tourTypeId);

            // compute average speed/pace, prevent divide by 0
            tourItem.colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
            tourItem.colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 1000 / dbDistance;

            tourItem.colPausedTime = tourItem.colTourRecordingTime - tourItem.colTourDrivingTime;

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
   }

   @Override
   public Long getTourId() {
      return null;
   }

}
