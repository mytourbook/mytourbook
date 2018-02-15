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
package net.tourbook.data;

public class NormalizedGeoData {

	public int[]	normalizedLat;
	public int[]	normalizedLon;

	/**
	 * Contains the index of the original data
	 */
	public int[]	normalized2OriginalIndices;

	/**
	 * Tour which is normalized
	 */
	public long		tourId;

	public int		firstIndex;
	public int		lastIndex;

	public NormalizedGeoData(	final Long tourId,
								final int firstIndex,
								final int lastIndex,
								final int[] normalizedLat,
								final int[] normalizedLon,
								final int[] normalized2OriginalIndices) {

		this.tourId = tourId;

		this.firstIndex = firstIndex;
		this.lastIndex = lastIndex;

		this.normalizedLat = normalizedLat;
		this.normalizedLon = normalizedLon;

		this.normalized2OriginalIndices = normalized2OriginalIndices;
	}

}
