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

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistics.Messages;
import net.tourbook.ui.TourTypeFilter;

public abstract class DataProvider {

   private static final String APP_UNIT_HHMMSS        = net.tourbook.Messages.App_Unit_HHMMSS;
   private static final String APP_UNIT_SECONDS_SMALL = net.tourbook.Messages.App_Unit_Seconds_Small;

   private static final String UNIT_NUMBER            = "#";                                         //$NON-NLS-1$

   private static final String VALUE_FORMAT_2D        = "%2d";                                       //$NON-NLS-1$
   private static final String VALUE_FORMAT_3D        = "%3d";                                       //$NON-NLS-1$
   private static final String VALUE_FORMAT_4D        = "%4d";                                       //$NON-NLS-1$
   private static final String VALUE_FORMAT_6D        = "%6d";                                       //$NON-NLS-1$
   private static final String VALUE_FORMAT_10D       = "%10d";                                      //$NON-NLS-1$
   private static final String VALUE_FORMAT_13D       = "%13d";                                      //$NON-NLS-1$

   private static final String VALUE_FORMAT_6_0F      = "%6.0f";                                     //$NON-NLS-1$
   private static final String VALUE_FORMAT_6_1F      = "%6.1f";                                     //$NON-NLS-1$
   private static final String VALUE_FORMAT_6_2F      = "%6.2f";                                     //$NON-NLS-1$
   private static final String VALUE_FORMAT_7_2F      = "%7.2f";                                     //$NON-NLS-1$
   private static final String VALUE_FORMAT_10_0F     = "%10.0f";                                    //$NON-NLS-1$
   private static final String VALUE_FORMAT_10_3F     = "%10.3f";                                    //$NON-NLS-1$

   static final String         VALUE_FORMAT_S         = "%s";                                        //$NON-NLS-1$
   private static final String VALUE_FORMAT_8_8S      = "%8.8s";                                     //$NON-NLS-1$
   private static final String VALUE_FORMAT_13S       = "%13s";                                      //$NON-NLS-1$
   private static final String VALUE_FORMAT_20_20S    = "%-20.20s";                                  //$NON-NLS-1$

   static final char           NL                     = net.tourbook.common.UI.NEW_LINE;

// SET_FORMATTING_OFF

   // labels and formatting for statistic values

   /*
    * Data
    */
   static final StatisticValue STAT_VALUE_SEQUENCE_NUMBER      = new StatisticValue(UNIT_NUMBER,                                             null,    null,    VALUE_FORMAT_6D,   6).withNoSpaceBefore().withSpaceAfter();

   /*
    * Date
    */
   static final StatisticValue STAT_VALUE_DATE_DAY             = new StatisticValue(Messages.Statistic_Value_Date_Day_Header1,               null,    null,    VALUE_FORMAT_3D,   3);
   static final StatisticValue STAT_VALUE_DATE_MONTH           = new StatisticValue(Messages.Statistic_Value_Date_Month_Header1,             null,    null,    VALUE_FORMAT_3D,   3);
   static final StatisticValue STAT_VALUE_DATE_YEAR            = new StatisticValue(Messages.Statistic_Value_Date_Year_Header1,              null,    null,    VALUE_FORMAT_4D,   4).withNoSpaceBefore();

   static final StatisticValue STAT_VALUE_DATE_WEEK            = new StatisticValue(Messages.Statistic_Value_Date_Week_Header1,              null,    null,    VALUE_FORMAT_2D,   2);
   static final StatisticValue STAT_VALUE_DATE_WEEK_START      = new StatisticValue(Messages.Statistic_Value_Date_FirstDay_Header1,          Messages.Statistic_Value_Date_FirstDay_Header2,   null,    VALUE_FORMAT_8_8S, 8);

