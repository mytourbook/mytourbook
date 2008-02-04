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

		if (paintManager.isShowTourInMap() == false) {
			return false;
		}

		final TourData tourData = paintManager.getTourData();

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		if (tourData == null || latitudeSerie == null || longitudeSerie == null) {
			return false;
		}

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
								Tile tile,
								final double latitude,
								final double longitude,
								final Image markerImage) {

		if (markerImage == null) {
			return false;
		}

		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();
		int tileSize = tileFactory.getInfo().getTileSize();

		// get world viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;
//		final java.awt.Rectangle tileViewport = new java.awt.Rectangle(worldTileX, worldTileY, tileSize, tileSize);

		// convert lat/long into world pixels
		Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldMarkerPos.x - worldTileX;
		final int devMarkerPosY = worldMarkerPos.y - worldTileY;

		boolean isMarkerInTile = isMarkerInTile(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);
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

		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();
		int tileSize = tileFactory.getInfo().getTileSize();

		// get viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;
		final java.awt.Rectangle tileViewport = new java.awt.Rectangle(worldTileX, worldTileY, tileSize, tileSize);

		Point worldPosition = null;
		Point devPosition = null;
		Point devPreviousPosition = null;

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		final int posImageWidth = fPositionImage.getBounds().width / 2;
		final int posImageHeight = fPositionImage.getBounds().height / 2;

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);

		boolean isTourInTile = false;
		int lastInsideIndex = -99;
		Point lastInsidePosition = null;

		gc.setForeground(systemColorBlue);
		gc.setLineWidth(2);
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

					gc.drawImage(fPositionImage, devPosition.x - posImageWidth, devPosition.y - posImageHeight);

					gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);
				}

				lastInsideIndex = serieIndex;
				lastInsidePosition = devPosition;

			} else {

				// current position is outside the tile

				if (serieIndex == lastInsideIndex + 1) {

					/*
					 * this position is the first which is outside of the tile, draw a line to from
					 * the last inside to the first outside position
					 */

					gc.drawLine(lastInsidePosition.x, lastInsidePosition.y, devPosition.x, devPosition.y);

				}
			}

			devPreviousPosition = devPosition;
		}

//		gc.setAntialias(SWT.OFF);

		return isTourInTile;
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
	private boolean isMarkerInTile(	Rectangle markerBounds,
									final int devMarkerPosX,
									final int devMarkerPosY,
									int tileSize) {

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
