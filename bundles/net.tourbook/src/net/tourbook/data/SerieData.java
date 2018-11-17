/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
/**
 *
 */
package net.tourbook.data;

import java.io.Serializable;
import java.util.Arrays;

import de.byteholder.geoclipse.map.UI;

/**
 * All time serie data from a device are stored in the database with this class, when data are not
 * available the value is set to <code>null</code>
 */
public class SerieData implements Serializable {

   private static final long serialVersionUID = 1L;

   public int[]              timeSerie;

   public float[]            distanceSerie20;
   public float[]            altitudeSerie20;
   public float[]            cadenceSerie20;
   public float[]            pulseSerie20;
   public float[]            temperatureSerie20;
   public float[]            speedSerie20;
   public float[]            powerSerie20;

   /**
    * Gears are in this format (left to right)
    * <p>
    * Front teeth<br>
    * Front gear number<br>
    * Back teeth<br>
    * Back gear number<br>
    *
    * <pre>
    *
    * public int getFrontGearNum() {
    *    return (int) (gears &gt;&gt; 16 &amp; 0xff);
    * }
    *
    * public int getFrontGearTeeth() {
    *    return (int) (gears &gt;&gt; 24 &amp; 0xff);
    * }
    *
    * public int getRearGearNum() {
    *    return (int) (gears &amp; 0xff);
    * }
    *
    * public int getRearGearTeeth() {
    *    return (int) (gears &gt;&gt; 8 &amp; 0xff);
    * }
    *
    * </pre>
    *
    * @since Db-version 27
    */
   public long[]             gears;

   public double[]           longitude;
   public double[]           latitude;

   /**
    * Pulse times in milliseconds.
    * <p>
    * <b>This data serie has not the same serie length as the other data series because 1 second can
    * have multiple values, depending on the heartrate.</b>
    */
   public int[]              pulseTimes;

   /*
    * Running dynamics data
    * @since Version 18.7
    */
   public short[]   runDyn_StanceTime;

   public short[]   runDyn_StanceTimeBalance;
   public short[]   runDyn_StepLength;
   public short[]   runDyn_VerticalOscillation;
   public short[]   runDyn_VerticalRatio;
   /*
    * Swim data
    * @since Version 18.10
    */
   public short[]   swim_LengthType;           // e.g. active, idle

   public short[]   swim_Cadence;              // strokes/min
   public short[]   swim_Strokes;              // number of strokes
   public short[]   swim_StrokeStyle;          // e.g. freestyle, breaststroke
   public int[]     swim_Time;                 // relative time to the start time
   /*
    * Swim data
    * @since Version 18.12
    */
   /**
    *
    */
   public boolean[] visibleDataPoint;

   /*
    * These data series cannot be removed because they are needed to convert from int to float in db
    * version 20
    */
   public int[] distanceSerie;

   public int[] altitudeSerie;
   public int[] cadenceSerie;
   public int[] pulseSerie;
   public int[] temperatureSerie;
   public int[] speedSerie;
   public int[] powerSerie;
   public int[] deviceMarker;

