/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import java.io.Serializable;
import java.util.Arrays;

import net.tourbook.common.UI;

/**
 * All time serie data from a device are stored in the database with this class, when data are not
 * available the value is set to <code>null</code>
 */
public class SerieData implements Serializable {

   private static final long   serialVersionUID      = 1L;

   private static final String NL                    = UI.NEW_LINE1;
   private static final String DATA_SERIE_FORMAT     = "%5d %s";    //$NON-NLS-1$

   private static final int    SERIE_MAX_LENGTH      = 100;

   /**
    * This is necessary otherwise the text is wrapped in the text control, it seems that the max
    * length of a line in the text controls is 1024
    */
   private static final int    VALUE_TEXT_MAX_LENGTH = 990;

   public int[]                timeSerie;

   public float[]              altitudeSerie20;
   public float[]              cadenceSerie20;
   public float[]              distanceSerie20;
   public float[]              powerSerie20;
   public float[]              pulseSerie20;
   public float[]              speedSerie20;
   public float[]              temperatureSerie20;

   /**
    * These data series cannot be removed because they are needed to convert from int to float
    *
    * @since Db version 20
    */
   public int[]                altitudeSerie;
   public int[]                cadenceSerie;
   public int[]                distanceSerie;
   public int[]                powerSerie;
   public int[]                pulseSerie;
   public int[]                speedSerie;
   public int[]                temperatureSerie;

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
   public long[]               gears;

   /**
    * These data series cannot be removed because they are needed to convert from old double to new
    * int
    *
    * @since Db version 43
    */
   public double[]             longitude;
   public double[]             latitude;

   /**
    * Source: https://stackoverflow.com/questions/6059691/android-google-map-accuracy-issue
    * <br>
    * Worst case scenario is at the equator where one degree in Longitude is 111.320km. With the E6
    * number you are able to represent 0.000001 degree or a distance of 0.11132m (less than 4.5
    * inches). You are never going to get that level of accuracy out of a GPS system available to
    * the public anyway, so the loss of a single digit of precision will never be noticed.
    */
   public int[]                longitudeE6;
   public int[]                latitudeE6;

   /**
    * Pulse times in milliseconds.
    * <p>
    * <b>This data serie has not the same serie length as the other data series because 1 second can
    * have multiple values, depending on the heartrate.</b>
    */
   public int[]                pulseTimes;

   /**
    * Contains the time index into {@link TourData#timeSerie} for the pulse time(s) in
    * {@link TourData#pulseTime_Milliseconds}
    */
   public int[]                pulseTime_TimeIndex;

   /**
    * Running dynamics data
    *
    * @since Version 18.7
    */
   public short[]              runDyn_StanceTime;
   public short[]              runDyn_StanceTimeBalance;
   public short[]              runDyn_StepLength;
   public short[]              runDyn_VerticalOscillation;
   public short[]              runDyn_VerticalRatio;

   /**
    * Swim data
    *
    * @since Version 18.10
    */
   public short[]              swim_LengthType;                     // e.g. active, idle

   public short[]              swim_Cadence;                        // strokes/min
   public short[]              swim_Strokes;                        // strokes/length
   public short[]              swim_StrokeStyle;                    // e.g. freestyle, breaststroke
   public int[]                swim_Time;                           // relative time to the start time

   /**
    * Is <code>true</code> when a time slice in a data serie is visible.
    *
    * @since Version 18.12
    */
   public boolean[]            visiblePoints_Surfing;

   /**
    * Containing the start time of each pause (in milliseconds)
    */
   public long[]               pausedTime_Start;

   /**
    * Containing the end time of each pause (in milliseconds)
    */
   public long[]               pausedTime_End;

   /**
    * This field is never used but it must be kept for old data
    */
   public int[]                deviceMarker;

   /**
    * Containing the battery time in seconds, relative to the tour start time
    *
    * @since 21.9
    */
   public int[]                battery_Time;

   /**
    * Containing the battery percentage value
    *
    * @since 21.9
    */
   public short[]              battery_Percentage;

   private String dataSerieValues(final boolean[] dataSerie) {

      if (dataSerie == null || dataSerie.length == 0) {
         return UI.EMPTY_STRING;
      }

      final int numDataSlices = dataSerie.length;

      final String formattedText = String.format(DATA_SERIE_FORMAT,
            numDataSlices,
            Arrays.toString(Arrays.copyOf(dataSerie, Math.min(numDataSlices, SERIE_MAX_LENGTH))));

      return formattedText.substring(0, Math.min(formattedText.length(), VALUE_TEXT_MAX_LENGTH));
   }

   private String dataSerieValues(final float[] dataSerie) {

      if (dataSerie == null || dataSerie.length == 0) {
         return UI.EMPTY_STRING;
      }

      final int numDataSlices = dataSerie.length;

      final String formattedText = String.format(DATA_SERIE_FORMAT,
            numDataSlices,
            Arrays.toString(Arrays.copyOf(dataSerie, Math.min(numDataSlices, SERIE_MAX_LENGTH))));

      return formattedText.substring(0, Math.min(formattedText.length(), VALUE_TEXT_MAX_LENGTH));
   }

