/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.TourManager;

public abstract class TVITourBookItem extends TreeViewerItem implements ITourItem {

   static ZonedDateTime calendar8 = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   static final char    NL        = net.tourbook.common.UI.NEW_LINE;

   static final String  SQL_SUM_COLUMNS;
   static final String  SQL_SUM_FIELDS;

   static {

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

   int   colPower_FTP;
   float colPower_TrainingStressScore;
   float colPower_IntensityFactor;

   float colPower_PowerToWeight;

   // ----------- TRAINING ---------

   float colTraining_TrainingEffect;
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

   public void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

// SET_FORMATTING_OFF

      colTourDistance      = result.getLong(startIndex + 0);

      colTourRecordingTime = result.getLong(startIndex + 1);
      colTourDrivingTime   = result.getLong(startIndex + 2);

      colAltitudeUp        = result.getLong(startIndex + 3);
      colAltitudeDown      = result.getLong(startIndex + 4);

      // VERY IMPORTANT !
      // Note that we don't do an AVG(avgAltitudeChange) as it would return wrong results.
      // Indeed, we can't do an mean average as we need to do a distance-weighted average.
      colAltitude_AvgChange  = colTourDistance <= 0 ? 0 : (colAltitudeUp + colAltitudeDown) / (colTourDistance / 1000f);

      colCounter           = result.getLong(startIndex + 5);

      colMaxSpeed          = result.getFloat(startIndex + 6);

      // compute average speed/pace, prevent divide by 0
      colAvgSpeed          = colTourDrivingTime == 0 ? 0 : 3.6f * colTourDistance / colTourDrivingTime;
      colAvgPace           = colTourDistance == 0 ? 0 : colTourDrivingTime * 1000f / colTourDistance;

      colMaxAltitude       = result.getLong(startIndex + 7);
      colMaxPulse          = result.getLong(startIndex + 8);

      colAvgPulse          = result.getFloat(startIndex + 9);
      colAvgCadence        = result.getFloat(startIndex + 10);
      colTemperature_Avg   = result.getFloat(startIndex + 11);

      colWindDir           = result.getInt(startIndex + 12);
      colWindSpd           = result.getInt(startIndex + 13);
      colRestPulse         = result.getInt(startIndex + 14);

      colCalories                = result.getLong(startIndex + 15);
      colPower_TotalWork         = result.getLong(startIndex + 16);

      colNumberOfTimeSlices      = result.getLong(startIndex + 17);
      colNumberOfPhotos          = result.getLong(startIndex + 18);

      colFrontShiftCount         = result.getLong(startIndex + 19);
      colRearShiftCount          = result.getLong(startIndex + 20);

      col_Surfing_NumberOfEvents = result.getLong(startIndex + 21);

      final int cadenceZone_SlowTime   = result.getInt(startIndex + 22);
      final int cadenceZone_FastTime   = result.getInt(startIndex + 23);
      colCadenceZonesDelimiter         = result.getInt(startIndex + 24);

      colTemperature_Min         = result.getFloat(startIndex + 25);
      colTemperature_Max         = result.getFloat(startIndex + 26);

// SET_FORMATTING_ON

      colPausedTime = colTourRecordingTime - colTourDrivingTime;

      colSlowVsFastCadence = TourManager.generateCadenceZones_TimePercentages(cadenceZone_SlowTime, cadenceZone_FastTime);
   }

   @Override
   public Long getTourId() {
      return null;
   }

}
