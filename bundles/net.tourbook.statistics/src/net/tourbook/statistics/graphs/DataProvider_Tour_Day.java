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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

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

         int colorOffset = 0;
         if (tourTypeFilter.showUndefinedTourTypes()) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

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
               + "   jTdataTtag.TourTag_tagId" + NL //               18 //$NON-NLS-1$

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
         final TFloatArrayList dbAllElevation = new TFloatArrayList();

         final TFloatArrayList dbAllTrain_Effect_Aerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Effect_Anaerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Performance = new TFloatArrayList();

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

               // round distance
               final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

               dbAllDistance.add(distance);
               dbAllElevation.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

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

         final int[] allYearsDOY = dbAllYearsDOY.toArray();

         final int[] durationTime_High = dbAllTourDurationTimes.toArray();

         final float[] distance_High = dbAllDistance.toArray();
         final float[] elevation_High = dbAllElevation.toArray();

         final float[] avgPace_High = dbAllAvgPace.toArray();
         final float[] avgSpeed_High = dbAllAvgSpeed.toArray();

         final float[] trainEffect_Aerob_High = dbAllTrain_Effect_Aerob.toArray();
         final float[] trainEffect_Anaerob_High = dbAllTrain_Effect_Anaerob.toArray();
         final float[] trainPerformance_High = dbAllTrain_Performance.toArray();

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
               adjustValues(dbAllElevation,              elevation_Low, elevation_High,   sameDOY_FirstIndex,  sameDOY_LastIndex);
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

         adjustValues(dbAllTourDurationTimes,           durationTime_Low,     durationTime_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllDistance,               distance_Low,     distance_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllElevation,              elevation_Low,    elevation_High,   sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgPace,                avgPace_Low,      avgPace_High,     sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgSpeed,               avgSpeed_Low,     avgSpeed_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllTrain_Effect_Aerob,     trainEffect_Aerob_Low,     trainEffect_Aerob_High,    sameDOY_FirstIndex,     sameDOY_LastIndex);
         adjustValues(dbAllTrain_Effect_Anaerob,   trainEffect_Anaerob_Low,   trainEffect_Anaerob_High,  sameDOY_FirstIndex,     sameDOY_LastIndex);

         adjustValues_Avg(dbAllTourDurationTimes,       dbAllTrain_Performance,    trainPerformance_Low,      trainPerformance_High,  sameDOY_FirstIndex,  sameDOY_LastIndex);

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
         _tourDayData.allElevation = dbAllElevation.toArray();
         _tourDayData.allElevation_Low = elevation_Low;
         _tourDayData.allElevation_High = elevation_High;

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

            + (isShowSequenceNumbers ? HEAD1_DATA_NUMBER : UI.EMPTY_STRING)

            + HEAD1_DATE_YEAR
            + HEAD1_DATE_MONTH
            + HEAD1_DATE_DAY
            + HEAD1_DATE_WEEK

            + HEAD1_TOUR_TYPE

            + HEAD1_DEVICE_TIME_ELAPSED
            + HEAD1_DEVICE_TIME_RECORDED
            + HEAD1_DEVICE_TIME_PAUSED

            + HEAD1_COMPUTED_TIME_MOVING
            + HEAD1_COMPUTED_TIME_BREAK

            + HEAD1_DISTANCE
            + HEAD1_ELEVATION

            + HEAD1_SPEED
            + HEAD1_PACE

            + HEAD1_TRAINING_AEROB
            + HEAD1_TRAINING_ANAEROB
            + HEAD1_TRAINING_PERFORMANCE

            + HEAD1_TOUR_TITLE

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? HEAD2_DATA_NUMBER : UI.EMPTY_STRING)

            + HEAD2_DATE_YEAR
            + HEAD2_DATE_MONTH
            + HEAD2_DATE_DAY
            + HEAD2_DATE_WEEK

            + HEAD2_TOUR_TYPE

            + HEAD2_DEVICE_TIME_ELAPSED
            + HEAD2_DEVICE_TIME_RECORDED
            + HEAD2_DEVICE_TIME_PAUSED

            + HEAD2_COMPUTED_TIME_MOVING
            + HEAD2_COMPUTED_TIME_BREAK

            + HEAD2_DISTANCE
            + HEAD2_ELEVATION

            + HEAD2_SPEED
            + HEAD2_PACE

            + HEAD2_TRAINING_AEROB
            + HEAD2_TRAINING_ANAEROB
            + HEAD2_TRAINING_PERFORMANCE

            + HEAD2_TOUR_TITLE;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? VALUE_DATA_NUMBER : "%s") //$NON-NLS-1$

            + VALUE_DATE_YEAR
            + VALUE_DATE_MONTH
            + VALUE_DATE_DAY
            + VALUE_DATE_WEEK

            + VALUE_TOUR_TYPE

            + VALUE_DEVICE_TIME_ELAPSED
            + VALUE_DEVICE_TIME_RECORDED
            + VALUE_DEVICE_TIME_PAUSED

            + VALUE_COMPUTED_TIME_MOVING
            + VALUE_COMPUTED_TIME_BREAK

            + VALUE_DISTANCE
            + VALUE_ELEVATION

            + VALUE_SPEED
            + VALUE_PACE

            + VALUE_TRAINING_AEROB
            + VALUE_TRAINING_ANAEROB
            + VALUE_TRAINING_PERFORMANCE

            + VALUE_TOUR_TITLE;

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

               _tourDayData.allDistance[dataIndex],
               _tourDayData.allElevation[dataIndex],

               _tourDayData.allAvgSpeed[dataIndex],
               _tourDayData.allAvgPace[dataIndex],

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
