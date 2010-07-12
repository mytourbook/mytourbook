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

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceMap;
import net.tourbook.ui.ColorCacheInt;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapPainter;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.gpx.GeoPosition;

/**
 * Paints a tour into the map
 */
public class TourPainter extends MapPainter {

	private static final int				MARKER_MARGIN		= 2;
	private static final int				MARKER_POLE			= 16;

	private final static IPreferenceStore	_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	private static IPropertyChangeListener	_prefChangeListener;

	private int[]							_dataSerie;
	private ILegendProvider					_legendProvider;

	// painting parameter
	private int								_lineWidth;
	private int								_lineWidth2;

	private static boolean					_prefIsDrawLine;
	private static boolean					_prefIsDrawSquare;
	private static int						_prefLineWidth;
	private static boolean					_prefWithBorder;
	private static int						_prefBorderWidth;

	private static boolean					_isImageAvailable	= false;

	/**
	 * Tour start/end image
	 */
	private static Image					_tourStartMarker;
	private static Image					_tourEndMarker;

	/**
	 * Tour Way Point image
	 */
	private static Image					_twpImage;
	private static Rectangle				_twpImageBounds;

	private final static ColorCacheInt		_colorCache			= new ColorCacheInt();

	static {

		/**
		 * this code optimizes the performance by reading from the pref store which is not very
		 * efficient
		 */
		getTourPainterSettings();

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {
					getTourPainterSettings();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

		// remove pre listener
		/*
		 * this listener is never removed
		 */

//		container.addDisposeListener(new DisposeListener() {
//			public void widgetDisposed(final DisposeEvent e) {
//				TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(_prefChangeListener);
//			}
//		});
	}

	public TourPainter() {

		super();

		/*
		 * I've not yet understood to manage this problem because TourPainter() is created from
		 * an extension point but setting the instance in the constructor is not valid according
		 * to FindBugs
		 */
	}

	/**
	 * Draw legend colors into the legend bounds
	 * 
	 * @param gc
	 * @param legendBounds
	 * @param isVertical
	 *            when <code>true</code> the legend is drawn vertical, when false the legend is
	 *            drawn horizontal
	 * @param colorId
	 * @return
	 */
	public static void drawLegendColors(final GC gc,
										final Rectangle legendBounds,
										final ILegendProvider legendProvider,
										final boolean isVertical) {

		if (legendProvider == null) {
			return;
		}

		final Device display = gc.getDevice();
		final LegendConfig config = legendProvider.getLegendConfig();

		// ensure units are available
		if (config.units == null /* || config.unitLabels == null */) {
			return;
		}

		// get configuration for the legend
		final ArrayList<Integer> legendUnits = new ArrayList<Integer>(config.units);
		final Integer unitFactor = config.unitFactor;
		final int legendMaxValue = config.legendMaxValue;
		final int legendMinValue = config.legendMinValue;
		final int legendDiffValue = legendMaxValue - legendMinValue;
		final String unitText = config.unitText;
		final List<String> unitLabels = config.unitLabels;

		Rectangle legendBorder;

		int legendPositionX;
		int legendPositionY;
		int legendWidth;
		int legendHeight;
		int availableLegendPixels;

		if (isVertical) {

			// vertical legend

			legendPositionX = legendBounds.x + 1;
			legendPositionY = legendBounds.y + TourMapView.LEGEND_MARGIN_TOP_BOTTOM;
			legendWidth = 20;
			legendHeight = legendBounds.height - 2 * TourMapView.LEGEND_MARGIN_TOP_BOTTOM;

			availableLegendPixels = legendHeight - 1;

			legendBorder = new Rectangle(legendPositionX - 1, //
					legendPositionY - 1,
					legendWidth + 1,
					legendHeight + 1);

		} else {

			// horizontal legend

			legendPositionX = legendBounds.x + 1;
			legendPositionY = legendBounds.y + 1;
			legendWidth = legendBounds.width - 1;
			legendHeight = legendBounds.height;

			availableLegendPixels = legendWidth - 1;

			legendBorder = legendBounds;
		}

		// pixelValue contains the value for ONE pixel
		final float pixelValue = (float) legendDiffValue / availableLegendPixels;

		// draw border around the colors
		final Color borderColor = display.getSystemColor(SWT.COLOR_GRAY);
		gc.setForeground(borderColor);
		gc.drawRectangle(legendBorder);

		final Color legendTextColor = display.getSystemColor(SWT.COLOR_BLACK);
		final Color legendTextBackgroundColor = display.getSystemColor(SWT.COLOR_WHITE);

		int legendValue = 0;

		int unitLabelIndex = 0;

		final Color textBorderColor = new Color(display, 0xF1, 0xEE, 0xE8);

		for (int pixelIndex = 0; pixelIndex <= availableLegendPixels; pixelIndex++) {

			legendValue = (int) (legendMinValue + pixelValue * pixelIndex);

			int valuePosition;
			if (isVertical) {
				valuePosition = legendPositionY + availableLegendPixels - pixelIndex;
			} else {
				valuePosition = legendPositionX + availableLegendPixels - pixelIndex;
			}

			/*
			 * draw legend unit
			 */

			if (isVertical) {

				// find a unit which corresponds to the current legend value

				for (final Integer unitValue : legendUnits) {
					if (legendValue >= unitValue) {

						/*
						 * get unit label
						 */
						String valueText;
						if (unitLabels == null) {
							// set default unit label
							final int unit = unitValue / unitFactor;
							valueText = Integer.toString(unit) + UI.SPACE + unitText;
						} else {
							// when unitLabels are available, they will overwrite the default labeling
							valueText = unitLabels.get(unitLabelIndex++);
						}
						final org.eclipse.swt.graphics.Point valueTextExtent = gc.textExtent(valueText);

						gc.setForeground(legendTextColor);
						gc.setBackground(legendTextBackgroundColor);

						gc.drawLine(legendWidth, //
								valuePosition, //
								legendWidth + 5,
								valuePosition);

						// draw unit value and text
//						if (unitLabels == null) {
//							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
//							gc.fillRectangle(legendWidth + 5,
//									valuePosition - valueTextExtent.y / 2,
//									valueTextExtent.x,
//									valueTextExtent.y);
//						}

						final int devXText = legendWidth + 5;
						final int devYText = valuePosition - valueTextExtent.y / 2;

						gc.setForeground(textBorderColor);
//						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
						gc.drawText(valueText, devXText - 1, devYText, true);
						gc.drawText(valueText, devXText + 1, devYText, true);
						gc.drawText(valueText, devXText, devYText - 1, true);
						gc.drawText(valueText, devXText, devYText + 1, true);

						gc.setForeground(legendTextColor);
						gc.drawText(valueText, devXText, devYText, true);

						// prevent to draw this unit again
						legendUnits.remove(unitValue);

						break;
					}
				}
			}

			/*
			 * draw legend color line
			 */

			final int lineColorValue = legendProvider.getColorValue(legendValue);

			final Color lineColor = _colorCache.get(lineColorValue);

			gc.setForeground(lineColor);

			if (isVertical) {

				// vertial legend

				gc.drawLine(legendPositionX, valuePosition, legendWidth, valuePosition);

			} else {

				// horizontal legend

				gc.drawLine(valuePosition, legendPositionY, valuePosition, legendHeight);
			}

		}

		_colorCache.dispose();
		textBorderColor.dispose();
	}

	/**
	 * @param legendConfig
	 * @param legendColor
	 * @param legendValue
	 * @param device
	 * @return Returns a {@link Color} which corresponst to the legend value
	 */
	static int getLegendColor(final LegendConfig legendConfig, final LegendColor legendColor, final int legendValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		final ValueColor[] valueColors = legendColor.valueColors;
		final float minBrightnessFactor = legendColor.minBrightnessFactor / (float) 100;
		final float maxBrightnessFactor = legendColor.maxBrightnessFactor / (float) 100;

		/*
		 * find the valueColor for the current value
		 */
		ValueColor valueColor;
		ValueColor minValueColor = null;
		ValueColor maxValueColor = null;

		for (final ValueColor valueColor2 : valueColors) {

			valueColor = valueColor2;
			if (legendValue > valueColor.value) {
				minValueColor = valueColor;
			}
			if (legendValue <= valueColor.value) {
				maxValueColor = valueColor;
			}

			if (minValueColor != null && maxValueColor != null) {
				break;
			}
		}

		if (minValueColor == null) {

			// legend value is smaller than minimum value

			valueColor = valueColors[0];
			red = valueColor.red;
			green = valueColor.green;
			blue = valueColor.blue;

			final int minValue = valueColor.value;
			final int minDiff = legendConfig.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (legendValue - minValue) / (float) minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (legendColor.minBrightness == LegendColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (legendColor.minBrightness == LegendColor.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else if (maxValueColor == null) {

			// legend value is larger than maximum value

			valueColor = valueColors[valueColors.length - 1];
			red = valueColor.red;
			green = valueColor.green;
			blue = valueColor.blue;

			final int maxValue = valueColor.value;
			final int maxDiff = legendConfig.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (legendValue - maxValue) / (float) maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (legendColor.maxBrightness == LegendColor.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (legendColor.maxBrightness == LegendColor.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else {

			// legend value is in the min/max range

			final int maxValue = maxValueColor.value;
			final int minValue = minValueColor.value;
			final int minRed = minValueColor.red;
			final int minGreen = minValueColor.green;
			final int minBlue = minValueColor.blue;

			final int redDiff = maxValueColor.red - minRed;
			final int greenDiff = maxValueColor.green - minGreen;
			final int blueDiff = maxValueColor.blue - minBlue;

			final int ratioDiff = maxValue - minValue;
			final float ratio = ratioDiff == 0 ? 1 : (legendValue - minValue) / (float) (ratioDiff);

			red = (int) (minRed + redDiff * ratio);
			green = (int) (minGreen + greenDiff * ratio);
			blue = (int) (minBlue + blueDiff * ratio);
		}

		// adjust color values to 0...255, this is optimized
		final int maxRed = (0 >= red) ? 0 : red;
		final int maxGreen = (0 >= green) ? 0 : green;
		final int maxBlue = (0 >= blue) ? 0 : blue;
		red = (255 <= maxRed) ? 255 : maxRed;
		green = (255 <= maxGreen) ? 255 : maxGreen;
		blue = (255 <= maxBlue) ? 255 : maxBlue;

		final int colorValue = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);

		return colorValue;
	}

	private static void getTourPainterSettings() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final String drawSymbol = prefStore.getString(ITourbookPreferences.MAP_LAYOUT_SYMBOL);
		_prefIsDrawLine = drawSymbol.equals(PrefPageAppearanceMap.MAP_TOUR_SYMBOL_LINE);
		_prefIsDrawSquare = drawSymbol.equals(PrefPageAppearanceMap.MAP_TOUR_SYMBOL_SQUARE);

		_prefLineWidth = prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
		_prefWithBorder = prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER);
		_prefBorderWidth = prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH);
	}

	private void createImages() {

		_tourStartMarker = TourbookPlugin.getImageDescriptor(Messages.Image_Map_TourStartMarker).createImage();
		_tourEndMarker = TourbookPlugin.getImageDescriptor(Messages.Image_Map_TourEndMarker).createImage();

		_twpImage = TourbookPlugin.getImageDescriptor(Messages.Image_Map_WayPoint).createImage();
		_twpImageBounds = _twpImage.getBounds();

		_isImageAvailable = true;
	}

	@Override
	protected void dispose() {

		disposeImage(_tourStartMarker);
		disposeImage(_tourEndMarker);

		_isImageAvailable = false;
	}

	private void disposeImage(final Image image) {
		if (image != null && !image.isDisposed()) {
			image.dispose();
		}
	}

	@Override
	protected void disposeTempResources() {
//		_colorCache.dispose();
	}

	@Override
	protected boolean doPaint(final GC gc, final Map map, final Tile tile, final int parts) {

		final PaintManager paintManager = PaintManager.getInstance();

		final ArrayList<TourData> tourDataList = paintManager.getTourData();
		if (tourDataList == null) {
			return false;
		}

		boolean isTourInTile = false;

		if (_isImageAvailable == false) {
			createImages();
		}

		// draw tour first, then the marker
		for (final TourData tourData : tourDataList) {

			if (tourData == null) {
				continue;
			}

			// check if position is available
			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;
			if (latitudeSerie == null || longitudeSerie == null) {
				continue;
			}

			setDataSerie(tourData);

			final boolean isDrawTourInTile = drawTour10InTile(gc, map, tile, tourData, parts);

			isTourInTile = isTourInTile || isDrawTourInTile;

			// status if a marker is drawn
			boolean isMarkerInTile = false;

			// draw start/end marker
			if (paintManager.isShowStartEndInMap()) {

				// draw end marker first
				isMarkerInTile = drawStaticMarker(
						gc,
						map,
						tile,
						latitudeSerie[latitudeSerie.length - 1],
						longitudeSerie[longitudeSerie.length - 1],
						_tourEndMarker,
						parts);

				isTourInTile = isTourInTile || isMarkerInTile;

				// draw start marker above the end marker
				isMarkerInTile = drawStaticMarker(//
						gc,
						map,
						tile,
						latitudeSerie[0],
						longitudeSerie[0],
						_tourStartMarker,
						parts);
			}

			isTourInTile = isTourInTile || isMarkerInTile;
		}

		if (paintManager._isShowTourMarker || paintManager._isShowWayPoints) {

			// draw marker above the tour

			for (final TourData tourData : tourDataList) {

				if (tourData == null) {
					continue;
				}

				// check if geo position is available
				final double[] latitudeSerie = tourData.latitudeSerie;
				final double[] longitudeSerie = tourData.longitudeSerie;
				if (latitudeSerie == null || longitudeSerie == null) {
					continue;
				}

				setDataSerie(tourData);

				if (paintManager._isShowTourMarker) {

					// ckeck if markers are available
					final ArrayList<TourMarker> sortedMarkers = tourData.getTourMarkersSorted();
					if (sortedMarkers.size() > 0) {

						// draw tour marker

						boolean isTourMarkerInTile = false;
						boolean isTourMarkerInTile2 = false;

						for (final TourMarker tourMarker : sortedMarkers) {

							if (tourMarker.getLabel().length() == 0) {
								// skip empty marker
								continue;
							}

							final int serieIndex = tourMarker.getSerieIndex();

							// draw tour marker
							isTourMarkerInTile2 = drawTourMarker(
									gc,
									map,
									tile,
									latitudeSerie[serieIndex],
									longitudeSerie[serieIndex],
									tourMarker,
									parts);

							isTourMarkerInTile = isTourMarkerInTile || isTourMarkerInTile2;
						}

						isTourInTile = isTourInTile || isTourMarkerInTile;
					}
				}

				if (paintManager._isShowWayPoints) {

					// ckeck if way points are available
					final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();
					if (wayPoints.size() > 0) {

						/*
						 * world positions are cached to optimize performance
						 */
						final MP mp = map.getMapProvider();
						final String projectionId = mp.getProjection().getId();
						final int mapZoomLevel = map.getZoom();

						HashMap<Integer, Point> allWayPointWorldPixel = tourData.getWorldPositionForWayPoints(
								projectionId,
								mapZoomLevel);

						if ((allWayPointWorldPixel == null)) {
							allWayPointWorldPixel = initWorldPixelWayPoint(
									tourData,
									wayPoints,
									mp,
									projectionId,
									mapZoomLevel);
						}

						// draw tour way points
						boolean isTourWayPointInTile = false;
						boolean isTourWayPointInTile2 = false;

						for (final TourWayPoint tourWayPoint : wayPoints) {

							final Point twpWorldPixel = allWayPointWorldPixel.get(tourWayPoint.hashCode());

							isTourWayPointInTile2 = drawTourWayPoint(gc, map, tile, tourWayPoint, twpWorldPixel, parts);

							isTourWayPointInTile = isTourWayPointInTile || isTourWayPointInTile2;
						}

						isTourInTile = isTourInTile || isTourWayPointInTile;
					}
				}
			}
		}

		return isTourInTile;
	}

	private boolean drawStaticMarker(	final GC gc,
										final Map map,
										final Tile tile,
										final double latitude,
										final double longitude,
										final Image markerImage,
										final int parts) {

		if (markerImage == null) {
			return false;
		}

		final MP mp = map.getMapProvider();
		final int zoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		// get world viewport for the current tile
		final int worldPixelTileX = tile.getX() * tileSize;
		final int worldPixelTileY = tile.getY() * tileSize;

		// convert lat/long into world pixels
		final Point worldPixelMarker = mp.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldPixelMarker.x - worldPixelTileX;
		final int devMarkerPosY = worldPixelMarker.y - worldPixelTileY;

		final boolean isMarkerInTile = isBoundsInTile(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);
		if (isMarkerInTile) {

			// get marker size
			final Rectangle bounds = markerImage.getBounds();
			final int markerWidth = bounds.width;
			final int markerWidth2 = markerWidth / 2;
			final int markerHeight = bounds.height;

			gc.drawImage(markerImage, //
					devMarkerPosX - markerWidth2 + devPartOffset,
					devMarkerPosY - markerHeight + devPartOffset);
		}

		return isMarkerInTile;
	}

	private boolean drawTour10InTile(	final GC gc,
										final Map map,
										final Tile tile,
										final TourData tourData,
										final int parts) {

		boolean isTourInTile = false;

		final MP mp = map.getMapProvider();
		final int mapZoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		// get viewport for the current tile
		final int tileWorldPixelX = tile.getX() * tileSize;
		final int tileWorldPixelY = tile.getY() * tileSize;
		final int tileWidth = tileSize;
		final int tileHeight = tileSize;

		int devFromWithOffsetX = 0;
		int devFromWithOffsetY = 0;

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * world positions are cached to optimize performance when multiple tours are selected
		 */
		final String projectionId = mp.getProjection().getId();
		Point tourWorldPixelPosAll[] = tourData.getWorldPositionForTour(projectionId, mapZoomLevel);

		if ((tourWorldPixelPosAll == null)) {

			tourWorldPixelPosAll = initWorldPixelTour(
					tourData,
					mp,
					mapZoomLevel,
					latitudeSerie,
					longitudeSerie,
					projectionId);
		}

		final Color systemColorBlue = gc.getDevice().getSystemColor(SWT.COLOR_BLUE);

		gc.setForeground(systemColorBlue);
		gc.setBackground(systemColorBlue);

		int lastInsideIndex = -99;
		boolean isBorder;

		// index == 0: paint border
		// index == 1: paint tour symbol
		for (int lineIndex = 0; lineIndex < 2; lineIndex++) {

			if (lineIndex == 0) {

				if (_prefWithBorder == false) {
					// skip border
					continue;
				}

				isBorder = true;

				// draw line border
				_lineWidth = _prefLineWidth + (_prefBorderWidth * 2);

			} else if (lineIndex == 1) {

				isBorder = false;

				// draw within the border
				_lineWidth = _prefLineWidth;

			} else {
				break;
			}

			_lineWidth2 = _lineWidth / 2;

			gc.setLineWidth(_lineWidth);

			for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

				final Point tourWorldPixel = tourWorldPixelPosAll[serieIndex];
				final int tourWorldPixelX = tourWorldPixel.x;
				final int tourWorldPixelY = tourWorldPixel.y;

				int devX = tourWorldPixelX - tileWorldPixelX;
				int devY = tourWorldPixelY - tileWorldPixelY;

				if (_prefIsDrawLine) {

					// check if position is in the viewport

					// get positions with the part offset
					final int devToWithOffsetX = devX + devPartOffset;
					final int devToWithOffsetY = devY + devPartOffset;

					if (serieIndex == 0) {

						// keep position
						devFromWithOffsetX = devToWithOffsetX;
						devFromWithOffsetY = devToWithOffsetY;

						continue;
					}

					Color color = null;

					// this condition is an inline for:
					// tileViewport.contains(tileWorldPos.x, tileWorldPos.y)

					if ((tourWorldPixelX >= tileWorldPixelX)
							&& (tourWorldPixelY >= tileWorldPixelY)
							&& tourWorldPixelX < (tileWorldPixelX + tileWidth)
							&& tourWorldPixelY < (tileWorldPixelY + tileHeight)) {

						// current position is inside the tile

						// check if position has changed
						if (devToWithOffsetX != devFromWithOffsetX || devToWithOffsetY != devFromWithOffsetY) {

							isTourInTile = true;

							color = getTourColor(isBorder, serieIndex);

							drawTour20Line(gc, //
									devFromWithOffsetX,
									devFromWithOffsetY,
									devToWithOffsetX,
									devToWithOffsetY,
									color);
						}

						lastInsideIndex = serieIndex;
					}

					// current position is outside the tile

					if (serieIndex == lastInsideIndex + 1) {

						/*
						 * this position is the first which is outside of the tile, draw a line from
						 * the
						 * last inside to the first outside position
						 */

						drawTour20Line(gc, //
								devFromWithOffsetX,
								devFromWithOffsetY,
								devToWithOffsetX,
								devToWithOffsetY,
								color);
					}

					// keep position
					devFromWithOffsetX = devToWithOffsetX;
					devFromWithOffsetY = devToWithOffsetY;

				} else {

					// draw tour with dots/squares

					// this is an inline for: tileViewport.contains(tileWorldPos.x, tileWorldPos.y)
					// check if position is in the viewport
					if ((tourWorldPixelX >= tileWorldPixelX)
							&& (tourWorldPixelY >= tileWorldPixelY)
							&& tourWorldPixelX < (tileWorldPixelX + tileWidth)
							&& tourWorldPixelY < (tileWorldPixelY + tileHeight)) {

						// current position is inside the tile

						// optimize drawing: check if position has changed
						if (devX != devFromWithOffsetX && devY != devFromWithOffsetY) {

							isTourInTile = true;

							// adjust positions with the part offset
							devX += devPartOffset;
							devY += devPartOffset;

							final Color color = getTourColor(isBorder, serieIndex);

							if (_prefIsDrawSquare) {
								drawTour30Square(gc, devX, devY, color);
							} else {
								drawTour40Dot(gc, devX, devY, color);
							}

							// set previous pixel
							devFromWithOffsetX = devX;
							devFromWithOffsetY = devY;
						}
					}
				}
			}
		}

		_colorCache.dispose();

		return isTourInTile;
	}

	private void drawTour20Line(final GC gc,
								final int devXFrom,
								final int devYFrom,
								final int devXTo,
								final int devYTo,
								final Color color) {

		if (color != null) {
			gc.setForeground(color);
		}

		drawTour40Dot(gc, devXTo, devYTo, color);

		// draw line with the color from the legend provider
		gc.drawLine(devXFrom, devYFrom, devXTo, devYTo);

	}

	private void drawTour30Square(final GC gc, final int devX, final int devY, final Color color) {

		if (color != null) {
			gc.setBackground(color);
		}

		gc.fillRectangle(devX - _lineWidth2, devY - _lineWidth2, _lineWidth, _lineWidth);
	}

	private void drawTour40Dot(final GC gc, final int devX, final int devY, final Color color) {

		if (color != null) {
			gc.setBackground(color);
		}

		if (_lineWidth == 2) {
			// oval is not filled by a width of 2
			gc.fillRectangle(devX, devY, _lineWidth, _lineWidth);
		} else {
			gc.fillOval(devX - _lineWidth2, devY - _lineWidth2, _lineWidth, _lineWidth);
		}
	}

	private boolean drawTourMarker(	final GC gc,
									final Map map,
									final Tile tile,
									final double latitude,
									final double longitude,
									final TourMarker tourMarker,
									final int parts) {

		final MP mp = map.getMapProvider();
		final int zoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		// get world viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;

		// convert lat/long into world pixels
		final Point worldMarkerPos = mp.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldMarkerPos.x - worldTileX;
		final int devMarkerPosY = worldMarkerPos.y - worldTileY;

		Rectangle markerBounds = tourMarker.getMarkerBounds();
		if (markerBounds == null) {

			/*
			 * create and cache marker bounds
			 */

			final org.eclipse.swt.graphics.Point labelExtent = gc.textExtent(tourMarker.getLabel());

			final int bannerWidth = labelExtent.x + 2 * MARKER_MARGIN + 1;
			final int bannerHeight = labelExtent.y + 2 * MARKER_MARGIN;

			final int markerImageWidth = bannerWidth;
			final int markerImageHeight = bannerHeight + MARKER_POLE;

			markerBounds = new Rectangle(bannerWidth, bannerHeight, markerImageWidth, markerImageHeight);

			tourMarker.setMarkerBounds(markerBounds);
		}

		final boolean isMarkerInTile = isBoundsInTile(markerBounds, devMarkerPosX, devMarkerPosY, tileSize);
		if (isMarkerInTile) {

			int devX;
			int devY;

			final Image tourMarkerImage = drawTourMarkerImage(gc.getDevice(), tourMarker.getLabel(), markerBounds);
			{
				devX = devMarkerPosX - markerBounds.width / 2;
				devY = devMarkerPosY - markerBounds.height;

				devX += devPartOffset;
				devY += devPartOffset;

				gc.drawImage(tourMarkerImage, devX, devY);
			}
			tourMarkerImage.dispose();

			tile.addMarkerBounds(devX, devY, markerBounds.x, markerBounds.y, zoomLevel, parts);
		}

		return isMarkerInTile;
	}

	/**
	 * create an image for the tour marker
	 * 
	 * @param device
	 * @param markerBounds
	 * @param tourMarker
	 * @return
	 */
	private Image drawTourMarkerImage(final Device device, final String markerLabel, final Rectangle markerBounds) {

		final int bannerWidth = markerBounds.x;
		final int bannerHeight = markerBounds.y;
		final int bannerWidth2 = bannerWidth / 2;

		final int markerImageWidth = markerBounds.width;
		final int markerImageHeight = markerBounds.height;

		final int arcSize = 5;

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData markerImageData = new ImageData(markerImageWidth, markerImageHeight, 24, new PaletteData(
				0xff,
				0xff00,
				0xff0000));

		markerImageData.transparentPixel = markerImageData.palette.getPixel(rgbTransparent);

		final Image markerImage = new Image(device, markerImageData);
		final Rectangle markerImageBounds = markerImage.getBounds();

		final Color transparentColor = new Color(device, rgbTransparent);
		final Color bannerColor = new Color(device, 0x65, 0xF9, 0x1F);
		final Color bannerBorderColor = new Color(device, 0x69, 0xAF, 0x3D);

		final GC gc = new GC(markerImage);

		{
			// fill transparent color
			gc.setBackground(transparentColor);
			gc.fillRectangle(markerImageBounds);

			gc.setBackground(bannerColor);
			gc.fillRoundRectangle(0, 0, bannerWidth, bannerHeight, arcSize, arcSize);

			// draw banner border
			gc.setForeground(bannerBorderColor);
			gc.drawRoundRectangle(0, 0, bannerWidth - 1, bannerHeight - 1, arcSize, arcSize);

			// draw text
			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			gc.drawText(markerLabel, //
					MARKER_MARGIN + 1,
					MARKER_MARGIN,
					true);

			// draw pole
			gc.setForeground(bannerBorderColor);
			gc.drawLine(bannerWidth2 - 1, bannerHeight, bannerWidth2 - 1, bannerHeight + MARKER_POLE);
			gc.drawLine(bannerWidth2 + 1, bannerHeight, bannerWidth2 + 1, bannerHeight + MARKER_POLE);

			gc.setForeground(bannerColor);
			gc.drawLine(bannerWidth2 - 0, bannerHeight, bannerWidth2 - 0, bannerHeight + MARKER_POLE);

			// draw image debug border
//			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//			gc.drawRectangle(0, 0, markerImageBounds.width - 1, markerImageBounds.height - 1);
		}

		gc.dispose();

		bannerColor.dispose();
		bannerBorderColor.dispose();
		transparentColor.dispose();

		return markerImage;
	}

	private boolean drawTourWayPoint(	final GC gc,
										final Map map,
										final Tile tile,
										final TourWayPoint twp,
										final Point twpWorldPixel,
										final int parts) {

		final MP mp = map.getMapProvider();
		final int zoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		// get world viewport for the current tile
		final int tileWorldPixelX = tile.getX() * tileSize;
		final int tilwWorldPixelY = tile.getY() * tileSize;

		// convert world position into device position
		final int devWayPointX = twpWorldPixel.x - tileWorldPixelX;
		final int devWayPointY = twpWorldPixel.y - tilwWorldPixelY;

		final boolean isBoundsInTile = isBoundsInTile(_twpImageBounds, devWayPointX, devWayPointY, tileSize);

		if (isBoundsInTile) {

			int devX = devWayPointX - _twpImageBounds.width / 2;
			int devY = devWayPointY - _twpImageBounds.height;

			devX += devPartOffset;
			devY += devPartOffset;

			gc.drawImage(_twpImage, devX, devY);

//			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//			gc.setLineWidth(1);
//			gc.drawRectangle(devX, devY, _twpImageBounds.width, _twpImageBounds.height);
//
			tile.addTourWayPointBounds(//
					twp,
					new Rectangle(
							devX - devPartOffset,
							devY - devPartOffset,
							_twpImageBounds.width,
							_twpImageBounds.height),
					zoomLevel,
					parts);

			/*
			 * check if the way point paints into a neighbour tile
			 */
			if (parts > 1) {

			}
		}

		return isBoundsInTile;
	}

	/**
	 * @param legendBounds
	 * @param valueIndex
	 * @return Returns the position for the value according to the value index in the legend,
	 *         {@link Integer#MIN_VALUE} when data are not initialized
	 */
	public int getLegendValuePosition(final Rectangle legendBounds, final int valueIndex) {

		if (_dataSerie == null || valueIndex >= _dataSerie.length) {
			return Integer.MIN_VALUE;
		}

		/*
		 * ONLY VERTICAL LEGENDS ARE SUPPORTED
		 */

		final int dataValue = _dataSerie[valueIndex];

		int valuePosition = 0;

		final LegendConfig config = _legendProvider.getLegendConfig();

//		final Integer unitFactor = config.unitFactor;
//		dataValue /= unitFactor;

		final int legendMaxValue = config.legendMaxValue;
		final int legendMinValue = config.legendMinValue;
		final int legendDiffValue = legendMaxValue - legendMinValue;

		if (dataValue >= legendMaxValue) {

			// value >= max

		} else if (dataValue <= legendMinValue) {

			// value <= min

		} else {

			// min < value < max

//			int legendPositionX = legendBounds.x + 1;
			final int legendPositionY = legendBounds.y + TourMapView.LEGEND_MARGIN_TOP_BOTTOM;
//			int legendWidth = 20;
			final int legendHeight = legendBounds.height - 2 * TourMapView.LEGEND_MARGIN_TOP_BOTTOM;

			final int pixelDiff = legendHeight - 1;

			// pixelValue contains the value for ONE pixel
//			final float pixelValue = (float) legendDiffValue / availableLegendPixels;

//			valuePosition = legendPositionY + availableLegendPixels - pixelIndex;

			final int dataValue0 = dataValue - legendMinValue;
			final float ratio = pixelDiff / (float) legendDiffValue;

			valuePosition = legendPositionY + (int) (dataValue0 * ratio);
		}

		return valuePosition;
	}

	private Color getTourColor(final boolean isBorder, final int serieIndex) {

		if (_dataSerie == null) {
			return null;
		}

		int colorValue = _legendProvider.getColorValue(_dataSerie[serieIndex]);
		if (isBorder) {

			// paint the border in a darker color

			final float brightness = 0.8f;

			final int red = (int) (((colorValue & 0xFF) >>> 0) * brightness);
			final int green = (int) (((colorValue & 0xFF00) >>> 8) * brightness);
			final int blue = (int) (((colorValue & 0xFF0000) >>> 16) * brightness);

			colorValue = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);
		}

		return _colorCache.get(colorValue);
	}

	/**
	 * world pixels are not yet cached, create them now
	 * 
	 * @param tourData
	 * @param mp
	 * @param mapZoomLevel
	 * @param latitudeSerie
	 * @param longitudeSerie
	 * @param projectionId
	 * @return
	 */
	private Point[] initWorldPixelTour(	final TourData tourData,
										final MP mp,
										final int mapZoomLevel,
										final double[] latitudeSerie,
										final double[] longitudeSerie,
										final String projectionId) {

		final Point[] tourWorldPixelPosAll = new Point[latitudeSerie.length];

		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			// convert lat/long into world pixels which depends on the map projection

			tourWorldPixelPosAll[serieIndex] = mp.geoToPixel(//
					new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
					mapZoomLevel);
		}

		tourData.setWorldPixelForTour(tourWorldPixelPosAll, mapZoomLevel, projectionId);
		return tourWorldPixelPosAll;
	}

