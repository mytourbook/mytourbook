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

package de.byteholder.geoclipse.map;

import java.awt.Point;
import java.awt.geom.Point2D;

import de.byteholder.gpx.GeoPosition;

/**
 * @author Joshua Marinacci
 * @author Michael Kanis
 */
public abstract class Projection {

	/**
	 * Given a position (latitude/longitude pair) and a zoom level, return the appropriate point in
	 * <em>pixels</em>. The zoom level is necessary because pixel coordinates are in terms of the
	 * zoom level
	 * 
	 * @param geoPosition
	 *            A lat/lon pair
	 * @param zoomLevel
	 *            the zoom level to extract the pixel coordinate for
	 */
	public abstract Point geoToPixel(GeoPosition geoPosition, int zoomLevel, TileFactoryInfo_OLD tileFactoryInfo);

	/**
	 * @param position1
	 * @param position2
	 * @param zoom
	 * @return Returns the distance in pixel between two geo positions
	 */
	public abstract double getHorizontalDistance(	GeoPosition position1,
													GeoPosition position2,
													int zoom,
													TileFactoryInfo_OLD info);

	/**
	 * @return Returns the id for the projection, each projection must have a unique id
	 */
	public abstract String getId();

	/**
	 * convert an on screen pixel coordinate and a zoom level to a geo position
	 */
	public abstract GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom, TileFactoryInfo_OLD info);

}
