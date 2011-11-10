/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.statistics;

import java.util.ArrayList;
import java.util.HashMap;

public class TourDataDay {

	long[]							tourIds;

	long[]							typeIds;
	int[]							typeColorIndex;

	int[]							yearValues;
	int[]							monthValues;
	int[]							weekValues;
	private int[]					_doyValues;
	private float[]					_doyValuesFloat;

	int[]							years;
	int[]							yearDays;
	int								allDaysInAllYears;

	float[]							distanceLow;
	float[]							distanceHigh;
	float[]							altitudeLow;
	float[]							altitudeHigh;

	private float[]					_timeLowFloat;
	private float[]					_timeHighFloat;

	int[]							recordingTime;
	int[]							drivingTime;

	int[]							tourStartValues;
	int[]							tourEndValues;

	float[]							tourDistanceValues;
	float[]							tourAltitudeValues;

	ArrayList<String>				tourTitle;
	ArrayList<String>				tourDescription;

	/**
	 * Contains the tags for the tour where the key is the tour ID
	 */
	HashMap<Long, ArrayList<Long>>	tagIds;

	public int[] getDoyValues() {
		return _doyValues;
	}

	public float[] getDoyValuesFloat() {
		return _doyValuesFloat;
	}

	public float[] getTimeHighFloat() {
		return _timeHighFloat;
	}

	public float[] getTimeLowFloat() {
		return _timeLowFloat;
	}

	public void setDoyValues(final int[] doyValues) {

		_doyValues = doyValues;
		_doyValuesFloat = new float[doyValues.length];

		for (int valueIndex = 0; valueIndex < doyValues.length; valueIndex++) {
			_doyValuesFloat[valueIndex] = doyValues[valueIndex];
		}
	}

	public void setTimeHigh(final int[] timeHigh) {

		_timeHighFloat = new float[timeHigh.length];

		for (int valueIndex = 0; valueIndex < timeHigh.length; valueIndex++) {
			_timeHighFloat[valueIndex] = timeHigh[valueIndex];
		}
	}

	public void setTimeLow(final int[] timeLow) {

		_timeLowFloat = new float[timeLow.length];

		for (int valueIndex = 0; valueIndex < timeLow.length; valueIndex++) {
			_timeLowFloat[valueIndex] = timeLow[valueIndex];
		}
	}

}
