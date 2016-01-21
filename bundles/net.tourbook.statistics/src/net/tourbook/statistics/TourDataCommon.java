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

/**
 * Common fields used in statistics
 */
public abstract class TourDataCommon {

	long[][]			typeIds;

	float[][]			distanceLow;
	float[][]			distanceHigh;

	float[][]			altitudeLow;
	float[][]			altitudeHigh;

	private float[][]	_timeLowFloat;
	private float[][]	_timeHighFloat;

	int[][]				recordingTime;
	int[][]				drivingTime;
	int[][]				breakTime;

	public float[][] getTimeHighFloat() {
		return _timeHighFloat;
	}

	public float[][] getTimeLowFloat() {
		return _timeLowFloat;
	}

	/**
	 * Set time values and convert it from int to float.
	 * 
	 * @param timeHigh
	 */
	public void setTimeHigh(final int[][] timeHigh) {

		if (timeHigh.length == 0 || timeHigh[0].length == 0) {
			_timeHighFloat = new float[0][0];
			return;
		}

		_timeHighFloat = new float[timeHigh.length][timeHigh[0].length];

		for (int outerIndex = 0; outerIndex < timeHigh.length; outerIndex++) {

			final int innerLength = timeHigh[outerIndex].length;

			for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
				_timeHighFloat[outerIndex][innerIndex] = timeHigh[outerIndex][innerIndex];
			}
		}
	}

	/**
	 * Set time values and convert it from int to float.
	 * 
	 * @param timeHigh
	 */
	public void setTimeLow(final int[][] timeLow) {

		if (timeLow.length == 0 || timeLow[0].length == 0) {
			_timeLowFloat = new float[0][0];
			return;
		}

		_timeLowFloat = new float[timeLow.length][timeLow[0].length];

		for (int outerIndex = 0; outerIndex < timeLow.length; outerIndex++) {

			final int innerLength = timeLow[outerIndex].length;

			for (int innerIndex = 0; innerIndex < innerLength; innerIndex++) {
				_timeLowFloat[outerIndex][innerIndex] = timeLow[outerIndex][innerIndex];
			}
		}
	}

}
