/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.TourManager;
import net.tourbook.weather.WeatherUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public abstract class TVITourBookItem extends TreeViewerItem implements ITourItem {

   private static final String                      SCRAMBLE_FIELD_PREFIX       = "col";                                                          //$NON-NLS-1$

   static ZonedDateTime                             calendar8                   = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   private static ConcurrentHashMap<String, String> _allCached_SqlAllTourFields = new ConcurrentHashMap<>();

   /**
    * All tour fields in the tourbook view, the first field is <code>tourId</code> which can be
    * prefixed with <code>DISTINCT</code>
    */
   private static final String                      SQL_ALL_TOUR_FIELDS;

   public static final int                          SQL_ALL_OTHER_FIELDS__COLUMN_START_NUMBER;

   /**
    * <b>All</b> fields which are used in {@link #SQL_SUM_COLUMNS} <b>MUST be defined in</b>
    * {@link #SQL_SUM_FIELDS}, otherwise the SQL fails
    */
   static final String                              SQL_SUM_COLUMNS;

   /**
    * SQL fields for {@link #SQL_SUM_COLUMNS}, the field ordering is NOT important
    */
   static final String                              SQL_SUM_FIELDS;

   static {

      SQL_ALL_TOUR_FIELDS = UI.EMPTY_STRING

            + "$i_$db_tourID," + NL //                                                 1     //$NON-NLS-1$

            + "$i_$db_startYear," + NL //                                              2     //$NON-NLS-1$
            + "$i_$db_startMonth," + NL //                                             3     //$NON-NLS-1$
            + "$i_$db_startDay," + NL //                                               4     //$NON-NLS-1$
            + "$i_$db_tourDistance," + NL //                                           5     //$NON-NLS-1$
            + "$i_$db_tourDeviceTime_Elapsed," + NL //                                 6     //$NON-NLS-1$
            + "$i_$db_tourComputedTime_Moving," + NL //                                7     //$NON-NLS-1$
            + "$i_$db_tourAltUp," + NL //                                              8     //$NON-NLS-1$
            + "$i_$db_tourAltDown," + NL //                                            9     //$NON-NLS-1$
            + "$i_$db_startDistance," + NL //                                          10    //$NON-NLS-1$
            + "$i_$db_tourType_typeId," + NL //                                        11    //$NON-NLS-1$
            + "$i_$db_tourTitle," + NL //                                              12    //$NON-NLS-1$
            + "$i_$db_deviceTimeInterval," + NL //                                     13    //$NON-NLS-1$
            + "$i_$db_maxSpeed," + NL //                                               14    //$NON-NLS-1$
            + "$i_$db_maxAltitude," + NL //                                            15    //$NON-NLS-1$
            + "$i_$db_maxPulse," + NL //                                               16    //$NON-NLS-1$
            + "$i_$db_avgPulse," + NL //                                               17    //$NON-NLS-1$
            + "$i_$db_avgCadence," + NL //                                             18    //$NON-NLS-1$
            + "$i_$db_weather_Temperature_Average_Device," + NL //                     19    //$NON-NLS-1$

            + "$i_$db_TourStartTime," + NL //                                          20    //$NON-NLS-1$
            + "$i_$db_TimeZoneId, " + NL //                                            21    //$NON-NLS-1$

            + "$i_$db_startWeek," + NL //                                              22    //$NON-NLS-1$
            + "$i_$db_startWeekYear," + NL //                                          23    //$NON-NLS-1$
            //
            + "$i_$db_weather_Wind_Direction," + NL //                                 24    //$NON-NLS-1$
            + "$i_$db_weather_Wind_Speed," + NL //                                     25    //$NON-NLS-1$
            + "$i_$db_weather_Clouds," + NL //                                         26    //$NON-NLS-1$
            //
            + "$i_$db_restPulse," + NL //                                              27    //$NON-NLS-1$
            + "$i_$db_calories," + NL //                                               28    //$NON-NLS-1$
            //
            + "$i_$db_tourPerson_personId," + NL //                                    29    //$NON-NLS-1$
            //
            + "$i_$db_numberOfTimeSlices," + NL //                                     30    //$NON-NLS-1$
            + "$i_$db_numberOfPhotos," + NL //                                         31    //$NON-NLS-1$
            + "$i_$db_dpTolerance," + NL //                                            32    //$NON-NLS-1$
            //
            + "$i_$db_frontShiftCount," + NL //                                        33    //$NON-NLS-1$
            + "$i_$db_rearShiftCount," + NL //                                         34    //$NON-NLS-1$
            //
            // ---------- POWER -------------
            //
            + "$i_$db_power_Avg," + NL //                                              35    //$NON-NLS-1$
            + "$i_$db_power_Max," + NL //                                              36    //$NON-NLS-1$
            + "$i_$db_power_Normalized," + NL //                                       37    //$NON-NLS-1$
            + "$i_$db_power_FTP," + NL //                                              38    //$NON-NLS-1$

            + "$i_$db_power_TotalWork," + NL //                                        39    //$NON-NLS-1$
            + "$i_$db_power_TrainingStressScore," + NL //                              40    //$NON-NLS-1$
            + "$i_$db_power_IntensityFactor," + NL //                                  41    //$NON-NLS-1$

            + "$i_$db_power_PedalLeftRightBalance," + NL //                            42    //$NON-NLS-1$
            + "$i_$db_power_AvgLeftTorqueEffectiveness," + NL //                       43    //$NON-NLS-1$
            + "$i_$db_power_AvgRightTorqueEffectiveness," + NL //                      44    //$NON-NLS-1$
            + "$i_$db_power_AvgLeftPedalSmoothness," + NL //                           45    //$NON-NLS-1$
            + "$i_$db_power_AvgRightPedalSmoothness," + NL //                          46    //$NON-NLS-1$

            + "$i_$db_bodyWeight," + NL //                                             47    //$NON-NLS-1$
            //
            // ---------- IMPORT -------------
            //
            + "$i_$db_tourImportFileName," + NL //                                     48    //$NON-NLS-1$
            + "$i_$db_tourImportFilePath," + NL //                                     49    //$NON-NLS-1$
            + "$i_$db_devicePluginName," + NL //                                       50    //$NON-NLS-1$
            + "$i_$db_deviceFirmwareVersion," + NL //                                  51    //$NON-NLS-1$

            + "$i_$db_cadenceMultiplier," + NL //                                      52    //$NON-NLS-1$

            //
            // ---------- RUNNING DYNAMICS -------------
            //
            + "$i_$db_runDyn_StanceTime_Min," + NL //                                  53    //$NON-NLS-1$
            + "$i_$db_runDyn_StanceTime_Max," + NL //                                  54    //$NON-NLS-1$
            + "$i_$db_runDyn_StanceTime_Avg," + NL //                                  55    //$NON-NLS-1$

            + "$i_$db_runDyn_StanceTimeBalance_Min," + NL //                           56    //$NON-NLS-1$
            + "$i_$db_runDyn_StanceTimeBalance_Max," + NL //                           57    //$NON-NLS-1$
            + "$i_$db_runDyn_StanceTimeBalance_Avg," + NL //                           58    //$NON-NLS-1$

            + "$i_$db_runDyn_StepLength_Min," + NL //                                  59    //$NON-NLS-1$
            + "$i_$db_runDyn_StepLength_Max," + NL //                                  60    //$NON-NLS-1$
            + "$i_$db_runDyn_StepLength_Avg," + NL //                                  61    //$NON-NLS-1$

            + "$i_$db_runDyn_VerticalOscillation_Min," + NL //                         62    //$NON-NLS-1$
            + "$i_$db_runDyn_VerticalOscillation_Max," + NL //                         63    //$NON-NLS-1$
            + "$i_$db_runDyn_VerticalOscillation_Avg," + NL //                         64    //$NON-NLS-1$

            + "$i_$db_runDyn_VerticalRatio_Min," + NL //                               65    //$NON-NLS-1$
            + "$i_$db_runDyn_VerticalRatio_Max," + NL //                               66    //$NON-NLS-1$
            + "$i_$db_runDyn_VerticalRatio_Avg," + NL //                               67    //$NON-NLS-1$

            //
            // ---------- SURFING -------------
            //
            + "$i_$db_surfing_NumberOfEvents," + NL //                                 68    //$NON-NLS-1$
            + "$i_$db_surfing_MinSpeed_StartStop," + NL //                             69    //$NON-NLS-1$
            + "$i_$db_surfing_MinSpeed_Surfing," + NL //                               70    //$NON-NLS-1$
            + "$i_$db_surfing_MinTimeDuration," + NL //                                71    //$NON-NLS-1$
            + "$i_$db_surfing_IsMinDistance," + NL //                                  72    //$NON-NLS-1$
            + "$i_$db_surfing_MinDistance," + NL //                                    73    //$NON-NLS-1$

            //
            // ---------- TRAINING -------------
            //
            + "$i_$db_training_TrainingEffect_Aerob," + NL //                          74    //$NON-NLS-1$
            + "$i_$db_training_TrainingEffect_Anaerob," + NL //                        75    //$NON-NLS-1$
            + "$i_$db_training_TrainingPerformance," + NL //                           76    //$NON-NLS-1$

            // ---------- CADENCE ZONE -------------

            + "$i_$db_cadenceZone_SlowTime," + NL //                                   77    //$NON-NLS-1$
            + "$i_$db_cadenceZone_FastTime," + NL //                                   78    //$NON-NLS-1$
            + "$i_$db_cadenceZones_DelimiterValue," + NL //                            79    //$NON-NLS-1$

            // ---------- WEATHER -------------
            + "$i_$db_weather_Temperature_Min_Device," + NL //                         80    //$NON-NLS-1$
            + "$i_$db_weather_Temperature_Max_Device," + NL //                         81    //$NON-NLS-1$
            + "$i_$db_temperatureScale," + NL //                                       82    //$NON-NLS-1$

            // ---------- TOUR START LOCATION -------------
            + "$i_$db_tourStartPlace," + NL //                                         83    //$NON-NLS-1$
            + "$i_$db_tourEndPlace," + NL //                                           84    //$NON-NLS-1$

            // -------- AVERAGE ALTITUDE CHANGE -----------
            + "$i_$db_avgAltitudeChange," + NL //                                      85    //$NON-NLS-1$

            // -------- TIME -----------
            + "$i_$db_tourDeviceTime_Recorded," + NL //                                86    //$NON-NLS-1$
            + "$i_$db_tourDeviceTime_Paused," + NL //                                  87    //$NON-NLS-1$

            // computed break time
            + "$i_($db_tourDeviceTime_Elapsed - $db_tourComputedTime_Moving)," + NL // 88    //$NON-NLS-1$

            // -------- BATTERY -----------
            + "$i_$db_Battery_Percentage_Start," + NL //                               89    //$NON-NLS-1$
            + "$i_$db_Battery_Percentage_End," + NL //                                 90    //$NON-NLS-1$

            // -------- WEATHER -----------
            + "$i_$db_weather_Temperature_Average," + NL //                            91    //$NON-NLS-1$
            + "$i_$db_weather_Temperature_Max," + NL //                                92    //$NON-NLS-1$
            + "$i_$db_weather_Temperature_Min," + NL //                                93    //$NON-NLS-1$
            + "$i_$db_weather_AirQuality," + NL //                                     94    //$NON-NLS-1$

            + "$i_$db_tourLocationStart_LocationID," + NL //                           95    //$NON-NLS-1$
            + "$i_$db_tourLocationEnd_LocationID," + NL //                             96    //$NON-NLS-1$

            + "$i_$db_hasGeoData," + NL //                                             97    //$NON-NLS-1$

            // this shortened field needs an alias otherwise the flat tour book view do not work !!!
            + "$i_SUBSTR($db_TourDescription, 1, 100) AS TourDescription," + NL //     98    //$NON-NLS-1$

            // -------- RADAR -----------
            + "$i_$db_numberOfPassedVehicles " + NL //                                 99    //$NON-NLS-1$
      ;

      SQL_ALL_OTHER_FIELDS__COLUMN_START_NUMBER = 100;

      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      //
      // !!! EXTREEMLY IMPORTANT !!!
      //
      // Adjust constant SQL_ALL_OTHER_FIELDS__COLUMN_START_NUMBER when sql fields are added,
      // otherwise tags and number of markers are not displayed or causing an exception
      //
      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

      SQL_SUM_FIELDS = UI.EMPTY_STRING

            + "TourDistance," + NL //                                      //$NON-NLS-1$
            + "TourDeviceTime_Elapsed," + NL //                            //$NON-NLS-1$
            + "TourComputedTime_Moving," + NL //                           //$NON-NLS-1$
            + "TourAltUp," + NL //                                         //$NON-NLS-1$
            + "TourAltDown," + NL //                                       //$NON-NLS-1$

            + "MaxAltitude," + NL //                                       //$NON-NLS-1$
            + "MaxPulse," + NL //                                          //$NON-NLS-1$
            + "MaxSpeed," + NL //                                          //$NON-NLS-1$

            + "AvgCadence," + NL //                                        //$NON-NLS-1$
            + "AvgPulse," + NL //                                          //$NON-NLS-1$

            + "CadenceMultiplier," + NL //                                 //$NON-NLS-1$
            + "TemperatureScale," + NL //                                  //$NON-NLS-1$

            + "Calories," + NL //                                          //$NON-NLS-1$
            + "RestPulse," + NL //                                         //$NON-NLS-1$

            + "Power_TotalWork," + NL //                                   //$NON-NLS-1$

            + "NumberOfTimeSlices," + NL //                                //$NON-NLS-1$
            + "NumberOfPhotos," + NL //                                    //$NON-NLS-1$

            + "FrontShiftCount," + NL //                                   //$NON-NLS-1$
            + "RearShiftCount," + NL //                                    //$NON-NLS-1$

            + "surfing_NumberOfEvents," + NL //                            //$NON-NLS-1$

            + "cadenceZone_SlowTime," + NL //                              //$NON-NLS-1$
            + "cadenceZone_FastTime," + NL //                              //$NON-NLS-1$
            + "cadenceZones_DelimiterValue," + NL //                       //$NON-NLS-1$

            + "Weather_Temperature_Average," + NL //                       //$NON-NLS-1$
            + "Weather_Temperature_Average_Device," + NL //                //$NON-NLS-1$
            + "Weather_Temperature_Min_Device," + NL //                    //$NON-NLS-1$
            + "Weather_Temperature_Max_Device," + NL //                    //$NON-NLS-1$
            + "Weather_Wind_Direction," + NL //                            //$NON-NLS-1$
            + "Weather_Wind_Speed," + NL //                                //$NON-NLS-1$

            + "tourDeviceTime_Recorded," + NL //                           //$NON-NLS-1$
            + "tourDeviceTime_Paused," + NL //                             //$NON-NLS-1$

            + "numberOfPassedVehicles" + NL //                             //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "SUM( CAST(TourDistance AS BIGINT))," + NL //             0  //$NON-NLS-1$
            + "SUM( CAST(TourDeviceTime_Elapsed AS BIGINT))," + NL //   1  //$NON-NLS-1$
            + "SUM( CAST(TourComputedTime_Moving AS BIGINT))," + NL //  2  //$NON-NLS-1$
            + "SUM( CAST(TourAltUp AS BIGINT))," + NL //                3  //$NON-NLS-1$
            + "SUM( CAST(TourAltDown AS BIGINT))," + NL //              4  //$NON-NLS-1$

            + "SUM(1)," + NL //                                         5  //$NON-NLS-1$
            //
            + "MAX(MaxSpeed)," + NL //                                  6  //$NON-NLS-1$
            + "MAX(MaxAltitude)," + NL //                               7  //$NON-NLS-1$
            + "MAX(MaxPulse)," + NL //                                  8  //$NON-NLS-1$
            //
            + "AVG( CASE WHEN AvgPulse = 0               THEN NULL ELSE AvgPulse END)," + NL //                                     9     //$NON-NLS-1$
            + "AVG( CASE WHEN AvgCadence = 0             THEN NULL ELSE DOUBLE(AvgCadence) * CadenceMultiplier END)," + NL //       10    //$NON-NLS-1$
            + "AVG( CASE WHEN weather_Temperature_Average_Device = 0 THEN NULL ELSE DOUBLE(weather_Temperature_Average_Device) / TemperatureScale END)," //$NON-NLS-1$
            + NL //    11
            + "AVG( CASE WHEN Weather_Wind_Direction = 0 THEN NULL ELSE Weather_Wind_Direction END)," + NL //                       12    //$NON-NLS-1$
            + "AVG( CASE WHEN Weather_Wind_Speed = 0     THEN NULL ELSE Weather_Wind_Speed END)," + NL //                           13    //$NON-NLS-1$
            + "AVG( CASE WHEN RestPulse = 0              THEN NULL ELSE RestPulse END)," + NL //                                    14    //$NON-NLS-1$
            //
            + "SUM( CAST(Calories AS BIGINT))," + NL //                 15 //$NON-NLS-1$
            + "SUM( CAST(Power_TotalWork AS BIGINT))," + NL //          16 //$NON-NLS-1$

            + "SUM( CAST(NumberOfTimeSlices AS BIGINT))," + NL //       17 //$NON-NLS-1$
            + "SUM( CAST(NumberOfPhotos AS BIGINT))," + NL //           18 //$NON-NLS-1$
            //
            + "SUM( CAST(FrontShiftCount AS BIGINT))," + NL //          19 //$NON-NLS-1$
            + "SUM( CAST(RearShiftCount AS BIGINT))," + NL //           20 //$NON-NLS-1$

            + "SUM( CAST(Surfing_NumberOfEvents AS BIGINT))," + NL //   21 //$NON-NLS-1$

            + "SUM( CAST(cadenceZone_SlowTime AS BIGINT))," + NL //     22 //$NON-NLS-1$
            + "SUM( CAST(cadenceZone_FastTime AS BIGINT))," + NL //     23 //$NON-NLS-1$
            + "AVG( CASE WHEN cadenceZones_DelimiterValue = 0 THEN NULL ELSE cadenceZones_DelimiterValue END)," + NL //       24 //$NON-NLS-1$

            + "MIN( CASE WHEN weather_Temperature_Min_Device = 0 THEN NULL ELSE weather_Temperature_Min_Device END)," + NL // 25 //$NON-NLS-1$
            + "MAX( CASE WHEN weather_Temperature_Max_Device = 0 THEN NULL ELSE weather_Temperature_Max_Device END)," + NL // 26 //$NON-NLS-1$

            + "SUM( CAST(tourDeviceTime_Recorded AS BIGINT))," + NL //  27 //$NON-NLS-1$
            + "SUM( CAST(tourDeviceTime_Paused AS BIGINT))," + NL //    28 //$NON-NLS-1$

            + "AVG( CASE WHEN weather_Temperature_Average = 0   THEN NULL ELSE DOUBLE(weather_Temperature_Average) / TemperatureScale END)," + NL // 29 //$NON-NLS-1$

            + "SUM( CAST(numberOfPassedVehicles AS BIGINT))" + NL //    30 //$NON-NLS-1$
      ;

   }

   protected static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   //
   //
   TourBookView tourBookView;

   String       treeColumn;

   int          tourYear;

   /**
    * Month starts with 1 for January
    */
   int          tourMonth;
   //
   int          tourYearSub;
   int          tourDay;
   //
   /**
    * Contains the tour date time with time zone info when available
    */
   TourDateTime colTourDateTime;
   String       colTimeZoneId;
   //
   String       colTourTitle;

   /**
    * Tour description which is truncated to 100 characters
    */
   String       colTourDescription;
   //
   // ----------- TOUR LOCATION ---------
   //
   public String colTourLocation_Start;        // db name: tourStartPlace
   public String colTourLocation_End;          // db name: tourEndPlace
   Object        colTourLocationID_Start;
   Object        colTourLocationID_End;

   long          colPersonId;                  // tourPerson_personId
   //
   long          colCounter;
   //
   long          colCalories;
   long          colTourDistance;
   float         colBodyWeight;
   int           colRestPulse;
   //
   long          colTourDeviceTime_Elapsed;
   long          colTourDeviceTime_Recorded;
   long          colTourComputedTime_Moving;
   long          colTourDeviceTime_Paused;
   long          colTourComputedTime_Break;
   //
   long          colAltitudeUp;
   long          colAltitudeDown;
   float         colAltitude_AvgChange;
   //
   float         colMaxSpeed;
   long          colMaxAltitude;
   long          colMaxPulse;
   //
   float         colAvgSpeed;
   float         colAvgPace;
   float         colAvgPulse;
   float         colAvgCadence;
   //
   float         colTemperature_Average;
   float         colTemperature_Min;
   float         colTemperature_Max;
   float         colTemperature_Average_Device;
   float         colTemperature_Min_Device;
   float         colTemperature_Max_Device;
   //
   int           colWindSpeed;
   int           colWindDirection;
   String        colClouds;
   int           colAirQualityIndex;
   //
   int           colWeekNo;
   String        colWeekDay;
   int           colWeekYear;
   //
   long          colNumberOfTimeSlices;
   long          colNumberOfPhotos;
   //
   int           colDPTolerance;
   //
   long          colFrontShiftCount;
   long          colRearShiftCount;
   //
   float         colCadenceMultiplier;
   String        colSlowVsFastCadence;

   int           colCadenceZonesDelimiter;
   //
   // ----------- Radar ---------
   //
   int colRadar_PassedVehicles;
   //
   // ----------- Running Dynamics ---------
   //
   int   colRunDyn_StanceTime_Min;
   int   colRunDyn_StanceTime_Max;
   float colRunDyn_StanceTime_Avg;
   float colRunDyn_StanceTimeBalance_Min;
   float colRunDyn_StanceTimeBalance_Max;
   float colRunDyn_StanceTimeBalance_Avg;
   //
   int   colRunDyn_StepLength_Min;
   int   colRunDyn_StepLength_Max;
   float colRunDyn_StepLength_Avg;
   //
   float colRunDyn_VerticalOscillation_Min;
   float colRunDyn_VerticalOscillation_Max;
   float colRunDyn_VerticalOscillation_Avg;
   //
   float colRunDyn_VerticalRatio_Min;
   float colRunDyn_VerticalRatio_Max;
   float colRunDyn_VerticalRatio_Avg;
   //
   // ----------- POWER ---------
   //
   float colPower_AvgLeftTorqueEffectiveness;
   float colPower_AvgRightTorqueEffectiveness;
   float colPower_AvgLeftPedalSmoothness;
   float colPower_AvgRightPedalSmoothness;
   int   colPower_PedalLeftRightBalance;
   //
   float colPower_Avg;
   int   colPower_Max;
   int   colPower_Normalized;
   long  colPower_TotalWork;
   //
   // ----------- TRAINING ---------
   //
   int   colTraining_FTP;
   float colTraining_TrainingStressScore;
   //
   float colTraining_IntensityFactor;
   float colTraining_PowerToWeight;
   //
   float colTraining_TrainingEffect_Aerob;
   float colTraining_TrainingEffect_Anaerobic;
   float colTraining_TrainingPerformance;
   //
   // ----------- SURFING ---------
   //
   long    col_Surfing_NumberOfEvents;
   short   col_Surfing_MinSpeed_StartStop;
   short   col_Surfing_MinSpeed_Surfing;
   short   col_Surfing_MinTimeDuration;
   boolean col_Surfing_IsMinDistance;
   short   col_Surfing_MinDistance;
   //
   // ----------- IMPORT ---------
   //
   String col_ImportFileName;
   String col_ImportFilePath;
   //
   // ----------- DEVICE ---------
   //
   short   colBatterySoC_Start;
   short   colBatterySoC_End;
   String  colDeviceName;
   //
   boolean colHasGeoData;

   //
   TVITourBookItem(final TourBookView view) {

      tourBookView = view;
   }

   /**
    * Append a db prefix to all fields and additional indent it
    *
    * @param dbPrefix
    * @param indent
    *
    * @return
    */
   public static String getSQL_ALL_TOUR_FIELDS(final String dbPrefix, final int indent) {

      final String key = dbPrefix + UI.SYMBOL_UNDERSCORE + Integer.toString(indent);

      final String cachedSqlFields = _allCached_SqlAllTourFields.get(key);

      if (cachedSqlFields != null) {
         return cachedSqlFields;
      }

      final StringBuilder sbIndent = new StringBuilder();
      for (int i = 0; i < indent; i++) {
         sbIndent.append(UI.SPACE);
      }

      String dbPrefixReplaced = UI.EMPTY_STRING;
      if (dbPrefix.length() > 0) {
         dbPrefixReplaced = dbPrefix + UI.SYMBOL_DOT;
      }

      String sqlReplaced = SQL_ALL_TOUR_FIELDS;

      sqlReplaced = sqlReplaced.replaceAll("\\$i_", sbIndent.toString());
      sqlReplaced = sqlReplaced.replaceAll("\\$db_", dbPrefixReplaced);

      _allCached_SqlAllTourFields.put(key, sqlReplaced);

      return sqlReplaced;
   }

   /**
    * Read tour data fields into {@code tourItem} from a resultset which is created with these
    * fields {@link #SQL_ALL_TOUR_FIELDS}
    *
    * @param result
    * @param tourItem
    *
    * @return
    *
    * @throws SQLException
    */
   public static TVITourBookTour getTourDataFields(final ResultSet result,
                                                   final TVITourBookTour tourItem) throws SQLException {

// SET_FORMATTING_OFF

      final int dbYear                    = result.getInt(2);
      final int dbMonth                   = result.getInt(3);
      final int dbDay                     = result.getInt(4);

      tourItem.treeColumn                 = Integer.toString(dbDay);
      tourItem.tourYear                   = dbYear;
      tourItem.tourMonth                  = dbMonth;
      tourItem.tourDay                    = dbDay;

      tourItem.colTourDistance            = result.getLong(5);
      tourItem.colTourDeviceTime_Elapsed  = result.getLong(6);
      tourItem.colTourComputedTime_Moving = result.getLong(7);
      tourItem.colAltitudeUp              = result.getLong(8);
      tourItem.colAltitudeDown            = result.getLong(9);

      tourItem.colStartDistance           = result.getLong(10);
      final Object tourTypeId             = result.getObject(11);
      tourItem.colTourTitle               = result.getString(12);
      tourItem.colTimeInterval            = result.getShort(13);

      tourItem.colMaxSpeed                = result.getFloat(14);
      tourItem.colMaxAltitude             = result.getLong(15);
      tourItem.colMaxPulse                = result.getLong(16);
      tourItem.colAvgPulse                = result.getFloat(17);
      final float dbAvgCadence            = result.getFloat(18);
      final float dbAvgTemperature_Device = result.getFloat(19);

      final long dbTourStartTime          = result.getLong(20);
      final String dbTimeZoneId           = result.getString(21);

      tourItem.colWeekNo                  = result.getInt(22);
      tourItem.colWeekYear                = result.getInt(23);

      tourItem.colWindDirection           = result.getInt(24);
      tourItem.colWindSpeed               = result.getInt(25);
      tourItem.colClouds                  = result.getString(26);
      tourItem.colRestPulse               = result.getInt(27);

      tourItem.colCalories                = result.getLong(28);
      tourItem.colPersonId                = result.getLong(29);

      tourItem.colNumberOfTimeSlices      = result.getLong(30);
      tourItem.colNumberOfPhotos          = result.getLong(31);
      tourItem.colDPTolerance             = result.getInt(32);

      tourItem.colFrontShiftCount         = result.getLong(33);
      tourItem.colRearShiftCount          = result.getLong(34);

      // ----------------- POWER ------------------

      final float dbAvgPower                          = result.getFloat(35);

      tourItem.colPower_Avg = dbAvgPower;
      tourItem.colPower_Max                           = result.getInt(36);
      tourItem.colPower_Normalized                    = result.getInt(37);
      tourItem.colTraining_FTP                        = result.getInt(38);

      tourItem.colPower_TotalWork                     = result.getLong(39);
      tourItem.colTraining_TrainingStressScore        = result.getFloat(40);
      tourItem.colTraining_IntensityFactor            = result.getFloat(41);

      tourItem.colPower_PedalLeftRightBalance         = result.getInt(42);
      tourItem.colPower_AvgLeftTorqueEffectiveness    = result.getFloat(43);
      tourItem.colPower_AvgRightTorqueEffectiveness   = result.getFloat(44);
      tourItem.colPower_AvgLeftPedalSmoothness        = result.getFloat(45);
      tourItem.colPower_AvgRightPedalSmoothness       = result.getFloat(46);

      final float dbBodyWeight                        = result.getFloat(47);

      // --------------------- IMPORT ------------------

      tourItem.col_ImportFileName                     = result.getString(48);
      tourItem.col_ImportFilePath                     = result.getString(49);

      String dbDeviceName                             = result.getString(50);
      String dbFirmwareVersion                        = result.getString(51);

      // -----------------------------------------------

      final float dbCadenceMultiplier                 = result.getFloat(52);

      // ---------- RUNNING DYNAMICS -------------

      tourItem.colRunDyn_StanceTime_Min               = result.getInt(53);
      tourItem.colRunDyn_StanceTime_Max               = result.getInt(54);
      tourItem.colRunDyn_StanceTime_Avg               = result.getFloat(55);

      tourItem.colRunDyn_StanceTimeBalance_Min        = result.getInt(56)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_StanceTimeBalance_Max        = result.getInt(57)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_StanceTimeBalance_Avg        = result.getFloat(58)   / TourData.RUN_DYN_DATA_MULTIPLIER;

      tourItem.colRunDyn_StepLength_Min               = result.getInt(59);
      tourItem.colRunDyn_StepLength_Max               = result.getInt(60);
      tourItem.colRunDyn_StepLength_Avg               = result.getFloat(61);

      tourItem.colRunDyn_VerticalOscillation_Min      = result.getInt(62)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_VerticalOscillation_Max      = result.getInt(63)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_VerticalOscillation_Avg      = result.getFloat(64)   / TourData.RUN_DYN_DATA_MULTIPLIER;

      tourItem.colRunDyn_VerticalRatio_Min            = result.getInt(65)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_VerticalRatio_Max            = result.getInt(66)     / TourData.RUN_DYN_DATA_MULTIPLIER;
      tourItem.colRunDyn_VerticalRatio_Avg            = result.getFloat(67)   / TourData.RUN_DYN_DATA_MULTIPLIER;

      // ---------- SURFING -------------

      tourItem.col_Surfing_NumberOfEvents             = result.getLong(68);
      tourItem.col_Surfing_MinSpeed_StartStop         = result.getShort(69);
      tourItem.col_Surfing_MinSpeed_Surfing           = result.getShort(70);
      tourItem.col_Surfing_MinTimeDuration            = result.getShort(71);

      tourItem.col_Surfing_IsMinDistance              = result.getBoolean(72);
      tourItem.col_Surfing_MinDistance                = result.getShort(73);

      // ---------- TRAINING -------------

      tourItem.colTraining_TrainingEffect_Aerob       = result.getFloat(74);
      tourItem.colTraining_TrainingEffect_Anaerobic   = result.getFloat(75);
      tourItem.colTraining_TrainingPerformance        = result.getFloat(76);

      // ---------- CADENCE ZONE -------------

      final int cadenceZone_SlowTime                  = result.getInt(77);
      final int cadenceZone_FastTime                  = result.getInt(78);
      tourItem.colCadenceZonesDelimiter               = result.getInt(79);

      // ---------- WEATHER -------------

      tourItem.colTemperature_Min_Device              = result.getFloat(80);
      tourItem.colTemperature_Max_Device              = result.getFloat(81);
      final int dbTemperatureScale                    = result.getInt(82);

      // ---------- TOUR START LOCATION -------------

      tourItem.colTourLocation_Start                  = result.getString(83);
      tourItem.colTourLocation_End                    = result.getString(84);

      // -------- AVERAGE ALTITUDE CHANGE -----------

      tourItem.colAltitude_AvgChange                  = result.getLong(85);

      // -------- TIME -----------

      tourItem.colTourDeviceTime_Recorded             = result.getLong(86);
      tourItem.colTourDeviceTime_Paused               = result.getLong(87);
      tourItem.colTourComputedTime_Break              = result.getLong(88);

      // -------- BATTERY -----------

      tourItem.colBatterySoC_Start                    = result.getShort(89);
      tourItem.colBatterySoC_End                      = result.getShort(90);

      // -------- WEATHER -----------

      final float dbAvgTemperature                    = result.getFloat(91);
      tourItem.colTemperature_Max                     = result.getFloat(92);
      tourItem.colTemperature_Min                     = result.getFloat(93);
      final String dbAirQuality                       = result.getString(94);

      // -------- TOUR LOCATIONS -----------

      tourItem.colTourLocationID_Start                = result.getObject(95);
      tourItem.colTourLocationID_End                  = result.getObject(96);

      // -------- GEO DATA -----------

      tourItem.colHasGeoData                          = result.getBoolean(97);

      // -------- TOUR -----------

      tourItem.colTourDescription                     = result.getString(98);

      // -------- RADAR -----------

      tourItem.colRadar_PassedVehicles                = result.getInt(99);


      // -----------------------------------------------


      tourItem.colBodyWeight                    = dbBodyWeight;
      tourItem.colTraining_PowerToWeight        = dbBodyWeight == 0 ? 0 : dbAvgPower / dbBodyWeight;

      tourItem.colAvgCadence                    = dbAvgCadence * dbCadenceMultiplier;
      tourItem.colCadenceMultiplier             = dbCadenceMultiplier;

      tourItem.colSlowVsFastCadence             = TourManager.generateCadenceZones_TimePercentages(cadenceZone_SlowTime, cadenceZone_FastTime);

      tourItem.colTemperature_Average_Device    = dbAvgTemperature_Device / dbTemperatureScale;
      tourItem.colTemperature_Average           = dbAvgTemperature / dbTemperatureScale;

      tourItem.colAirQualityIndex               = WeatherUtils.getWeather_AirQuality_TextIndex(dbAirQuality);

// SET_FORMATTING_ON

      // -----------------------------------------------

      dbDeviceName = dbDeviceName == null ? UI.EMPTY_STRING : dbDeviceName;
      dbFirmwareVersion = dbFirmwareVersion == null ? UI.EMPTY_STRING : dbFirmwareVersion;

      final String deviceName = dbFirmwareVersion.length() == 0
            ? dbDeviceName
            : dbDeviceName
                  + UI.SPACE
                  + UI.SYMBOL_BRACKET_LEFT
                  + dbFirmwareVersion
                  + UI.SYMBOL_BRACKET_RIGHT;

      tourItem.colDeviceName = deviceName;

      // -----------------------------------------------

      final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbTourStartTime, dbTimeZoneId);

      tourItem.colTourDateTime = tourDateTime;
      tourItem.colDateTime_MS = TimeTools.toEpochMilli(tourDateTime.tourZonedDateTime);
      tourItem.colDateTime_Text = TimeTools.Formatter_Date_S.format(tourDateTime.tourZonedDateTime);
      tourItem.colTimeZoneId = dbTimeZoneId;
      tourItem.colWeekDay = tourDateTime.weekDay;

      tourItem.tourTypeId = (tourTypeId == null
            ? TourDatabase.ENTITY_IS_NOT_SAVED
            : (Long) tourTypeId);

      // compute average speed/pace, prevent divide by 0
      final long dbDistance = tourItem.colTourDistance;
      final long dbRecordedTime = tourItem.colTourDeviceTime_Recorded;
      final long dbMovingTime = tourItem.colTourComputedTime_Moving;
      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long time = isPaceAndSpeedFromRecordedTime ? dbRecordedTime : dbMovingTime;
      tourItem.colAvgSpeed = time == 0 ? 0 : 3.6f * dbDistance / time;
      tourItem.colAvgPace = dbDistance == 0 ? 0 : time * 1000f / dbDistance;

      if (UI.IS_SCRAMBLE_DATA) {

         tourItem.scrambleData();

         tourItem.treeColumn = UI.scrambleText(tourItem.treeColumn);
      }

      return tourItem;
   }

   void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

