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
package net.tourbook.ui.views.tourCatalog;

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
public class GeoPart_LoaderItem {

	long	executorId;

	/**
	 * Requested geo parts
	 */
	int[]	geoParts;

	/**
	 * Latatitudes which should be compared
	 */
	int[]	latPartSerie5;

	/**
	 * Longitudes which should be compared
	 */
	int[]	lonPartSerie5;

	/**
	 * Tour id's which are having at least one of the {@link #geoParts}
	 */
	long[]	tourIds;

	boolean	isUseAppFilter;

	/**
	 * Time in ms to calculate sql data
	 */
	long	sqlRunningTime;

	public GeoPart_LoaderItem(	final long executorId,
								final int[] geoParts,
								final int[] latPartSerie5,
								final int[] lonPartSerie5,
								final boolean useAppFilter) {

		this.executorId = executorId;

		this.geoParts = geoParts;

		this.latPartSerie5 = latPartSerie5;
		this.lonPartSerie5 = lonPartSerie5;

		this.isUseAppFilter = useAppFilter;
	}

}
