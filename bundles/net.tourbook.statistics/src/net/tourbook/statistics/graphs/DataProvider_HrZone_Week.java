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

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_HrZone_Week extends DataProvider {

   private TourStatisticData_WeekHrZones _weekData;

   String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_weekData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()
            + STAT_VALUE_DATE_WEEK.getHead1()
            + STAT_VALUE_DATE_WEEK_START.getHead1()

            + STAT_VALUE_HR_ZONE_1.getHead1()
            + STAT_VALUE_HR_ZONE_2.getHead1()
            + STAT_VALUE_HR_ZONE_3.getHead1()
            + STAT_VALUE_HR_ZONE_4.getHead1()
            + STAT_VALUE_HR_ZONE_5.getHead1()
            + STAT_VALUE_HR_ZONE_6.getHead1()
            + STAT_VALUE_HR_ZONE_7.getHead1()
            + STAT_VALUE_HR_ZONE_8.getHead1()
            + STAT_VALUE_HR_ZONE_9.getHead1()
            + STAT_VALUE_HR_ZONE_10.getHead1()

            + STAT_VALUE_HR_SUMMARY_SECONDS.getHead1()
            + STAT_VALUE_HR_SUMMARY_HHMMSS.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()
            + STAT_VALUE_DATE_WEEK.getHead2()
            + STAT_VALUE_DATE_WEEK_START.getHead2()

            + STAT_VALUE_HR_ZONE_1.getHead2()
            + STAT_VALUE_HR_ZONE_2.getHead2()
            + STAT_VALUE_HR_ZONE_3.getHead2()
            + STAT_VALUE_HR_ZONE_4.getHead2()
            + STAT_VALUE_HR_ZONE_5.getHead2()
            + STAT_VALUE_HR_ZONE_6.getHead2()
            + STAT_VALUE_HR_ZONE_7.getHead2()
            + STAT_VALUE_HR_ZONE_8.getHead2()
            + STAT_VALUE_HR_ZONE_9.getHead2()
            + STAT_VALUE_HR_ZONE_10.getHead2()

            + STAT_VALUE_HR_SUMMARY_SECONDS.getHead2()
            + STAT_VALUE_HR_SUMMARY_HHMMSS.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : VALUE_FORMAT_S)

            + STAT_VALUE_DATE_YEAR.getValueFormatting()
            + STAT_VALUE_DATE_WEEK.getValueFormatting()
            + STAT_VALUE_DATE_WEEK_START.getValueFormatting()

            + STAT_VALUE_HR_ZONE_1.getValueFormatting()
            + STAT_VALUE_HR_ZONE_2.getValueFormatting()
            + STAT_VALUE_HR_ZONE_3.getValueFormatting()
            + STAT_VALUE_HR_ZONE_4.getValueFormatting()
            + STAT_VALUE_HR_ZONE_5.getValueFormatting()
            + STAT_VALUE_HR_ZONE_6.getValueFormatting()
            + STAT_VALUE_HR_ZONE_7.getValueFormatting()
            + STAT_VALUE_HR_ZONE_8.getValueFormatting()
            + STAT_VALUE_HR_ZONE_9.getValueFormatting()
            + STAT_VALUE_HR_ZONE_10.getValueFormatting()

            + STAT_VALUE_HR_SUMMARY_SECONDS.getValueFormatting()
            + STAT_VALUE_HR_SUMMARY_HHMMSS.getValueFormatting()

      ;

      final StringBuilder sb = new StringBuilder();
      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final int[][] hrZoneValues = _weekData.hrZoneValues;
      final int numWeeks = hrZoneValues[0].length;
      final int firstYear = statistic_LastYear - statistic_NumberOfYears + 1;
      int prevYear = firstYear;

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

      for (int weekIndex = 0; weekIndex < numWeeks; weekIndex++) {

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

         int sumSeconds = 0;
         for (final int[] hrZoneValue : hrZoneValues) {
            sumSeconds += hrZoneValue[weekIndex];
         }

         if (sumSeconds > 0) {

            final String sumHHMMSS = net.tourbook.common.UI.format_hhh_mm_ss(sumSeconds);

            Object sequenceNumberValue = UI.EMPTY_STRING;
            if (isShowSequenceNumbers) {
               sequenceNumberValue = ++sequenceNumber;
            }

            final LocalDate valueStatisticDay = firstStatisticDay.plusWeeks(weekIndex);
            final String weekStartDay = TimeTools.Formatter_Date_S.format(valueStatisticDay);

            // group values
            if (year != prevYear) {

               prevYear = year;

               sb.append(NL);
            }

            sb.append(String.format(valueFormatting,

                  sequenceNumberValue,

                  year,
                  week + 1,
                  weekStartDay,

                  hrZoneValues[0][weekIndex],
                  hrZoneValues[1][weekIndex],
                  hrZoneValues[2][weekIndex],
                  hrZoneValues[3][weekIndex],
                  hrZoneValues[4][weekIndex],
                  hrZoneValues[5][weekIndex],
                  hrZoneValues[6][weekIndex],
                  hrZoneValues[7][weekIndex],
                  hrZoneValues[8][weekIndex],
                  hrZoneValues[9][weekIndex],

                  sumSeconds,
                  sumHHMMSS

            ));

            sb.append(NL);
         }
      }
      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   TourStatisticData_WeekHrZones getWeekData(final TourPerson person,
                                             final TourTypeFilter tourTypeFilter,
                                             final int lastYear,
                                             final int numYears,
                                             final boolean refreshData) {

      /*
       * check if the required data are already loaded
       */
      if (statistic_ActivePerson == person
            && statistic_ActiveTourTypeFilter == tourTypeFilter
            && lastYear == statistic_LastYear
            && numYears == statistic_NumberOfYears
            && refreshData == false) {

         return _weekData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;

         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numYears;

         setupYearNumbers();

         final int maxZones = 10; // hr zones: 0...9
         int numberOfWeeks = 0;
         for (final int weeks : allYear_NumWeeks) {
            numberOfWeeks += weeks;
         }

         final int serieLength = maxZones;
         final int valueLength = numberOfWeeks;

         _weekData = new TourStatisticData_WeekHrZones();

         String sqlFromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder(true);

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            sqlFromTourData = UI.EMPTY_STRING

                  + "FROM (" + NL //                                    //$NON-NLS-1$

                  + "   SELECT" + NL //                                 //$NON-NLS-1$

                  + "      StartWeekYear," + NL //                      //$NON-NLS-1$
                  + "      StartWeek," + NL //                          //$NON-NLS-1$

                  + "      HrZone0," + NL //                            //$NON-NLS-1$
                  + "      HrZone1," + NL //                            //$NON-NLS-1$
                  + "      HrZone2," + NL //                            //$NON-NLS-1$
                  + "      HrZone3," + NL //                            //$NON-NLS-1$
                  + "      HrZone4," + NL //                            //$NON-NLS-1$
                  + "      HrZone5," + NL //                            //$NON-NLS-1$
                  + "      HrZone6," + NL //                            //$NON-NLS-1$
                  + "      HrZone7," + NL //                            //$NON-NLS-1$
                  + "      HrZone8," + NL //                            //$NON-NLS-1$
                  + "      HrZone9" + NL //                             //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //   //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //        //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                   //$NON-NLS-1$

                  + "   WHERE StartWeekYear IN (" + getYearList(lastYear, numYears) + ")" + NL //  //$NON-NLS-1$ //$NON-NLS-2$
                  + "      AND NumberOfHrZones > 0" + NL //                                        //$NON-NLS-1$
                  + "      " + sqlAppFilter.getWhereClause() + NL //                               //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                                  //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            sqlFromTourData = UI.EMPTY_STRING

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                 //$NON-NLS-1$

                  + "WHERE StartWeekYear IN (" + getYearList(lastYear, numYears) + ")" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
                  + "   AND NumberOfHrZones > 0" + NL //                                           //$NON-NLS-1$
                  + "   " + sqlAppFilter.getWhereClause() + NL //                                  //$NON-NLS-1$

            ;
         }

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$

               + "   StartWeekYear," + NL //                                        1  //$NON-NLS-1$
               + "   StartWeek," + NL //                                            2  //$NON-NLS-1$

               + "   SUM(CASE WHEN hrZone0 > 0 THEN hrZone0 ELSE 0 END)," + NL //   3  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone1 > 0 THEN hrZone1 ELSE 0 END)," + NL //   4  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone2 > 0 THEN hrZone2 ELSE 0 END)," + NL //   5  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone3 > 0 THEN hrZone3 ELSE 0 END)," + NL //   6  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone4 > 0 THEN hrZone4 ELSE 0 END)," + NL //   7  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone5 > 0 THEN hrZone5 ELSE 0 END)," + NL //   8  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone6 > 0 THEN hrZone6 ELSE 0 END)," + NL //   9  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone7 > 0 THEN hrZone7 ELSE 0 END)," + NL //   10 //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone8 > 0 THEN hrZone8 ELSE 0 END)," + NL //   11 //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone9 > 0 THEN hrZone9 ELSE 0 END)" + NL //    12 //$NON-NLS-1$

               + sqlFromTourData

               + "GROUP BY StartWeekYear, StartWeek" + NL //                           //$NON-NLS-1$
               + "ORDER BY StartWeekYear, StartWeek" + NL //                           //$NON-NLS-1$
         ;

         final int[][] dbHrZoneValues = new int[serieLength][valueLength];

         {
            final PreparedStatement prepStmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

            sqlAppFilter.setParameters(prepStmt, paramIndex);

            final ResultSet result = prepStmt.executeQuery();
            while (result.next()) {

               final int dbValue_CW_Year = result.getInt(1);
               final int dbValue_CW_Week = result.getInt(2);

               // get number of weeks for the current year in the db
               final int dbYearIndex = numYears - (lastYear - dbValue_CW_Year + 1);
               int allWeeks = 0;
               for (int yearIndex = 0; yearIndex <= dbYearIndex; yearIndex++) {
                  if (yearIndex > 0) {
                     allWeeks += allYear_NumWeeks[yearIndex - 1];
                  }
               }

               final int weekIndex = allWeeks + dbValue_CW_Week - 1;

               dbHrZoneValues[0][weekIndex] = result.getInt(3);
               dbHrZoneValues[1][weekIndex] = result.getInt(4);
               dbHrZoneValues[2][weekIndex] = result.getInt(5);
               dbHrZoneValues[3][weekIndex] = result.getInt(6);
               dbHrZoneValues[4][weekIndex] = result.getInt(7);
               dbHrZoneValues[5][weekIndex] = result.getInt(8);
               dbHrZoneValues[6][weekIndex] = result.getInt(9);
               dbHrZoneValues[7][weekIndex] = result.getInt(10);
               dbHrZoneValues[8][weekIndex] = result.getInt(11);
               dbHrZoneValues[9][weekIndex] = result.getInt(12);
            }
         }

         _weekData.hrZoneValues = dbHrZoneValues;

         _weekData.years = allYear_Numbers;
         _weekData.yearWeeks = allYear_NumWeeks;
         _weekData.yearDays = allYear_NumDays;

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      setStatisticValues();

      return _weekData;
   }

   private void setStatisticValues() {

      final StringBuilder sb = new StringBuilder();

//      sb.append("\n" //$NON-NLS-1$
//
//            + " week-year," //$NON-NLS-1$
//            + "     zone1," //$NON-NLS-1$
//            + "     zone2," //$NON-NLS-1$
//            + "     zone3," //$NON-NLS-1$
//            + "     zone4," //$NON-NLS-1$
//            + "     zone5," //$NON-NLS-1$
//            + "     zone6," //$NON-NLS-1$
//            + "     zone7," //$NON-NLS-1$
//            + "     zone8," //$NON-NLS-1$
//            + "     zone9," //$NON-NLS-1$
//            + "    zone10," //$NON-NLS-1$
//
//            + "      sum-sec," //$NON-NLS-1$
//            + " sum-hh-mm-ss" //$NON-NLS-1$
//
//      );

      sb.append(UI.EMPTY_STRING

            + "Year," //$NON-NLS-1$
            + "  Week," //$NON-NLS-1$
            + "     Zone1," //$NON-NLS-1$
            + "      Zone2," //$NON-NLS-1$
            + "      Zone3," //$NON-NLS-1$
            + "      Zone4," //$NON-NLS-1$
            + "      Zone5," //$NON-NLS-1$
            + "      Zone6," //$NON-NLS-1$
            + "      Zone7," //$NON-NLS-1$
            + "      Zone8," //$NON-NLS-1$
            + "      Zone9," //$NON-NLS-1$
            + "     Zone10," //$NON-NLS-1$

            + "      Summary," //$NON-NLS-1$
            + "       Summary" //$NON-NLS-1$

            + NL);

      sb.append(UI.EMPTY_STRING

            // year, week
            + "    ," //$NON-NLS-1$
            + "     ," //$NON-NLS-1$

            // zones
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$

            // summary
            + "          (s)," //$NON-NLS-1$
            + "    (hh:mm:ss)" //$NON-NLS-1$

            + NL);

      final String valueFormatting = UI.EMPTY_STRING

            // year, week
            + "%4d,  %4d," //$NON-NLS-1$

            // zone 1...10
            + "%10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d," //$NON-NLS-1$

            // summaries
            + "%13d, %13s" //$NON-NLS-1$

            + NL;

      final int[][] hrZoneValues = _weekData.hrZoneValues;
      final int numWeeks = hrZoneValues[0].length;

      int yearIndex = 0;
      int prevSumWeeks = 0;
      int sumYearWeeks = allYear_NumWeeks[yearIndex];

      // setup previous year
      int prevYear = allYear_Numbers[0];

      for (int weekIndex = 0; weekIndex < numWeeks; weekIndex++) {

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

         int sumSeconds = 0;
         for (final int[] hrZoneValue : hrZoneValues) {
            sumSeconds += hrZoneValue[weekIndex];
         }

         final String sumHHMMSS = net.tourbook.common.UI.format_hhh_mm_ss(sumSeconds);

         // group by year
         if (year != prevYear) {
            prevYear = year;
            sb.append(NL);
         }

         sb.append(String.format(valueFormatting,

               year,
               week + 1,

               hrZoneValues[0][weekIndex],
               hrZoneValues[1][weekIndex],
               hrZoneValues[2][weekIndex],
               hrZoneValues[3][weekIndex],
               hrZoneValues[4][weekIndex],
               hrZoneValues[5][weekIndex],
               hrZoneValues[6][weekIndex],
               hrZoneValues[7][weekIndex],
               hrZoneValues[8][weekIndex],
               hrZoneValues[9][weekIndex],

               sumSeconds,
               sumHHMMSS

         ));
      }

      _weekData.statisticValuesRaw = sb.toString();

   }
}
