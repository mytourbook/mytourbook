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

import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.ILegendProviderGradientColors;
import net.tourbook.data.TourData;
import net.tourbook.map2.view.ILegendProviderDiscreteColors;

public class TourTrack {

	private ITrackPath			_trackPath;

	private TourData			_tourData;
	private TourMap3Position[]	_trackPositions;
	private ILegendProvider		_colorProvider;
	private boolean				_isTourTrackPicked;

	private int					_tourTrackPickIndex;

	private PositionColors		_originalPositionColors;

	public TourTrack(	final ITrackPath trackPath,
						final TourData tourData,
						final ArrayList<TourMap3Position> trackPositions,
						final ILegendProvider colorProvider) {

		_trackPath = trackPath;

		_tourData = tourData;
		_trackPositions = trackPositions.toArray(new TourMap3Position[trackPositions.size()]);
		_colorProvider = colorProvider;
	}

	Color getColor(final Position pos, final Integer ordinal) {

		if (_isTourTrackPicked) {

			// prevent setting position colors
			return null;
//			return Color.pink;
		}

		final PositionColors positionColors = _trackPath.getPathPositionColors();
		if (positionColors instanceof TourPositionColors) {

			final TourPositionColors tourPosColors = (TourPositionColors) positionColors;
			final TourMap3Position trackPosition = _trackPositions[ordinal];

			if (_colorProvider instanceof ILegendProviderGradientColors) {

				return tourPosColors.getGradientColor(
						trackPosition.dataSerieValue,
						ordinal,
						_isTourTrackPicked,
						_tourTrackPickIndex);

			} else if (_colorProvider instanceof ILegendProviderDiscreteColors) {

				return tourPosColors.getDiscreteColor(trackPosition.colorValue);
			}
		}

		/**
		 * <code>null</code> is very important when <b>NO</b> position colors are set.
		 */

		return null;
	}

	public void setPicked(final boolean isTourTrackedPicked, final Integer pickIndex) {

		_isTourTrackPicked = isTourTrackedPicked;

		if (pickIndex == null) {
			_tourTrackPickIndex = -1;
		} else {
			_tourTrackPickIndex = pickIndex;
		}

		if (isTourTrackedPicked) {

			// tour IS picked

			_trackPath.resetPathTessellatedColors();

			final PositionColors positionColors = _trackPath.getPathPositionColors();

			if (positionColors != null) {

				_originalPositionColors = positionColors;

				_trackPath.setPathPositionColors(null);
			}

		} else {

			// tour is NOT picked

			if (_originalPositionColors != null) {

				_trackPath.setPathPositionColors(_originalPositionColors);
			}
		}
	}

	@Override
	public String toString() {

		return this.getClass().getSimpleName() + ("\t" + _tourData) //
//				+ ("\tTrack positions: " + _trackPositions.length)
				//
		;
	}
}
