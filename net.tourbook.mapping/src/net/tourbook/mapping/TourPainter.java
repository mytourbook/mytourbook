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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.Tile;
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

	private int					fTourColorId;
	private int[]				fTourDataSerie;

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

		final int width = 8;
		final int height = 8;

		final Image positionImage = new Image(display, width, height);
		final Color colorTransparent = new Color(display, 0xff, 0xff, 0xfe);

		final GC gc = new GC(positionImage);

//		gc.setAntialias(SWT.ON);

		gc.setBackground(colorTransparent);
		gc.fillRectangle(0, 0, width, height);

		gc.setBackground(positionColor);
		gc.fillOval(1, 1, width - 2, height - 2);

		/*
		 * set transparency
		 */
		final ImageData imageData = positionImage.getImageData();
		imageData.transparentPixel = imageData.getPixel(0, 0);
		final Image transparentImage = new Image(display, imageData);

//		gc.setAntialias(SWT.OFF);

		gc.dispose();
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
	protected void doPaint(final GC gc, final Map map) {}

	@Override
	protected boolean doPaint(final GC gc, final Map map, final Tile tile) {

		final PaintManager paintManager = PaintManager.getInstance();

		final TourData tourData = paintManager.getTourData();
		if (tourData == null) {
			return false;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;
		if (latitudeSerie == null || longitudeSerie == null) {
			return false;
		}

		inizializeTourColor(tourData);

		// draw tour
		boolean isOverlayInTile = drawTourInTile(gc, map, tile, tourData);

		boolean isMarkerInTile = false;

		// draw end marker
		isMarkerInTile = drawMarker(gc,
				map,
				tile,
				latitudeSerie[latitudeSerie.length - 1],
				longitudeSerie[longitudeSerie.length - 1],
				fImageEndMarker);
		isOverlayInTile = isOverlayInTile || isMarkerInTile;

		// draw start marker
		isMarkerInTile = drawMarker(gc, map, tile, latitudeSerie[0], longitudeSerie[0], fImageStartMarker);
		isOverlayInTile = isOverlayInTile || isMarkerInTile;

		return isOverlayInTile;
	}

	private boolean drawMarker(	final GC gc,
								final Map map,
								final Tile tile,
								final double latitude,
								final double longitude,
								final Image markerImage) {

		if (markerImage == null) {
			return false;
		}

		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();
		final int tileSize = tileFactory.getInfo().getTileSize();

		// get world viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;

		// convert lat/long into world pixels
		final Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldMarkerPos.x - worldTileX;
		final int devMarkerPosY = worldMarkerPos.y - worldTileY;

		final boolean isMarkerInTile = isMarkerInTile(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);
		if (isMarkerInTile) {

			// get marker size
			final Rectangle bounds = markerImage.getBounds();
			final int markerWidth = bounds.width;
			final int markerWidth2 = markerWidth / 2;
			final int markerHeight = bounds.height;

			gc.drawImage(markerImage, devMarkerPosX - markerWidth2, devMarkerPosY - markerHeight);
		}

		return isMarkerInTile;
	}

	private boolean drawTourInTile(final GC gc, final Map map, final Tile tile, final TourData tourData) {

		final int lineWidth = 7;

		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();
		final int tileSize = tileFactory.getInfo().getTileSize();

		// get viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;
		final java.awt.Rectangle tileViewport = new java.awt.Rectangle(worldTileX, worldTileY, tileSize, tileSize);

		Point worldPosition = null;
		Point devPosition = null;
		Point devPreviousPosition = null;

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);
		gc.setForeground(systemColorBlue);
		gc.setLineWidth(lineWidth);

		boolean isTourInTile = false;
		int lastInsideIndex = -99;
		Point lastInsidePosition = null;

//		gc.setAntialias(SWT.ON);

		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			// convert lat/long into world pixels
			worldPosition = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[serieIndex],
					longitudeSerie[serieIndex]), zoomLevel);

			// convert world position into device position
			devPosition = new java.awt.Point(worldPosition.x - worldTileX, worldPosition.y - worldTileY);

			// initialize previous pixel
			if (devPreviousPosition == null) {
				devPreviousPosition = devPosition;
			}

			// check if position is in the viewport or position has changed
			if (tileViewport.contains(worldPosition)) {

				// current position is inside the tile

				if (devPosition.equals(devPreviousPosition) == false) {

					isTourInTile = true;

//					gc.drawImage(fPositionImage, devPosition.x - posImageWidth, devPosition.y - posImageHeight);

					drawTourLine(gc, serieIndex, devPosition, devPreviousPosition);
				}

				lastInsideIndex = serieIndex;
				lastInsidePosition = devPosition;

			} else {

				// current position is outside the tile

				if (serieIndex == lastInsideIndex + 1) {

					/*
					 * this position is the first which is outside of the tile, draw a line from the
					 * last inside to the first outside position
					 */

					drawTourLine(gc, serieIndex, devPosition, lastInsidePosition);
				}
			}

			devPreviousPosition = devPosition;
		}

