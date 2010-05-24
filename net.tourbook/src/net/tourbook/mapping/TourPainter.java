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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceMap;
import net.tourbook.ui.ColorCacheInt;

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
 * Paints the tour into the map
 */
public class TourPainter extends MapPainter {

	private static final int				MARGIN				= 2;
	private static final int				MARKER_POLE			= 16;

	private static final String				SPACER				= " ";												//$NON-NLS-1$
	private static final String				IMAGE_START_MARKER	= "map-marker-start.png";							//$NON-NLS-1$
	private static final String				IMAGE_END_MARKER	= "map-marker-end.png";							//$NON-NLS-1$
	private static final String				IMAGE_TOUR_MARKER	= "map-marker-tour.png";							//$NON-NLS-1$

	private static TourPainter				_instance;
	private static boolean					_isInitialized		= false;

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

	/*
	 * resources
	 */
	private static Image					_imageStartMarker;
	private static Image					_imageEndMarker;
	private static Image					_imageTourMarker;
	private static Image					_positionImage;
	private static Image					_markerImage;

	private final static ColorCacheInt		_colorCache			= new ColorCacheInt();

	static {

		/**
		 * this code optimizes the performance the reading from the pref store which is not very
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

		if (_isInitialized == false) {

			/*
			 * I've not yet understood to manage this problem because TourPainter() is created from
			 * an extension point but setting the instance in the constructor is not valid according
			 * to FindBugs
			 */
			_isInitialized = true;

			final Display display = Display.getCurrent();

			final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);
			final Color systemColorRed = display.getSystemColor(SWT.COLOR_RED);

			_positionImage = createPositionImage(systemColorBlue);
			_markerImage = createPositionImage(systemColorRed);

