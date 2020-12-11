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

import net.tourbook.data.TourType;

/**
 * Common fields used in statistics
 */
public abstract class TourStatisticData_Common {

   long[][] typeIds;
   long[][] typeIds_Resorted;

   int      numUsedTourTypes;
   int[][]  typeColorIndex;

   int[][]  elapsedTime;
   int[][]  elapsedTime_Resorted;
   int[][]  recordedTime;
   int[][]  recordedTime_Resorted;
   int[][]  pausedTime;
   int[][]  pausedTime_Resorted;
   int[][]  movingTime;
   int[][]  movingTime_Resorted;
   int[][]  breakTime;
   int[][]  breakTime_Resorted;

   /*
    * Chart low/high data series
    */
   float[][] distance_Low;
   float[][] distance_Low_Resorted;
   float[][] distance_High;
   float[][] distance_High_Resorted;

   float[][] elevationUp_Low;
   float[][] elevationUp_Low_Resorted;
   float[][] elevationUp_High;
   float[][] elevationUp_High_Resorted;

   float[][] numTours_Low;
   float[][] numTours_Low_Resorted;
   float[][] numTours_High;
   float[][] numTours_High_Resorted;

   float[][] durationTime_Low;
   float[][] durationTime_High;
   float[][] durationTime_Low_Resorted;
   float[][] durationTime_High_Resorted;

   float[]   athleteBodyWeight_Low;
   float[]   athleteBodyWeight_High;
   float[]   athleteBodyFat_Low;
   float[]   athleteBodyFat_High;

   /**
    * Contains the used {@link TourType} ID or -1 when not available. This data has the same length
    * as the other common data.
    */
   long[]    usedTourTypeIds;

   /**
    * Resort statistic bars according to the sequence start
    *
    * @param barOrderStart
    * @param statContext
    */
   void reorderStatisticData(final int barOrderStart, final boolean isDataAvailable) {

// SET_FORMATTING_OFF

      if (isDataAvailable==false) {

         // there are no data available, create dummy data that the UI do not fail

         typeIds_Resorted              = new long[1][1];

         elevationUp_Low_Resorted      = new float[1][1];
         elevationUp_High_Resorted     = new float[1][1];
         distance_Low_Resorted         = new float[1][1];
         distance_High_Resorted        = new float[1][1];
         numTours_Low_Resorted         = new float[1][1];
         numTours_High_Resorted        = new float[1][1];

         durationTime_Low_Resorted     = new float[1][1];
         durationTime_High_Resorted    = new float[1][1];

         elapsedTime_Resorted          = new int[1][1];
         recordedTime_Resorted         = new int[1][1];
         pausedTime_Resorted           = new int[1][1];
         movingTime_Resorted           = new int[1][1];
         breakTime_Resorted            = new int[1][1];

         return;
      }

      int resortedIndex = 0;

      final int numBars = elevationUp_High.length;

      typeIds_Resorted                 = new long[numBars][];

      elevationUp_Low_Resorted         = new float[numBars][];
      elevationUp_High_Resorted        = new float[numBars][];
      distance_Low_Resorted            = new float[numBars][];
      distance_High_Resorted           = new float[numBars][];

      numTours_Low_Resorted            = new float[numBars][];
      numTours_High_Resorted           = new float[numBars][];

      durationTime_Low_Resorted        = new float[numBars][];
      durationTime_High_Resorted       = new float[numBars][];

      elapsedTime_Resorted             = new int[numBars][];
      recordedTime_Resorted            = new int[numBars][];
      pausedTime_Resorted              = new int[numBars][];
      movingTime_Resorted              = new int[numBars][];
      breakTime_Resorted               = new int[numBars][];

// SET_FORMATTING_ON

      if (barOrderStart >= numBars) {

         final int barOrderStartSequence = barOrderStart % numBars;

         // set types starting from the sequence start
         for (int serieIndex = barOrderStartSequence; serieIndex >= 0; serieIndex--) {

            setResortedData(serieIndex, resortedIndex);

            resortedIndex++;
         }

         // set types starting from the last
         for (int serieIndex = numBars - 1; resortedIndex < numBars; serieIndex--) {

            setResortedData(serieIndex, resortedIndex);

            resortedIndex++;
         }

      } else {

         // set types starting from the sequence start
         for (int serieIndex = barOrderStart; serieIndex < numBars; serieIndex++) {

            setResortedData(serieIndex, resortedIndex);

            resortedIndex++;
         }

         // set types starting from 0
         for (int serieIndex = 0; resortedIndex < numBars; serieIndex++) {

            setResortedData(serieIndex, resortedIndex);

            resortedIndex++;
         }
      }
   }

   /**
    * Set time values and convert it from int to float.
    *
    * @param timeHigh
    */
   public void setDurationTimeHigh(final int[][] timeHigh) {

      if (timeHigh.length == 0 || timeHigh[0].length == 0) {
         durationTime_High = new float[0][0];
         return;
      }

      durationTime_High = new float[timeHigh.length][timeHigh[0].length];

      for (int outerIndex = 0; outerIndex < timeHigh.length; outerIndex++) {

         final int innerLength = timeHigh[outerIndex].length;

         for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
            durationTime_High[outerIndex][innerIndex] = timeHigh[outerIndex][innerIndex];
         }
      }
   }

   /**
    * Set time values and convert it from int to float.
    *
    * @param timeHigh
    */
   public void setDurationTimeLow(final int[][] timeLow) {

      if (timeLow.length == 0 || timeLow[0].length == 0) {
         durationTime_Low = new float[0][0];
         return;
      }

      durationTime_Low = new float[timeLow.length][timeLow[0].length];

      for (int outerIndex = 0; outerIndex < timeLow.length; outerIndex++) {

         final int innerLength = timeLow[outerIndex].length;

         for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
            durationTime_Low[outerIndex][innerIndex] = timeLow[outerIndex][innerIndex];
         }
      }
   }

   private void setResortedData(final int serieIndex, final int resortedIndex) {

// SET_FORMATTING_OFF

      typeIds_Resorted[resortedIndex]              = typeIds[serieIndex];

      distance_Low_Resorted[resortedIndex]         = distance_Low[serieIndex];
      distance_High_Resorted[resortedIndex]        = distance_High[serieIndex];

      elevationUp_Low_Resorted[resortedIndex]      = elevationUp_Low[serieIndex];
      elevationUp_High_Resorted[resortedIndex]     = elevationUp_High[serieIndex];

      numTours_Low_Resorted[resortedIndex]         = numTours_Low[serieIndex];
      numTours_High_Resorted[resortedIndex]        = numTours_High[serieIndex];

      durationTime_Low_Resorted[resortedIndex]     = durationTime_Low[serieIndex];
      durationTime_High_Resorted[resortedIndex]    = durationTime_High[serieIndex];

      elapsedTime_Resorted[resortedIndex]          = elapsedTime[serieIndex];
      recordedTime_Resorted[resortedIndex]         = recordedTime[serieIndex];
      pausedTime_Resorted[resortedIndex]           = pausedTime[serieIndex];
      movingTime_Resorted[resortedIndex]           = movingTime[serieIndex];
      breakTime_Resorted[resortedIndex]            = breakTime[serieIndex];

// SET_FORMATTING_ON
   }

}
