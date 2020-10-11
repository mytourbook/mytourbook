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

public class TourStatisticData_Time {

   int[]                          allYear_Numbers;
   int                            numDaysInAllYears;

   long[]                         allTourIds;
   long[]                         allTypeIds;

   int[]                          allTypeColorIndices;

   /**
    * Number for all days in each year
    */
   int[]                          allYear_NumDays;

   int[]                          allTourYears;
   int[]                          allTourMonths;
   int[]                          allTourDays;

   int[]                          allTourDOYs;
   int[]                          allWeeks;

   int[]                          allTourTimeStart;
   int[]                          allTourTimeEnd;
   ArrayList<ZonedDateTime>       allTourStartDateTimes;
   ArrayList<String>              allTourTimeZoneOffsets;

   float[]                        allTourElevations;
   float[]                        allTourDistances;

   int[]                          allTourDeviceTime_Elapsed;
   int[]                          allTourDeviceTime_Recorded;
   int[]                          allTourDeviceTime_Paused;
   int[]                          allTourComputedTime_Moving;

   ArrayList<String>              allTourTitles;
   ArrayList<String>              allTourDescriptions;

   /**
    * Contains the tags for the tour, key is the tour ID
    */
   HashMap<Long, ArrayList<Long>> allTagIds;
}
