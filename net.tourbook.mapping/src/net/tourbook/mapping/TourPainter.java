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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
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

	private final Image			fImageStartMarker;
	private final Image			fImageEndMarker;
	private final Image			fPositionImage;
	private final Image			fMarkerImage;

	public TourPainter() {

		super();

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);
		final Color systemColorRed = display.getSystemColor(SWT.COLOR_RED);
		fPositionImage = createPositionImage(systemColorBlue);
		fMarkerImage = createPositionImage(systemColorRed);

		fImageStartMarker = Activator.getIconImageDescriptor(IMAGE_START_MARKER).createImage();
		fImageEndMarker = Activator.getIconImageDescriptor(IMAGE_END_MARKER).createImage();

	}

	private Image createPositionImage(final Color positionColor) {

		final Display display = Display.getCurrent();

		final int width = 7;
		final int height = 7;

		final Image positionImage = new Image(display, width, height);
		final Color colorTransparent = new Color(display, 0xff, 0xff, 0xfe);

		final GC gcImage = new GC(positionImage);

//		gcImage.setAntialias(SWT.ON);

		gcImage.setBackground(colorTransparent);
		gcImage.fillRectangle(0, 0, width, height);

		gcImage.setBackground(positionColor);
		gcImage.fillOval(1, 1, width - 2, height - 2);

		/*
		 * set transparency
		 */
		final ImageData imageData = positionImage.getImageData();
		imageData.transparentPixel = imageData.getPixel(0, 0);
		final Image transparentImage = new Image(display, imageData);

//		gcImage.setAntialias(SWT.OFF);

		gcImage.dispose();
		positionImage.dispose();
		colorTransparent.dispose();

		return transparentImage;
	}

	@Override
	protected void dispose() {

		disposeImage(fImageStartMarker);
		disposeImage(fImageEndMarker);
		disposeImage(fPositionImage);
		disposeImage(fMarkerImage);

	}

	private void disposeImage(final Image image) {
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
	}

	@Override
	protected void doPaint(final GC gc, final Map map) {

		final PaintManager paintManager = PaintManager.getInstance();

		if (paintManager.isShowTourInMap() == false) {
			return;
		}

		final TourData tourData = paintManager.getTourData();

		if (tourData == null || tourData.latitudeSerie == null || tourData.longitudeSerie == null) {
			return;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		drawTour(gc, map, tourData);

		drawMarker(gc,
				map,
				latitudeSerie[latitudeSerie.length - 1],
				longitudeSerie[longitudeSerie.length - 1],
				fImageEndMarker);

		drawMarker(gc, map, latitudeSerie[0], longitudeSerie[0], fImageStartMarker);

	}

	private void drawMarker(final GC gc,
							final Map map,
							final double latitude,
							final double longitude,
							final Image markerImage) {

		if (markerImage == null) {
			return;
		}

		final java.awt.Rectangle viewport = map.getViewport();
		final TileFactory tileFactory = map.getTileFactory();

		Point position = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), map.getZoom());

		if (viewport.contains(position)) {

			// create relative pixel
			position = new java.awt.Point(position.x - viewport.x, position.y - viewport.y);

			final Rectangle bounds = markerImage.getBounds();
			final int imageWidth = bounds.width;
			final int imageHeight = bounds.height;

			gc.drawImage(markerImage, position.x - imageWidth / 2, position.y - imageHeight);
		}
	}

//	private void drawTour(final GC gc, final Map map, final double[] latitudeSerie, final double[] longitudeSerie) {
	private void drawTour(GC gc, Map map, TourData tourData) {

		final PaintManager paintManager = PaintManager.getInstance();

		final java.awt.Rectangle viewport = map.getViewport();
		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();

		/*
		 * check if tour is visible in the viewport
		 */
//		Point posMin = tileFactory.geoToPixel(new GeoPosition(tourData.mapMinLatitude, tourData.mapMinLongitude),
//				zoomLevel);
//		Point posMax = tileFactory.geoToPixel(new GeoPosition(tourData.mapMaxLatitude, tourData.mapMaxLongitude),
//				zoomLevel);
//
//		final java.awt.Rectangle tourRect = new java.awt.Rectangle(posMin.x, posMin.y, //
//				posMax.x - posMin.x,
//				posMin.y - posMax.y);
//		if (viewport.contains(tourRect) == false) {
//			return;
//		}
		double[] latitudeSerie = tourData.latitudeSerie;
		double[] longitudeSerie = tourData.longitudeSerie;
		final int leftSliderIndex = paintManager.getLeftSliderValueIndex();
		final int rightSliderIndex = paintManager.getRightSliderValueIndex();

		Point prevPixel = null;
		Point absolutePixel = null;
		Point relativePixel = null;
		gc.setLineWidth(2);

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);
		final Color systemColorRed = display.getSystemColor(SWT.COLOR_RED);

		final int posImageWidth = fPositionImage.getBounds().width / 2;
		final int posImageHeight = fPositionImage.getBounds().height / 2;

//		gc.setAntialias(SWT.ON);
		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			// Pixel zu Koordinaten abfragen
			absolutePixel = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[serieIndex],
					longitudeSerie[serieIndex]), zoomLevel);

			// get relative pixel
			relativePixel = new java.awt.Point(absolutePixel.x - viewport.x, absolutePixel.y - viewport.y);

			// initialize previous pixel
			if (prevPixel == null) {
				prevPixel = relativePixel;
			}

			// check if position is in the viewport or position has changed
			if (viewport.contains(absolutePixel) && relativePixel.equals(prevPixel) == false) {

				if (serieIndex < leftSliderIndex || serieIndex > rightSliderIndex) {
					gc.setForeground(systemColorBlue);
					gc.drawImage(fPositionImage, relativePixel.x - posImageWidth, relativePixel.y - posImageHeight);
				} else {
					gc.setForeground(systemColorRed);
					gc.drawImage(fMarkerImage, relativePixel.x - posImageWidth, relativePixel.y - posImageHeight);
				}

				gc.drawLine(prevPixel.x, prevPixel.y, relativePixel.x, relativePixel.y);
			}

			prevPixel = relativePixel;
		}

//		System.out.println(counter);
//		gc.setAntialias(SWT.OFF);

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
