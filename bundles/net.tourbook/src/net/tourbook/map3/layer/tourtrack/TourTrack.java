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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path.PositionColors;

import java.awt.Color;
import java.util.ArrayList;

import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.map2.view.IDiscreteColorProvider;

public class TourTrack {

	private ITrackPath					_trackPath;

	private TourData					_tourData;

	private TourMap3Position[]			_trackPositions;
	private ArrayList<TourMap3Position>	_trackPositionsList;

	private IMapColorProvider			_colorProvider;

	private boolean						_isHovered;
	private boolean						_isSelected;

	private int							_tourTrackHoverIndex;

	public TourTrack(	final ITrackPath trackPath,
						final TourData tourData,
						final ArrayList<TourMap3Position> trackPositions,
						final IMapColorProvider colorProvider) {

		_trackPath = trackPath;

		_tourData = tourData;

		_trackPositions = trackPositions.toArray(new TourMap3Position[trackPositions.size()]);
		_trackPositionsList = trackPositions;

		_colorProvider = colorProvider;
	}

	Color getColor(final Position pos, final Integer ordinal) {

		final PositionColors positionColors = _trackPath.getPathPositionColors();
		if (positionColors instanceof TourPositionColors) {

			final TourPositionColors tourPosColors = (TourPositionColors) positionColors;
			final TourMap3Position trackPosition = _trackPositions[ordinal];

			if (_colorProvider instanceof IGradientColorProvider) {

				return tourPosColors.getGradientColor(trackPosition.dataSerieValue, ordinal, _tourTrackHoverIndex);

			} else if (_colorProvider instanceof IDiscreteColorProvider) {

				return tourPosColors.getDiscreteColor(trackPosition.colorValue);
			}
		}

		/**
		 * <code>null</code> is very important when <b>NO</b> position colors are set.
		 */

		return null;
	}

	public TourData getTourData() {
		return _tourData;
	}

	public ArrayList<TourMap3Position> getTrackPositions() {
		return _trackPositionsList;
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

	void updateColors(final double trackOpacity) {

		final PositionColors positionColors = _trackPath.getPathPositionColors();
		if (positionColors instanceof TourPositionColors) {

			final TourPositionColors tourPosColors = (TourPositionColors) positionColors;
			tourPosColors.updateColors(trackOpacity);
		}
	}

}
