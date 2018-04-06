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

import java.util.Arrays;

public class NormalizedGeoData {

	/**
	 * <pre>
	 * 
	 *	decimal	decimal
	 *	places	degrees 	DMS 			qualitative scale that          N/S or E/W		E/W at		E/W at		E/W at
	 *										can be identified 				at equator		23N/S 		45N/S 		67N/S
	 * 
	 * 	0 		1.0 		1° 00′ 0″ 		country or large region 		111.32 km 		102.47 km 	78.71 km 	43.496 km
	 * 	1 		0.1 		0° 06′ 0″ 		large city or district 			11.132 km 		10.247 km 	7.871 km 	4.3496 km
	 * 	2 		0.01 		0° 00′ 36″ 		town or village 				1.1132 km 		1.0247 km 	787.1 m 	434.96 m
	 * 	3 		0.001 		0° 00′ 3.6″ 	neighborhood, street 			111.32 m 		102.47 m 	78.71 m 	43.496 m
	 * 	4 		0.0001 		0° 00′ 0.36″ 	individual street, land parcel 	11.132 m 		10.247 m 	7.871 m 	4.3496 m
	 * 	5 		0.00001 	0° 00′ 0.036″ 	individual trees 				1.1132 m 		1.0247 m 	787.1 mm 	434.96 mm
	 * 	6 		0.000001 	0° 00′ 0.0036″ 	individual humans 				111.32 mm 		102.47 mm 	78.71 mm 	43.496 mm
	 * 
	 * https://en.wikipedia.org/wiki/Decimal_degrees
	 * 
	 * Factor to normalize lat/lon
	 * 
	 * Degree * Integer = Resolution
	 * ---------------------------------------
	 * Deg *     100      1570 m
	 * Deg *   1_000       157 m
	 * Deg *  10_000        16 m
	 * Deg * 100_000         1.6 m
	 * </pre>
	 */
	public int		geoAccuracy			= 100_000;

	/**
	 * Distance interval in meters
	 */
	public int		distanceAccuracy	= 100;

	public int[]	normalizedLat;

	public int[]	normalizedLon;
	/**
	 * Contains the index of the original data
	 */
	public int[]	normalized2OriginalIndices;

	/**
	 * Normalized distance in meters
	 */
	public float	normalizedDistance;

	/**
	 * Normalized tour
	 */
	public long		tourId;

	/**
	 * First index of the original (not normalized) data
	 */
	public int		originalFirstIndex;

	/**
	 * Last index of the original (not normalized) data
	 */
	public int		originalLastIndex;

	@Override
	public String toString() {

		return "NormalizedGeoData ["

				+ "tourId=" + tourId + ", "

				+ "geoAccuracy=" + geoAccuracy + ", "
				+ "distanceAccuracy=" + distanceAccuracy + ", "

				+ "normalizedLat=" + Arrays.toString(normalizedLat) + ", "
				+ "normalizedLon=" + Arrays.toString(normalizedLon) + ", "
				+ "normalized2OriginalIndices=" + Arrays.toString(normalized2OriginalIndices) + ", "

				+ "originalFirstIndex=" + originalFirstIndex + ", "
				+ "originalLastIndex=" + originalLastIndex

				+ "]";
	}

}
