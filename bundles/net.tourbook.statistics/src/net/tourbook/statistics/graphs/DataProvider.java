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

import java.time.ZonedDateTime;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourPerson;
import net.tourbook.statistic.DurationTime;
import net.tourbook.ui.TourTypeFilter;

public abstract class DataProvider {

   static final char NL = net.tourbook.common.UI.NEW_LINE;

// SET_FORMATTING_OFF

   // labels and formatting for statistic values

   /*
    * Data
    */
   static final String HEAD1_DATA_NUMBER                    = "     #  \t ";                //$NON-NLS-1$
   static final String HEAD2_DATA_NUMBER                    = "        \t ";                //$NON-NLS-1$
   static final String VALUE_DATA_NUMBER                    = "%6d\t   ";                   //$NON-NLS-1$

   /*
    * Date
    */
   static final String HEAD1_DATE_YEAR                      = "Year\t";                   //$NON-NLS-1$
   static final String HEAD2_DATE_YEAR                      = "    \t";                   //$NON-NLS-1$
   static final String VALUE_DATE_YEAR                      = "%4d\t";                    //$NON-NLS-1$

   static final String HEAD1_DATE_MONTH                     = " Month\t";                 //$NON-NLS-1$
   static final String HEAD2_DATE_MONTH                     = "      \t";                 //$NON-NLS-1$
   static final String VALUE_DATE_MONTH                     = "   %3d\t";                 //$NON-NLS-1$

   static final String HEAD1_DATE_DAY                       = " Day\t";                   //$NON-NLS-1$
   static final String HEAD2_DATE_DAY                       = "    \t";                   //$NON-NLS-1$
   static final String VALUE_DATE_DAY                       = " %3d\t";                   //$NON-NLS-1$

   static final String HEAD1_DATE_WEEK                      = " Week\t";                  //$NON-NLS-1$
   static final String HEAD2_DATE_WEEK                      = "     \t";                  //$NON-NLS-1$
   static final String VALUE_DATE_WEEK                      = "   %2d\t";                 //$NON-NLS-1$

   static final String HEAD1_DATE_WEEK_START                = "    First\t";              //$NON-NLS-1$
   static final String HEAD2_DATE_WEEK_START                = "      Day\t";              //$NON-NLS-1$
   static final String VALUE_DATE_WEEK_START                = " %8.8s\t";                 //$NON-NLS-1$

   static final String HEAD1_DATE_DOY                       = " DOY\t";                   //$NON-NLS-1$
   static final String HEAD2_DATE_DOY                       = "    \t";                   //$NON-NLS-1$
   static final String VALUE_DATE_DOY                       = " %3d\t";                   //$NON-NLS-1$

   /*
    * Time
    */
   static final String HEAD1_DEVICE_TIME_ELAPSED            = "    Elapsed\t";               //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_ELAPSED            = "        (s)\t";               //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_ELAPSED            = " %10d\t";                  //$NON-NLS-1$

   static final String HEAD1_DEVICE_TIME_RECORDED           = "   Recorded\t";              //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_RECORDED           = "        (s)\t";              //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_RECORDED           = " %10d\t";                 //$NON-NLS-1$

   static final String HEAD1_DEVICE_TIME_PAUSED             = "     Paused\t";                //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_PAUSED             = "        (s)\t";                //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_PAUSED             = " %10d\t";                   //$NON-NLS-1$

   static final String HEAD1_COMPUTED_TIME_MOVING           = "     Moving\t";                //$NON-NLS-1$
   static final String HEAD2_COMPUTED_TIME_MOVING           = "        (s)\t";                //$NON-NLS-1$
   static final String VALUE_COMPUTED_TIME_MOVING           = " %10d\t";                   //$NON-NLS-1$

   static final String HEAD1_COMPUTED_TIME_BREAK            = "      Break\t";                //$NON-NLS-1$
   static final String HEAD2_COMPUTED_TIME_BREAK            = "        (s)\t";                //$NON-NLS-1$
   static final String VALUE_COMPUTED_TIME_BREAK            = " %10d\t";                   //$NON-NLS-1$

