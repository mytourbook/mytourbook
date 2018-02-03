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

public class GeoPart_LoaderItem {

	long	executorId;

	/**
	 * Requested geo parts
	 */
	int[]	geoParts;

	/**
	 * Tour id's which are having the at least one of the {@link #geoParts}
	 */
	long[]	tourIds;

	boolean	isUseAppFilter;

	public GeoPart_LoaderItem(final long executorId, final int[] geoParts, final boolean useAppFilter) {

		this.executorId = executorId;

		this.geoParts = geoParts;
		this.isUseAppFilter = useAppFilter;
	}

}
