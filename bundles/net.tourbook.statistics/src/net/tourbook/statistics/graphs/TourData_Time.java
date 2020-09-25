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
import java.util.ArrayList;
import java.util.HashMap;

public class TourData_Time {

   int[]                          allYear_Numbers;
   int                            numDaysInAllYears;

   long[]                         allTourIds;
   long[]                         allTypeIds;

   int[]                          allTypeColorIndices;

   /**
    * Number for all days in each year
    */
   int[]                          allYear_NumDays;

   int[]                          allTourYearValues;
   int[]                          allTourMonthValues;
   int[]                          allTourDOYValues;
   int[]                          allWeekValues;

   int[]                          allTourTimeStartValues;
   int[]                          allTourTimeEndValues;
   ArrayList<ZonedDateTime>       allTourStartDateTimes;
   ArrayList<String>              allTourTimeZoneOffsets;

   int[]                          allTourElevationValues;
   int[]                          allTourDistanceValues;

   int[]                          allTourDeviceTime_ElapsedValues;
   int[]                          allTourDeviceTime_RecordedValues;
   int[]                          allTourComputedTime_MovingValues;

   ArrayList<String>              allTourTitles;
   ArrayList<String>              allTourDescriptions;

   String                         statisticValuesRaw;

   /**
    * Contains the tags for the tour, key is the tour ID
    */
   HashMap<Long, ArrayList<Long>> allTagIds;
}