   static final String HEAD1_ELEVATION                      = "  Elevation\t";             //$NON-NLS-1$
   static final String HEAD2_ELEVATION                      = "        (m)\t";             //$NON-NLS-1$
   static final String VALUE_ELEVATION                      = " %10.0f\t";              //$NON-NLS-1$

   static final String HEAD1_DISTANCE                       = "   Distance\t";              //$NON-NLS-1$
   static final String HEAD2_DISTANCE                       = "        (m)\t";              //$NON-NLS-1$
   static final String VALUE_DISTANCE                       = " %10.0f\t";                 //$NON-NLS-1$

   /*
    * Speed
    */
   static final String HEAD1_SPEED                          = "  Speed\t";                //$NON-NLS-1$
   static final String HEAD2_SPEED                          = " (km/h)\t";                //$NON-NLS-1$
   static final String VALUE_SPEED                          = "%7.2f\t";                  //$NON-NLS-1$

   static final String HEAD1_PACE                           = "     Pace\t";              //$NON-NLS-1$
   static final String HEAD2_PACE                           = " (min/km)\t";              //$NON-NLS-1$
   static final String VALUE_PACE                           = "   %6.2f\t";               //$NON-NLS-1$

   /*
    * HR Zones
    */
   static final String HEAD1_HR_ZONE_1                      = "     Zone 1\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_2                      = "     Zone 2\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_3                      = "     Zone 3\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_4                      = "     Zone 4\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_5                      = "     Zone 5\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_6                      = "     Zone 6\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_7                      = "     Zone 7\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_8                      = "     Zone 8\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_9                      = "     Zone 9\t";              //$NON-NLS-1$
   static final String HEAD1_HR_ZONE_10                     = "    Zone 10\t";              //$NON-NLS-1$
   static final String HEAD1_HR_SUMMARY                     = "       Summary\t";              //$NON-NLS-1$

   static final String HEAD2_HR_SUMMARY_SECONDS             = "           (s)\t";              //$NON-NLS-1$
   static final String VALUE_HR_SUMMARY_SECONDS             = " %13d\t";              //$NON-NLS-1$

   static final String HEAD2_HR_SUMMARY_HHMMSS              = "    (hh:mm:ss)\t";              //$NON-NLS-1$
   static final String VALUE_HR_SUMMARY_HHMMSS              = " %13s\t";              //$NON-NLS-1$

   static final String HEAD2_HR_ZONE                        = "        (s)\t";              //$NON-NLS-1$
   static final String VALUE_HR_ZONE                        = " %10d\t";              //$NON-NLS-1$

   /*
    * Training
    */
   static final String HEAD1_TRAINING_AEROB                 = " Training\t";              //$NON-NLS-1$
   static final String HEAD2_TRAINING_AEROB                 = "    Aerob\t";              //$NON-NLS-1$
   static final String VALUE_TRAINING_AEROB                 = "   %6.1f\t";               //$NON-NLS-1$

   static final String HEAD1_TRAINING_ANAEROB               = " Training\t";              //$NON-NLS-1$
   static final String HEAD2_TRAINING_ANAEROB               = "  Anaerob\t";              //$NON-NLS-1$
   static final String VALUE_TRAINING_ANAEROB               = "   %6.1f\t";               //$NON-NLS-1$

   static final String HEAD1_TRAINING_PERFORMANCE           = " Training\t";              //$NON-NLS-1$
   static final String HEAD2_TRAINING_PERFORMANCE           = "  Perform\t";              //$NON-NLS-1$
   static final String VALUE_TRAINING_PERFORMANCE           = "   %6.2f\t";               //$NON-NLS-1$

   /*
    * Tour
    */
   static final String HEAD1_NUMBER_OF_TOURS                = "  Tours\t";                //$NON-NLS-1$
   static final String HEAD2_NUMBER_OF_TOURS                = "    (#)\t";                //$NON-NLS-1$
   static final String VALUE_NUMBER_OF_TOURS                = " %6.0f\t";                 //$NON-NLS-1$