	private HashMap<Integer, Point> initWorldPixelWayPoint(	final TourData tourData,
															final Set<TourWayPoint> wayPoints,
															final MP mp,
															final String projectionId,
															final int mapZoomLevel) {
		// world pixels are not yet cached, create them now

		final HashMap<Integer, Point> allWayPointWorldPixel = new HashMap<Integer, Point>();

		for (final TourWayPoint twp : wayPoints) {

			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition geoPosition = new GeoPosition(twp.getLatitude(), twp.getLongitude());

			allWayPointWorldPixel.put(twp.hashCode(), mp.geoToPixel(geoPosition, mapZoomLevel));
		}

		tourData.setWorldPixelForWayPoints(allWayPointWorldPixel, mapZoomLevel, projectionId);

		return allWayPointWorldPixel;
	}

	/**
	 * Checks if an image bounds is within the tile. The image is above the image position and
	 * one half to the left and right side
	 * 
	 * @param imageBounds
	 *            bounds of the image
	 * @param devImagePosX
	 *            x position for the image
	 * @param devImagePosY
	 *            y position for the image
	 * @param tileSize
	 *            width and height of the tile
	 * @return Returns <code>true</code> when the image is visible in the tile
	 */
	private boolean isBoundsInTile(	final Rectangle imageBounds,
									final int devImagePosX,
									final int devImagePosY,
									final int tileSize) {

		// get image size
		final int imageWidth = imageBounds.width;
		final int imageWidth2 = imageWidth / 2;
		final int imageHeight = imageBounds.height;

		final int devImagePosLeft = devImagePosX - imageWidth2;
		final int devImagePosRight = devImagePosX + imageWidth2;

		// image position top is in the opposite direction
		final int devImagePosTop = devImagePosY - imageHeight;

		if (((devImagePosLeft >= 0 && devImagePosLeft <= tileSize) || (devImagePosRight >= 0 && devImagePosRight <= tileSize))
				&& (devImagePosY >= 0 && devImagePosY <= tileSize || devImagePosTop >= 0 && devImagePosTop <= tileSize)) {
			return true;
		}

		return false;
	}

