/*
 * WaypointMapOverlay.java
 *
 * Created on April 1, 2006, 4:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Set;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import de.byteholder.gpx.Waypoint;

/**
 * Paints waypoints on a {@link Map}. This is an instance of {@link MapPainter} that in turn only
 * can draw on top of Maps.
 * 
 * @author rbair
 * @author Michael Kanis
 */
public class WaypointPainter extends MapPainter {

	private WaypointRenderer	renderer	= new DefaultWaypointRenderer();

	private Set<Waypoint>		waypoints;

	public WaypointPainter() {}

	@Override
	protected void dispose() {}

//	@Override
//	protected void doPaint(final GC gc, final Map map) {}
 
	/**
	 * {@inheritDoc}
	 * 
	 * @param e
	 * @param map
	 * @param width
	 * @param height
	 */
	@Override
	protected boolean doPaint(final GC gc, final Map map, final Tile tile, final int parts) {

		if (renderer == null) {
			return false;
		}

		if (waypoints == null) {
			return true;
		}

		// figure out which waypoints are within this map viewport
		final java.awt.Rectangle viewportBounds = map.getMapPixelViewport();
		final int zoom = map.getZoom();
		final Dimension sizeInTiles = map.getMapProvider().getMapTileSize(zoom);
		final int tileSize = map.getMapProvider().getTileSize();
//		final Dimension sizeInPixels = new Dimension(sizeInTiles.width * tileSize, sizeInTiles.height * tileSize);
		final int sizeInPixels = sizeInTiles.width * tileSize;
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		int vpx = viewportBounds.x;
		// normalize the left edge of the viewport to be positive
		while (vpx < 0) {
			vpx += sizeInPixels;
		}
		// normalize the left edge of the viewport to no wrap around the world
		while (vpx > sizeInPixels) {
			vpx -= sizeInPixels;
		}

		// create two new viewports next to eachother
		final Rectangle vp2 = new Rectangle(vpx, viewportBounds.y, viewportBounds.width, viewportBounds.height);
		final Rectangle vp3 = new Rectangle(
				vpx - sizeInPixels,
				viewportBounds.y,
				viewportBounds.width,
				viewportBounds.height);

		//for each waypoint within these bounds
		for (final Waypoint w : getWaypoints()) {
			final Point point = map.getMapProvider().geoToPixel(w.getPosition(), map.getZoom());
			if (vp2.contains(point)) {
				final int x = (point.x - vp2.x);
				final int y = (point.y - vp2.y);

				final Transform t = new Transform(Display.getCurrent());

				t.translate(x, y);
				gc.setTransform(t);

				paintWaypoint(w, map, gc, devPartOffset);

				gc.setTransform(null);
				t.dispose();
			}
			if (vp3.contains(point)) {
				final int x = (point.x - vp3.x);
				final int y = (point.y - vp3.y);

				final Transform t = new Transform(Display.getCurrent());

				t.translate(x, y);
				gc.setTransform(t);

				paintWaypoint(w, map, gc, devPartOffset);

				gc.setTransform(null);
				t.dispose();
			}
		}

		return true;
	}

	/**
	 * Gets the current set of waypoints to paint
	 * 
	 * @return a typed Set of Waypoints
	 */
	public Set<Waypoint> getWaypoints() {
		return waypoints;
	}

	/**
	 * <p>
	 * Override this method if you want more control over how a waypoint is painted than what you
	 * can get by just plugging in a custom waypoint renderer. Most developers should not need to
	 * override this method and can use a WaypointRenderer instead.
	 * </p>
	 * <p>
	 * This method will be called to each waypoint with the graphics object pre-translated so that
	 * 0,0 is at the center of the waypoint. This saves the developer from having to deal with
	 * lat/long => screen coordinate transformations.
	 * </p>
	 * 
	 * @param w
	 *            the current waypoint
	 * @param map
	 *            the current map
	 * @param devPartOffset
	 * @param g
	 *            the current graphics context
	 * @see setRenderer(WaypointRenderer)
	 * @see WaypointRenderer
	 */
	protected void paintWaypoint(final Waypoint w, final Map map, final GC gc, final int devPartOffset) {
		renderer.paintWaypoint(gc, map, w, devPartOffset);
	}

	/**
	 * Sets the waypoint renderer to use when painting waypoints
	 * 
	 * @param r
	 *            the new WaypointRenderer to use
	 */
	public void setRenderer(final WaypointRenderer r) {
		this.renderer = r;
	}

	/**
	 * Sets the current set of waypoints to paint
	 * 
	 * @param waypoints
	 *            the new Set of Waypoints to use
	 */
	public void setWaypoints(final Set<Waypoint> waypoints) {
		this.waypoints = waypoints;
	}
}