   static final String HEAD1_TOUR_TITLE                     = " Title\t";                 //$NON-NLS-1$
   static final String HEAD2_TOUR_TITLE                     = "\t";                       //$NON-NLS-1$
   static final String VALUE_TOUR_TITLE                     = " %s\t";                    //$NON-NLS-1$  // do not truncate text, it is displayed at the end

   static final String HEAD1_TOUR_TYPE                      = " TourType            \t";  //$NON-NLS-1$
   static final String HEAD2_TOUR_TYPE                      = "                     \t";  //$NON-NLS-1$
   static final String VALUE_TOUR_TYPE                      = " %-20.20s\t";              //$NON-NLS-1$  // truncate 20+ characters to force column layout

// SET_FORMATTING_ON

   static ZonedDateTime calendar8 = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   TourPerson           statistic_ActivePerson;
   TourTypeFilter       statistic_ActiveTourTypeFilter;

   int                  statistic_LastYear;

   /**
    * Number of years
    */
   int                  statistic_NumberOfYears;

   /**
    *
    */
   String               statistic_RawStatisticValues;

   /**
    *
    */
   boolean              statistic_isShowSequenceNumbers;

   /**
    * All years numbers (Jahreszahl), e.g. 2016, 2017, ... 2020
    */
   int[]                allYear_Numbers;

   /**
    * Number of days in a year
    */
   int[]                allYear_NumDays;

   /**
    * Number of weeks in a year
    */
   int[]                allYear_NumWeeks;

   static String createSQL_SumDurationTime(final DurationTime durationTime) {

      String sqlSumDurationTime = null;

      switch (durationTime) {
      case BREAK:

         sqlSumDurationTime = "SUM(TourDeviceTime_Elapsed - TourComputedTime_Moving),"; //$NON-NLS-1$
         break;

      case ELAPSED:

         sqlSumDurationTime = "SUM(TourDeviceTime_Elapsed),"; //$NON-NLS-1$
         break;

      case PAUSED:

         sqlSumDurationTime = "SUM(TourDeviceTime_Paused),"; //$NON-NLS-1$
         break;

      case RECORDED:

         sqlSumDurationTime = "SUM(TourDeviceTime_Recorded),"; //$NON-NLS-1$
         break;

      case MOVING:
      default:
         // this is also the old implementation for the duration values
         sqlSumDurationTime = "SUM(CASE WHEN TourComputedTime_Moving > 0 THEN TourComputedTime_Moving ELSE TourDeviceTime_Elapsed END),"; //$NON-NLS-1$
         break;
      }

      return sqlSumDurationTime;
   }

   /**
    * @param finalYear
    * @param numberOfYears
    * @return Returns a list with all years
    */
   static String getYearList(final int finalYear, final int numberOfYears) {

      final StringBuilder sb = new StringBuilder();

      for (int currentYear = finalYear; currentYear >= finalYear - numberOfYears + 1; currentYear--) {

         if (currentYear != finalYear) {
            sb.append(',');
         }

         sb.append(Integer.toString(currentYear));
      }

      return sb.toString();
   }

   /**
    * @param currentYear
    * @param numberOfYears
    * @return Returns the number of days between {@link #statistic_LastYear} and currentYear
    */
   int getYearDOYs(final int selectedYear) {

      int yearDOYs = 0;
      int yearIndex = 0;

      for (int currentYear = statistic_LastYear - statistic_NumberOfYears + 1; currentYear < selectedYear; currentYear++) {

         if (currentYear == selectedYear) {
            return yearDOYs;
         }

         yearDOYs += allYear_NumDays[yearIndex];

         yearIndex++;
      }

      return yearDOYs;
   }

