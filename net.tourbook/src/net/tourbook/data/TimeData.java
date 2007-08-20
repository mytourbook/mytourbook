/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

/**
 * Contains data for one time slice
 */
public class TimeData implements Serializable {

	public long					id;

	private static final long	serialVersionUID	= -3435859239371853427L;

	/**
	 * this value is not
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
	 * relative value for altitude, this is the difference to the altitude of the previous time
	 * slice
	 */
	public short				altitude;

	/**
	 * realative value for distance, this is the difference to the distance of the previous time
	 * slice
	 */
	public int					distance;

	public short				marker;
}
