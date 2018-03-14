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

import java.util.ArrayList;

import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourReference;

/**
 * Contains all data for a tour comparison
 */
public class GeoPartItem {

	long							executorId;

	/**
	 * When <code>true</code> then the loading/comparing of tours in this loader is canceled.
	 */
	boolean							isCanceled;

	boolean							isUseAppFilter;

	/**
	 * Geo part which should be compared
	 */
	public NormalizedGeoData		normalizedTourPart;

	/**
	 * Geo parts which are affected
	 * 
	 * <pre>
	 * 	Degree*INT    resolution
		---------------------------------------
		Deg*100		   1570 m
		Deg*1000		157 m
		Deg*10000		 16 m
		Deg*100000		1.6 m
	 * </pre>
	 */
	int[]							geoParts;

	/**
	 * Tour id's which are having at least one of the {@link #geoParts}
	 */
	long[]							tourIds;

	/**
	 * Results of the compared tours
	 */
	ArrayList<GeoPartComparerItem>	comparedTours	= new ArrayList<>();

	/**
	 * Id of the {@link TourReference}, is -1 when not available
	 */
	public long						refId;

	/**
	 * Time in ms to calculate sql data
	 */
	long							sqlRunningTime;

	public GeoPartItem(	final long executorId,
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
		return "GeoPartItem [" //$NON-NLS-1$
				+ "executorId=" + executorId + ", " //$NON-NLS-1$ //$NON-NLS-2$
				//				+ "geoParts=" + Arrays.toString(geoParts) + ", "
				//				+ "latPartSerie5=" + Arrays.toString(latPartSerie5) + ", "
				//				+ "lonPartSerie5=" + Arrays.toString(lonPartSerie5) + ", tourIds=" + Arrays.toString(tourIds) + ", "
				//				+ "isUseAppFilter=" + isUseAppFilter + ", "
				//				+ "sqlRunningTime=" + sqlRunningTime
				+ "]"; //$NON-NLS-1$
	}

}
