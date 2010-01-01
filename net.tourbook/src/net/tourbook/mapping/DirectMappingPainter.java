/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.DirectPainterContext;
import de.byteholder.geoclipse.map.IDirectPainter;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.gpx.GeoPosition;

public class DirectMappingPainter implements IDirectPainter {

	private static final String	IMAGE_LEFT_SLIDER	= "map-marker-slider-left.png";	//$NON-NLS-1$
	private static final String	IMAGE_RIGHT_SLIDER	= "map-marker-slider-right.png";	//$NON-NLS-1$

	private Map					fMap;
	private boolean				fIsTourVisible;
	private TourData			fTourData;

	private int					fLeftSliderValueIndex;
	private int					fRightSliderValueIndex;

	private boolean				fIsShowSliderInMap;
	private boolean				fIsShowSliderInLegend;

	private final Image			fImageLeftSlider;
	private final Image			fImageRightSlider;

	/**
	 * 
	 */
	public DirectMappingPainter() {
		fImageLeftSlider = TourbookPlugin.getImageDescriptor(IMAGE_LEFT_SLIDER).createImage();
		fImageRightSlider = TourbookPlugin.getImageDescriptor(IMAGE_RIGHT_SLIDER).createImage();
	}

	/**
	 * set paint context to draw nothing
	 */
	public void disablePaintContext() {
		fMap = null;
		fTourData = null;
		fIsTourVisible = false;
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

	private void drawSliderMarker(	final DirectPainterContext painterContext,
									int sliderValueIndex,
									final Image markerImage) {

		final TileFactory tileFactory = fMap.getTileFactory();
		final int zoomLevel = fMap.getZoom();

		final double[] latitudeSerie = fTourData.latitudeSerie;
		final double[] longitudeSerie = fTourData.longitudeSerie;

		// get world position for the slider coordinates
		sliderValueIndex = Math.min(sliderValueIndex, latitudeSerie.length - 1);
		final java.awt.Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[sliderValueIndex],
				longitudeSerie[sliderValueIndex]), zoomLevel);

		// check if slider is visible
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

			// draw marker for the slider
			painterContext.gc.drawImage(markerImage, devMarkerPosX - markerWidth2, devMarkerPosY - markerHeight);
		}
	}

	private void drawValueMarkerInLegend(final DirectPainterContext painterContext) {

		final MapLegend mapLegend = fMap.getLegend();

		if (mapLegend == null) {
			return;
		}

		final Image legendImage = mapLegend.getImage();
		if (legendImage == null || legendImage.isDisposed()) {
			return;
		}

		final Rectangle legendImageBounds = legendImage.getBounds();

		final int leftValueInlegendPosition = TourPainter.getInstance().getLegendValuePosition(legendImageBounds,
				fLeftSliderValueIndex);
		if (leftValueInlegendPosition == Integer.MIN_VALUE) {
			return;
		}

		final int rightValueInlegendPosition = TourPainter.getInstance().getLegendValuePosition(legendImageBounds,
				fRightSliderValueIndex);
		if (rightValueInlegendPosition == Integer.MIN_VALUE) {
			return;
		}

		final Point legendInMapPosition = mapLegend.getLegendPosition();
		final int positionX = legendInMapPosition.x;
		final int positionY = legendInMapPosition.y + legendImageBounds.height - 2;

		final GC gc = painterContext.gc;

		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		int linePositionY = positionY - leftValueInlegendPosition;
		gc.drawLine(positionX + 1, linePositionY, positionX + 20, linePositionY);

		linePositionY = positionY - rightValueInlegendPosition;
		gc.drawLine(positionX + 1, linePositionY, positionX + 20, linePositionY);
	}

	public void paint(final DirectPainterContext painterContext) {

		if (fMap == null || fTourData == null || fIsTourVisible == false) {
			return;
		}

		if (fIsShowSliderInMap) {
			drawSliderMarker(painterContext, fRightSliderValueIndex, fImageRightSlider);
			drawSliderMarker(painterContext, fLeftSliderValueIndex, fImageLeftSlider);
		}

		if (fIsShowSliderInLegend) {
			drawValueMarkerInLegend(painterContext);
		}
	}

	/**
	 * @param map
	 * @param isTourVisible
	 *            <code>true</code> when tour is visible
	 * @param tourData
	 * @param leftSliderValuesIndex
	 * @param rightSliderValuesIndex
	 * @param isShowSliderInLegend
	 * @param isShowSliderInMap
	 * @param legendImageBounds
	 */
	public void setPaintContext(final Map map,
								final boolean isTourVisible,
								final TourData tourData,
								final int leftSliderValuesIndex,
								final int rightSliderValuesIndex,
								final boolean isShowSliderInMap,
								final boolean isShowSliderInLegend) {
		fMap = map;
		fIsTourVisible = isTourVisible;
		fTourData = tourData;
		fLeftSliderValueIndex = leftSliderValuesIndex;
		fRightSliderValueIndex = rightSliderValuesIndex;
		fIsShowSliderInMap = isShowSliderInMap;
		fIsShowSliderInLegend = isShowSliderInLegend;
	}

}
