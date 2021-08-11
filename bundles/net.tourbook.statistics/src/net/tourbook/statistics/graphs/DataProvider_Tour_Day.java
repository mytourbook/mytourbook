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
package net.tourbook.statistics.graphs;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;

public class DataProvider_Tour_Day extends DataProvider {

   private TourStatisticData_Day _tourDayData;

   private boolean               _isAdjustSamePosition;
   private boolean               _isShowTrainingPerformance_AvgValue;

   private DurationTime          _durationTime;

   private void adjustValues(final TFloatArrayList dbAllValues,
                             final float[] lowValues,
                             final float[] highValues,
                             final int sameDOY_FirstIndex,
                             final int sameDOY_LastIndex) {

      if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         float maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {
            maxValue += dbAllValues.get(valueIndex);
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = maxValue;
         }
      }
   }

   private void adjustValues(final TIntArrayList dbAllTourDuration,
                             final int[] duration_Low,
                             final int[] duration_High,
                             final int sameDOY_FirstIndex,
                             final int sameDOY_LastIndex) {

      if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         int maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {
            maxValue += dbAllTourDuration.get(valueIndex);
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            duration_Low[avgIndex] = 0;
            duration_High[avgIndex] = maxValue;
         }
      }
   }

   /**
    * For a given list of tour start times, this procedure computes the average values
    * for each unique tour start day.
    *
    * @param dbAllTourStartDateTime
    *           A list of tour start times.
    * @param values
    *           The values to average for each unique tour start date.
    */
   private void adjustValues_Avg(final ArrayList<ZonedDateTime> dbAllTourStartDateTime,
                                 final float[] values) {

      final Map<LocalDate, ArrayList<Float>> uniqueDates = new HashMap<>();

      ArrayList<Float> valuesList;
      for (int index = 0; index < dbAllTourStartDateTime.size(); ++index) {

         final LocalDate tourLocalDate = dbAllTourStartDateTime.get(index).toLocalDate();

         if (!uniqueDates.containsKey(tourLocalDate)) {
            valuesList = new ArrayList<>();
            valuesList.add(values[index]);
            uniqueDates.put(tourLocalDate, valuesList);
         } else {
            valuesList = uniqueDates.get(tourLocalDate);
            valuesList.add(values[index]);
            uniqueDates.replace(tourLocalDate, valuesList);
         }
      }

      final float[] adjustedValues = new float[uniqueDates.size()];

      //We compute and store the average of those values
      int index = 0;
      for (final Entry<LocalDate, ArrayList<Float>> uniqueDate : uniqueDates.entrySet()) {

         final ArrayList<Float> dayValues = uniqueDate.getValue();

         final OptionalDouble averageDouble = dayValues.stream().mapToDouble(d -> d).average();

         if (averageDouble.isPresent()) {
            adjustedValues[index] = (float) averageDouble.getAsDouble();
         }
         ++index;
      }
   }

   private void adjustValues_Avg(final TIntArrayList dbAllTourDuration,

                                 final TFloatArrayList dbAllValues,
                                 final float[] lowValues,
                                 final float[] highValues,

                                 final int sameDOY_FirstIndex,
                                 final int sameDOY_LastIndex) {

      if (_isShowTrainingPerformance_AvgValue && sameDOY_LastIndex != -1) {

         // compute average values

         double valueSquare = 0;
         double timeSquare = 0;

         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            final float value = dbAllValues.get(avgIndex);
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
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = avgValue;
         }

      } else if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         float maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {

            final float value = dbAllValues.get(valueIndex);

            maxValue += value;
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = maxValue;
         }
      }
   }

   TourStatisticData_Day getDayData(final TourPerson person,
                                    final TourTypeFilter tourTypeFilter,
                                    final int lastYear,
                                    final int numberOfYears,
                                    final boolean refreshData,
                                    final DurationTime durationTime) {

      // don't reload data which are already available
      if (person == statistic_ActivePerson
            && tourTypeFilter == statistic_ActiveTourTypeFilter
            && lastYear == statistic_LastYear
            && numberOfYears == statistic_NumberOfYears
            && _durationTime == durationTime
            && refreshData == false

      ) {

         return _tourDayData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;

         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numberOfYears;

         _durationTime = durationTime;

         setupYearNumbers();

         boolean isDurationTime_Break = false;
         boolean isDurationTime_Elapsed = false;
         boolean isDurationTime_Paused = false;
         boolean isDurationTime_Recorded = false;

         switch (durationTime) {
         case BREAK:
            isDurationTime_Break = true;
            break;

         case ELAPSED:
            isDurationTime_Elapsed = true;
            break;

         case PAUSED:
            isDurationTime_Paused = true;
            break;

         case RECORDED:
            isDurationTime_Recorded = true;
            break;

         case MOVING:
         default:
            // this is also the old implementation for the duration value
            break;
         }

         // get the tour types
         final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
         final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + "   TourId," + NL //                                1  //$NON-NLS-1$

               + "   StartYear," + NL //                             2  //$NON-NLS-1$
               + "   StartWeek," + NL //                             3  //$NON-NLS-1$
               + "   TourStartTime," + NL //                         4  //$NON-NLS-1$
               + "   TimeZoneId," + NL //                            5  //$NON-NLS-1$

               + "   TourDeviceTime_Elapsed," + NL //                6  //$NON-NLS-1$
               + "   TourDeviceTime_Recorded," + NL //               7  //$NON-NLS-1$
               + "   TourDeviceTime_Paused," + NL //                 8  //$NON-NLS-1$
               + "   TourComputedTime_Moving," + NL //               9  //$NON-NLS-1$

               + "   TourDistance," + NL //                          10 //$NON-NLS-1$
               + "   TourAltUp," + NL //                             11 //$NON-NLS-1$
               + "   TourTitle," + NL //                             12 //$NON-NLS-1$
               + "   TourDescription," + NL //                       13 //$NON-NLS-1$

               + "   training_TrainingEffect_Aerob," + NL //         14 //$NON-NLS-1$
               + "   training_TrainingEffect_Anaerob," + NL //       15 //$NON-NLS-1$
               + "   training_TrainingPerformance," + NL //          16 //$NON-NLS-1$

               + "   TourType_typeId," + NL //                       17 //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId," + NL //               18 //$NON-NLS-1$

               + "   BodyWeight,         " + NL //      19 //$NON-NLS-1$
               + "   BodyFat          " + NL //      20 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //         //$NON-NLS-1$

               // get/filter tag id's
               + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //                //$NON-NLS-1$
               + " ON TourId = jTdataTtag.TourData_tourId" + NL //                              //$NON-NLS-1$

               + "WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
               + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

               + "ORDER BY TourStartTime" + NL; //                                              //$NON-NLS-1$

         final TLongArrayList dbAllTourIds = new TLongArrayList();

         final TIntArrayList dbAllYears = new TIntArrayList();
         final TIntArrayList dbAllMonths = new TIntArrayList();
         final TIntArrayList dbAllDays = new TIntArrayList();
         final TIntArrayList dbAllYearsDOY = new TIntArrayList(); // DOY...Day Of Year

         final TIntArrayList dbAllTourStartTime = new TIntArrayList();
         final TIntArrayList dbAllTourEndTime = new TIntArrayList();
         final TIntArrayList dbAllTourStartWeek = new TIntArrayList();
         final ArrayList<ZonedDateTime> dbAllTourStartDateTime = new ArrayList<>();

         final TIntArrayList dbAllTourDeviceTime_Elapsed = new TIntArrayList();
         final TIntArrayList dbAllTourDeviceTime_Recorded = new TIntArrayList();
         final TIntArrayList dbAllTourDeviceTime_Paused = new TIntArrayList();
         final TIntArrayList dbAllTourComputedTime_Moving = new TIntArrayList();
         final TIntArrayList dbAllTourDurationTimes = new TIntArrayList();

         final TFloatArrayList dbAllDistance = new TFloatArrayList();
         final TFloatArrayList dbAllAvgSpeed = new TFloatArrayList();
         final TFloatArrayList dbAllAvgPace = new TFloatArrayList();
         final TFloatArrayList dbAllElevationUp = new TFloatArrayList();

         final TFloatArrayList dbAllTrain_Effect_Aerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Effect_Anaerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Performance = new TFloatArrayList();

         final TFloatArrayList dbAllBodyWeight = new TFloatArrayList();
         final TFloatArrayList dbAllBodyFat = new TFloatArrayList();

         final ArrayList<String> dbAllTourTitle = new ArrayList<>();
         final ArrayList<String> dbAllTourDescription = new ArrayList<>();

         final TLongArrayList allTypeIds = new TLongArrayList();
         final TIntArrayList allTypeColorIndex = new TIntArrayList();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();

         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(18);

            if (dbTourId == lastTourId) {

               // get additional tags from tag join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record from a tour

// SET_FORMATTING_OFF

               final int dbTourYear                   = result.getShort(2);
               final int dbTourStartWeek              = result.getInt(3);

               final long dbStartTimeMilli            = result.getLong(4);
               final String dbTimeZoneId              = result.getString(5);

               final int dbElapsedTime                = result.getInt(6);
               final int dbRecordedTime               = result.getInt(7);
               final int dbPausedTime                 = result.getInt(8);
               final int dbMovingTime                 = result.getInt(9);

               final float dbDistance                 = result.getFloat(10);
               final int dbAltitudeUp                 = result.getInt(11);

               final String dbTourTitle               = result.getString(12);
               final String dbDescription             = result.getString(13);

               final float trainingEffect             = result.getFloat(14);
               final float trainingEffect_Anaerobic   = result.getFloat(15);
               final float trainingPerformance        = result.getFloat(16);

               final Long dbValue_TourTypeIdObject    = (Long) result.getObject(17);

               final float bodyWeight    =  result.getFloat(19) * UI.UNIT_VALUE_WEIGHT;
               final float bodyFat    =  result.getFloat(20);

// SET_FORMATTING_ON

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number for day of year, starts with 0
               final int tourDOY = tourDateTime.tourZonedDateTime.get(ChronoField.DAY_OF_YEAR) - 1;
               final int yearDOYs = getYearDOYs(dbTourYear);

               final int startDayTime = (zonedStartDateTime.getHour() * 3600)
                     + (zonedStartDateTime.getMinute() * 60)
                     + zonedStartDateTime.getSecond();

               int durationTimeValue = 0;

               if (isDurationTime_Break) {
                  durationTimeValue = dbElapsedTime - dbMovingTime;
               } else if (isDurationTime_Elapsed) {
                  durationTimeValue = dbElapsedTime;
               } else if (isDurationTime_Recorded) {
                  durationTimeValue = dbRecordedTime;
               } else if (isDurationTime_Paused) {
                  durationTimeValue = dbPausedTime;
               } else {
                  // moving time, this is also the old implementation for the duration value
                  durationTimeValue = dbMovingTime == 0 ? dbElapsedTime : dbMovingTime;
               }

               dbAllTourIds.add(dbTourId);

               dbAllYears.add(dbTourYear);
               dbAllMonths.add(zonedStartDateTime.getMonthValue());
               dbAllDays.add(zonedStartDateTime.getDayOfMonth());
               dbAllYearsDOY.add(yearDOYs + tourDOY);
               dbAllTourStartWeek.add(dbTourStartWeek);

               dbAllTourStartDateTime.add(zonedStartDateTime);
               dbAllTourStartTime.add(startDayTime);
               dbAllTourEndTime.add((startDayTime + dbElapsedTime));

               dbAllTourDeviceTime_Elapsed.add(dbElapsedTime);
               dbAllTourDeviceTime_Recorded.add(dbRecordedTime);
               dbAllTourDeviceTime_Paused.add(dbPausedTime);
               dbAllTourComputedTime_Moving.add(dbMovingTime);

               dbAllTourDurationTimes.add(durationTimeValue);

               dbAllBodyWeight.add(bodyWeight);
               dbAllBodyFat.add(bodyFat);

               // round distance
               final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

               dbAllDistance.add(distance);
               dbAllElevationUp.add(dbAltitudeUp / UI.UNIT_VALUE_ELEVATION);

               dbAllAvgPace.add(distance == 0 ? 0 : dbMovingTime * 1000f / distance / 60.0f);
               dbAllAvgSpeed.add(dbMovingTime == 0 ? 0 : 3.6f * distance / dbMovingTime);

               dbAllTrain_Effect_Aerob.add(trainingEffect);
               dbAllTrain_Effect_Anaerob.add(trainingEffect_Anaerobic);
               dbAllTrain_Performance.add(trainingPerformance);

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

               if (dbValue_TourTypeIdObject instanceof Long) {

                  dbTypeId = dbValue_TourTypeIdObject;

                  for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                     if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
                        colorIndex = typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(colorIndex);
               allTypeIds.add(dbTypeId);
            }

            lastTourId = dbTourId;
         }

         final int[] allYearsDOY = dbAllYearsDOY.toArray();

         final int[] durationTime_High = dbAllTourDurationTimes.toArray();

         final float[] distance_High = dbAllDistance.toArray();
         final float[] elevation_High = dbAllElevationUp.toArray();

         final float[] avgPace_High = dbAllAvgPace.toArray();
         final float[] avgSpeed_High = dbAllAvgSpeed.toArray();

         final float[] trainEffect_Aerob_High = dbAllTrain_Effect_Aerob.toArray();
         final float[] trainEffect_Anaerob_High = dbAllTrain_Effect_Anaerob.toArray();
         final float[] trainPerformance_High = dbAllTrain_Performance.toArray();

         final float[] bodyWeight_High = dbAllBodyWeight.toArray();
         final float[] bodyFat_High = dbAllBodyFat.toArray();

         final int serieLength = durationTime_High.length;

         final int[] durationTime_Low = new int[serieLength];
         final float[] elevation_Low = new float[serieLength];
         final float[] avgPace_Low = new float[serieLength];
         final float[] avgSpeed_Low = new float[serieLength];
         final float[] distance_Low = new float[serieLength];

         final float[] trainEffect_Aerob_Low = new float[serieLength];
         final float[] trainEffect_Anaerob_Low = new float[serieLength];
         final float[] trainPerformance_Low = new float[serieLength];

         /*
          * Adjust low/high values when a day has multiple tours
          */
         int prevTourDOY = -1;

         int sameDOY_FirstIndex = -1;
         int sameDOY_LastIndex = -1;

         // create low/high values
         for (int tourIndex = 0; tourIndex < allYearsDOY.length; tourIndex++) {

            final int tourDOY = allYearsDOY[tourIndex];

            if (prevTourDOY == tourDOY) {

               // current tour is at the same day as the previous tour

               sameDOY_LastIndex = tourIndex;

               if (sameDOY_FirstIndex == -1) {

                  // use previous index as first time slice
                  sameDOY_FirstIndex = tourIndex - 1;
               }

// SET_FORMATTING_OFF

               durationTime_High[tourIndex]           += durationTime_Low[tourIndex]         = durationTime_High[tourIndex - 1];

               elevation_High[tourIndex]              += elevation_Low[tourIndex]            = elevation_High[tourIndex - 1];
               avgPace_High[tourIndex]                += avgPace_Low[tourIndex]              = avgPace_High[tourIndex - 1];
               avgSpeed_High[tourIndex]               += avgSpeed_Low[tourIndex]             = avgSpeed_High[tourIndex - 1];
               distance_High[tourIndex]               += distance_Low[tourIndex]             = distance_High[tourIndex - 1];

               trainEffect_Aerob_High[tourIndex]      += trainEffect_Aerob_Low[tourIndex]    = trainEffect_Aerob_High[tourIndex - 1];
               trainEffect_Anaerob_High[tourIndex]    += trainEffect_Anaerob_Low[tourIndex]  = trainEffect_Anaerob_High[tourIndex - 1];
               trainPerformance_High[tourIndex]       += trainPerformance_Low[tourIndex]     = trainPerformance_High[tourIndex - 1];

// SET_FORMATTING_ON

            } else {

               // current tour is at another day as the tour before

               prevTourDOY = tourDOY;

// SET_FORMATTING_OFF

               adjustValues(dbAllTourDurationTimes,           durationTime_Low,  durationTime_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

               adjustValues(dbAllDistance,               distance_Low,  distance_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllElevationUp,              elevation_Low, elevation_High,   sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllAvgPace,                avgPace_Low,   avgPace_High,     sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllAvgSpeed,               avgSpeed_Low,  avgSpeed_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

               adjustValues(dbAllTrain_Effect_Aerob,     trainEffect_Aerob_Low,     trainEffect_Aerob_High,    sameDOY_FirstIndex,     sameDOY_LastIndex);
               adjustValues(dbAllTrain_Effect_Anaerob,   trainEffect_Anaerob_Low,   trainEffect_Anaerob_High,  sameDOY_FirstIndex,     sameDOY_LastIndex);

               adjustValues_Avg(dbAllTourDurationTimes,       dbAllTrain_Performance,    trainPerformance_Low,      trainPerformance_High,  sameDOY_FirstIndex,  sameDOY_LastIndex);

// SET_FORMATTING_ON

               sameDOY_FirstIndex = -1;
               sameDOY_LastIndex = -1;
            }
         }

         // compute for the last values

// SET_FORMATTING_OFF

         adjustValues(dbAllTourDurationTimes,      durationTime_Low,          durationTime_High,         sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllDistance,               distance_Low,              distance_High,             sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllElevationUp,            elevation_Low,             elevation_High,            sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgPace,                avgPace_Low,               avgPace_High,              sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgSpeed,               avgSpeed_Low,              avgSpeed_High,             sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllTrain_Effect_Aerob,     trainEffect_Aerob_Low,     trainEffect_Aerob_High,    sameDOY_FirstIndex,     sameDOY_LastIndex);
         adjustValues(dbAllTrain_Effect_Anaerob,   trainEffect_Anaerob_Low,   trainEffect_Anaerob_High,  sameDOY_FirstIndex,     sameDOY_LastIndex);

         adjustValues_Avg(dbAllTourDurationTimes,  dbAllTrain_Performance,    trainPerformance_Low,      trainPerformance_High,  sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues_Avg(dbAllTourStartDateTime,  bodyWeight_High);
         adjustValues_Avg(dbAllTourStartDateTime,  bodyFat_High);

//SET_FORMATTING_ON

         // get number of days for all years
         int yearDays = 0;
         for (final int doy : allYear_NumDays) {
            yearDays += doy;
         }

         _tourDayData = new TourStatisticData_Day();

         _tourDayData.allTourIds = dbAllTourIds.toArray();

         _tourDayData.allYears = dbAllYears.toArray();
         _tourDayData.allMonths = dbAllMonths.toArray();
         _tourDayData.allDays = dbAllDays.toArray();
         _tourDayData.setDoyValues(allYearsDOY);
         _tourDayData.allWeeks = dbAllTourStartWeek.toArray();

         _tourDayData.allStartTime = dbAllTourStartTime.toArray();
         _tourDayData.allEndTime = dbAllTourEndTime.toArray();
         _tourDayData.allStartDateTimes = dbAllTourStartDateTime;

         _tourDayData.allDaysInAllYears = yearDays;
         _tourDayData.allYearDays = allYear_NumDays;
         _tourDayData.allYearNumbers = allYear_Numbers;

         _tourDayData.allDeviceTime_Elapsed = dbAllTourDeviceTime_Elapsed.toArray();
         _tourDayData.allDeviceTime_Recorded = dbAllTourDeviceTime_Recorded.toArray();
         _tourDayData.allDeviceTime_Paused = dbAllTourDeviceTime_Paused.toArray();
         _tourDayData.allComputedTime_Moving = dbAllTourComputedTime_Moving.toArray();

         _tourDayData.allTypeIds = allTypeIds.toArray();
         _tourDayData.allTypeColorIndices = allTypeColorIndex.toArray();

         _tourDayData.tagIds = allTagIds;

         _tourDayData.setDurationLow(durationTime_Low);
         _tourDayData.setDurationHigh(durationTime_High);

         _tourDayData.allDistance = dbAllDistance.toArray();
         _tourDayData.allDistance_Low = distance_Low;
         _tourDayData.allDistance_High = distance_High;
         _tourDayData.allElevationUp = dbAllElevationUp.toArray();
         _tourDayData.allElevationUp_Low = elevation_Low;
         _tourDayData.allElevationUp_High = elevation_High;

         _tourDayData.allAvgPace = dbAllAvgPace.toArray();
         _tourDayData.allAvgPace_Low = avgPace_Low;
         _tourDayData.allAvgPace_High = avgPace_High;
         _tourDayData.allAvgSpeed = dbAllAvgSpeed.toArray();
         _tourDayData.allAvgSpeed_Low = avgSpeed_Low;
         _tourDayData.allAvgSpeed_High = avgSpeed_High;

         _tourDayData.allTraining_Effect_Aerob = dbAllTrain_Effect_Aerob.toArray();
         _tourDayData.allTraining_Effect_Aerob_Low = trainEffect_Aerob_Low;
         _tourDayData.allTraining_Effect_Aerob_High = trainEffect_Aerob_High;
         _tourDayData.allTraining_Effect_Anaerob = dbAllTrain_Effect_Anaerob.toArray();
         _tourDayData.allTraining_Effect_Anaerob_Low = trainEffect_Anaerob_Low;
         _tourDayData.allTraining_Effect_Anaerob_High = trainEffect_Anaerob_High;
         _tourDayData.allTraining_Performance = dbAllTrain_Performance.toArray();
         _tourDayData.allTraining_Performance_Low = trainPerformance_Low;
         _tourDayData.allTraining_Performance_High = trainPerformance_High;

         _tourDayData.allAthleteBodyWeight_Low = new float[yearDays];
         _tourDayData.allAthleteBodyWeight_High = bodyWeight_High;

         _tourDayData.allAthleteBodyFat_Low = new float[yearDays];
         _tourDayData.allAthleteBodyFat_High = bodyFat_High;

         _tourDayData.allTourTitles = dbAllTourTitle;
         _tourDayData.allTourDescriptions = dbAllTourDescription;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourDayData;
   }

   String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_tourDayData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()
            + STAT_VALUE_DATE_MONTH.getHead1()
            + STAT_VALUE_DATE_DAY.getHead1()
            + STAT_VALUE_DATE_WEEK.getHead1()

            + STAT_VALUE_TOUR_TYPE.getHead1()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead1()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead1()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead1()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead1()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead1()

            + STAT_VALUE_MOTION_DISTANCE.withUnitLabel(UI.UNIT_LABEL_DISTANCE).getHead1()
            + STAT_VALUE_MOTION_SPEED.withUnitLabel(UI.UNIT_LABEL_SPEED).getHead1()
            + STAT_VALUE_MOTION_PACE.withUnitLabel(UI.UNIT_LABEL_PACE).getHead1()

            + STAT_VALUE_ELEVATION_UP.withUnitLabel(UI.UNIT_LABEL_ELEVATION).getHead1()

            + STAT_VALUE_TRAINING_AEROB.getHead1()
            + STAT_VALUE_TRAINING_ANAEROB.getHead1()
            + STAT_VALUE_TRAINING_PERFORMANCE.getHead1()

            + STAT_VALUE_TOUR_TITLE.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()
            + STAT_VALUE_DATE_MONTH.getHead2()
            + STAT_VALUE_DATE_DAY.getHead2()
            + STAT_VALUE_DATE_WEEK.getHead2()

            + STAT_VALUE_TOUR_TYPE.getHead2()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead2()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead2()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead2()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead2()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead2()

            + STAT_VALUE_MOTION_DISTANCE.getHead2()
            + STAT_VALUE_MOTION_SPEED.getHead2()
            + STAT_VALUE_MOTION_PACE.getHead2()

            + STAT_VALUE_ELEVATION_UP.getHead2()

            + STAT_VALUE_TRAINING_AEROB.getHead2()
            + STAT_VALUE_TRAINING_ANAEROB.getHead2()
            + STAT_VALUE_TRAINING_PERFORMANCE.getHead2()

            + STAT_VALUE_TOUR_TITLE.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : VALUE_FORMAT_S)

            + STAT_VALUE_DATE_YEAR.getValueFormatting()
            + STAT_VALUE_DATE_MONTH.getValueFormatting()
            + STAT_VALUE_DATE_DAY.getValueFormatting()
            + STAT_VALUE_DATE_WEEK.getValueFormatting()

            + STAT_VALUE_TOUR_TYPE.getValueFormatting()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getValueFormatting()

            + STAT_VALUE_MOTION_DISTANCE.getValueFormatting()
            + STAT_VALUE_MOTION_SPEED.getValueFormatting()
            + STAT_VALUE_MOTION_PACE.getValueFormatting()

            + STAT_VALUE_ELEVATION_UP.getValueFormatting()

            + STAT_VALUE_TRAINING_AEROB.getValueFormatting()
            + STAT_VALUE_TRAINING_ANAEROB.getValueFormatting()
            + STAT_VALUE_TRAINING_PERFORMANCE.getValueFormatting()

            + STAT_VALUE_TOUR_TITLE.getValueFormatting()

      ;

      final StringBuilder sb = new StringBuilder();
      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      int sequenceNumber = 0;
      final int numDataItems = _tourDayData.allDays.length;

      // set initial value
      int prevMonth = numDataItems > 0 ? _tourDayData.allMonths[0] : 0;

      for (int dataIndex = 0; dataIndex < numDataItems; dataIndex++) {

         final int month = _tourDayData.allMonths[dataIndex];

         // group by month
         if (month != prevMonth) {
            prevMonth = month;
            sb.append(NL);
         }

         final int elapsedTime = _tourDayData.allDeviceTime_Elapsed[dataIndex];
         final int movingTime = _tourDayData.allComputedTime_Moving[dataIndex];
         final int breakTime = elapsedTime - movingTime;

         String tourTitle = _tourDayData.allTourTitles.get(dataIndex);
         if (tourTitle == null) {
            tourTitle = UI.EMPTY_STRING;
         }

         Object sequenceNumberValue = UI.EMPTY_STRING;
         if (isShowSequenceNumbers) {
            sequenceNumberValue = ++sequenceNumber;
         }

         sb.append(String.format(valueFormatting,

               sequenceNumberValue,

               _tourDayData.allYears[dataIndex],
               month,
               _tourDayData.allDays[dataIndex],
               _tourDayData.allWeeks[dataIndex],

               TourDatabase.getTourTypeName(_tourDayData.allTypeIds[dataIndex]),

               elapsedTime,
               _tourDayData.allDeviceTime_Recorded[dataIndex],
               _tourDayData.allDeviceTime_Paused[dataIndex],
               movingTime,
               breakTime,

               _tourDayData.allDistance[dataIndex] / 1000f,
               _tourDayData.allAvgSpeed[dataIndex],
               _tourDayData.allAvgPace[dataIndex],

               _tourDayData.allElevationUp[dataIndex],

               _tourDayData.allTraining_Effect_Aerob[dataIndex],
               _tourDayData.allTraining_Effect_Anaerob[dataIndex],
               _tourDayData.allTraining_Performance[dataIndex],

               tourTitle

         ));

         sb.append(NL);
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   public void setGraphContext(final boolean isShowTrainingPerformance_AvgValue, final boolean isAdjustmentSamePosition) {

      _isShowTrainingPerformance_AvgValue = isShowTrainingPerformance_AvgValue;
      _isAdjustSamePosition = isAdjustmentSamePosition;
   }
}