   private String dataSerieValues(final int[] dataSerie) {

      if (dataSerie == null || dataSerie.length == 0) {
         return UI.EMPTY_STRING;
      }

      final int numDataSlices = dataSerie.length;

      final String formattedText = String.format(DATA_SERIE_FORMAT,
            numDataSlices,
            Arrays.toString(Arrays.copyOf(dataSerie, Math.min(numDataSlices, SERIE_MAX_LENGTH))));

      return formattedText.substring(0, Math.min(formattedText.length(), VALUE_TEXT_MAX_LENGTH));
   }

   private String dataSerieValues(final long[] dataSerie) {

      if (dataSerie == null || dataSerie.length == 0) {
         return UI.EMPTY_STRING;
      }

      final int numDataSlices = dataSerie.length;

      final String formattedText = String.format(DATA_SERIE_FORMAT,
            numDataSlices,
            Arrays.toString(Arrays.copyOf(dataSerie, Math.min(numDataSlices, SERIE_MAX_LENGTH))));

      return formattedText.substring(0, Math.min(formattedText.length(), VALUE_TEXT_MAX_LENGTH));
   }

   private String dataSerieValues(final short[] dataSerie) {

      if (dataSerie == null || dataSerie.length == 0) {
         return UI.EMPTY_STRING;
      }

      final int numDataSlices = dataSerie.length;

      final String formattedText = String.format(DATA_SERIE_FORMAT,
            numDataSlices,
            Arrays.toString(Arrays.copyOf(dataSerie, Math.min(numDataSlices, SERIE_MAX_LENGTH))));

      return formattedText.substring(0, Math.min(formattedText.length(), VALUE_TEXT_MAX_LENGTH));
   }

   @Override
   public String toString() {

// SET_FORMATTING_OFF

      // these formatted data are displayed in the tour data view

      return NL + NL

            + "   timeSerie                  " + dataSerieValues(timeSerie)                     + NL //$NON-NLS-1$
            + "   pausedTime_Start           " + dataSerieValues(pausedTime_Start)              + NL //$NON-NLS-1$
            + "   pausedTime_End             " + dataSerieValues(pausedTime_End)                + NL //$NON-NLS-1$
            + "   pulseTimes                 " + dataSerieValues(pulseTimes)                    + NL //$NON-NLS-1$

            + "   altitudeSerie20            " + dataSerieValues(altitudeSerie20)               + NL //$NON-NLS-1$
            + "   cadenceSerie20             " + dataSerieValues(cadenceSerie20)                + NL //$NON-NLS-1$
            + "   distanceSerie20            " + dataSerieValues(distanceSerie20)               + NL //$NON-NLS-1$
            + "   powerSerie20               " + dataSerieValues(powerSerie20)                  + NL //$NON-NLS-1$
            + "   pulseSerie20               " + dataSerieValues(pulseSerie20)                  + NL //$NON-NLS-1$
            + "   speedSerie20               " + dataSerieValues(speedSerie20)                  + NL //$NON-NLS-1$
            + "   temperatureSerie20         " + dataSerieValues(temperatureSerie20)            + NL //$NON-NLS-1$

            + "   gears                      " + dataSerieValues(gears)                         + NL //$NON-NLS-1$

            + "   latitudeE6                 " + dataSerieValues(latitudeE6)                    + NL //$NON-NLS-1$
            + "   longitudeE6                " + dataSerieValues(longitudeE6)                   + NL //$NON-NLS-1$

            + "   runDyn_StanceTime          " + dataSerieValues(runDyn_StanceTime)             + NL //$NON-NLS-1$
            + "   runDyn_StanceTimeBalance   " + dataSerieValues(runDyn_StanceTimeBalance)      + NL //$NON-NLS-1$
            + "   runDyn_StepLength          " + dataSerieValues(runDyn_StepLength)             + NL //$NON-NLS-1$
            + "   runDyn_VerticalOscillation " + dataSerieValues(runDyn_VerticalOscillation)    + NL //$NON-NLS-1$
            + "   runDyn_VerticalRatio       " + dataSerieValues(runDyn_VerticalRatio)          + NL //$NON-NLS-1$

            + "   swim_LengthType            " + dataSerieValues(swim_LengthType)               + NL //$NON-NLS-1$
            + "   swim_Cadence               " + dataSerieValues(swim_Cadence)                  + NL //$NON-NLS-1$
            + "   swim_Strokes               " + dataSerieValues(swim_Strokes)                  + NL //$NON-NLS-1$
            + "   swim_StrokeStyle           " + dataSerieValues(swim_StrokeStyle)              + NL //$NON-NLS-1$
            + "   swim_Time                  " + dataSerieValues(swim_Time)                     + NL //$NON-NLS-1$

            + "   battery_Time               " + dataSerieValues(battery_Time)                  + NL //$NON-NLS-1$
            + "   battery_Percentage         " + dataSerieValues(battery_Percentage)            + NL //$NON-NLS-1$

            + "   visiblePoints_Surfing      " + dataSerieValues(visiblePoints_Surfing)         + NL //$NON-NLS-1$

            ;
   }

// SET_FORMATTING_ON

}
