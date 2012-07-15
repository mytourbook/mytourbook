/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;

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
import de.byteholder.geoclipse.map.MapPainter;
import de.byteholder.geoclipse.mapprovider.MP;

public class DirectMappingPainter implements IDirectPainter {

	private Map			_map;
	private TourData	_tourData;

	private int			_leftSliderValueIndex;
	private int			_rightSliderValueIndex;

	private boolean		_isTourVisible;
	private boolean		_isShowSliderInMap;
	private boolean		_isShowSliderInLegend;

	private final Image	_imageLeftSlider;
	private final Image	_imageRightSlider;

	/**
	 * 
	 */
	public DirectMappingPainter() {
		_imageLeftSlider = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderLeft).createImage();
		_imageRightSlider = TourbookPlugin.getImageDescriptor(Messages.Image_Map_MarkerSliderRight).createImage();
	}

	/**
	 * set paint context to draw nothing
	 */
	public void disablePaintContext() {
		_map = null;
		_tourData = null;
		_isTourVisible = false;
	}

	public void dispose() {
		disposeImage(_imageLeftSlider);
		disposeImage(_imageRightSlider);
	}

	private void disposeImage(final Image image) {
		if ((image != null) && !image.isDisposed()) {
			image.dispose();
		}
	}

	private void drawSliderMarker(	final DirectPainterContext painterContext,
									int sliderValueIndex,
									final Image markerImage) {

		final MP mp = _map.getMapProvider();
		final int zoomLevel = _map.getZoom();

		final double[] latitudeSerie = _tourData.latitudeSerie;
		final double[] longitudeSerie = _tourData.longitudeSerie;

		// get world position for the slider coordinates
		sliderValueIndex = Math.min(sliderValueIndex, latitudeSerie.length - 1);
		final java.awt.Point worldPixelMarkerAWT = mp.geoToPixel(new GeoPosition(
				latitudeSerie[sliderValueIndex],
				longitudeSerie[sliderValueIndex]), zoomLevel);

		// convert awt to swt point
		final Point worldPixelMarker = new Point(worldPixelMarkerAWT.x, worldPixelMarkerAWT.y);

		// check if slider is visible
		final Rectangle viewport = painterContext.viewport;
		if (viewport.contains(worldPixelMarker)) {

			// convert world position into device position
			final int devMarkerPosX = worldPixelMarker.x - viewport.x;
			final int devMarkerPosY = worldPixelMarker.y - viewport.y;

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

		final MapLegend mapLegend = _map.getLegend();

		if (mapLegend == null) {
			return;
		}

		final Image legendImage = mapLegend.getImage();
		if ((legendImage == null) || legendImage.isDisposed()) {
			return;
		}

		final List<MapPainter> allMapPainter = _map.getMapPainter();
		if (allMapPainter == null || allMapPainter.size() == 0) {
			return;
		}

		// get first tour painter
		TourPainter tourPainter = null;
		for (final MapPainter mapPainter : allMapPainter) {
			if (mapPainter instanceof TourPainter) {
				tourPainter = (TourPainter) mapPainter;
				break;
			}
		}
		if (tourPainter == null) {
			return;
		}

		final Rectangle legendImageBounds = legendImage.getBounds();

		final int leftValueInlegendPosition = tourPainter.getLegendValuePosition(
				legendImageBounds,
				_leftSliderValueIndex);

		if (leftValueInlegendPosition == Integer.MIN_VALUE) {
			return;
		}

		final int rightValueInlegendPosition = tourPainter.getLegendValuePosition(
				legendImageBounds,
				_rightSliderValueIndex);

		if (rightValueInlegendPosition == Integer.MIN_VALUE) {
			return;
		}

		final Point legendInMapPosition = mapLegend.getLegendPosition();
		if (legendInMapPosition == null) {
			return;
		}

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

		if ((_map == null) || (_tourData == null) || (_isTourVisible == false)) {
			return;
		}

		if (_isShowSliderInMap) {
			drawSliderMarker(painterContext, _rightSliderValueIndex, _imageRightSlider);
			drawSliderMarker(painterContext, _leftSliderValueIndex, _imageLeftSlider);
		}

		if (_isShowSliderInLegend) {
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
		_map = map;
		_isTourVisible = isTourVisible;
		_tourData = tourData;
		_leftSliderValueIndex = leftSliderValuesIndex;
		_rightSliderValueIndex = rightSliderValuesIndex;
		_isShowSliderInMap = isShowSliderInMap;
		_isShowSliderInLegend = isShowSliderInLegend;
	}

}
