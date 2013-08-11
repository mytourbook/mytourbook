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
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TrackPathResolutionAll extends Path implements ITrackPath {

	private TourTrack	_tourTrack;

	private class TourPathData extends PathData {

		public TourPathData(final DrawContext dc, final Path shape) {
			super(dc, shape);
		}

	}

	public TrackPathResolutionAll(final ArrayList<TourMap3Position> trackPositions) {

		super(trackPositions);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to return a new instance of an accessable PathData.
	 */
	@Override
	protected AbstractShapeData createCacheEntry(final DrawContext dc) {
		return new TourPathData(dc, this);
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

		final PathData currentPathData = getCurrentPathData();

		currentPathData.setTessellatedColors(tessellatedColors);

//		if (tessellatedColors == null) {
//			currentPathData.setTessellatedPositions(null);
//		}
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack) {

		_tourTrack = tourTrack;
	}
}
