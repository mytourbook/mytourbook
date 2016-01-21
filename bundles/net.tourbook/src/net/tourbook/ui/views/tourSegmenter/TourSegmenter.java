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
package net.tourbook.ui.views.tourSegmenter;

import net.tourbook.ui.views.tourSegmenter.TourSegmenterView.SegmenterType;

class TourSegmenter implements Comparable<Object> {

	SegmenterType	segmenterType;

	String			name;
	int				requiredDataSeries;

	public TourSegmenter(final SegmenterType segmenterType, final String name, final int requiredDataSeries) {

		this.segmenterType = segmenterType;
		this.name = name;
		this.requiredDataSeries = requiredDataSeries;
	}

	@Override
	public int compareTo(final Object obj) {

		if (obj instanceof TourSegmenter) {
			final TourSegmenter otherSegmenter = (TourSegmenter) obj;
			return name.compareTo(otherSegmenter.name);
		}

		return 0;
	}

}
