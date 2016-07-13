/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;

public class TourData_Day {

	long[]							tourIds;

	long[]							typeIds;
	int[]							typeColorIndex;

	int[]							yearValues;
	int[]							monthValues;
	int[]							weekValues;
	private int[]					_doyValues;
	private double[]				_doyValuesDouble;

	int[]							years;
	int[]							yearDays;
	int								allDaysInAllYears;

	float[]							altitudeLow;
	float[]							altitudeHigh;
	float[]							avgPaceLow;
	float[]							avgPaceHigh;
	float[]							avgSpeedLow;
	float[]							avgSpeedHigh;
	float[]							distanceLow;
	float[]							distanceHigh;

	private float[]					_durationLowFloat;
	private float[]					_durationHighFloat;

	int[]							recordingTime;
	int[]							drivingTime;

	int[]							tourStartValues;
	int[]							tourEndValues;
	ArrayList<DateTime>				tourStartDateTimes;

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

	public double[] getDoyValuesDouble() {
		return _doyValuesDouble;
	}

	public float[] getDurationHighFloat() {
		return _durationHighFloat;
	}

	public float[] getDurationLowFloat() {
		return _durationLowFloat;
	}

	public void setDoyValues(final int[] doyValues) {

		_doyValues = doyValues;
		_doyValuesDouble = new double[doyValues.length];

		for (int valueIndex = 0; valueIndex < doyValues.length; valueIndex++) {
			_doyValuesDouble[valueIndex] = doyValues[valueIndex];
		}
	}

	public void setDurationHigh(final int[] timeHigh) {

		_durationHighFloat = new float[timeHigh.length];

		for (int valueIndex = 0; valueIndex < timeHigh.length; valueIndex++) {
			_durationHighFloat[valueIndex] = timeHigh[valueIndex];
		}
	}

	public void setDurationLow(final int[] timeLow) {

		_durationLowFloat = new float[timeLow.length];

		for (int valueIndex = 0; valueIndex < timeLow.length; valueIndex++) {
			_durationLowFloat[valueIndex] = timeLow[valueIndex];
		}
	}

}