   /**
    * Get different data for each year, data are set into <br>
    * <br>
    * All years in {@link #allYear_Numbers} <br>
    * Number of day's in {@link #allYear_NumDays} <br>
    * Number of week's in {@link #allYear_NumWeeks}
    */
   void setupYearNumbers() {

      /**
       * Log num weeks in a year, 2012 has 54 weeks but computed value is 52 !
       * <code>
       *
       * 2012 - number of weeks: 52
       *
       *            day  week  week
       *                   no  year
       *
       *       1.1.2012    52  2011
       *       5.1.2012     1  2012
       *     25.12.2012    52  2012
       *     31.12.2012     1  2013
       *
       * </code>
       */
//      final WeekFields cw = TimeTools.calendarWeek;
//      final TemporalField weekOfWeekBasedYear = cw.weekOfWeekBasedYear();
//      final TemporalField weekBasedYear = cw.weekBasedYear();
//
//      System.out.println();
//      System.out.println();
//      System.out.println();
//      System.out.println();

      allYear_Numbers = new int[statistic_NumberOfYears];
      allYear_NumDays = new int[statistic_NumberOfYears];
      allYear_NumWeeks = new int[statistic_NumberOfYears];

      final int firstYear = statistic_LastYear - statistic_NumberOfYears + 1;
      int yearIndex = 0;

      for (int currentYear = firstYear; currentYear <= statistic_LastYear; currentYear++) {

         final int numOfWeeksInCurrentYear = TimeTools.getNumberOfWeeksWithYear(currentYear);

//         if (currentYear == firstYear) {
//
//            // add one week when first day's of the first year are in a week of the previous year, e.g. 1.1.2012
//
//            final LocalDate jan_1_1 = LocalDate.of(currentYear, 1, 1);
//            final int jan_1_1_Week = jan_1_1.get(weekOfWeekBasedYear);
//
//            if (jan_1_1_Week > 10) {
//               numOfWeeksInCurrentYear++;
//            }
//         }
//
//         if (currentYear == statistic_LastYear) {
//
//            // add one week when the last day's of the last year are in the next year, e.g. 31.12.2012
//
//            final LocalDate dez_31_12 = LocalDate.of(currentYear, 12, 31);
//            final int dez_31_12_Week = dez_31_12.get(weekOfWeekBasedYear);
//
//            if (dez_31_12_Week < 10) {
//               numOfWeeksInCurrentYear++;
//            }
//         }

         allYear_Numbers[yearIndex] = currentYear;
         allYear_NumDays[yearIndex] = TimeTools.getNumberOfDaysWithYear(currentYear);
         allYear_NumWeeks[yearIndex] = numOfWeeksInCurrentYear;

//         final LocalDate jan_1_1 = LocalDate.of(currentYear, 1, 1);
//         final LocalDate jan_1_5 = LocalDate.of(currentYear, 1, 5);
//         final LocalDate dez_31_12 = LocalDate.of(currentYear, 12, 31);
//         final LocalDate dez_25_12 = LocalDate.of(currentYear, 12, 25);
//
//         final int jan_1_1_Week = jan_1_1.get(weekOfWeekBasedYear);
//         final int jan_1_1_Year = jan_1_1.get(weekBasedYear);
//
//         final int jan_1_5_Week = jan_1_5.get(weekOfWeekBasedYear);
//         final int jan_1_5_Year = jan_1_5.get(weekBasedYear);
//
//         final int dez_31_12_Week = dez_31_12.get(weekOfWeekBasedYear);
//         final int dez_31_12_Year = dez_31_12.get(weekBasedYear);
//
//         final int dez_25_12_Week = dez_25_12.get(weekOfWeekBasedYear);
//         final int dez_25_12_Year = dez_25_12.get(weekBasedYear);
//
//         System.out.println();
//         System.out.println("" + currentYear + " - num weeks:" + allYear_NumWeeks[yearIndex]);
//         System.out.println("     1.1. " + jan_1_1_Week + " / " + jan_1_1_Year);
//         System.out.println("     5.1. " + jan_1_5_Week + " / " + jan_1_5_Year);
//         System.out.println("   25.12. " + dez_25_12_Week + " / " + dez_25_12_Year);
//         System.out.println("   31.12. " + dez_31_12_Week + " / " + dez_31_12_Year);
//         // TODO remove SYSTEM.OUT.PRINTLN

         yearIndex++;
      }
   }

}
