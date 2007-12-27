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
package net.tourbook.ui.views.tourMap;

import net.tourbook.data.TourData;

public class TourDataNormalizer {

	public static final int	NORMALIZED_DISTANCE	= 100;

	private int[]			normalizedDistance	= null;
	private int[]			normalizedAltitude	= null;

	public void normalizeAltitude(TourData tourData, int measureStartIndex, int measureEndIndex) {

		int[] measureAltitudes = tourData.altitudeSerie;
		int[] measureDistances = tourData.getMetricDistanceSerie();

		if (measureAltitudes == null || measureDistances == null) {
			return;
		}

		// create normalized data, the distance will be normalized to 100m
		int normStartDistance = measureDistances[measureStartIndex] / NORMALIZED_DISTANCE;
		int normEndDistance = measureDistances[measureEndIndex] / NORMALIZED_DISTANCE;
		int normSize = normEndDistance - normStartDistance + 1;

		normalizedDistance = new int[normSize];
		normalizedAltitude = new int[normSize];

		int normDistance = normStartDistance * NORMALIZED_DISTANCE;
		int normAltitude = 0;

		int measureIndex = measureStartIndex;
		int measureLastDistance = measureDistances[measureIndex];
		int measureLastAltitude = measureAltitudes[measureStartIndex];

		int measureNextDistance = 0;
		int measureNextAltitude = 0;

		int measureDistanceDiff;
		int measureAltitudeDiff;
		int distanceDiff = 0;

		for (int normIndex = 0; normIndex < normSize; normIndex++) {

			// get the last measure point before the next normalized distance
			while (measureNextDistance <= normDistance && measureIndex < measureDistances.length - 1) {

				// set the index to the next measure point
				measureIndex++;

				measureNextDistance = measureDistances[measureIndex];
				measureNextAltitude = measureAltitudes[measureIndex];
			}

			// make sure to get data which are not out of the array range
			if (measureIndex != 0 || measureIndex < measureDistances.length) {
				measureLastDistance = measureDistances[measureIndex - 1];
				measureLastAltitude = measureAltitudes[measureIndex - 1];
			}

			if (measureNextDistance == normDistance) {

				// normalized distance is the current measure distance

				normAltitude = measureLastAltitude;

			} else {

				// measured distance is not at a normalized distance but still
				// below the normalized distance

				measureDistanceDiff = measureNextDistance - measureLastDistance;
				measureAltitudeDiff = measureNextAltitude - measureLastAltitude;
				distanceDiff = normDistance - measureLastDistance;

				if (measureDistanceDiff == 0 || distanceDiff == 0) {
					normAltitude = 0;
				} else {
					normAltitude = (int) ((float) measureAltitudeDiff / (float) measureDistanceDiff * distanceDiff);
				}
			}

			normalizedDistance[normIndex] = normDistance;
			normalizedAltitude[normIndex] = measureLastAltitude + normAltitude;

			// next normalized distance
			normDistance += NORMALIZED_DISTANCE;
		}
	}

	/**
	 * @return Returns the normalizedDistance.
	 */
	public int[] getNormalizedDistance() {
		return normalizedDistance;
	}

	/**
	 * @return Returns the normalizedAltitude.
	 */
	public int[] getNormalizedAltitude() {
		return normalizedAltitude;
	}
}
