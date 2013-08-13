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

//	/**
//	 * This is overwritten to check if tessellatedColors are available.
//	 * <p/>
//	 * {@inheritDoc}
//	 */
//	@Override
//	protected void addTessellatedPosition(	final Position pos,
//											final Color color,
//											final Integer ordinal,
//											final PathData pathData) {
//
//		final TourPathData tourPathData = (TourPathData) pathData;
//
//		if (ordinal != null) {
//			// NOTE: Assign these indices before adding the new position to the tessellatedPositions list.
//			final int index = tourPathData.getTessellatedPositions().size() * 2;
//			tourPathData.getPolePositions().put(index).put(index + 1);
//
//			if (tourPathData.isHasExtrusionPoints()) {
//				tourPathData.getPositionPoints().put(index);
//			} else {
//				tourPathData.getPositionPoints().put(tourPathData.getTessellatedPositions().size());
//			}
//		}
//
//		tourPathData.getTessellatedPositions().add(pos); // be sure to do the add after the pole position is set
//
//		if (color != null) {
//
//			final List<Color> tessellatedColors = tourPathData.getTessellatedColors();
//
//			if (tessellatedColors != null) {
//				tessellatedColors.add(color);
//			}
//		}
//	}

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
		return _tourTrack.getColor(pos, ordinal);
	}

	@Override
	public PositionColors getPathPositionColors() {
		return positionColors;
	}

	@Override
	public List<Color> getPathTessellatedColors() {
		return getCurrentPathData().getTessellatedColors();
	}

	@Override
	public void render(final DrawContext dc) {

		super.render(dc);

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \trender: " + dc);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	@Override
	public void resetPathTessellatedColors() {
		getCurrentPathData().setTessellatedColors(null);
	}

	@Override
	public void setPathHighlighted(final boolean isHighlighted) {
		setHighlighted(isHighlighted);
	}

	@Override
	public void setPathPositionColors(final PositionColors positionColors) {
		this.positionColors = positionColors;
	}

	@Override
	public void setPicked(final boolean isPicked, final Integer pickPositionIndex) {

		_tourTrack.setPicked(isPicked, pickPositionIndex);

		if (isPicked == false) {

			// after picking, ensure that the positions colors are set again

			getCurrentPathData().setExpired(true);
		}
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack) {
		_tourTrack = tourTrack;
	}
}
