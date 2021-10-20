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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;

public class DataProvider_Tour_Week extends DataProvider {

   private TourStatisticData_Week _tourWeekData;

   String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_tourWeekData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      if (_tourWeekData.numTours_High.length == 0) {

         // there are no real data -> show info

         return Messages.Tour_StatisticValues_Label_NoData;
      }

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()
            + STAT_VALUE_DATE_WEEK.getHead1()
            + STAT_VALUE_DATE_WEEK_START.getHead1()

            + STAT_VALUE_TOUR_TYPE.getHead1()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead1()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead1()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead1()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead1()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead1()

            + STAT_VALUE_MOTION_DISTANCE.withUnitLabel(UI.UNIT_LABEL_DISTANCE).getHead1()
            + STAT_VALUE_ELEVATION_UP.withUnitLabel(UI.UNIT_LABEL_ELEVATION).getHead1()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()
            + STAT_VALUE_DATE_WEEK.getHead2()
            + STAT_VALUE_DATE_WEEK_START.getHead2()

            + STAT_VALUE_TOUR_TYPE.getHead2()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead2()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead2()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead2()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead2()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead2()

            + STAT_VALUE_MOTION_DISTANCE.getHead2()
            + STAT_VALUE_ELEVATION_UP.getHead2()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : VALUE_FORMAT_S)

            + STAT_VALUE_DATE_YEAR.getValueFormatting()
            + STAT_VALUE_DATE_WEEK.getValueFormatting()
            + STAT_VALUE_DATE_WEEK_START.getValueFormatting()

            + STAT_VALUE_TOUR_TYPE.getValueFormatting()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getValueFormatting()

            + STAT_VALUE_MOTION_DISTANCE.getValueFormatting()
            + STAT_VALUE_ELEVATION_UP.getValueFormatting()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getValueFormatting()

      ;

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final float[][] allNumTours = _tourWeekData.numTours_High;
      final int numAllWeeks = allNumTours[0].length;
      final int firstYear = statistic_LastYear - statistic_NumberOfYears + 1;
      int prevYear = firstYear;

      final long[][] allTourTypeIds = _tourWeekData.typeIds;
      final long[] allUsedTourTypeIds = _tourWeekData.usedTourTypeIds;

      int yearIndex = 0;
      int prevSumWeeks = 0;
      int sumYearWeeks = allYear_NumWeeks[yearIndex];

      int sequenceNumber = 0;

      /*
       * Set week start day
       */
      final WeekFields calendarWeek = TimeTools.calendarWeek;
      final TemporalField weekOfWeekBasedYear = calendarWeek.weekOfWeekBasedYear();
      final TemporalField dayOfWeek = calendarWeek.dayOfWeek();

      // first day in the statistic calendar
      final LocalDate jan_1_1 = LocalDate.of(firstYear, 1, 1);

      final int jan_1_1_DayOfWeek = jan_1_1.get(dayOfWeek) - 1;

      final int jan_1_1_WeekOfYear = jan_1_1.get(weekOfWeekBasedYear);
      LocalDate firstStatisticDay;

      if (jan_1_1_WeekOfYear > 33) {

         // the week from 1.1.January is from the last year -> this is not displayed
         firstStatisticDay = jan_1_1.plusDays(7 - jan_1_1_DayOfWeek);

      } else {

         firstStatisticDay = jan_1_1.minusDays(jan_1_1_DayOfWeek);
      }

      // loop: all weeks in all years
      for (int weekIndex = 0; weekIndex < numAllWeeks; weekIndex++) {

         if (weekIndex < sumYearWeeks) {

            // is still in the same year

         } else {

            // advance to the next year

            yearIndex++;

            final int yearWeeks = allYear_NumWeeks[yearIndex];

            prevSumWeeks = sumYearWeeks;
            sumYearWeeks += yearWeeks;
         }

         final int year = allYear_Numbers[yearIndex];
         final int week = weekIndex - prevSumWeeks;

         // loop: all tour types
         for (int tourTypeIndex = 0; tourTypeIndex < allNumTours.length; tourTypeIndex++) {

            final long tourTypeId = allTourTypeIds[tourTypeIndex][weekIndex];

            /*
             * Check if this type is used
             */
            String tourTypeName = UI.EMPTY_STRING;

            boolean isDataForTourType = false;

            for (final long usedTourTypeIdValue : allUsedTourTypeIds) {
               if (usedTourTypeIdValue == tourTypeId) {

                  isDataForTourType = usedTourTypeIdValue != TourType.TOUR_TYPE_IS_NOT_USED;

                  tourTypeName = isDataForTourType
                        ? TourDatabase.getTourTypeName(tourTypeId)
                        : UI.EMPTY_STRING;

                  break;
               }
            }

            final float numTours = _tourWeekData.numTours_High[tourTypeIndex][weekIndex];

            if (isDataForTourType && numTours > 0) {

               // group values
               if (year != prevYear) {

                  prevYear = year;

                  sb.append(NL);
               }

               Object sequenceNumberValue = UI.EMPTY_STRING;
               if (isShowSequenceNumbers) {
                  sequenceNumberValue = ++sequenceNumber;
               }

               final LocalDate valueStatisticDay = firstStatisticDay.plusWeeks(weekIndex);
               final String weekStartDay = TimeTools.Formatter_Date_S.format(valueStatisticDay);

               sb.append(String.format(valueFormatting,

                     sequenceNumberValue,

                     year,
                     week + 1,
                     weekStartDay,

                     tourTypeName,

                     _tourWeekData.elapsedTime[tourTypeIndex][weekIndex],
                     _tourWeekData.recordedTime[tourTypeIndex][weekIndex],
                     _tourWeekData.pausedTime[tourTypeIndex][weekIndex],

                     _tourWeekData.movingTime[tourTypeIndex][weekIndex],
                     _tourWeekData.breakTime[tourTypeIndex][weekIndex],

                     _tourWeekData.distance_High[tourTypeIndex][weekIndex] / 1000,
                     _tourWeekData.elevationUp_High[tourTypeIndex][weekIndex],

                     numTours

               ));

               sb.append(NL);
            }
         }
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   TourStatisticData_Week getWeekData(final TourPerson person,
                                      final TourTypeFilter tourTypeFilter,
                                      final int lastYear,
                                      final int numberOfYears,
                                      final boolean refreshData,
                                      final DurationTime durationTime) {

      // when the data for the year are already loaded, all is done
      if (statistic_ActivePerson == person
            && statistic_ActiveTourTypeFilter == tourTypeFilter
            && lastYear == statistic_LastYear
            && numberOfYears == statistic_NumberOfYears
            && refreshData == false) {

         return _tourWeekData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;

         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numberOfYears;

         setupYearNumbers();

         _tourWeekData = new TourStatisticData_Week();

         // get the tour types
         final ArrayList<TourType> allActiveTourTypesList = TourDatabase.getActiveTourTypes();
         final TourType[] allActiveTourTypes = allActiveTourTypesList.toArray(new TourType[allActiveTourTypesList.size()]);

         int numAllWeeks = 0;
         for (final int weeks : allYear_NumWeeks) {
            numAllWeeks += weeks;
         }

         int numTourTypes = allActiveTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         String fromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder(true);

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            fromTourData = NL

                  + "FROM (" + NL //                                                                     //$NON-NLS-1$

                  + "   SELECT" + NL //                                                                  //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                                     //$NON-NLS-1$

                  + "      StartWeekYear," + NL //                                                       //$NON-NLS-1$
                  + "      StartWeek," + NL //                                                           //$NON-NLS-1$

                  + "      TourDeviceTime_Elapsed," + NL //                                              //$NON-NLS-1$
                  + "      TourDeviceTime_Recorded," + NL //                                             //$NON-NLS-1$
                  + "      TourDeviceTime_Paused," + NL //                                               //$NON-NLS-1$
                  + "      TourComputedTime_Moving," + NL //                                             //$NON-NLS-1$

                  + "      TourDistance," + NL //                                                        //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                           //$NON-NLS-1$

                  + "      TourType_TypeId," + NL //                                                      //$NON-NLS-1$

                  + "      BodyWeight,         " + NL //       //$NON-NLS-1$
                  + "      BodyFat          " + NL //       //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                    //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //              //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                         //$NON-NLS-1$

                  + "   WHERE StartWeekYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() + NL //                                     //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                                        //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                       //$NON-NLS-1$

                  + "WHERE StartWeekYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

            ;
         }

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                //$NON-NLS-1$

               + "   StartWeekYear," + NL //                                  1  //$NON-NLS-1$
               + "   StartWeek," + NL //                                      2  //$NON-NLS-1$

               + "   TourType_TypeId," + NL //                                3  //$NON-NLS-1$

               + "   SUM(TourDeviceTime_Elapsed)," + NL //                    4  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Recorded)," + NL //                   5  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Paused)," + NL //                     6  //$NON-NLS-1$
               + "   SUM(TourComputedTime_Moving)," + NL //                   7  //$NON-NLS-1$
               + "   " + createSQL_SumDurationTime(durationTime) + NL //      8  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                              9  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                                 10 //$NON-NLS-1$

               + "   SUM(1)," + NL //                                          11 //$NON-NLS-1$

               + "   AVG( CASE WHEN BodyWeight = 0         THEN NULL ELSE BodyWeight END)," + NL //      12 //$NON-NLS-1$
               + "   AVG( CASE WHEN BodyFat = 0         THEN NULL ELSE BodyFat END)" + NL //      13 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartWeekYear, StartWeek, tourType_typeId" + NL //    //$NON-NLS-1$
               + "ORDER BY StartWeekYear, StartWeek" + NL //                     //$NON-NLS-1$
         ;

         final long[][] allDbTypeIds = new long[numTourTypes][numAllWeeks];
         final long[] allUsedTourTypeIds = new long[numTourTypes];

         /*
          * Initialize tour types, when there are 0 tours for some years/weeks, a tour
          * type 0 could be a valid tour type which is the default values for native arrays
          * -> wrong tour type
          */
         Arrays.fill(allUsedTourTypeIds, TourType.TOUR_TYPE_IS_NOT_USED);

         final int[][] allDbDurationTime = new int[numTourTypes][numAllWeeks];
         final int[][] allDbElapsedTime = new int[numTourTypes][numAllWeeks];
         final int[][] allDbRecordedTime = new int[numTourTypes][numAllWeeks];
         final int[][] allDbPausedTime = new int[numTourTypes][numAllWeeks];
         final int[][] allDbMovingTime = new int[numTourTypes][numAllWeeks];
         final int[][] allDbBreakTime = new int[numTourTypes][numAllWeeks];

         final float[][] allDbDistance = new float[numTourTypes][numAllWeeks];
         final float[][] allDbElevation = new float[numTourTypes][numAllWeeks];

         final float[][] allDbNumTours = new float[numTourTypes][numAllWeeks];

         final float[] allDbBodyWeight = new float[numAllWeeks];
         final float[] allDbBodyFat = new float[numAllWeeks];

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final int dbValue_CW_Year = result.getInt(1);
            final int dbValue_CW_Week = result.getInt(2);

            // get number of weeks for the current year in the db
            final int dbYearIndex = numberOfYears - (lastYear - dbValue_CW_Year + 1);
            int allWeeks = 0;
            for (int yearIndex = 0; yearIndex <= dbYearIndex; yearIndex++) {
               if (yearIndex > 0) {
                  allWeeks += allYear_NumWeeks[yearIndex - 1];
               }
            }

            final int weekIndex = allWeeks + dbValue_CW_Week - 1;

            if (weekIndex < 0) {

               /**
                * This can occur when dbWeek == 0, tour is in the previous year and not displayed
                * in the week stats
                */

               continue;
            }

            if (weekIndex >= numAllWeeks) {

               /**
                * This problem occurred but is not yet fully fixed, it needs more investigation.
                * <p>
                * Problem with this configuration</br>
                * Statistic: Week summary</br>
                * Tour type: Velo (3 bars)</br>
                * Displayed years: 2013 + 2014
                * <p>
                * Problem occurred when selecting year 2015
                */
               continue;
            }

// SET_FORMATTING_OFF

            final Long dbValue_TypeIdObject     = (Long) result.getObject(3);

            final int dbValue_ElapsedTime       = result.getInt(4);
            final int dbValue_RecordedTime      = result.getInt(5);
            final int dbValue_PausedTime        = result.getInt(6);
            final int dbValue_MovingTime        = result.getInt(7);

            final int dbValue_DurationTime      = result.getInt(8);

            final int dbValue_Distance          = (int) (result.getInt(9) / UI.UNIT_VALUE_DISTANCE);
            final int dbValue_Elevation         = (int) (result.getInt(10) / UI.UNIT_VALUE_ELEVATION);

            final int dbValue_NumTours          = result.getInt(11);

            final float dbValue_BodyWeight      = result.getFloat(12) * UI.UNIT_VALUE_WEIGHT;
            final float dbValue_BodyFat         = result.getFloat(13);

// SET_FORMATTING_ON

            /*
             * Convert type id to the type index in the tour types list which is also the color
             * index
             */
            int colorIndex = 0;
            if (dbValue_TypeIdObject != null) {
               final long dbTypeId = dbValue_TypeIdObject;
               for (int typeIndex = 0; typeIndex < allActiveTourTypes.length; typeIndex++) {
                  if (dbTypeId == allActiveTourTypes[typeIndex].getTypeId()) {
                     colorIndex = typeIndex;
                     break;
                  }
               }
            }
            final long dbTypeId = dbValue_TypeIdObject == null ? TourDatabase.ENTITY_IS_NOT_SAVED : dbValue_TypeIdObject;

            allDbTypeIds[colorIndex][weekIndex] = dbTypeId;
            allUsedTourTypeIds[colorIndex] = dbTypeId;

            allDbElapsedTime[colorIndex][weekIndex] = dbValue_ElapsedTime;
            allDbRecordedTime[colorIndex][weekIndex] = dbValue_RecordedTime;
            allDbPausedTime[colorIndex][weekIndex] = dbValue_PausedTime;
            allDbMovingTime[colorIndex][weekIndex] = dbValue_MovingTime;
            allDbBreakTime[colorIndex][weekIndex] = dbValue_ElapsedTime - dbValue_MovingTime;
            allDbDurationTime[colorIndex][weekIndex] = dbValue_DurationTime;

            allDbDistance[colorIndex][weekIndex] = dbValue_Distance;
            allDbElevation[colorIndex][weekIndex] = dbValue_Elevation;

            allDbNumTours[colorIndex][weekIndex] = dbValue_NumTours;

            if (dbValue_BodyWeight > 0) {
               allDbBodyWeight[weekIndex] = dbValue_BodyWeight;
            }
            if (dbValue_BodyFat > 0) {
               allDbBodyFat[weekIndex] = dbValue_BodyFat;
            }
         }

         _tourWeekData.years = allYear_Numbers;
         _tourWeekData.yearWeeks = allYear_NumWeeks;
         _tourWeekData.yearDays = allYear_NumDays;

         _tourWeekData.typeIds = allDbTypeIds;
         _tourWeekData.usedTourTypeIds = allUsedTourTypeIds;

         _tourWeekData.elapsedTime = allDbElapsedTime;
         _tourWeekData.recordedTime = allDbRecordedTime;
         _tourWeekData.pausedTime = allDbPausedTime;
         _tourWeekData.movingTime = allDbMovingTime;
         _tourWeekData.breakTime = allDbBreakTime;

         _tourWeekData.setDurationTimeLow(new int[numTourTypes][numAllWeeks]);
         _tourWeekData.setDurationTimeHigh(allDbDurationTime);

         _tourWeekData.distance_Low = new float[numTourTypes][numAllWeeks];
         _tourWeekData.distance_High = allDbDistance;

         _tourWeekData.elevationUp_Low = new float[numTourTypes][numAllWeeks];
         _tourWeekData.elevationUp_High = allDbElevation;

         _tourWeekData.numTours_Low = new float[numTourTypes][numAllWeeks];
         _tourWeekData.numTours_High = allDbNumTours;

         _tourWeekData.athleteBodyWeight_Low = new float[numAllWeeks];
         _tourWeekData.athleteBodyWeight_High = allDbBodyWeight;
         _tourWeekData.athleteBodyFat_Low = new float[numAllWeeks];
         _tourWeekData.athleteBodyFat_High = allDbBodyFat;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourWeekData;
   }
}
