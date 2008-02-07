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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.DirectPainterContext;
import de.byteholder.geoclipse.swt.IDirectPainter;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.gpx.GeoPosition;

public class DirectMappingPainter implements IDirectPainter {

	private static final String	IMAGE_LEFT_SLIDER	= "map-marker-slider-left.png";	//$NON-NLS-1$
	private static final String	IMAGE_RIGHT_SLIDER	= "map-marker-slider-right.png";	//$NON-NLS-1$

	private Map					fMap;
	private TourData			fTourData;

	private int					fLeftSliderValueIndex;
	private int					fRightSliderValueIndex;

	private final Image			fImageLeftSlider;
	private final Image			fImageRightSlider;

	/**
	 * 
	 */
	public DirectMappingPainter() {
		fImageLeftSlider = Activator.getIconImageDescriptor(IMAGE_LEFT_SLIDER).createImage();
		fImageRightSlider = Activator.getIconImageDescriptor(IMAGE_RIGHT_SLIDER).createImage();
	}

	public void dispose() {
		disposeImage(fImageLeftSlider);
		disposeImage(fImageRightSlider);
	}

	private void disposeImage(final Image image) {
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
	}

	private void drawSliderMarker(final DirectPainterContext painterContext, int sliderValueIndex, Image markerImage) {

		final TileFactory tileFactory = fMap.getTileFactory();
		final int zoomLevel = fMap.getZoom();

		final double[] latitudeSerie = fTourData.latitudeSerie;
		final double[] longitudeSerie = fTourData.longitudeSerie;

		sliderValueIndex = Math.min(sliderValueIndex, latitudeSerie.length - 1);
		final Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[sliderValueIndex],
				longitudeSerie[sliderValueIndex]), zoomLevel);

		final java.awt.Rectangle viewport = painterContext.viewport;
		if (viewport.contains(worldMarkerPos)) {

			// convert world position into device position
			final int devMarkerPosX = worldMarkerPos.x - viewport.x;
			final int devMarkerPosY = worldMarkerPos.y - viewport.y;

			// get marker size
			final Rectangle bounds = markerImage.getBounds();
			final int markerWidth = bounds.width;
			final int markerWidth2 = markerWidth / 2;
			final int markerHeight = bounds.height;

			painterContext.gc.drawImage(markerImage, devMarkerPosX - markerWidth2, devMarkerPosY - markerHeight);
		}
	}

	public void paint(final DirectPainterContext painterContext) {

		if (fMap == null || fTourData == null) {
			return;
		}

		drawSliderMarker(painterContext, fRightSliderValueIndex, fImageRightSlider);
		drawSliderMarker(painterContext, fLeftSliderValueIndex, fImageLeftSlider);
	}

	public void setPaintContext(Map map,
								final TourData tourData,
								final int leftSliderValuesIndex,
								final int rightSliderValuesIndex) {
		fMap = map;
		fTourData = tourData;
		fLeftSliderValueIndex = leftSliderValuesIndex;
		fRightSliderValueIndex = rightSliderValuesIndex;
	}

}
