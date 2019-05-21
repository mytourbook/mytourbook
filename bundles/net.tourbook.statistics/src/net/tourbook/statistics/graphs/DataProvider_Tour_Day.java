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
package net.tourbook.statistics.graphs;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Day extends DataProvider {

   private static DataProvider_Tour_Day _instance;

   private TourData_Day                 _tourDayData;

   private boolean                      _isShowTrainingPerformance_AvgValue;

   private DataProvider_Tour_Day() {}

   public static DataProvider_Tour_Day getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Day();
      }

      return _instance;
   }

   private void computePerformanceAverage(final TIntArrayList dbAllTourDuration,
                                          final TFloatArrayList dbAllTrainingPerformance,
                                          final float[] trainingPerformance_High,
                                          final float[] trainingPerformance_Low,
                                          final int avgValue_FirstIndex,
                                          final int avgValue_LastIndex) {

      if (_isShowTrainingPerformance_AvgValue && avgValue_LastIndex != -1) {

         // compute average values

         double valueSquare = 0;
         double timeSquare = 0;

         for (int avgIndex = avgValue_FirstIndex; avgIndex <= avgValue_LastIndex; avgIndex++) {

            final float value = dbAllTrainingPerformance.get(avgIndex);
            final float duration = dbAllTourDuration.get(avgIndex);

            // ignore 0 values
            if (value > 0) {

               valueSquare += value * duration;
               timeSquare += duration;
            }
         }

         final float avgValue = (float) (timeSquare == 0 ? 0 : valueSquare / timeSquare);

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = avgValue_FirstIndex; avgIndex <= avgValue_LastIndex; avgIndex++) {

            trainingPerformance_Low[avgIndex] = 0;
            trainingPerformance_High[avgIndex] = avgValue;
         }
      }
   }

   TourData_Day getDayData(final TourPerson person,
                           final TourTypeFilter tourTypeFilter,
                           final int lastYear,
                           final int numberOfYears,
                           final boolean refreshData) {

      // don't reload data which are already available
      if (person == _activePerson
            && tourTypeFilter == _activeTourTypeFilter
            && lastYear == _lastYear
            && numberOfYears == _numberOfYears
            && refreshData == false) {

         return _tourDayData;
      }

      _activePerson = person;
      _activeTourTypeFilter = tourTypeFilter;

      _lastYear = lastYear;
      _numberOfYears = numberOfYears;

      initYearNumbers();

      int colorOffset = 0;
      if (tourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      // get the tour types
      final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
      final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

// SET_FORMATTING_OFF

      final String sqlString = NL

            + "SELECT "                               + NL //        //$NON-NLS-1$

            + " TourId,"                              + NL //  1     //$NON-NLS-1$

            + " StartYear,"                           + NL //  2     //$NON-NLS-1$
            + " StartWeek,"                           + NL //  3     //$NON-NLS-1$
            + " TourStartTime,"                       + NL //  4     //$NON-NLS-1$
            + " TimeZoneId,"                          + NL //  5     //$NON-NLS-1$

            + " TourDrivingTime,"                     + NL //  6     //$NON-NLS-1$
            + " TourRecordingTime,"                   + NL //  7     //$NON-NLS-1$

            + " TourDistance,"                        + NL //  8     //$NON-NLS-1$
            + " TourAltUp,"                           + NL //  9     //$NON-NLS-1$
            + " TourTitle,"                           + NL //  10    //$NON-NLS-1$
            + " TourDescription,"                     + NL //  11    //$NON-NLS-1$

            + " training_TrainingEffect_Aerob,"       + NL //  12    //$NON-NLS-1$
            + " training_TrainingEffect_Anaerob,"     + NL //  13    //$NON-NLS-1$
            + " training_TrainingPerformance,"        + NL //  14    //$NON-NLS-1$

            + " TourType_typeId,"                     + NL //  15    //$NON-NLS-1$
            + " jTdataTtag.TourTag_tagId"             + NL //  16    //$NON-NLS-1$

            + NL

            + (" FROM " + TourDatabase.TABLE_TOUR_DATA + NL) //$NON-NLS-1$

            // get tag id's
            + (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
            + (" ON tourID = jTdataTtag.TourData_tourId" + NL) //$NON-NLS-1$

            + (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL) //$NON-NLS-1$ //$NON-NLS-2$
            + sqlFilter.getWhereClause()

            + (" ORDER BY TourStartTime" + NL + NL); //$NON-NLS-1$

// SET_FORMATTING_ON

      try {

         final TLongArrayList dbAllTourIds = new TLongArrayList();

         final TIntArrayList dbAllYears = new TIntArrayList();
         final TIntArrayList dbAllMonths = new TIntArrayList();
         final TIntArrayList dbAllYearsDOY = new TIntArrayList(); // DOY...Day Of Year

         final TIntArrayList dbAllTourStartTime = new TIntArrayList();
         final TIntArrayList dbAllTourEndTime = new TIntArrayList();
         final TIntArrayList dbAllTourStartWeek = new TIntArrayList();
         final ArrayList<ZonedDateTime> dbAllTourStartDateTime = new ArrayList<>();

         final TIntArrayList dbAllTourDuration = new TIntArrayList();
         final TIntArrayList dbAllTourRecordingTime = new TIntArrayList();
         final TIntArrayList dbAllTourDrivingTime = new TIntArrayList();

         final TFloatArrayList dbAllDistance = new TFloatArrayList();
         final TFloatArrayList dbAllAvgSpeed = new TFloatArrayList();
         final TFloatArrayList dbAllAvgPace = new TFloatArrayList();
         final TFloatArrayList dbAllAltitudeUp = new TFloatArrayList();

         final TFloatArrayList dbAllTrainingEffect_Aerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrainingEffect_Anaerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrainingPerformance = new TFloatArrayList();

         final ArrayList<String> dbAllTourTitle = new ArrayList<>();
         final ArrayList<String> dbAllTourDescription = new ArrayList<>();

         final TLongArrayList allTypeIds = new TLongArrayList();
         final TIntArrayList allTypeColorIndex = new TIntArrayList();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final Connection conn = TourDatabase.getInstance().getConnection();

         final PreparedStatement statement = conn.prepareStatement(sqlString);
         sqlFilter.setParameters(statement, 1);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(16);

            if (dbTourId == lastTourId) {

               // get additional tags from outer join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record from a tour

               final int dbTourYear = result.getShort(2);
               final int dbTourStartWeek = result.getInt(3);

               final long dbStartTimeMilli = result.getLong(4);
               final String dbTimeZoneId = result.getString(5);

               final int dbDrivingTime = result.getInt(6);
               final int dbRecordingTime = result.getInt(7);

               final float dbDistance = result.getFloat(8);
               final int dbAltitudeUp = result.getInt(9);

               final String dbTourTitle = result.getString(10);
               final String dbDescription = result.getString(11);

               final float trainingEffect = result.getFloat(12);
               final float trainingEffect_Anaerobic = result.getFloat(13);
               final float trainingPerformance = result.getFloat(14);

               final Object dbTypeIdObject = result.getObject(15);

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number for day of year, starts with 0
               final int tourDOY = tourDateTime.tourZonedDateTime.get(ChronoField.DAY_OF_YEAR) - 1;
               final int yearDOYs = getYearDOYs(dbTourYear);

               final int startDayTime = (zonedStartDateTime.getHour() * 3600)
                     + (zonedStartDateTime.getMinute() * 60)
                     + zonedStartDateTime.getSecond();

               dbAllTourIds.add(dbTourId);

               dbAllYears.add(dbTourYear);
               dbAllMonths.add(zonedStartDateTime.getMonthValue());
               dbAllYearsDOY.add(yearDOYs + tourDOY);
               dbAllTourStartWeek.add(dbTourStartWeek);

               dbAllTourStartDateTime.add(zonedStartDateTime);
               dbAllTourStartTime.add(startDayTime);
               dbAllTourEndTime.add((startDayTime + dbRecordingTime));
               dbAllTourRecordingTime.add(dbRecordingTime);
               dbAllTourDrivingTime.add(dbDrivingTime);

               dbAllTourDuration.add(dbDrivingTime == 0 ? dbRecordingTime : dbDrivingTime);

               // round distance
               final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

               dbAllDistance.add(distance);
               dbAllAltitudeUp.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

               dbAllAvgPace.add(distance == 0 ? 0 : dbDrivingTime * 1000f / distance / 60.0f);
               dbAllAvgSpeed.add(dbDrivingTime == 0 ? 0 : 3.6f * distance / dbDrivingTime);

               dbAllTrainingEffect_Aerob.add(trainingEffect);
               dbAllTrainingEffect_Anaerob.add(trainingEffect_Anaerobic);
               dbAllTrainingPerformance.add(trainingPerformance);

               dbAllTourTitle.add(dbTourTitle);
               dbAllTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

               if (dbTagId instanceof Long) {

                  tagIds = new ArrayList<>();
                  tagIds.add((Long) dbTagId);

                  allTagIds.put(dbTourId, tagIds);
               }

               /*
                * Convert type id to the type index in the tour types list which is also the color
                * index
                */
               int colorIndex = 0;
               long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

               if (dbTypeIdObject instanceof Long) {

                  dbTypeId = (Long) dbTypeIdObject;

                  for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                     if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
                        colorIndex = colorOffset + typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(colorIndex);
               allTypeIds.add(dbTypeId);
            }

            lastTourId = dbTourId;
         }

         conn.close();

         final int[] allYearsDOY = dbAllYearsDOY.toArray();

         final int[] durationHigh = dbAllTourDuration.toArray();
         final int serieLength = durationHigh.length;

         final float[] altitude_High = dbAllAltitudeUp.toArray();
         final float[] avgPace_High = dbAllAvgPace.toArray();
         final float[] avgSpeed_High = dbAllAvgSpeed.toArray();
         final float[] distance_High = dbAllDistance.toArray();

         final float[] trainingEffect_High = dbAllTrainingEffect_Aerob.toArray();
         final float[] trainingEffect_Anaerobic_High = dbAllTrainingEffect_Anaerob.toArray();
         final float[] trainingPerformance_High = dbAllTrainingPerformance.toArray();

         final int[] durationLow = new int[serieLength];
         final float[] altitudeLow = new float[serieLength];
         final float[] avgPaceLow = new float[serieLength];
         final float[] avgSpeedLow = new float[serieLength];
         final float[] distanceLow = new float[serieLength];

         final float[] trainingEffect_Low = new float[serieLength];
         final float[] trainingEffect_Anaerobic_Low = new float[serieLength];
         final float[] trainingPerformance_Low = new float[serieLength];

         /*
          * Adjust low/high values when a day has multiple tours
          */
         int prevTourDOY = -1;

         int avgValue_FirstIndex = -1;
         int avgValue_LastIndex = -1;

         for (int tourIndex = 0; tourIndex < allYearsDOY.length; tourIndex++) {

            final int tourDOY = allYearsDOY[tourIndex];

            if (prevTourDOY == tourDOY) {

               // current tour is at the same day as the previous tour

               avgValue_LastIndex = tourIndex;

               if (avgValue_FirstIndex == -1) {
                  // use previous index as first time slice
                  avgValue_FirstIndex = tourIndex - 1;
               }

// SET_FORMATTING_OFF

               durationHigh[tourIndex]    += durationLow[tourIndex]  = durationHigh[tourIndex - 1];

               altitude_High[tourIndex]   += altitudeLow[tourIndex]  = altitude_High[tourIndex - 1];
               avgPace_High[tourIndex]    += avgPaceLow[tourIndex]   = avgPace_High[tourIndex - 1];
               avgSpeed_High[tourIndex]   += avgSpeedLow[tourIndex]  = avgSpeed_High[tourIndex - 1];
               distance_High[tourIndex]   += distanceLow[tourIndex]  = distance_High[tourIndex - 1];

               trainingEffect_High[tourIndex]            += trainingEffect_Low[tourIndex]             = trainingEffect_High[tourIndex - 1];
               trainingEffect_Anaerobic_High[tourIndex]  += trainingEffect_Anaerobic_Low[tourIndex]   = trainingEffect_Anaerobic_High[tourIndex - 1];
               trainingPerformance_High[tourIndex]       += trainingPerformance_Low[tourIndex]        = trainingPerformance_High[tourIndex - 1];

// SET_FORMATTING_ON

            } else {

               // current tour is at another day as the tour before

               prevTourDOY = tourDOY;

               computePerformanceAverage(dbAllTourDuration,
                     dbAllTrainingPerformance,
                     trainingPerformance_High,
                     trainingPerformance_Low,
                     avgValue_FirstIndex,
                     avgValue_LastIndex);

               avgValue_FirstIndex = -1;
               avgValue_LastIndex = -1;
            }
         }

         // compute for the last values
         computePerformanceAverage(dbAllTourDuration,
               dbAllTrainingPerformance,
               trainingPerformance_High,
               trainingPerformance_Low,
               avgValue_FirstIndex,
               avgValue_LastIndex);

         // get number of days for all years
         int yearDays = 0;
         for (final int doy : _yearDays) {
            yearDays += doy;
         }

         _tourDayData = new TourData_Day();

         _tourDayData.tourIds = dbAllTourIds.toArray();

         _tourDayData.yearValues = dbAllYears.toArray();
         _tourDayData.monthValues = dbAllMonths.toArray();
         _tourDayData.setDoyValues(allYearsDOY);
         _tourDayData.weekValues = dbAllTourStartWeek.toArray();

         _tourDayData.allDaysInAllYears = yearDays;
         _tourDayData.yearDays = _yearDays;
         _tourDayData.years = _years;

         _tourDayData.typeIds = allTypeIds.toArray();
         _tourDayData.typeColorIndex = allTypeColorIndex.toArray();

         _tourDayData.tagIds = allTagIds;

         _tourDayData.setDurationLow(durationLow);
         _tourDayData.setDurationHigh(durationHigh);

         _tourDayData.altitude_Low = altitudeLow;
         _tourDayData.altitude_High = altitude_High;
         _tourDayData.distance_Low = distanceLow;
         _tourDayData.distance_High = distance_High;

         _tourDayData.avgPace_Low = avgPaceLow;
         _tourDayData.avgPace_High = avgPace_High;
         _tourDayData.avgSpeed_Low = avgSpeedLow;
         _tourDayData.avgSpeed_High = avgSpeed_High;

         _tourDayData.trainingEffect_Aerob_Low = trainingEffect_Low;
         _tourDayData.trainingEffect_Aerob_High = trainingEffect_High;
         _tourDayData.trainingEffect_Anaerob_Low = trainingEffect_Anaerobic_Low;
         _tourDayData.trainingEffect_Anaerob_High = trainingEffect_Anaerobic_High;
         _tourDayData.trainingPerformance_Low = trainingPerformance_Low;
         _tourDayData.trainingPerformance_High = trainingPerformance_High;

         _tourDayData.allStartTime = dbAllTourStartTime.toArray();
         _tourDayData.allEndTime = dbAllTourEndTime.toArray();
         _tourDayData.allStartDateTimes = dbAllTourStartDateTime;

         _tourDayData.allDistance = dbAllDistance.toArray();
         _tourDayData.allAltitude = dbAllAltitudeUp.toArray();

         _tourDayData.allTraining_Effect = dbAllTrainingEffect_Aerob.toArray();
         _tourDayData.allTraining_Effect_Anaerobic = dbAllTrainingEffect_Anaerob.toArray();
         _tourDayData.allTraining_Performance = dbAllTrainingPerformance.toArray();

         _tourDayData.allRecordingTime = dbAllTourRecordingTime.toArray();
         _tourDayData.allDrivingTime = dbAllTourDrivingTime.toArray();

         _tourDayData.tourTitle = dbAllTourTitle;
         _tourDayData.tourDescription = dbAllTourDescription;

      } catch (final SQLException e) {

         StatusUtil.log(sqlString);
         UI.showSQLException(e);
      }

      if (isLogStatisticValues) {
         logValues();
      }

      return _tourDayData;
   }

   private void logValues() {

      System.out.println();
      System.out.println();

      final String part_1_1 = "Year Month DOY       Duration       Altitude           Distance              Speed           Pace"; //$NON-NLS-1$
      final String part_1_2 = "                        (sec)            (m)                (m)             (km/h)       (min/km)"; //$NON-NLS-1$

      final String part_2_1 = "       Training       Training       Training"; //$NON-NLS-1$
      final String part_2_2 = "          Aerob        Anaerob    Performance"; //$NON-NLS-1$

      System.out.println(part_1_1 + part_2_1);
      System.out.println(part_1_2 + part_2_2);

      System.out.println();

      final float[] durationLow = _tourDayData.getDurationLowFloat();
      final float[] durationHigh = _tourDayData.getDurationHighFloat();
      final int[] doyValues = _tourDayData.getDoyValues();

      for (int dataIndex = 0; dataIndex < durationLow.length; dataIndex++) {

         System.out.println(String.format(UI.EMPTY_STRING

               // date
               + "%4d %3d %5d" //$NON-NLS-1$

               // duration
               + "  %6.0f %6.0f" //$NON-NLS-1$

               // altitude
               + "  %6.0f %6.0f" //$NON-NLS-1$

               // distance
               + "  %8.0f %8.0f" //$NON-NLS-1$

               // speed
               + "  %8.2f %8.2f" //$NON-NLS-1$

               // pace
               + "  %6.2f %6.2f" //$NON-NLS-1$

               // training aerob
               + "  %6.1f %6.1f" //$NON-NLS-1$

               // training anaerob
               + "  %6.1f %6.1f" //$NON-NLS-1$

               // training performance
               + "  %6.2f %6.2f" //$NON-NLS-1$

               ,

               _tourDayData.yearValues[dataIndex],
               _tourDayData.monthValues[dataIndex],
               doyValues[dataIndex],

               durationLow[dataIndex],
               durationHigh[dataIndex],

               _tourDayData.altitude_Low[dataIndex],
               _tourDayData.altitude_High[dataIndex],

               _tourDayData.distance_Low[dataIndex],
               _tourDayData.distance_High[dataIndex],

               _tourDayData.avgSpeed_Low[dataIndex],
               _tourDayData.avgSpeed_High[dataIndex],

               _tourDayData.avgPace_Low[dataIndex],
               _tourDayData.avgPace_High[dataIndex],

               _tourDayData.trainingEffect_Aerob_Low[dataIndex],
               _tourDayData.trainingEffect_Aerob_High[dataIndex],
               _tourDayData.trainingEffect_Anaerob_Low[dataIndex],
               _tourDayData.trainingEffect_Anaerob_High[dataIndex],
               _tourDayData.trainingPerformance_Low[dataIndex],
               _tourDayData.trainingPerformance_High[dataIndex]

         ));
      }

      System.out.println();
   }

   public void setIsShowTrainingPerformance_AvgValue(final boolean isShowTrainingPerformance_AvgValue) {
      _isShowTrainingPerformance_AvgValue = isShowTrainingPerformance_AvgValue;
   }
}
