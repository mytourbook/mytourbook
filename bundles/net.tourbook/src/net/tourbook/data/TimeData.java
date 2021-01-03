/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
/*
 * Author: Wolfgang Schramm Created: 03.06.2005
 *
 *
 */
package net.tourbook.data;

import java.io.Serializable;

import net.tourbook.common.time.TimeTools;

/**
 * Contains data for one time slice.
 * <p>
 * Data are not available when a value is set to {@link Long#MIN_VALUE}, {@link Float#MIN_VALUE} or
 * {@link Double#MIN_VALUE}.
 */
public class TimeData implements Serializable {

   private static final long serialVersionUID           = -3435859239371853427L;

   public long               id;

   /**
    * contains the difference to the previous time in seconds
    */
   public int                time;

   /**
    * Contains number of milliseconds since the epoch of 1970-01-01T00:00:00Z value or
    * {@link Long#MIN_VALUE} when the time is not set. This time value is set when the tour is
    * created by GPS devices
    */
   public long               absoluteTime               = Long.MIN_VALUE;

   /**
    * Contains time in seconds relative to the tour start
    */
   public int                relativeTime;

   /**
    * Absolute value for temperature in metric measurement system or {@link Float#MIN_VALUE} when
    * value is not set
    */
   public float              temperature                = Float.MIN_VALUE;

   /**
    * absolute value for cadence or {@link Float#MIN_VALUE} when cadence is not set
    */
   public float              cadence                    = Float.MIN_VALUE;

   /**
    *
    */
   public long               gear;

   /**
    * absolute value for pulse or {@link Float#MIN_VALUE} when value is not set
    */
   public float              pulse                      = Float.MIN_VALUE;

   /**
    * relative value for altitude, this is the difference for the altitude with the previous time
    * slice. Contains {@link Float#MIN_VALUE} when value is not set.
    */
   public float              altitude                   = Float.MIN_VALUE;

   /**
    * Contains the absolute altitude in meters or {@link Float#MIN_VALUE} when altitude is not set.
    */
   public float              absoluteAltitude           = Float.MIN_VALUE;

   /**
    * relative value for distance in meters, this is the difference for the distance with the
    * previous time slice. Contains {@link Float#MIN_VALUE} when value is not set.
    */
   public float              distance                   = Float.MIN_VALUE;

   /**
    * Relative value for distance in meters, this is the difference for the distance with the
    * previous time slice. Contains {@link Float#MIN_VALUE} when value is not set.
    */
   public float              gpxDistance                = Float.MIN_VALUE;

   /**
    * contains the absolute distance in meters or {@link Float#MIN_VALUE} when distance is not set
    */
   public float              absoluteDistance           = Float.MIN_VALUE;

   /**
    * absolute value for power, power is typically provided by an ergo trainer. Contains
    * {@link Float#MIN_VALUE} when value is not set.
    */
   public float              power                      = Float.MIN_VALUE;

   /**
    * Name of the power data source or sensor
    */
   public String             powerDataSource;

   /**
    * Speed in km/h, speed is typically provided by an ergo trainer not from a bike computer, Polar
    * provides speed but is ignored. Contains {@link Float#MIN_VALUE} when value is not set.
    */
   public float              speed                      = Float.MIN_VALUE;

   /**
    * Absolute value for latitude. Contains {@link Double#MIN_VALUE} when value is not set.
    */
   public double             latitude                   = Double.MIN_VALUE;

   /**
    * Absolute value for longitude. Contains {@link Double#MIN_VALUE} when value is not set.
    */
   public double             longitude                  = Double.MIN_VALUE;

   /**
    * A marker is set when {@link TimeData#marker} is NOT 0
    */
   public int                marker                     = 0;

   public String             markerLabel;

   /**
    * Pulse time in milliseconds
    */
   public int[]              pulseTime;

   /**
    * Running dynamics data: Stance time. Contains {@link Short#MIN_VALUE} when value is not set.
    */
   public short              runDyn_StanceTime          = Short.MIN_VALUE;

   /**
    * Running dynamics data: Stance time balance. Contains {@link Short#MIN_VALUE} when value is not
    * set.
    */
   public short              runDyn_StanceTimeBalance   = Short.MIN_VALUE;

   /**
    * Running dynamics data: Step length. Contains {@link Short#MIN_VALUE} when value is not set.
    */
   public short              runDyn_StepLength          = Short.MIN_VALUE;

   /**
    * Running dynamics data: Vertical oscillation. Contains {@link Short#MIN_VALUE} when value is
    * not set.
    * <p>
    * This value is multiplied with {@link TourData#RUN_DYN_DATA_MULTIPLIER} and the unit is mm
    * (Millimeter)
    */
   public short              runDyn_VerticalOscillation = Short.MIN_VALUE;

   /**
    * Running dynamics data: Vertical ratio. Contains {@link Short#MIN_VALUE} when value is not set.
    */
   public short              runDyn_VerticalRatio       = Short.MIN_VALUE;


   //CUSTOM TRACKS
   public CustomTrackValue[] customTracks;

   public TimeData() {
      super();
   }

   @Override
   public String toString() {
      return "TimeData [" //$NON-NLS-1$

//            + ("id=" + id) //$NON-NLS-1$
//            + (", time=" + time) //$NON-NLS-1$
            + (", absoluteTime=" + TimeTools.getZonedDateTime(absoluteTime)) //$NON-NLS-1$
//            + (", relativeTime=" + relativeTime) //$NON-NLS-1$
//            + (", pulse=" + pulse) //$NON-NLS-1$
//            + (", temperature=" + temperature)
//            + (", cadence=" + cadence) //$NON-NLS-1$
//            + (", altitude=" + altitude) //$NON-NLS-1$
//            + (", absoluteAltitude=" + absoluteAltitude) //$NON-NLS-1$
//            + (", distance=" + distance) //$NON-NLS-1$
//            + (", gpxDistance=" + gpxDistance)
//            + (", absoluteDistance=" + absoluteDistance)
//            + (", power=" + power)
            + (", speed=" + speed) //$NON-NLS-1$
//            + (", latitude=" + latitude)
//            + (", longitude=" + longitude)
//            + (", marker=" + marker)
//            + (", markerLabel=" + markerLabel)
            + "]\n"; //$NON-NLS-1$
   }
}
