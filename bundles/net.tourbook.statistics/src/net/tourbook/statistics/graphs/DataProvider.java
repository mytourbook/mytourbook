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

   static final String HEAD1_DATE_YEAR                      = "Year,";           //$NON-NLS-1$
   static final String HEAD2_DATE_YEAR                      = "    ,";           //$NON-NLS-1$
   static final String VALUE_DATE_YEAR                      = "%4d,";            //$NON-NLS-1$

   static final String HEAD1_DATE_MONTH                     = " Month,";         //$NON-NLS-1$
   static final String HEAD2_DATE_MONTH                     = "      ,";         //$NON-NLS-1$
   static final String VALUE_DATE_MONTH                     = "   %3d,";         //$NON-NLS-1$

   static final String HEAD1_DATE_DAY                       = " Day,";           //$NON-NLS-1$
   static final String HEAD2_DATE_DAY                       = "    ,";           //$NON-NLS-1$
   static final String VALUE_DATE_DAY                       = " %3d,";           //$NON-NLS-1$

   static final String HEAD1_DATE_DOY                       = " DOY,";           //$NON-NLS-1$
   static final String HEAD2_DATE_DOY                       = "    ,";           //$NON-NLS-1$
   static final String VALUE_DATE_DOY                       = " %3d,";           //$NON-NLS-1$

   static final String HEAD1_DEVICE_TIME_ELAPSED            = " Elapsed,";       //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_ELAPSED            = "     (s),";       //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_ELAPSED            = "  %6d,";          //$NON-NLS-1$

   static final String HEAD1_DEVICE_TIME_RECORDED           = " Recorded,";      //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_RECORDED           = "      (s),";      //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_RECORDED           = "   %6d,";         //$NON-NLS-1$

   static final String HEAD1_DEVICE_TIME_PAUSED             = " Paused,";        //$NON-NLS-1$
   static final String HEAD2_DEVICE_TIME_PAUSED             = "    (s),";        //$NON-NLS-1$
   static final String VALUE_DEVICE_TIME_PAUSED             = " %6d,";           //$NON-NLS-1$

   static final String HEAD1_COMPUTED_TIME_MOVING           = " Moving,";        //$NON-NLS-1$
   static final String HEAD2_COMPUTED_TIME_MOVING           = "    (s),";        //$NON-NLS-1$
   static final String VALUE_COMPUTED_TIME_MOVING           = " %6d,";           //$NON-NLS-1$

   static final String HEAD1_COMPUTED_TIME_BREAK            = "  Break,";        //$NON-NLS-1$
   static final String HEAD2_COMPUTED_TIME_BREAK            = "    (s),";        //$NON-NLS-1$
   static final String VALUE_COMPUTED_TIME_BREAK            = " %6d,";           //$NON-NLS-1$

   static final String HEAD1_DURATION_LOW                   = "      ,";         //$NON-NLS-1$
   static final String HEAD2_DURATION_LOW                   = "      ,";         //$NON-NLS-1$
   static final String VALUE_DURATION_LOW                   = "  %6.0f,";        //$NON-NLS-1$

   static final String HEAD1_DURATION_HIGH                  = " Duration,";      //$NON-NLS-1$
   static final String HEAD2_DURATION_HIGH                  = "      (s),";      //$NON-NLS-1$
   static final String VALUE_DURATION_HIGH                  = " %6.0f,";         //$NON-NLS-1$

   static final String HEAD1_ELEVATION_LOW                  = "      ,";         //$NON-NLS-1$
   static final String HEAD2_ELEVATION_LOW                  = "      ,";         //$NON-NLS-1$
   static final String VALUE_ELEVATION_LOW                  = "   %6.0f,";       //$NON-NLS-1$

   static final String HEAD1_ELEVATION_HIGH                 = " Elevation,";     //$NON-NLS-1$
   static final String HEAD2_ELEVATION_HIGH                 = "       (m),";     //$NON-NLS-1$
   static final String VALUE_ELEVATION_HIGH                 = " %6.0f,";         //$NON-NLS-1$

   static final String HEAD1_DISTANCE_LOW                   = "          ,";     //$NON-NLS-1$
   static final String HEAD2_DISTANCE_LOW                   = "          ,";     //$NON-NLS-1$
   static final String VALUE_DISTANCE_LOW                   = "  %8.0f,";        //$NON-NLS-1$

   static final String HEAD1_DISTANCE_HIGH                  = " Distance,";      //$NON-NLS-1$
   static final String HEAD2_DISTANCE_HIGH                  = "      (m),";      //$NON-NLS-1$
   static final String VALUE_DISTANCE_HIGH                  = " %8.0f,";         //$NON-NLS-1$

   static final String HEAD1_SPEED_LOW                      = "         ,";      //$NON-NLS-1$
   static final String HEAD2_SPEED_LOW                      = "         ,";      //$NON-NLS-1$
   static final String VALUE_SPEED_LOW                      = " %7.2f,";         //$NON-NLS-1$

   static final String HEAD1_SPEED_HIGH                     = "  Speed,";        //$NON-NLS-1$
   static final String HEAD2_SPEED_HIGH                     = " (km/h),";        //$NON-NLS-1$
   static final String VALUE_SPEED_HIGH                     = " %7.2f,";         //$NON-NLS-1$

   static final String HEAD1_PACE_LOW                       = "          ,";     //$NON-NLS-1$
   static final String HEAD2_PACE_LOW                       = "      ,";         //$NON-NLS-1$
   static final String VALUE_PACE_LOW                       = "  %6.2f,";        //$NON-NLS-1$

   static final String HEAD1_PACE_HIGH                      = " Pace,";          //$NON-NLS-1$
   static final String HEAD2_PACE_HIGH                      = " (min/km),";      //$NON-NLS-1$
   static final String VALUE_PACE_HIGH                      = " %6.2f,";         //$NON-NLS-1$

   static final String HEAD1_TRAINING_AEROB_LOW             = "      ,";         //$NON-NLS-1$
   static final String HEAD2_TRAINING_AEROB_LOW             = "         ,";      //$NON-NLS-1$
   static final String VALUE_TRAINING_AEROB_LOW             = "  %6.1f,";        //$NON-NLS-1$

   static final String HEAD1_TRAINING_AEROB_HIGH            = " Training,";      //$NON-NLS-1$
   static final String HEAD2_TRAINING_AEROB_HIGH            = " Aerob,";         //$NON-NLS-1$
   static final String VALUE_TRAINING_AEROB_HIGH            = " %6.1f,";         //$NON-NLS-1$

   static final String HEAD1_TRAINING_ANAEROB_LOW           = "      ,";         //$NON-NLS-1$
   static final String HEAD2_TRAINING_ANAEROB_LOW           = "       ,";        //$NON-NLS-1$
   static final String VALUE_TRAINING_ANAEROB_LOW           = "  %6.1f,";        //$NON-NLS-1$

   static final String HEAD1_TRAINING_ANAEROB_HIGH          = " Training,";      //$NON-NLS-1$
   static final String HEAD2_TRAINING_ANAEROB_HIGH          = " Anaerob,";       //$NON-NLS-1$
   static final String VALUE_TRAINING_ANAEROB_HIGH          = " %6.1f,";         //$NON-NLS-1$

   static final String HEAD1_TRAINING_PERFORMANCE_LOW       = "      ,";         //$NON-NLS-1$
   static final String HEAD2_TRAINING_PERFORMANCE_LOW       = "   ,";            //$NON-NLS-1$
   static final String VALUE_TRAINING_PERFORMANCE_LOW       = "  %6.2f,";        //$NON-NLS-1$

   static final String HEAD1_TRAINING_PERFORMANCE_HIGH      = " Training";       //$NON-NLS-1$
   static final String HEAD2_TRAINING_PERFORMANCE_HIGH      = " Performance";    //$NON-NLS-1$
   static final String VALUE_TRAINING_PERFORMANCE_HIGH      = " %6.2f";          //$NON-NLS-1$

