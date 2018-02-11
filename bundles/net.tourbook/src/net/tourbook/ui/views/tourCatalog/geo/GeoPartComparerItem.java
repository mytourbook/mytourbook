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

public class GeoPartComparerItem {

	long				tourId;

	GeoPartLoaderItem	loaderItem;

	/*
	 * Compare results
	 */
	long[]				tourLatLonDiff;
	int					tourMinDiffIndex;

	float				avgPulse;
	float				speed;

	public GeoPartComparerItem(final long tourId, final GeoPartLoaderItem loaderItem) {

		this.tourId = tourId;
		this.loaderItem = loaderItem;
	}

	@Override
	public String toString() {
		return "GeoPartComparerItem ["
				+ "tourId=" + tourId + ", "
				+ "loaderItem=" + loaderItem + "]";
	}

}
