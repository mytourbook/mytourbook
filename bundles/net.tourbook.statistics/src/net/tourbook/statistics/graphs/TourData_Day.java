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

public class TourData_Day {

   long[]                         allTourIds;

   long[]                         allTypeIds;
   int[]                          allTypeColorIndices;

   int[]                          allYears;
   int[]                          allMonths;
   int[]                          allDays;
   int[]                          allWeeks;

   private int[]                  _allDoys;
   private double[]               _allDoyDoubles;

   int[]                          allYearNumbers;
   int[]                          allYearDays;
   int                            allDaysInAllYears;

   float[]                        allElevation_Low;
   float[]                        allElevation_High;
   float[]                        allAvgPace_Low;
   float[]                        allAvgPace_High;
   float[]                        allAvgSpeed_Low;
   float[]                        allAvgSpeed_High;
   float[]                        allDistance_Low;
   float[]                        allDistance_High;

   float[]                        allTrainingEffect_Aerob_Low;
   float[]                        allTrainingEffect_Aerob_High;
   float[]                        allTrainingEffect_Anaerob_Low;
   float[]                        allTrainingEffect_Anaerob_High;
   float[]                        allTrainingPerformance_Low;
   float[]                        allTrainingPerformance_High;

   private float[]                _allDurationLowFloat;
   private float[]                _allDurationHighFloat;

   int[]                          allDeviceTime_Elapsed;
   int[]                          allComputedTime_Moving;
   int[]                          allDeviceTime_Recorded;
   int[]                          allDeviceTime_Paused;

   int[]                          allStartTime;
   int[]                          allEndTime;
   ArrayList<ZonedDateTime>       allStartDateTimes;

   float[]                        allDistance;
   float[]                        allAltitude;

   float[]                        allTraining_Effect;
   float[]                        allTraining_Effect_Anaerobic;
   float[]                        allTraining_Performance;

   ArrayList<String>              allTourTitles;
   ArrayList<String>              allTourDescriptions;

   String                         statisticValuesRaw;

   /**
    * Contains the tags for the tour where the key is the tour ID
    */
   HashMap<Long, ArrayList<Long>> tagIds;

   public int[] getDoyValues() {
      return _allDoys;
   }

   public double[] getDoyValuesDouble() {
      return _allDoyDoubles;
   }

   public float[] getDurationHighFloat() {
      return _allDurationHighFloat;
   }

   public float[] getDurationLowFloat() {
      return _allDurationLowFloat;
   }

   public void setDoyValues(final int[] doyValues) {

      _allDoys = doyValues;
      _allDoyDoubles = new double[doyValues.length];

      for (int valueIndex = 0; valueIndex < doyValues.length; valueIndex++) {
         _allDoyDoubles[valueIndex] = doyValues[valueIndex];
      }
   }

   public void setDurationHigh(final int[] timeHigh) {

      _allDurationHighFloat = new float[timeHigh.length];

      for (int valueIndex = 0; valueIndex < timeHigh.length; valueIndex++) {
         _allDurationHighFloat[valueIndex] = timeHigh[valueIndex];
      }
   }

   public void setDurationLow(final int[] timeLow) {

      _allDurationLowFloat = new float[timeLow.length];

      for (int valueIndex = 0; valueIndex < timeLow.length; valueIndex++) {
         _allDurationLowFloat[valueIndex] = timeLow[valueIndex];
      }
   }

}