// SET_FORMATTING_ON

   static ZonedDateTime calendar8 = ZonedDateTime.now().with(TimeTools.calendarWeek.dayOfWeek(), 1);

   TourPerson           _activePerson;
   TourTypeFilter       _activeTourTypeFilter;

   int                  _lastYear;

   int                  _numberOfYears;

   /**
    * All years numbers, e.g. 2016, 2017, ... 2020
    */
   int[]                allYearNumbers;

   /**
    * Number of days in a year
    */
   int[]                allYearDays;

   /**
    * Number of weeks in a year
    */
   int[]                allYearWeeks;

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
    * @return Returns the number of days between {@link #_lastYear} and currentYear
    */
   int getYearDOYs(final int selectedYear) {

      int yearDOYs = 0;
      int yearIndex = 0;

      for (int currentYear = _lastYear - _numberOfYears + 1; currentYear < selectedYear; currentYear++) {

         if (currentYear == selectedYear) {
            return yearDOYs;
         }

         yearDOYs += allYearDays[yearIndex];

         yearIndex++;
      }

      return yearDOYs;
   }

   /**
    * Get different data for each year, data are set into <br>
    * <br>
    * All years in {@link #allYearNumbers} <br>
    * Number of day's in {@link #allYearDays} <br>
    * Number of week's in {@link #allYearWeeks}
    */
   void initYearNumbers() {

      allYearNumbers = new int[_numberOfYears];
      allYearDays = new int[_numberOfYears];
      allYearWeeks = new int[_numberOfYears];

      final int firstYear = _lastYear - _numberOfYears + 1;
      int yearIndex = 0;

      for (int currentYear = firstYear; currentYear <= _lastYear; currentYear++) {

         allYearNumbers[yearIndex] = currentYear;

         allYearDays[yearIndex] = TimeTools.getNumberOfDaysWithYear(currentYear);
         allYearWeeks[yearIndex] = TimeTools.getNumberOfWeeksWithYear(currentYear);

         yearIndex++;
      }
   }

}
