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

   long[]                         tourIds;

   long[]                         typeIds;
   int[]                          typeColorIndex;

   int[]                          yearValues;
   int[]                          monthValues;
   int[]                          dayValues;
   int[]                          weekValues;

   private int[]                  _doyValues;
   private double[]               _doyValuesDouble;

   int[]                          years;
   int[]                          yearDays;
   int                            allDaysInAllYears;

   float[]                        altitude_Low;
   float[]                        altitude_High;
   float[]                        avgPace_Low;
   float[]                        avgPace_High;
   float[]                        avgSpeed_Low;
   float[]                        avgSpeed_High;
   float[]                        distance_Low;
   float[]                        distance_High;

   float[]                        trainingEffect_Aerob_Low;
   float[]                        trainingEffect_Aerob_High;
   float[]                        trainingEffect_Anaerob_Low;
   float[]                        trainingEffect_Anaerob_High;
   float[]                        trainingPerformance_Low;
   float[]                        trainingPerformance_High;

   private float[]                _durationLowFloat;
   private float[]                _durationHighFloat;

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

   ArrayList<String>              tourTitle;
   ArrayList<String>              tourDescription;

   String                         statisticValuesRaw;

   /**
    * Contains the tags for the tour where the key is the tour ID
    */
   HashMap<Long, ArrayList<Long>> tagIds;

   public int[] getDoyValues() {
      return _doyValues;
   }

   public double[] getDoyValuesDouble() {
      return _doyValuesDouble;
   }

   public float[] getDurationHighFloat() {
      return _durationHighFloat;
   }

   public float[] getDurationLowFloat() {
      return _durationLowFloat;
   }

   public void setDoyValues(final int[] doyValues) {

      _doyValues = doyValues;
      _doyValuesDouble = new double[doyValues.length];

      for (int valueIndex = 0; valueIndex < doyValues.length; valueIndex++) {
         _doyValuesDouble[valueIndex] = doyValues[valueIndex];
      }
   }

   public void setDurationHigh(final int[] timeHigh) {

      _durationHighFloat = new float[timeHigh.length];

      for (int valueIndex = 0; valueIndex < timeHigh.length; valueIndex++) {
         _durationHighFloat[valueIndex] = timeHigh[valueIndex];
      }
   }

   public void setDurationLow(final int[] timeLow) {

      _durationLowFloat = new float[timeLow.length];

      for (int valueIndex = 0; valueIndex < timeLow.length; valueIndex++) {
         _durationLowFloat[valueIndex] = timeLow[valueIndex];
      }
   }

}
