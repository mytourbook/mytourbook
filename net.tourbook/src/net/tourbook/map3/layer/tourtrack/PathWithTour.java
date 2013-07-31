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
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;

import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.ILegendProviderGradientColors;
import net.tourbook.map2.view.ILegendProviderDiscreteColors;

public class PathWithTour extends Path {

	private TourMap3Position[]	_trackPositions;
	private ILegendProvider		_colorProvider;

	public PathWithTour(final ArrayList<TourMap3Position> trackPositions, final ILegendProvider colorProvider) {

		super(trackPositions);

		_colorProvider = colorProvider;

		_trackPositions = trackPositions.toArray(new TourMap3Position[trackPositions.size()]);
	}

	@Override
	protected Color getColor(final Position pos, final Integer ordinal) {

		if (positionColors instanceof TourPositionColors) {

			final TourPositionColors tourPosColors = (TourPositionColors) positionColors;

			final TourMap3Position trackPosition = _trackPositions[ordinal];

			if (_colorProvider instanceof ILegendProviderGradientColors) {

				return tourPosColors.getColor(trackPosition.dataSerieValue);

			} else if (_colorProvider instanceof ILegendProviderDiscreteColors) {

				return tourPosColors.getColorFromColorValue(trackPosition.colorValue);
			}

		}

		return Color.CYAN;
	}

}
