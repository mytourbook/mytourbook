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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.tourbook.data.TourData;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

/**
 * Paints the tour into the map
 */
public class TourPainter extends MapPainter {

	private static final String	IMAGE_PATH			= "icons/";

	private static final String	IMAGE_START_MARKER	= "map-marker-start.png";
	private static final String	IMAGE_END_MARKER	= "map-marker-end.png";

	private BufferedImage		fImageStartMarker;
	private BufferedImage		fImageEndMarker;

	public TourPainter() {
		super();
	}

	@Override
	protected void doPaint(Graphics2D g2d, final Map map) {

		final TourData tourData = PaintManager.getInstance().getTourData();

		if (tourData == null || tourData.latitudeSerie == null || tourData.longitudeSerie == null) {
			return;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		g2d = (Graphics2D) g2d.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawTour(g2d, map, latitudeSerie, longitudeSerie);

		drawMarker(g2d,
				map,
				latitudeSerie[latitudeSerie.length - 1],
				longitudeSerie[longitudeSerie.length - 1],
				getImage(fImageEndMarker, IMAGE_END_MARKER));
		drawMarker(g2d, map, latitudeSerie[0], longitudeSerie[0], getImage(fImageStartMarker, IMAGE_START_MARKER));

		g2d.dispose();
	}

	private void drawMarker(Graphics2D g2d, Map map, double latitude, double longitude, BufferedImage bufferedImage) {

		if (bufferedImage == null) {
			return;
		}

		Rectangle viewport = map.getViewport();
		TileFactory tileFactory = map.getTileFactory();

		Point position = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), map.getZoom());

		if (viewport.contains(position)) {

			position = map.getRelativePixel(position);

			final int imageWidth = bufferedImage.getWidth();
			final int imageHeight = bufferedImage.getHeight();

			g2d.drawImage(bufferedImage,
					position.x - imageWidth / 2,
					position.y - imageHeight,
					imageWidth,
					imageHeight,
					null);
		}
	}

	private void drawTour(Graphics2D g2d, final Map map, final double[] latitudeSerie, final double[] longitudeSerie) {

		Rectangle viewport = map.getViewport();
		TileFactory tileFactory = map.getTileFactory();

		int leftSliderIndex = PaintManager.getInstance().getLeftSliderValueIndex();
		int rightSliderIndex = PaintManager.getInstance().getRightSliderValueIndex();

		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			// Pixel zu Koordinaten abfragen
//			Point pixel = map.getTileFactory().geoToPixel(new GeoPosition(48.139722, 11.574444), map.getZoom());
			Point pixel = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
					map.getZoom());

			if (viewport.contains(pixel)) {
				pixel = map.getRelativePixel(pixel);

				if (serieIndex < leftSliderIndex || serieIndex > rightSliderIndex) {
					g2d.setColor(Color.BLUE);
				} else {
					g2d.setColor(Color.RED);
				}

//				g2d.setClip(pixel.x - 2, pixel.y - 2, 4, 4);
				g2d.fillOval(pixel.x - 2, pixel.y - 2, 4, 4);

//				g2d.setClip(pixel.x - 1, pixel.y - 1, 2, 2);
//				g2d.fillOval(pixel.x - 1, pixel.y - 1, 2, 2);
			}
		}
	}

	private BufferedImage getImage(BufferedImage bufferedImage, String imagePath) {

		if (bufferedImage != null) {
			return bufferedImage;
		}

		return bufferedImage = loadImage(IMAGE_PATH + imagePath);
	}

	/**
	 * Load image resouce
	 * 
	 * @param imagePath
	 * @return Returns loaded image
	 */
	private BufferedImage loadImage(String imagePath) {

		try {
			if (this.getClass().getResource("/" + imagePath) != null) {
				//when its inside a jar, does this
				InputStream imageStream = this.getClass().getResourceAsStream("/" + imagePath);
				return ImageIO.read(imageStream);
			} else {
				//when its running from eclipse, does this
				return ImageIO.read(new File(imagePath));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
