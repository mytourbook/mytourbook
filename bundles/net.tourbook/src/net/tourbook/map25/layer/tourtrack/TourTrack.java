/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.layer.tourtrack;

import net.tourbook.data.TourData;

public class TourTrack {

	private ITrackPath					_trackPath;

	private TourData					_tourData;



	private boolean						_isHovered;
	private boolean						_isSelected;

	private int							_tourTrackHoverIndex;

	public TourTrack(	final ITrackPath trackPath,
						final TourData tourData) {

		_trackPath = trackPath;
		_tourData = tourData;
	}


	public TourData getTourData() {
		return _tourData;
	}


	boolean isHovered() {
		return _isHovered;
	}

	boolean isSelected() {
		return _isSelected;
	}

	void setHovered(final boolean isHovered, final Integer hoveredTrackIndex) {

		_isHovered = isHovered;

		if (hoveredTrackIndex == null) {
			_tourTrackHoverIndex = -1;
		} else {
			_tourTrackHoverIndex = hoveredTrackIndex;
		}
	}

	void setSelected(final boolean isSelected) {
		_isSelected = isSelected;
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName() + ("\t" + _tourData) // //$NON-NLS-1$
//				+ ("\tTrack positions: " + _trackPositions.length)
				//
		;
	}


}
