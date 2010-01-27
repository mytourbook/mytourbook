/* *****************************************************************************
 *  Copyright (C) 2008 Michael Kanis and others
 *  
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>. 
 *******************************************************************************/

package de.byteholder.gpx;

import java.util.LinkedList;
import java.util.List;

/**
 * A Track Segment holds a list of Track Points which are logically connected
 * in order. To represent a single GPS track where GPS reception was lost,
 * or the GPS receiver was turned off, start a new Track Segment for each
 * continuous span of track data.
 * 
 * @author Michael Kanis
 */
public class TrackSegment {

	/**
	 * A Track Point holds the coordinates, elevation, timestamp, and metadata
	 * for a single point in a track.
	 */
	private List<Waypoint> trackPoints;

	
	public TrackSegment() {
		trackPoints = new LinkedList<Waypoint>();
	}
	
	
	public List<Waypoint> getTrackPoints() {
		return trackPoints;
	}
	
	public void addTrackPoint(Waypoint waypoint) {
		trackPoints.add(waypoint);
	}
}
