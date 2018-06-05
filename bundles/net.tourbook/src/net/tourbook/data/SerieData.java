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

/**
 * All time serie data from a device are stored in the database with this class, when data are not
 * available the value is set to <code>null</code>
 */
public class SerieData implements Serializable {

	private static final long	serialVersionUID	= 1L;

	public int[]				timeSerie;

	public float[]				distanceSerie20;
	public float[]				altitudeSerie20;
	public float[]				cadenceSerie20;
	public float[]				pulseSerie20;
	public float[]				temperatureSerie20;
	public float[]				speedSerie20;
	public float[]				powerSerie20;

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
	 * 	return (int) (gears &gt;&gt; 16 &amp; 0xff);
	 * }
	 * 
	 * public int getFrontGearTeeth() {
	 * 	return (int) (gears &gt;&gt; 24 &amp; 0xff);
	 * }
	 * 
	 * public int getRearGearNum() {
	 * 	return (int) (gears &amp; 0xff);
	 * }
	 * 
	 * public int getRearGearTeeth() {
	 * 	return (int) (gears &gt;&gt; 8 &amp; 0xff);
	 * }
	 * 
	 * </pre>
	 * 
	 * @since Db-version 27
	 */
	public long[]				gears;

	public double[]				longitude;
	public double[]				latitude;

	/**
	 * Pulse times in milliseconds.
	 * <p>
	 * <b>This data serie has not the same serie length as the other data series because 1 second
	 * can have multiple values, depending on the heartrate.</b>
	 */
	public int[]				pulseTimes;

	/*
	 * Running dynamics data
	 * @since Version 18.7
	 */
	public short[]				runDyn_StanceTime;
	public short[]				runDyn_StanceTime_Balance;
	public short[]				runDyn_StepLength;
	public short[]				runDyn_Vertical_Oscillation;
	public short[]				runDyn_Vertical_Ratio;

	/*
	 * these data series cannot be removed because they are needed to convert from int to float in
	 * db version 20
	 */
	public int[]				distanceSerie;
	public int[]				altitudeSerie;
	public int[]				cadenceSerie;
	public int[]				pulseSerie;
	public int[]				temperatureSerie;
	public int[]				speedSerie;
	public int[]				powerSerie;

	public int[]				deviceMarker;

}