//		gc.setAntialias(SWT.OFF);

		return isTourInTile;
	}

	private void drawTourLine(	final GC gc,
								final int serieIndex,
								final Point devPosition,
								final Point devPreviousPosition) {

		if (fTourDataSerie == null) {

			gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);

		} else {

			Color lineColor = null;
			switch (fTourColorId) {
			case MappingView.TOUR_COLOR_GRADIENT:
				lineColor = getGradientColor(fTourDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_PULSE:
				lineColor = getPulseColor(fTourDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_SPEED:
				lineColor = getSpeedColor(fTourDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_PACE:
				lineColor = getPaceColor(fTourDataSerie[serieIndex]);
				break;
			default:
				break;
			}

			{
				gc.setForeground(lineColor);
				gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);
			}
			lineColor.dispose();
		}

	}

	/**
	 * Get the color for the gradient value
	 * 
	 * @param value
	 *        current gradient in the tour
	 * @return Returns the color for the gradient which is red>25.5%, blue<25.5%, green==0 and
	 *         gradient colors between these gradient values
	 */
	private Color getGradientColor(final int value) {

		final int highValue = 30;
		final float accelerate = 1.3F;

		final int maxRed = 255;
		final int maxGreen = 200;
		final int maxBlue = 255;

		final int accelaratedValue = Math.min(255, Math.abs((int) (value * accelerate)));

		final int red = value < 0 ? 0 : value > highValue ? maxRed : Math.min(maxRed, accelaratedValue);
		final int green = value > -highValue && value < highValue ? maxGreen : accelaratedValue;
		final int blue = value > 0 ? 0 : value < -highValue ? maxBlue : accelaratedValue;

//		System.out.println(value + "\t" + accelaratedValue + "\t" + red + "\t" + green + "\t" + blue);

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private Color getPaceColor(int paceValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private Color getPulseColor(int pulseValue) {

		// green/blue
		//   0 bpm = 128
		//  50 bpm = 128
		// 100 bpm = 0
		// 200 bpm = 0
		float percent = pulseValue / 100F;
		float value = 128 - 128 * percent * 100 / 40;
		float greenBlue = pulseValue > 100 ? 0 : pulseValue < 40 ? 128 : value + 128;

		// red
		//   0 bpm = 255
		// 100 bpm = 255;
		// 200 bpm = 200;
		int redValue = pulseValue < 100 ? 255 : pulseValue > 200 ? 200 : pulseValue;

		int red = Math.max(0, Math.min(255, (int) redValue));
		int green = Math.max(0, Math.min(255, (int) greenBlue));
		int blue = green;

//		System.out.println(pulseValue + "\t" + red + "\t" + green + "\t" + blue);

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private Color getSpeedColor(int speedValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private void inizializeTourColor(final TourData tourData) {

		fTourColorId = PaintManager.getInstance().getTourColorId();

		switch (fTourColorId) {
		case MappingView.TOUR_COLOR_DEFAULT:

			fTourDataSerie = null;
			break;

		case MappingView.TOUR_COLOR_GRADIENT:

			final int[] gradientSerie = tourData.getGradientSerie();
			if (gradientSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_DEFAULT;
				fTourDataSerie = null;
			} else {
				fTourDataSerie = gradientSerie;
			}
			break;

		case MappingView.TOUR_COLOR_PULSE:

			final int[] pulseSerie = tourData.pulseSerie;
			if (pulseSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_DEFAULT;
				fTourDataSerie = null;
			} else {
				fTourDataSerie = pulseSerie;
			}
			break;

		case MappingView.TOUR_COLOR_SPEED:

			final int[] speedSerie = tourData.getSpeedSerie();
			if (speedSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_DEFAULT;
				fTourDataSerie = null;
			} else {
				fTourDataSerie = speedSerie;
			}
			break;

		case MappingView.TOUR_COLOR_PACE:

			final int[] paceSerie = tourData.getPaceSerie();
			if (paceSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_DEFAULT;
				fTourDataSerie = null;
			} else {
				fTourDataSerie = paceSerie;
			}
			break;

		default:
			break;
		}
	}

	/**
	 * Checks if the marker position is within the tile. The marker is above the marker position and
	 * one half to the left and right side
	 * 
	 * @param markerBounds
	 *        marker bounds
	 * @param devMarkerPosX
	 *        x position for the marker
	 * @param devMarkerPosY
	 *        y position for the marker
	 * @param tileSize
	 *        width and height of the tile
	 * @return Returns <code>true</code> when the marker is visible in the tile
	 */
	private boolean isMarkerInTile(	final Rectangle markerBounds,
									final int devMarkerPosX,
									final int devMarkerPosY,
									final int tileSize) {

		// get marker size
		final int markerWidth = markerBounds.width;
		final int markerWidth2 = markerWidth / 2;
		final int markerHeight = markerBounds.height;

		final int devMarkerPosLeft = devMarkerPosX - markerWidth2;
		final int devMarkerPosRight = devMarkerPosX + markerWidth2;

		// marker position top is in the opposite direction
		final int devMarkerPosTop = devMarkerPosY - markerHeight;

		if ((devMarkerPosLeft >= 0 && devMarkerPosLeft <= tileSize)
				|| (devMarkerPosRight >= 0 && devMarkerPosRight <= tileSize)) {

			if (devMarkerPosY >= 0 && devMarkerPosY <= tileSize || devMarkerPosTop >= 0 && devMarkerPosTop <= tileSize) {
				return true;
			}
		}

		return false;
	}

}