// SET_FORMATTING_OFF

      colTourDistance                  = result.getLong(startIndex + 0);

      colTourDeviceTime_Elapsed        = result.getLong(startIndex + 1);
      colTourComputedTime_Moving       = result.getLong(startIndex + 2);

      colAltitudeUp                    = result.getLong(startIndex + 3);
      colAltitudeDown                  = result.getLong(startIndex + 4);

      // VERY IMPORTANT !
      // Note that we don't do an AVG(avgAltitudeChange) as it would return wrong results.
      // Indeed, we can't do a mean average as we need to do a distance-weighted average.
      colAltitude_AvgChange            = UI.computeAverageElevationChange(colAltitudeUp + colAltitudeDown, colTourDistance);

      colCounter                       = result.getLong(startIndex + 5);

      colMaxSpeed                      = result.getFloat(startIndex + 6);

      // compute average speed/pace, prevent divide by 0
      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long timeField = isPaceAndSpeedFromRecordedTime
            ? colTourDeviceTime_Recorded
            : colTourComputedTime_Moving;
      colAvgSpeed                      = timeField       == 0 ? 0 : 3.6f * colTourDistance / timeField;
      colAvgPace                       = colTourDistance == 0 ? 0 : timeField * 1000f / colTourDistance;

      colMaxAltitude                   = result.getLong(startIndex + 7);
      colMaxPulse                      = result.getLong(startIndex + 8);

      colAvgPulse                      = result.getFloat(startIndex + 9);
      colAvgCadence                    = result.getFloat(startIndex + 10);
      colTemperature_Average_Device    = result.getFloat(startIndex + 11);

      colWindDirection                 = result.getInt(startIndex + 12);
      colWindSpeed                     = result.getInt(startIndex + 13);
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

      colTemperature_Min_Device        = result.getFloat(startIndex + 25);
      colTemperature_Max_Device        = result.getFloat(startIndex + 26);

      colTourDeviceTime_Recorded       = result.getLong(startIndex + 27);
      colTourDeviceTime_Paused         = result.getLong(startIndex + 28);

      colTemperature_Average           = result.getFloat(startIndex + 29);

      colRadar_PassedVehicles          = result.getInt(startIndex + 30);

      colTourDeviceTime_Paused         = colTourDeviceTime_Elapsed - colTourDeviceTime_Recorded;
      colTourComputedTime_Break        = colTourDeviceTime_Elapsed - colTourComputedTime_Moving;

      colSlowVsFastCadence             = TourManager.generateCadenceZones_TimePercentages(
                                                cadenceZone_SlowTime,
                                                cadenceZone_FastTime);

