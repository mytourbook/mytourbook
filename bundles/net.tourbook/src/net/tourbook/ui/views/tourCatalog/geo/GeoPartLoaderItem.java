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
package net.tourbook.ui.views.tourCatalog.geo;

import net.tourbook.data.NormalizedGeoData;

/**
 * Loads tour id's which are contained in the {@link #geoParts}
 * 
 * <pre>
 * 
 * Degree*INT    resolution
   ---------------------------------------
   Deg*100		   1570 m
   Deg*1000			157 m
   Deg*10000		 16 m
   Deg*100000		  1.6 m
 * </pre>
 */
public class GeoPartLoaderItem {

	long				executorId;

	/**
	 * When <code>true</code> then the loading/comparing of tours in this loader is canceled.
	 */
	boolean				isCanceled;

	/**
	 * Requested geo parts
	 */
	int[]				geoParts;

	/**
	 * Geo part which should be compared
	 */
	NormalizedGeoData	normalizedTourPart;

	/**
	 * Tour id's which are having at least one of the {@link #geoParts}
	 */
	long[]				tourIds;

	boolean				isUseAppFilter;

	/**
	 * Time in ms to calculate sql data
	 */
	long				sqlRunningTime;

	public GeoPartLoaderItem(	final long executorId,
								final int[] geoParts,
								final NormalizedGeoData normalizedTourPart,
								final boolean useAppFilter) {

		this.executorId = executorId;

		this.geoParts = geoParts;
		this.normalizedTourPart = normalizedTourPart;

		this.isUseAppFilter = useAppFilter;
	}

	@Override
	public String toString() {
		return "GeoPartLoaderItem ["
				+ "executorId=" + executorId + ", "
				//				+ "geoParts=" + Arrays.toString(geoParts) + ", "
				//				+ "latPartSerie5=" + Arrays.toString(latPartSerie5) + ", "
				//				+ "lonPartSerie5=" + Arrays.toString(lonPartSerie5) + ", tourIds=" + Arrays.toString(tourIds) + ", "
				//				+ "isUseAppFilter=" + isUseAppFilter + ", "
				//				+ "sqlRunningTime=" + sqlRunningTime
				+ "]";
	}

}
