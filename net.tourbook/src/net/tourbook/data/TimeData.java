/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.sql.Date;

import net.tourbook.ui.UI;

/**
 * Contains data for one time slice, when MIN_VALUE is finally set, these data are not available
 */
public class TimeData implements Serializable {

	public long					id;

	private static final long	serialVersionUID	= -3435859239371853427L;

	/**
	 * contains the difference to the previous time in seconds
	 */
	public int					time;

	/**
	 * Contains the time value from {@link Date#getTime()} or {@link Long#MIN_VALUE} when the time
	 * is not set. This time value is set when the tour is created by GPS devices
	 */
	public long					absoluteTime		= Long.MIN_VALUE;

	/**
	 * Contains time in seconds relative to the tour start
	 */
	public int					relativeTime;

	/**
	 * Absolute value for temperature in metric measurement system or {@link Integer#MIN_VALUE} when
	 * value is not set
	 */
	public int					temperature			= Integer.MIN_VALUE;

	/**
	 * absolute value for cadence or {@link Integer#MIN_VALUE} when cadence is not set
	 */
	public int					cadence				= Integer.MIN_VALUE;

	/**
	 * absolute value for pulse or {@link Integer#MIN_VALUE} when value is not set
	 */
	public int					pulse				= Integer.MIN_VALUE;

	/**
	 * relative value for altitude, this is the difference for the altitude with the previous time
	 * slice. Contains {@link Integer#MIN_VALUE} when value is not set.
	 */
	public int					altitude			= Integer.MIN_VALUE;

	/**
	 * contains the absolute altitude in meters or {@link Float#MIN_VALUE} when altitude is not set.
	 * Contains {@link Integer#MIN_VALUE} when value is not set.
	 */
	public float				absoluteAltitude	= Float.MIN_VALUE;

	/**
	 * relative value for distance in meters, this is the difference for the distance with the
	 * previous time slice. Contains {@link Integer#MIN_VALUE} when value is not set.
	 */
	public int					distance			= Integer.MIN_VALUE;

	/**
	 * Relative value for distance in meters, this is the difference for the distance with the
	 * previous time slice. Contains {@link Float#MIN_VALUE} when value is not set.
	 */
	public float				gpxDistance			= Float.MIN_VALUE; 

	/**
	 * contains the absolute distance in meters or {@link Float#MIN_VALUE} when distance is not set
	 */
	public float				absoluteDistance	= Float.MIN_VALUE;

	/**
	 * absolute value for power, power is typically provided by an ergo trainer. Contains
	 * {@link Integer#MIN_VALUE} when value is not set.
	 */
	public int					power				= Integer.MIN_VALUE;

	/**
	 * speed in km/h multiplied by 10, speed is typically provided by an ergo trainer not from a
	 * bike computer, Polar provides speed but is ignored. Contains {@link Integer#MIN_VALUE} when
	 * value is not set.
	 */
	public int					speed				= Integer.MIN_VALUE;

	/**
	 * Absolute value for latitude. Contains {@link Double#MIN_VALUE} when value is not set.
	 */
	public double				latitude			= Double.MIN_VALUE;

	/**
	 * Absolute value for longitude. Contains {@link Double#MIN_VALUE} when value is not set.
	 */
	public double				longitude			= Double.MIN_VALUE;

	/**
	 * a marker is set when {@link TimeData#marker} is NOT 0
	 */
	public int					marker				= 0;

	public String				markerLabel;

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder()//
				.append("absoluteTime:") //				//$NON-NLS-1$
				.append(absoluteTime)
				.append(UI.DASH_WITH_SPACE)
				.append("marker:") //		//$NON-NLS-1$
				.append(marker)
				.append(UI.DASH_WITH_SPACE)
				.append("markerLabel:") //		//$NON-NLS-1$
				.append(markerLabel);

		return sb.toString();
	}
}
