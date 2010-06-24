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

import java.util.ArrayList;
import java.util.List;

/**
 * GPX documents contain a metadata header, followed by waypoints, routes, and
 * tracks. You can add your own elements to the extensions section of the GPX
 * document.
 * 
 * @author Michael Kanis
 */
public class Document {

	/**
	 * Metadata about the file.
	 */
	private Metadata metadata;
	
	/**
	 * A list of waypoints.
	 */
	private List<Waypoint> waypoints;
	
	/**
	 * A list of routes.
	 */
	private List<Route> routes;
	
	/**
	 * A list of tracks.
	 */
	private List<Track> tracks;

	
	public Document() {
		waypoints = new ArrayList<Waypoint>();
		routes = new ArrayList<Route>();
		tracks = new ArrayList<Track>();
	}
	
	
	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public List<Track> getTracks() {
		return tracks;
	}

	public List<Waypoint> getWaypoints() {
		return waypoints;
	}
	
	public void addTrack(Track track) {
		tracks.add(track);
	}
}
