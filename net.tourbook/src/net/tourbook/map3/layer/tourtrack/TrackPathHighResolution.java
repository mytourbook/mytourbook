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

public class TrackPathHighResolution extends Path implements ITrackPath {

	private TourTrack	_tourTrack;

	public TrackPathHighResolution(final ArrayList<TourMap3Position> trackPositions) {
		super(trackPositions);
	}

	@Override
	protected void addTessellatedPosition(final Position pos, final Color color, final Integer ordinal, final PathData pathData) {

		try {
			super.addTessellatedPosition(pos, color, ordinal, pathData);
		} catch (final Exception e) {
			// ignore, it can happen
		}
	}

	@Override
	protected void drawPointsVBO(final DrawContext dc, final int[] vboIds, final PathData pathData) {

		try {
			super.drawPointsVBO(dc, vboIds, pathData);
		} catch (final Exception e) {
			// ignore, it can happen
		}
	}
	@Override
	protected Color getColor(final Position pos, final Integer ordinal) {
		return _tourTrack.getColor(pos, ordinal);
	}

	@Override
	public Path getPath() {
		return this;
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
	public TourTrack getTourTrack() {
		return _tourTrack;
	}

	@Override
	public void resetPathTessellatedColors() {

		final PathData currentPathData = getCurrentPathData();

		/*
		 * can be null when not initialized, this happened when a tour is reselected but not yet
		 * displayed
		 */
		if (currentPathData != null) {
			currentPathData.setTessellatedColors(null);
		}
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

		_tourTrack.setHovered(isPicked, pickPositionIndex);

		if (isPicked == false) {

			/*
			 * This hack prevents an tess color NPE, it took me many days to understand 3D drawing
			 * and find this "solution".
			 */
			getCurrentPathData().setTessellatedPositions(null);
		}

		// after picking, ensure that the positions colors are set again
		getCurrentPathData().setExpired(true);
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack) {
		_tourTrack = tourTrack;
	}

	@Override
	protected boolean shouldUseVBOs(final DrawContext dc) {

		final List<Position> tessellatedPositions = this.getCurrentPathData().getTessellatedPositions();

		// NPE can occure
		if (tessellatedPositions == null) {
			return true;
		}

		return tessellatedPositions.size() > VBO_THRESHOLD && super.shouldUseVBOs(dc);
	}
}
