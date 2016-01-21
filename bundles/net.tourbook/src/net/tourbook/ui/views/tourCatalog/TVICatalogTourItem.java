/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;

/**
 * this contains a tree item which represents a tour
 */
public abstract class TVICatalogTourItem extends TVICatalogItem {

	/**
	 * id for the {@link TourData} entity
	 */
	private long	tourId;

	/**
	 * @return Returns the TourId for the {@link TourData} entity
	 */
	public long getTourId() {
		return tourId;
	}

	/**
	 * Set the tour id for the tour item
	 * 
	 * @param tourId
	 */
	public void setTourId(final long tourId) {
		this.tourId = tourId;
	}

}
