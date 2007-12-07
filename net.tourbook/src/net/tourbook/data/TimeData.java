/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

/**
 * Contains data for one time slice
 */
public class TimeData implements Serializable {

	public long					id;

	private static final long	serialVersionUID	= -3435859239371853427L;

	/**
	 * contains the difference to the previous time in seconds
	 */
	public short				time;

	/**
	 * absolute value for temperature
	 */
	public short				temperature;

	/**
	 * absolute value for cadence
	 */
	public short				cadence;

	/**
	 * absolute value for pulse
	 */
	public short				pulse;

	/**
	 * relative value for altitude, this is the difference for the altitude with the previous time
	 * slice
	 */
	public short				altitude;

	/**
	 * realative value for distance, this is the difference for the distance with the previous time
	 * slice
	 */
	public int					distance;

	/**
	 * contains the time value from {@link Date#getTime()} or {@link Long#MIN_VALUE} when the time
	 * is not set
	 */
	public long					absoluteTime		= Long.MIN_VALUE;

	/**
	 * contains the absolute altitude in meters or {@link Float#MIN_VALUE} when altitude is not set
	 */
	public float				absoluteAltitude	= Float.MIN_VALUE;

	/**
	 * contains the absolute distance in meters or {@link Float#MIN_VALUE} when distance is not set
	 */
	public float				absoluteDistance	= Float.MIN_VALUE;

	/**
	 * absolute value for latitude
	 */
	public double				latitude			= Double.MIN_VALUE;

	/**
	 * absolute value for longitude
	 */
	public double				longitude			= Double.MIN_VALUE;

	/**
	 * a marker is set when {@link TimeData#marker} is not 0
	 */
	public short				marker;

	public String				markerLabel;
}
