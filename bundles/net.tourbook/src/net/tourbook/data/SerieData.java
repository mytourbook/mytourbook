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
/**
 *
 */
package net.tourbook.data;

import de.byteholder.geoclipse.map.UI;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

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
   public short[] runDyn_StanceTime;
   public short[] runDyn_StanceTimeBalance;
   public short[] runDyn_StepLength;
   public short[] runDyn_VerticalOscillation;
   public short[] runDyn_VerticalRatio;

   /*
    * Swim data
    * @since Version 18.10
    */
   public short[]   swim_LengthType;      // e.g. active, idle

   public short[]   swim_Cadence;         // strokes/min
   public short[]   swim_Strokes;         // strokes/length
   public short[]   swim_StrokeStyle;     // e.g. freestyle, breaststroke
   public int[]     swim_Time;            // relative time to the start time

   /**
    * Is <code>true</code> when a time slice in a data serie is visible.
    *
    * @since Version 18.12
    */
   public boolean[] visiblePoints_Surfing;

   /*
    * These data series cannot be removed because they are needed to convert from int to float in db
    * version 20
    */
   public int[]  distanceSerie;

   public int[]  altitudeSerie;
   public int[]  cadenceSerie;
   public int[]  pulseSerie;
   public int[]  temperatureSerie;
   public int[]  speedSerie;
   public int[]  powerSerie;
   public int[]  deviceMarker;

   /**
    * An array containing the start time of each pause (in milliseconds)
    */
   public long[] pausedTime_Start;
   /**
    * An array containing the end time of each pause (in milliseconds)
    */
   public long[] pausedTime_End;

   //CUSTOM TRACKS
   public HashMap<String, float[]>                   customTracks;
   public HashMap<String, CustomTrackStatisticEntry> customTracksStat;
   public HashMap<String, CustomTrackDefinition>     customTracksDefinition;

   @Override
   public String toString() {

// SET_FORMATTING_OFF

      // this formatted data are displayed in the tour info view

      final int maxLen = 10;

      //CUSTOM TRACKS
      String custTrack = UI.NEW_LINE;
      if(customTracksDefinition!=null && !customTracksDefinition.isEmpty()) {
         custTrack += "  --CUSTOM TRACKS Definition...len=" + Integer.toString(customTracksDefinition.size()) + "--  " + UI.NEW_LINE; //$NON-NLS-1$ //$NON-NLS-2$
         for (final String i : customTracksDefinition.keySet()) {
            final CustomTrackDefinition item = customTracksDefinition.get(i);
            custTrack += "  Id=\"" + i + "\" ,Name=\"" + item.getName() + "\"" + " ,Unit=\"" + item.getUnit() + "\"" + UI.NEW_LINE; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
         }
         custTrack += "  --CUSTOM TRACKS Definition End--  " + UI.NEW_LINE; //$NON-NLS-1$
      }
      if(customTracks!=null && !customTracks.isEmpty()) {
         custTrack += "  --CUSTOM TRACKS--  " + UI.NEW_LINE; //$NON-NLS-1$
         for (final String i : customTracks.keySet()) {
            custTrack += "  Id:\"" + i + "\"    " + (customTracks.get(i) != null         ? Arrays.toString(Arrays.copyOf(customTracks.get(i),          Math.min(customTracks.get(i).length, maxLen)))        : UI.EMPTY_STRING) + UI.NEW_LINE; //$NON-NLS-1$ //$NON-NLS-2$
            if(customTracksStat!= null && customTracksStat.get(i)!=null) {
               final CustomTrackStatisticEntry valE = customTracksStat.get(i);
               custTrack += "  Id:\"" + i + "\"    " + "[Avg=" + String.format("%.2f", valE.value_Avg); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
               custTrack +=  ";Min=" + String.format("%.2f", valE.value_Min); //$NON-NLS-1$ //$NON-NLS-2$
               custTrack +=  ";Max=" + String.format("%.2f", valE.value_Max); //$NON-NLS-1$ //$NON-NLS-2$
               custTrack += "]" + UI.NEW_LINE; //$NON-NLS-1$
            }
         }
         custTrack += "  --END CUSTOM TRACKS--  " + UI.NEW_LINE; //$NON-NLS-1$
      }

      return UI.NEW_LINE

            + "   timeSerie                  " + (timeSerie != null          ? Arrays.toString(Arrays.copyOf(timeSerie,            Math.min(timeSerie.length, maxLen)))             : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   distanceSerie20            " + (distanceSerie20 != null    ? Arrays.toString(Arrays.copyOf(distanceSerie20,      Math.min(distanceSerie20.length, maxLen)))       : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   altitudeSerie20            " + (altitudeSerie20 != null    ? Arrays.toString(Arrays.copyOf(altitudeSerie20,      Math.min(altitudeSerie20.length, maxLen)))       : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   cadenceSerie20             " + (cadenceSerie20 != null     ? Arrays.toString(Arrays.copyOf(cadenceSerie20,       Math.min(cadenceSerie20.length, maxLen)))        : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   pulseSerie20               " + (pulseSerie20 != null       ? Arrays.toString(Arrays.copyOf(pulseSerie20,         Math.min(pulseSerie20.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   temperatureSerie20         " + (temperatureSerie20 != null ? Arrays.toString(Arrays.copyOf(temperatureSerie20,   Math.min(temperatureSerie20.length, maxLen)))    : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   speedSerie20               " + (speedSerie20 != null       ? Arrays.toString(Arrays.copyOf(speedSerie20,         Math.min(speedSerie20.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   powerSerie20               " + (powerSerie20 != null       ? Arrays.toString(Arrays.copyOf(powerSerie20,         Math.min(powerSerie20.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   gears                      " + (gears != null              ? Arrays.toString(Arrays.copyOf(gears,                Math.min(gears.length, maxLen)))                 : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   longitude                  " + (longitude != null          ? Arrays.toString(Arrays.copyOf(longitude,            Math.min(longitude.length, maxLen)))             : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   latitude                   " + (latitude != null           ? Arrays.toString(Arrays.copyOf(latitude,             Math.min(latitude.length, maxLen)))              : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   pulseTimes                 " + (pulseTimes != null         ? Arrays.toString(Arrays.copyOf(pulseTimes,           Math.min(pulseTimes.length, maxLen)))            : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   runDyn_StanceTime          " + (runDyn_StanceTime != null           ? Arrays.toString(Arrays.copyOf(runDyn_StanceTime,          Math.min(runDyn_StanceTime.length, maxLen)))              : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   runDyn_StanceTimeBalance   " + (runDyn_StanceTimeBalance != null    ? Arrays.toString(Arrays.copyOf(runDyn_StanceTimeBalance,   Math.min(runDyn_StanceTimeBalance.length, maxLen)))       : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   runDyn_StepLength          " + (runDyn_StepLength != null           ? Arrays.toString(Arrays.copyOf(runDyn_StepLength,          Math.min(runDyn_StepLength.length, maxLen)))              : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   runDyn_VerticalOscillation " + (runDyn_VerticalOscillation != null  ? Arrays.toString(Arrays.copyOf(runDyn_VerticalOscillation, Math.min(runDyn_VerticalOscillation.length, maxLen)))     : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   runDyn_VerticalRatio       " + (runDyn_VerticalRatio != null        ? Arrays.toString(Arrays.copyOf(runDyn_VerticalRatio,       Math.min(runDyn_VerticalRatio.length, maxLen)))           : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   swim_LengthType            " + (swim_LengthType != null        ? Arrays.toString(Arrays.copyOf(swim_LengthType,         Math.min(swim_LengthType.length, maxLen)))       : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   swim_Cadence               " + (swim_Cadence != null           ? Arrays.toString(Arrays.copyOf(swim_Cadence,            Math.min(swim_Cadence.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   swim_Strokes               " + (swim_Strokes != null           ? Arrays.toString(Arrays.copyOf(swim_Strokes,            Math.min(swim_Strokes.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   swim_StrokeStyle           " + (swim_StrokeStyle != null       ? Arrays.toString(Arrays.copyOf(swim_StrokeStyle,        Math.min(swim_StrokeStyle.length, maxLen)))      : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   swim_Time                  " + (swim_Time != null              ? Arrays.toString(Arrays.copyOf(swim_Time,               Math.min(swim_Time.length, maxLen)))             : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   visiblePoints_Surfing      " + (visiblePoints_Surfing != null  ? Arrays.toString(Arrays.copyOf(visiblePoints_Surfing,   Math.min(visiblePoints_Surfing.length, maxLen)))    : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$

            + "   distanceSerie              " + (distanceSerie != null          ? Arrays.toString(Arrays.copyOf(distanceSerie,           Math.min(distanceSerie.length, maxLen)))         : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   altitudeSerie              " + (altitudeSerie != null          ? Arrays.toString(Arrays.copyOf(altitudeSerie,           Math.min(altitudeSerie.length, maxLen)))         : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   cadenceSerie               " + (cadenceSerie != null           ? Arrays.toString(Arrays.copyOf(cadenceSerie,            Math.min(cadenceSerie.length, maxLen)))          : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   pulseSerie                 " + (pulseSerie != null             ? Arrays.toString(Arrays.copyOf(pulseSerie,              Math.min(pulseSerie.length, maxLen)))            : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   temperatureSerie           " + (temperatureSerie != null       ? Arrays.toString(Arrays.copyOf(temperatureSerie,        Math.min(temperatureSerie.length, maxLen)))      : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   speedSerie                 " + (speedSerie != null             ? Arrays.toString(Arrays.copyOf(speedSerie,              Math.min(speedSerie.length, maxLen)))            : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   powerSerie                 " + (powerSerie != null             ? Arrays.toString(Arrays.copyOf(powerSerie,              Math.min(powerSerie.length, maxLen)))            : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   pausedTime_Start           " + (pausedTime_Start != null       ? Arrays.toString(Arrays.copyOf(pausedTime_Start,        Math.min(pausedTime_Start.length, maxLen)))      : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + "   pausedTime_End             " + (pausedTime_End != null         ? Arrays.toString(Arrays.copyOf(pausedTime_End,          Math.min(pausedTime_End.length, maxLen)))        : UI.EMPTY_STRING) + UI.NEW_LINE //$NON-NLS-1$
            + custTrack
            + "   deviceMarker               " + (deviceMarker != null           ? Arrays.toString(Arrays.copyOf(deviceMarker,            Math.min(deviceMarker.length, maxLen)))          : UI.EMPTY_STRING) ; //$NON-NLS-1$

   }

// SET_FORMATTING_ON

}
