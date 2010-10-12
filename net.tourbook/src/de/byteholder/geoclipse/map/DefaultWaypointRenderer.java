/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import de.byteholder.gpx.Waypoint;

/**
 * This is a standard waypoint renderer. It draws all waypoints as blue
 * circles with crosshairs over the waypoint center
 * 
 * @author joshy
 */
public class DefaultWaypointRenderer implements WaypointRenderer {

	public DefaultWaypointRenderer() {}

	/**
	 * {@inheritDoc}
	 * 
	 * @param gc
	 * @param map
	 * @param waypoint
	 * @return
	 */
	public void paintWaypoint(final GC gc, final Map map, final Waypoint waypoint, final int devPartOffset) {

		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		gc.drawOval(devPartOffset + -10, devPartOffset + -10, 20, 20);

		gc.drawLine(devPartOffset + -10, devPartOffset + 0, devPartOffset + 10, devPartOffset + 0);
		gc.drawLine(devPartOffset + 0, devPartOffset + -10, devPartOffset + 0, devPartOffset + 10);
	}

}