// SET_FORMATTING_ON
   }

   @Override
   public void clearChildren() {

      // cleanup
      tourBookView = null;

// disabled because object compare depends on it
//      colTourDateTime = null;

      super.clearChildren();
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

      if (this instanceof final TVITourBookTour tviTourBookTour && obj instanceof final TVITourBookTour objTviTourBookTour) {

         // cloned tours can have all the same data except the tour ID

         final TVITourBookTour thisTour = tviTourBookTour;
         final TVITourBookTour otherTour = objTviTourBookTour;

         if (thisTour.tourId != otherTour.tourId) {
            return false;
         }
      }

      final TVITourBookItem other = (TVITourBookItem) obj;
      if (colTourDateTime == null) {
         if (other.colTourDateTime != null) {
            return false;
         }
      } else if (!colTourDateTime.equals(other.colTourDateTime)) {
         return false;
      }

      return true;
   }

   protected void fetchTourItems(final PreparedStatement statement) throws SQLException {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      long prevTourId = -1;

      HashSet<Long> tagIds = null;
      HashSet<Long> markerIds = null;
      HashSet<Long> nutritionProductIds = null;
      HashSet<Long> allEquipmentIDs = null;

      final ResultSet result = statement.executeQuery();
      while (result.next()) {

         final long result_TourId = result.getLong(1);

         final int columnStartNumber = TVITourBookItem.SQL_ALL_OTHER_FIELDS__COLUMN_START_NUMBER;

// SET_FORMATTING_OFF

         final Object result_TagId              = result.getObject(columnStartNumber);
         final Object result_MarkerId           = result.getObject(columnStartNumber + 1);
         final Object result_NutritionProductId = result.getObject(columnStartNumber + 2);
         final Object result_EquipmentID        = result.getObject(columnStartNumber + 3);

// SET_FORMATTING_ON

         if (result_TourId == prevTourId) {

            // these are additional result set's for the same tour

            // get tags from outer join
            if (result_TagId instanceof final Long tagId) {
               tagIds.add(tagId);
            }

            // get markers from outer join
            if (result_MarkerId instanceof final Long markerId) {
               markerIds.add(markerId);
            }

            // get nutrition products from outer join
            if (result_NutritionProductId instanceof final Long nutritionProductId) {
               nutritionProductIds.add(nutritionProductId);
            }

            // get equipment from outer join
            if (result_EquipmentID instanceof final Long equipmentID) {
               allEquipmentIDs.add(equipmentID);
            }

         } else {

            // first resultset for a new tour

            final TVITourBookTour tourItem = new TVITourBookTour(tourBookView, this);

            tourItem.tourId = result_TourId;
            tourItem.tourYearSub = tourYearSub;

            getTourDataFields(result, tourItem);

            if (UI.IS_SCRAMBLE_DATA) {
               tourItem.tourYearSub = UI.scrambleNumbers(tourItem.tourYearSub);
            }

            children.add(tourItem);

            // get first tag id
            if (result_TagId instanceof final Long tagId) {

               tagIds = new HashSet<>();
               tagIds.add(tagId);

               tourItem.setTagIds(tagIds);
            }

            // get first marker id
            if (result_MarkerId instanceof final Long markerId) {

               markerIds = new HashSet<>();
               markerIds.add(markerId);

               tourItem.setMarkerIds(markerIds);
            }

            // get first nutrition product id
            if (result_NutritionProductId instanceof final Long nutritionProductId) {

               nutritionProductIds = new HashSet<>();
               nutritionProductIds.add(nutritionProductId);

               tourItem.setNutritionProductsIds(nutritionProductIds);
            }

            // get first equipment id
            if (result_EquipmentID instanceof final Long equipmentID) {

               allEquipmentIDs = new HashSet<>();
               allEquipmentIDs.add(equipmentID);

               tourItem.setEquipmentIDs(allEquipmentIDs);
            }
         }

         prevTourId = result_TourId;
      }
   }

   @Override
   public Long getTourId() {
      return null;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((colTourDateTime == null) ? 0 : colTourDateTime.hashCode());
      return result;
   }

   /**
    * Scramble all fields which fieldname is starting with "col"
    */
   void scrambleData() {

      try {

         for (final Field field : TVITourBookItem.class.getDeclaredFields()) {

            final String fieldName = field.getName();

            if ("colClouds".equals(fieldName)) { //$NON-NLS-1$

               // skip cloud field otherwise the cloud icon is not displayed
               continue;
            }

            if (fieldName.startsWith(SCRAMBLE_FIELD_PREFIX)) {

               final Type fieldType = field.getGenericType();

               if (Integer.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getInt(this)));

               } else if (Long.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getLong(this)));

               } else if (Float.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getFloat(this)));

               } else if (String.class.equals(fieldType)) {

                  final String fieldValue = (String) field.get(this);
                  final String scrambledText = UI.scrambleText(fieldValue);

                  field.set(this, scrambledText);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         e.printStackTrace();
      }
   }

}
