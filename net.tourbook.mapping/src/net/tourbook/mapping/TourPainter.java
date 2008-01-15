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

package net.tourbook.mapping;

import java.awt.Point;

import net.tourbook.data.TourData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

/**
 * Paints the tour into the map
 */
public class TourPainter extends MapPainter {

	private static final String	IMAGE_START_MARKER	= "map-marker-start.png";	//$NON-NLS-1$
	private static final String	IMAGE_END_MARKER	= "map-marker-end.png";	//$NON-NLS-1$

	private Image				fImageStartMarker;
	private Image				fImageEndMarker;

	public TourPainter() {

		super();

		getImage(fImageStartMarker, IMAGE_START_MARKER);
		getImage(fImageEndMarker, IMAGE_END_MARKER);
	}

	@Override
	protected void doPaint(GC gc, Map map) {

		final TourData tourData = PaintManager.getInstance().getTourData();

		if (tourData == null || tourData.latitudeSerie == null || tourData.longitudeSerie == null) {
			return;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		drawTour(gc, map, latitudeSerie, longitudeSerie);

		drawMarker(gc,
				map,
				latitudeSerie[latitudeSerie.length - 1],
				longitudeSerie[longitudeSerie.length - 1],
				fImageEndMarker);

		drawMarker(gc, map, latitudeSerie[0], longitudeSerie[0], fImageStartMarker);

	}

	private void drawMarker(GC gc, Map map, double latitude, double longitude, Image markerImage) {

		if (markerImage == null) {
			return;
		}

		java.awt.Rectangle viewport = map.getViewport();
		TileFactory tileFactory = map.getTileFactory();

		Point position = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), map.getZoom());

		if (viewport.contains(position)) {

			position = map.getRelativePixel(position);

			final Rectangle bounds = markerImage.getBounds();
			final int imageWidth = bounds.width;
			final int imageHeight = bounds.height;

			gc.drawImage(markerImage, position.x - imageWidth / 2, position.y - imageHeight);
		}
	}

	private void drawTour(GC gc, final Map map, final double[] latitudeSerie, final double[] longitudeSerie) {

		java.awt.Rectangle viewport = map.getViewport();
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
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
				} else {
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}

//				g2d.setClip(pixel.x - 2, pixel.y - 2, 4, 4);
				gc.fillOval(pixel.x - 2, pixel.y - 2, 4, 4);
			}
		}
	}

	private Image getImage(Image image, String imagePath) {

		if (image != null) {
			return image;
		}

		return image = Activator.getIconImageDescriptor(imagePath).createImage();
	}

	@Override
	protected void dispose() {

		if (fImageStartMarker != null && !fImageStartMarker.isDisposed()) {
			fImageStartMarker.dispose();
		}
		if (fImageEndMarker != null && !fImageEndMarker.isDisposed()) {
			fImageEndMarker.dispose();
		}
	}

//	/**
//	 * Load image resouce from jar or plugin
//	 * 
//	 * @param imagePath
//	 * @return Returns loaded image
//	 */
//	private BufferedImage loadImage(String imagePath) {
//		
//		try {
//			if (this.getClass().getResource("/" + imagePath) != null) { //$NON-NLS-1$
//				//when its inside a jar, does this
//				InputStream imageStream = this.getClass().getResourceAsStream("/" + imagePath); //$NON-NLS-1$
//				return ImageIO.read(imageStream);
//			} else {
//				//when its running from eclipse, does this
//				return ImageIO.read(new File(imagePath));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		return null;
//	}

}
