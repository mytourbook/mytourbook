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

public class TrackPathHighResolution extends Path implements ITrackPath {

	private TourTrack		_tourTrack;

	@SuppressWarnings("unused")
	private TourTrackConfig	_tourTrackConfig;

	public TrackPathHighResolution(final ArrayList<TourMap3Position> trackPositions) {
		super(trackPositions);
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
	public TourTrack getTourTrack() {
		return _tourTrack;
	}

	@Override
	public void setExpired() {

		final AbstractShapeData pathData = getCurrentData();

		// it was null when implementing and testing
		if (pathData != null) {
			pathData.setExpired(true);
		}
	}

	@Override
	public void setPicked(final boolean isPicked, final Integer pickPositionIndex) {

		_tourTrack.setHovered(isPicked, pickPositionIndex);
	}

	@Override
	public void setTourTrack(final TourTrack tourTrack, final TourTrackConfig tourTrackConfig) {

		_tourTrack = tourTrack;
		_tourTrackConfig = tourTrackConfig;
	}

}