			_imageStartMarker = TourbookPlugin.getImageDescriptor(IMAGE_START_MARKER).createImage();
			_imageEndMarker = TourbookPlugin.getImageDescriptor(IMAGE_END_MARKER).createImage();
			_imageTourMarker = TourbookPlugin.getImageDescriptor(IMAGE_TOUR_MARKER).createImage();

		}
	}

	private static Image createPositionImage(final Color positionColor) {

		final Display display = Display.getCurrent();

		final int width = 8;
		final int height = 8;

		final Image positionImage = new Image(display, width, height);
		final Color colorTransparent = new Color(display, 0xff, 0xff, 0xfe);

		final GC gc = new GC(positionImage);
		{
			gc.setBackground(colorTransparent);
			gc.fillRectangle(0, 0, width, height);

			gc.setBackground(positionColor);
			gc.fillOval(1, 1, width - 2, height - 2);
		}

		/*
		 * set transparency
		 */
		final ImageData imageData = positionImage.getImageData();
		imageData.transparentPixel = imageData.getPixel(0, 0);
		final Image transparentImage = new Image(display, imageData);

		gc.dispose();
		positionImage.dispose();
		colorTransparent.dispose();

		return transparentImage;
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
							valueText = Integer.toString(unit) + SPACER + unitText;
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

	public static TourPainter getInstance() {

		if (_instance == null) {
			_instance = new TourPainter();
		}

		return _instance;
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

	@Override
	protected void dispose() {

		disposeImage(_imageTourMarker);
		disposeImage(_imageStartMarker);
		disposeImage(_imageEndMarker);
		disposeImage(_positionImage);
		disposeImage(_markerImage);

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

				// draw end marker
				isMarkerInTile = drawMarker(
						gc,
						map,
						tile,
						latitudeSerie[latitudeSerie.length - 1],
						longitudeSerie[longitudeSerie.length - 1],
						_imageEndMarker);

				isTourInTile = isTourInTile || isMarkerInTile;

				// draw start marker
				isMarkerInTile = drawMarker(gc,//
						map,
						tile,
						latitudeSerie[0],
						longitudeSerie[0],
						_imageStartMarker);
			}

			isTourInTile = isTourInTile || isMarkerInTile;
		}

		if (paintManager.isShowTourMarker()) {

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

				boolean isTourMarkerInTile = false;

				// draw tour marker

				boolean isTourMarkerInTile2 = false;

				// sort markers by time
				final Set<TourMarker> tourMarkers = tourData.getTourMarkers();

				final ArrayList<TourMarker> sortedMarkers = new ArrayList<TourMarker>(tourMarkers);
				Collections.sort(sortedMarkers, new Comparator<TourMarker>() {
					public int compare(final TourMarker marker1, final TourMarker marker2) {
						return marker1.getTime() - marker2.getTime();
					}
				});

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
							_imageTourMarker,
							tourMarker,
							parts);

					isTourMarkerInTile = isTourMarkerInTile || isTourMarkerInTile2;
				}

				isTourInTile = isTourInTile || isTourMarkerInTile;
			}
		}

		return isTourInTile;
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

		final MP mp = map.getMapProvider();
		final int zoomLevel = map.getZoom();
		final int tileSize = mp.getTileSize();

		// get world viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;

		// convert lat/long into world pixels
		final java.awt.Point worldMarkerPos = mp.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

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
		final Rectangle tileViewport = new Rectangle(tileWorldPixelX, tileWorldPixelY, tileSize, tileSize);

		int devFromWithOffsetX = 0;
		int devFromWithOffsetY = 0;

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * world positions are cached to optimize performance when multiple tours are selected
		 */
		final String projectionId = mp.getProjection().getId();
		Point tourWorldPixelPosAll[] = tourData.getWorldPosition(projectionId, mapZoomLevel);

		if ((tourWorldPixelPosAll == null)) {

			// world pixels are not yet cached, create them now

			tourWorldPixelPosAll = new Point[latitudeSerie.length];

			for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

				// convert lat/long into world pixels which depends on the map projection

				tourWorldPixelPosAll[serieIndex] = mp.geoToPixel(//
						new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
						mapZoomLevel);
			}

			tourData.setWorldPosition(projectionId, tourWorldPixelPosAll, mapZoomLevel);
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

				int devX = tourWorldPixel.x - tileWorldPixelX;
				int devY = tourWorldPixel.y - tileWorldPixelY;

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

					/*
					 * this condition is an inline for: tileViewport.contains(tileWorldPos.x,
					 * tileWorldPos.y)
					 */
					final int x = tourWorldPixel.x;
					final int y = tourWorldPixel.y;

					if ((x >= tileViewport.x)
							&& (y >= tileViewport.y)
							&& x < (tileViewport.x + tileViewport.width)
							&& y < (tileViewport.y + tileViewport.height)) {

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
					final int x = tourWorldPixel.x;
					final int y = tourWorldPixel.y;

					// check if position is in the viewport
					if ((x >= tileViewport.x)
							&& (y >= tileViewport.y)
							&& x < (tileViewport.x + tileViewport.width)
							&& y < (tileViewport.y + tileViewport.height)) {

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
									final Image markerImage,
									final TourMarker tourMarker,
									final int parts) {

		if (markerImage == null) {
			return false;
		}

		final MP tileFactory = map.getMapProvider();
		final int zoomLevel = map.getZoom();
		final int tileSize = tileFactory.getTileSize();
		final int devPartOffset = ((parts - 1) / 2) * tileSize;

		// get world viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;

		// convert lat/long into world pixels
		final Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldMarkerPos.x - worldTileX;
		final int devMarkerPosY = worldMarkerPos.y - worldTileY;
		int devX;
		int devY;

		final boolean isMarkerInTile = isMarkerInTile(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);

		if (isMarkerInTile) {

			final String markerLabel = tourMarker.getLabel();
			final org.eclipse.swt.graphics.Point labelExtent = gc.textExtent(markerLabel);

			final Image tourMarkerImage = drawTourMarkerImage(markerLabel, labelExtent, gc.getDevice());
			{
				final Rectangle markerBounds = tourMarkerImage.getBounds();

				devX = devMarkerPosX - markerBounds.width / 2;
				devY = devMarkerPosY - markerBounds.height;

				devX += devPartOffset;
				devY += devPartOffset;

				gc.drawImage(tourMarkerImage, devX, devY);
			}
			tourMarkerImage.dispose();

		}

		return isMarkerInTile;
	}

	/**
	 * create an image for the tour marker
	 * 
	 * @param labelExtent
	 * @param markerLabel
	 * @param device
	 * @return
	 */
	private Image drawTourMarkerImage(	final String markerLabel,
										final org.eclipse.swt.graphics.Point labelExtent,
										final Device device) {

		final int bannerWidth = labelExtent.x + 2 * MARGIN;
		final int bannerHeight = labelExtent.y + 2 * MARGIN;
		final int bannerWidth2 = bannerWidth / 2;

		final int markerWidth = bannerWidth;
		final int markerHeight = bannerHeight + MARKER_POLE;
		final int arcSize = 5;

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(//
				markerWidth,
				markerHeight,
				24,
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Image markerImage = new Image(device, overlayImageData);
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
					MARGIN,
					MARGIN,
					true);

			// draw pole
			gc.setForeground(bannerBorderColor);
			gc.drawLine(bannerWidth2, bannerHeight, bannerWidth2, bannerHeight + MARKER_POLE);
			gc.drawLine(bannerWidth2 + 1, bannerHeight, bannerWidth2 + 1, bannerHeight + MARKER_POLE);

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
	 * Checks if the marker position is within the tile. The marker is above the marker position and
	 * one half to the left and right side
	 * 
	 * @param markerBounds
	 *            marker bounds
	 * @param devMarkerPosX
	 *            x position for the marker
	 * @param devMarkerPosY
	 *            y position for the marker
	 * @param tileSize
	 *            width and height of the tile
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

	@Override
	protected boolean isPaintingNeeded(final Map map, final Tile tile) {

		final PaintManager paintManager = PaintManager.getInstance();

		final ArrayList<TourData> tourDataList = paintManager.getTourData();
		if (tourDataList == null) {
			return false;
		}

		for (final TourData tourData : tourDataList) {

			// check tour data
			if (tourData == null) {
				continue;
			}

			// check if position is available
			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;
			if (latitudeSerie == null || longitudeSerie == null) {
				continue;
			}

			final MP mp = map.getMapProvider();
			final int mapZoomLevel = map.getZoom();
			final int tileSize = mp.getTileSize();
			final String projectionId = mp.getProjection().getId();

			// get viewport for the current tile
			final int tileWorldPixelX = tile.getX() * tileSize;
			final int tileWorldPixelY = tile.getY() * tileSize;
			final Rectangle tileViewport = new Rectangle(tileWorldPixelX, tileWorldPixelY, tileSize, tileSize);

			/*
			 * world positions are cached to optimize performance when multiple tours are selected
			 */
			Point tourWorldPixelPosAll[] = tourData.getWorldPosition(projectionId, mapZoomLevel);
			if ((tourWorldPixelPosAll == null)) {

				// world pixels are not yet cached, create them now

				tourWorldPixelPosAll = new Point[latitudeSerie.length];

				for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

					// convert lat/long into world pixels which depends on the map projection

					tourWorldPixelPosAll[serieIndex] = mp.geoToPixel(//
							new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
							mapZoomLevel);
				}

				tourData.setWorldPosition(projectionId, tourWorldPixelPosAll, mapZoomLevel);
			}

			for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

				final Point tourWorldPixel = tourWorldPixelPosAll[serieIndex];

				// this is an inline for: tileViewport.contains(tileWorldPos.x, tileWorldPos.y)
				final int x = tourWorldPixel.x;
				final int y = tourWorldPixel.y;

				// check if position is in the viewport
				if ((x >= tileViewport.x)
						&& (y >= tileViewport.y)
						&& x < (tileViewport.x + tileViewport.width)
						&& y < (tileViewport.y + tileViewport.height)) {

					// current position is inside the tile

					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Sets the data serie which is painted
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
