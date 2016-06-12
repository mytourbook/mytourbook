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

import net.tourbook.data.TourType;

/**
 * Common fields used in statistics
 */
public abstract class TourData_Common {

	long[][]			typeIds;
	int[][]				typeColorIndex;

	float[][]			distanceLow;
	float[][]			distanceHigh;

	float[][]			altitudeLow;
	float[][]			altitudeHigh;

	private float[][]	_durationTimeLowFloat;
	private float[][]	_durationTimeHighFloat;

	int[][]				recordingTime;
	int[][]				drivingTime;
	int[][]				breakTime;

	/**
	 * Contains the used {@link TourType} ID or -1 when not available. This data has the same length
	 * as the other common data.
	 */
	long[]				usedTourTypeIds;

	public float[][] getDurationTimeHighFloat() {
		return _durationTimeHighFloat;
	}

	public float[][] getDurationTimeLowFloat() {
		return _durationTimeLowFloat;
	}

	/**
	 * Set time values and convert it from int to float.
	 * 
	 * @param timeHigh
	 */
	public void setDurationTimeHigh(final int[][] timeHigh) {

		if (timeHigh.length == 0 || timeHigh[0].length == 0) {
			_durationTimeHighFloat = new float[0][0];
			return;
		}

		_durationTimeHighFloat = new float[timeHigh.length][timeHigh[0].length];

		for (int outerIndex = 0; outerIndex < timeHigh.length; outerIndex++) {

			final int innerLength = timeHigh[outerIndex].length;

			for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
				_durationTimeHighFloat[outerIndex][innerIndex] = timeHigh[outerIndex][innerIndex];
			}
		}
	}

	/**
	 * Set time values and convert it from int to float.
	 * 
	 * @param timeHigh
	 */
	public void setDurationTimeLow(final int[][] timeLow) {

		if (timeLow.length == 0 || timeLow[0].length == 0) {
			_durationTimeLowFloat = new float[0][0];
			return;
		}

		_durationTimeLowFloat = new float[timeLow.length][timeLow[0].length];

		for (int outerIndex = 0; outerIndex < timeLow.length; outerIndex++) {

			final int innerLength = timeLow[outerIndex].length;

			for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
				_durationTimeLowFloat[outerIndex][innerIndex] = timeLow[outerIndex][innerIndex];
			}
		}
	}

}
