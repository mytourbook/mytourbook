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
import gov.nasa.worldwind.render.MultiResolutionPath;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TrackPathResolutionViewport extends MultiResolutionPath implements ITrackPath {

	private TourTrack	_tourTrack;

	public TrackPathResolutionViewport(final ArrayList<TourMap3Position> trackPositions) {

		super(trackPositions);
	}

	@Override
	protected Color getColor(final Position pos, final Integer ordinal) {

		return _tourTrack.getColor(positionColors, pos, ordinal);
	}

	@Override
	public List<Color> getTessellatedColors() {

		return getCurrentPathData().getTessellatedColors();
	}

	@Override
	public void setPicked(final boolean isPicked, final Integer pickPositionIndex) {

		_tourTrack.setPicked(positionColors, isPicked, pickPositionIndex);
	}

	@Override
	public void setTessellatedColors(final ArrayList<Color> tessellatedColors) {

		getCurrentPathData().setTessellatedColors(tessellatedColors);
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack) {

		_tourTrack = tourTrack;
	}

}
