/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.osm;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import net.tourbook.data.TourData;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

public class TourPainter extends MapPainter {

	public TourPainter() {
		super();
	}

	@Override
	protected void doPaint(Graphics2D g2d, final Map map) {

		final TourData tourData = PaintManager.getInstance().getTourData();

		if (tourData == null || tourData.latitudeSerie == null || tourData.longitudeSerie == null) {
			return;
		}

		final TileFactory tileFactory = map.getTileFactory();
		final Rectangle viewport = map.getViewport();

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		g2d = (Graphics2D) g2d.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			// Pixel zu Koordinaten abfragen
//			Point pixel = map.getTileFactory().geoToPixel(new GeoPosition(48.139722, 11.574444), map.getZoom());
			Point pixel = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
					map.getZoom());

			if (viewport.contains(pixel)) {
				pixel = map.getRelativePixel(pixel);
				g2d.setColor(Color.RED);
				g2d.setClip(pixel.x - 2, pixel.y - 2, 4, 4);
				g2d.fillOval(pixel.x - 2, pixel.y - 2, 4, 4);
//				g2d.setClip(pixel.x - 1, pixel.y - 1, 2, 2);
//				g2d.fillOval(pixel.x - 1, pixel.y - 1, 2, 2);
			}
		}

		// paint a marker when the tour is not easy to detect
		if (longitudeSerie.length > 0) {

			Rectangle2D tourBounds = getPositionRect(PaintManager.getInstance().getTourBounds(), map);
			Point pixel = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[0], longitudeSerie[0]), map.getZoom());

			if (tourBounds.getWidth() < 3 || tourBounds.getHeight() < 3) {

				pixel = map.getRelativePixel(pixel);

				g2d.setColor(Color.BLUE);
//				g2d.setBackground(Color.GREEN);

//				g2d.fillOval(pixel.x, pixel.y, 20, 20);
				int radius = 30;
				int radius2 = radius / 2;

				g2d.setClip(pixel.x - radius2, pixel.y - radius2, radius + 1, radius + 1);
				g2d.drawOval(pixel.x - radius2, pixel.y - radius2, radius, radius);
			}

		}

		g2d.dispose();
	}

	private Rectangle2D getPositionRect(final Set<GeoPosition> positions, Map map) {

		final TileFactory tileFactory = map.getTileFactory();
		final int zoom = map.getZoom();

		Point2D point1 = tileFactory.geoToPixel(positions.iterator().next(), zoom);
		Rectangle2D rect = new Rectangle2D.Double(point1.getX(), point1.getY(), 0, 0);

		for (GeoPosition pos : positions) {
			Point2D point = tileFactory.geoToPixel(pos, zoom);
			rect.add(point);
		}

		return rect;
	}

}