	@Override
	protected boolean isPaintingNeeded(final Map map, final Tile tile) {

		final PaintManager paintManager = PaintManager.getInstance();

		final ArrayList<TourData> tourDataList = paintManager.getTourData();
		if (tourDataList == null) {
			return false;
		}

		if (_isImageAvailable == false) {
			createImages();
		}

		final MP mp = map.getMapProvider();
		final int mapZoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();
		final String projectionId = mp.getProjection().getId();

		// get viewport for the current tile
		final int tileWorldPixelLeft = tile.getX() * tileSize;
		final int tileWorldPixelRight = tileWorldPixelLeft + tileSize;

		final int tileWorldPixelTop = tile.getY() * tileSize;
		final int tileWorldPixelBottom = tileWorldPixelTop + tileSize;

		/*
		 * check tours
		 */
		for (final TourData tourData : tourDataList) {

			// check tour data
			if (tourData == null) {
				continue;
			}

			// check if position is available
			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;
			if (latitudeSerie != null && longitudeSerie != null) {

				/*
				 * world positions are cached to optimize performance when multiple tours are
				 * selected
				 */
				Point tourWorldPixelPosAll[] = tourData.getWorldPositionForTour(projectionId, mapZoomLevel);
				if ((tourWorldPixelPosAll == null)) {

					// world pixels are not yet cached, create them now

					tourWorldPixelPosAll = initWorldPixelTour(
							tourData,
							mp,
							mapZoomLevel,
							latitudeSerie,
							longitudeSerie,
							projectionId);
				}

				for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

					final Point tourWorldPixel = tourWorldPixelPosAll[serieIndex];

					// this is an inline for: tileViewport.contains(tileWorldPos.x, tileWorldPos.y)
					final int tourWorldPixelX = tourWorldPixel.x;
					final int tourWorldPixelY = tourWorldPixel.y;

					// check if position is within the tile viewport
					if ((tourWorldPixelX >= tileWorldPixelLeft)
							&& (tourWorldPixelY >= tileWorldPixelTop)
							&& tourWorldPixelX < tileWorldPixelRight
							&& tourWorldPixelY < tileWorldPixelBottom) {

						// current position is inside the tile

						return true;
					}
				}
			}

			/*
			 * check way points
			 */
			final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();
			if (wayPoints.size() > 0) {

				HashMap<Integer, Point> allWayPointWorldPixel = tourData.getWorldPositionForWayPoints(
						projectionId,
						mapZoomLevel);

				if ((allWayPointWorldPixel == null)) {
					allWayPointWorldPixel = initWorldPixelWayPoint(tourData, wayPoints, mp, projectionId, mapZoomLevel);
				}

				// get image size
				final int imageWidth = _twpImageBounds.width;
				final int imageWidth2 = imageWidth / 2;
				final int imageHeight = _twpImageBounds.height;

				for (final TourWayPoint twp : wayPoints) {

					final Point twpWorldPixel = allWayPointWorldPixel.get(twp.hashCode());

					// this is an inline for: tileViewport.contains(tileWorldPos.x, tileWorldPos.y)
					final int twpWorldPixelX = twpWorldPixel.x;
					final int twpWorldPixelY = twpWorldPixel.y;

					final int twpImageWorldPixelX = twpWorldPixelX - imageWidth2;

					// check if twp image is within the tile viewport
					if (twpImageWorldPixelX + imageWidth >= tileWorldPixelLeft
							&& twpWorldPixelX < tileWorldPixelRight
							&& twpWorldPixelY >= tileWorldPixelTop
							&& twpWorldPixelY < tileWorldPixelBottom + imageHeight) {

						// current position is inside the tile

						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Sets data serie which is painted
	 * 
	 * @param tourData
	 */
	private void setDataSerie(final TourData tourData) {

		final ILegendProvider legendProvider = PaintManager.getInstance().getLegendProvider();
		if (legendProvider == null) {
			_dataSerie = null;
			return;
		}

		_legendProvider = legendProvider;

		switch (_legendProvider.getTourColorId()) {
		case TourMapView.TOUR_COLOR_ALTITUDE:

			final int[] altitudeSerie = tourData.getAltitudeSerie();
			if (altitudeSerie == null) {
				_dataSerie = null;
			} else {
				_dataSerie = altitudeSerie;
			}
			break;

		case TourMapView.TOUR_COLOR_GRADIENT:

			final int[] gradientSerie = tourData.getGradientSerie();
			if (gradientSerie == null) {
				_dataSerie = null;
			} else {
				_dataSerie = gradientSerie;
			}
			break;

		case TourMapView.TOUR_COLOR_PULSE:

			final int[] pulseSerie = tourData.pulseSerie;
			if (pulseSerie == null) {
				_dataSerie = null;
			} else {
				_dataSerie = pulseSerie;
			}
			break;

		case TourMapView.TOUR_COLOR_SPEED:

			final int[] speedSerie = tourData.getSpeedSerie();
			if (speedSerie == null) {
				_dataSerie = null;
			} else {
				_dataSerie = speedSerie;
			}
			break;

		case TourMapView.TOUR_COLOR_PACE:

			final int[] paceSerie = tourData.getPaceSerie();
			if (paceSerie == null) {
				_dataSerie = null;
			} else {
				_dataSerie = paceSerie;
			}
			break;

		default:
			break;
		}
	}

}
