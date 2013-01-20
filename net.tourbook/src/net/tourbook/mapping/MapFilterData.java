/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

public class MapFilterData {

	public int	allPhotos;
	public int	filteredPhotos;

	/**
	 * @param allPhotos
	 *            Number of photos which could be displayed in the map.
	 * @param filteredPhotos
	 *            Number of photos which are filtered.
	 */
	public MapFilterData(final int allPhotos, final int filteredPhotos) {

		this.allPhotos = allPhotos;
		this.filteredPhotos = filteredPhotos;
	}

}