   /*
    * Time
    */
   static final StatisticValue STAT_VALUE_TIME_DEVICE_ELAPSED  = new StatisticValue(Messages.Statistic_Value_Time_Device_Elapsed_Header1,    null,    APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_TIME_DEVICE_RECORDED = new StatisticValue(Messages.Statistic_Value_Time_Device_Recorded_Header1,   null,    APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_TIME_DEVICE_PAUSED   = new StatisticValue(Messages.Statistic_Value_Time_Device_Paused_Header1,     null,    APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_TIME_COMPUTED_MOVING = new StatisticValue(Messages.Statistic_Value_Time_Computed_Moving_Header1,   null,    APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_TIME_COMPUTED_BREAK  = new StatisticValue(Messages.Statistic_Value_Time_Computed_Break_Header1,    null,    APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);

   /*
    * Motion
    */
   static final StatisticValue STAT_VALUE_MOTION_DISTANCE      = new StatisticValue(Messages.Statistic_Value_Motion_Distance_Header1,        null,    null,    VALUE_FORMAT_10_3F,   10);
   static final StatisticValue STAT_VALUE_MOTION_SPEED         = new StatisticValue(Messages.Statistic_Value_Motion_Speed_Header1,           null,    null,    VALUE_FORMAT_7_2F,   7);
   static final StatisticValue STAT_VALUE_MOTION_PACE          = new StatisticValue(Messages.Statistic_Value_Motion_Pace_Header1,            null,    null,    VALUE_FORMAT_6_2F,   6);

   /*
    * Elevation
    */
   static final StatisticValue STAT_VALUE_ELEVATION_UP         = new StatisticValue(Messages.Statistic_Value_Elevation_ElevationUp_Header1,   null,    null,    VALUE_FORMAT_10_0F,   10);

   /*
    * HR Zones
    */
   static final StatisticValue STAT_VALUE_HR_ZONE_1            = new StatisticValue(Messages.Statistic_Value_HR_Zone_1_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_2            = new StatisticValue(Messages.Statistic_Value_HR_Zone_2_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_3            = new StatisticValue(Messages.Statistic_Value_HR_Zone_3_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_4            = new StatisticValue(Messages.Statistic_Value_HR_Zone_4_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_5            = new StatisticValue(Messages.Statistic_Value_HR_Zone_5_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_6            = new StatisticValue(Messages.Statistic_Value_HR_Zone_6_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_7            = new StatisticValue(Messages.Statistic_Value_HR_Zone_7_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_8            = new StatisticValue(Messages.Statistic_Value_HR_Zone_8_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_9            = new StatisticValue(Messages.Statistic_Value_HR_Zone_9_Header1,               null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_ZONE_10           = new StatisticValue(Messages.Statistic_Value_HR_Zone_10_Header1,              null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_10D,   10);
   static final StatisticValue STAT_VALUE_HR_SUMMARY_SECONDS   = new StatisticValue(Messages.Statistic_Value_HR_Summary_Header1,              null,   APP_UNIT_SECONDS_SMALL,    VALUE_FORMAT_13D,   13);
   static final StatisticValue STAT_VALUE_HR_SUMMARY_HHMMSS    = new StatisticValue(Messages.Statistic_Value_HR_Summary_Header1,              null,   APP_UNIT_HHMMSS,           VALUE_FORMAT_13S,   13);

   /*
    * Training
    */
   static final StatisticValue STAT_VALUE_TRAINING_AEROB       = new StatisticValue(Messages.Statistic_Value_Training_Header1,    Messages.Statistic_Value_Training_Aerob_Header2,        null,  VALUE_FORMAT_6_1F,    6);
   static final StatisticValue STAT_VALUE_TRAINING_ANAEROB     = new StatisticValue(Messages.Statistic_Value_Training_Header1,    Messages.Statistic_Value_Training_Anaerob_Header2,      null,  VALUE_FORMAT_6_1F,    6);
   static final StatisticValue STAT_VALUE_TRAINING_PERFORMANCE = new StatisticValue(Messages.Statistic_Value_Training_Header1,    Messages.Statistic_Value_Training_Performance_Header2,  null,  VALUE_FORMAT_6_2F,    6);

   /*
    * Tour
    */
   static final StatisticValue STAT_VALUE_TOUR_NUMBER_OF_TOURS = new StatisticValue(Messages.Statistic_Value_Tour_NumberOfTours_Header1,      null,   UNIT_NUMBER,   VALUE_FORMAT_6_0F,   6);
   static final StatisticValue STAT_VALUE_TOUR_TITLE           = new StatisticValue(Messages.Statistic_Value_Tour_Title_Header1,              null,   null,          VALUE_FORMAT_S,      TourData.DB_LENGTH_TOUR_TITLE).withLeftAlign().withNoPadding();   // do not truncate text, it is displayed at the end
   static final StatisticValue STAT_VALUE_TOUR_TYPE            = new StatisticValue(Messages.Statistic_Value_Tour_TourType_Header1,           null,   null,          VALUE_FORMAT_20_20S, 20)                           .withLeftAlign();                   // truncate 20+ characters to force column layout

   /*
    * Battery
    */
   static final StatisticValue STAT_VALUE_BATTERY_SOC_START    = new StatisticValue(Messages.Statistic_Value_BatterySoC_Start,                null,   UI.SYMBOL_PERCENTAGE,    VALUE_FORMAT_3D,   3);
   static final StatisticValue STAT_VALUE_BATTERY_SOC_END      = new StatisticValue(Messages.Statistic_Value_BatterySoC_End,                  null,   UI.SYMBOL_PERCENTAGE,    VALUE_FORMAT_3D,   3);

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
