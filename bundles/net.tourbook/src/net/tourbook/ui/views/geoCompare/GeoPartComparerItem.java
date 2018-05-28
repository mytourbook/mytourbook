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
package net.tourbook.ui.views.geoCompare;

import java.time.ZonedDateTime;

/**
 * Contains data for one comapred tour
 */
public class GeoPartComparerItem {

	public long			tourId;

	public GeoPartItem	geoPartItem;

	/*
	 * Compare results
	 */
	public float[]		tourLatLonDiff;

	public int			tourFirstIndex;
	public int			tourLastIndex;

	/**
	 * <ul>
	 * <li>-2 : Value is not yet set</li>
	 * <li>-1 : Value is invalid</li>
	 * <li>0...max : A Valid value is set</li>
	 * </ul>
	 */
	long				minDiffValue	= -2;

	float				avgPulse;
	float				avgSpeed;

	ZonedDateTime		tourStartTime;
	long				tourStartTimeMS;

	public GeoPartComparerItem(final long tourId, final GeoPartItem geoPartItem) {

		this.tourId = tourId;
		this.geoPartItem = geoPartItem;
	}

	@Override
	public String toString() {
		return "GeoPartComparerItem [" //$NON-NLS-1$
				+ "tourId=" + tourId + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "geoPartItem=" + geoPartItem + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