   @Override
   public String toString() {

// SET_FORMATTING_OFF
      
      // this formatted data are displayed in the tour info view

      final int maxLen = 10;

      return "\n"

            + "   timeSerie            " + (timeSerie != null          ? Arrays.toString(Arrays.copyOf(timeSerie,            Math.min(timeSerie.length, maxLen)))             : UI.EMPTY_STRING) + "\n"

            + "   distanceSerie20      " + (distanceSerie20 != null    ? Arrays.toString(Arrays.copyOf(distanceSerie20,      Math.min(distanceSerie20.length, maxLen)))       : UI.EMPTY_STRING) + "\n"
            + "   altitudeSerie20      " + (altitudeSerie20 != null    ? Arrays.toString(Arrays.copyOf(altitudeSerie20,      Math.min(altitudeSerie20.length, maxLen)))       : UI.EMPTY_STRING) + "\n"
            + "   cadenceSerie20       " + (cadenceSerie20 != null     ? Arrays.toString(Arrays.copyOf(cadenceSerie20,       Math.min(cadenceSerie20.length, maxLen)))        : UI.EMPTY_STRING) + "\n"
            + "   pulseSerie20         " + (pulseSerie20 != null       ? Arrays.toString(Arrays.copyOf(pulseSerie20,         Math.min(pulseSerie20.length, maxLen)))          : UI.EMPTY_STRING) + "\n"
            + "   temperatureSerie20   " + (temperatureSerie20 != null ? Arrays.toString(Arrays.copyOf(temperatureSerie20,   Math.min(temperatureSerie20.length, maxLen)))    : UI.EMPTY_STRING) + "\n"
            + "   speedSerie20         " + (speedSerie20 != null       ? Arrays.toString(Arrays.copyOf(speedSerie20,         Math.min(speedSerie20.length, maxLen)))          : UI.EMPTY_STRING) + "\n"
            + "   powerSerie20         " + (powerSerie20 != null       ? Arrays.toString(Arrays.copyOf(powerSerie20,         Math.min(powerSerie20.length, maxLen)))          : UI.EMPTY_STRING) + "\n"

            + "   gears                " + (gears != null              ? Arrays.toString(Arrays.copyOf(gears,                Math.min(gears.length, maxLen)))                 : UI.EMPTY_STRING) + "\n"
            + "   longitude            " + (longitude != null          ? Arrays.toString(Arrays.copyOf(longitude,            Math.min(longitude.length, maxLen)))             : UI.EMPTY_STRING) + "\n"
            + "   latitude             " + (latitude != null           ? Arrays.toString(Arrays.copyOf(latitude,             Math.min(latitude.length, maxLen)))              : UI.EMPTY_STRING) + "\n"
            + "   pulseTimes           " + (pulseTimes != null         ? Arrays.toString(Arrays.copyOf(pulseTimes,           Math.min(pulseTimes.length, maxLen)))            : UI.EMPTY_STRING) + "\n"

            + "   runDyn_StanceTime          " + (runDyn_StanceTime != null           ? Arrays.toString(Arrays.copyOf(runDyn_StanceTime,          Math.min(runDyn_StanceTime.length, maxLen)))              : UI.EMPTY_STRING) + "\n"
            + "   runDyn_StanceTimeBalance   " + (runDyn_StanceTimeBalance != null    ? Arrays.toString(Arrays.copyOf(runDyn_StanceTimeBalance,   Math.min(runDyn_StanceTimeBalance.length, maxLen)))       : UI.EMPTY_STRING) + "\n"
            + "   runDyn_StepLength          " + (runDyn_StepLength != null           ? Arrays.toString(Arrays.copyOf(runDyn_StepLength,          Math.min(runDyn_StepLength.length, maxLen)))              : UI.EMPTY_STRING) + "\n"
            + "   runDyn_VerticalOscillation " + (runDyn_VerticalOscillation != null  ? Arrays.toString(Arrays.copyOf(runDyn_VerticalOscillation, Math.min(runDyn_VerticalOscillation.length, maxLen)))     : UI.EMPTY_STRING) + "\n"
            + "   runDyn_VerticalRatio       " + (runDyn_VerticalRatio != null        ? Arrays.toString(Arrays.copyOf(runDyn_VerticalRatio,       Math.min(runDyn_VerticalRatio.length, maxLen)))           : UI.EMPTY_STRING) + "\n"

            + "   swim_LengthType      " + (swim_LengthType != null    ? Arrays.toString(Arrays.copyOf(swim_LengthType,      Math.min(swim_LengthType.length, maxLen)))       : UI.EMPTY_STRING) + "\n"
            + "   swim_Cadence         " + (swim_Cadence != null       ? Arrays.toString(Arrays.copyOf(swim_Cadence,         Math.min(swim_Cadence.length, maxLen)))          : UI.EMPTY_STRING) + "\n"
            + "   swim_Strokes         " + (swim_Strokes != null       ? Arrays.toString(Arrays.copyOf(swim_Strokes,         Math.min(swim_Strokes.length, maxLen)))          : UI.EMPTY_STRING) + "\n"
            + "   swim_StrokeStyle     " + (swim_StrokeStyle != null   ? Arrays.toString(Arrays.copyOf(swim_StrokeStyle,     Math.min(swim_StrokeStyle.length, maxLen)))      : UI.EMPTY_STRING) + "\n"
            + "   swim_Time            " + (swim_Time != null          ? Arrays.toString(Arrays.copyOf(swim_Time,            Math.min(swim_Time.length, maxLen)))             : UI.EMPTY_STRING) + "\n"

            + "   visibleDataPoint     " + (visibleDataPoint != null   ? Arrays.toString(Arrays.copyOf(visibleDataPoint,     Math.min(visibleDataPoint.length, maxLen)))      : UI.EMPTY_STRING) + "\n"

            + "   distanceSerie        " + (distanceSerie != null      ? Arrays.toString(Arrays.copyOf(distanceSerie,        Math.min(distanceSerie.length, maxLen)))         : UI.EMPTY_STRING) + "\n"
            + "   altitudeSerie        " + (altitudeSerie != null      ? Arrays.toString(Arrays.copyOf(altitudeSerie,        Math.min(altitudeSerie.length, maxLen)))         : UI.EMPTY_STRING) + "\n"
            + "   cadenceSerie         " + (cadenceSerie != null       ? Arrays.toString(Arrays.copyOf(cadenceSerie,         Math.min(cadenceSerie.length, maxLen)))          : UI.EMPTY_STRING) + "\n"
            + "   pulseSerie           " + (pulseSerie != null         ? Arrays.toString(Arrays.copyOf(pulseSerie,           Math.min(pulseSerie.length, maxLen)))            : UI.EMPTY_STRING) + "\n"
            + "   temperatureSerie     " + (temperatureSerie != null   ? Arrays.toString(Arrays.copyOf(temperatureSerie,     Math.min(temperatureSerie.length, maxLen)))      : UI.EMPTY_STRING) + "\n"
            + "   speedSerie           " + (speedSerie != null         ? Arrays.toString(Arrays.copyOf(speedSerie,           Math.min(speedSerie.length, maxLen)))            : UI.EMPTY_STRING) + "\n"
            + "   powerSerie           " + (powerSerie != null         ? Arrays.toString(Arrays.copyOf(powerSerie,           Math.min(powerSerie.length, maxLen)))            : UI.EMPTY_STRING) + "\n"

            + "   deviceMarker         " + (deviceMarker != null       ? Arrays.toString(Arrays.copyOf(deviceMarker,         Math.min(deviceMarker.length, maxLen)))          : UI.EMPTY_STRING) ;
   }

// SET_FORMATTING_ON

}
