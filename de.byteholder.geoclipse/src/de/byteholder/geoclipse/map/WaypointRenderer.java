/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.GC;

import de.byteholder.gpx.Waypoint;

/**
 * A interface that draws waypoints. Implementations of WaypointRenderer can
 * be set on a WayPointPainter to draw waypoints on a JXMapViewer
 * 
 * @author joshua.marinacci@sun.com
 */
public interface WaypointRenderer {
	/**
	 * paint the specified waypoint on the specified map and graphics context
	 * 
	 * @param g
	 * @param map
	 * @param waypoint
	 * @param devPartOffset
	 */
    public void paintWaypoint(GC gc, Map map, Waypoint waypoint, int devPartOffset);
    
}
