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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMapAppearance;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
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

	private static int			LINE_WIDTH			= 7;
	private int					LINE_WIDTH2			= LINE_WIDTH / 2;

	private static final String	SPACER				= " ";						//$NON-NLS-1$
	private static final String	IMAGE_START_MARKER	= "map-marker-start.png";	//$NON-NLS-1$
	private static final String	IMAGE_END_MARKER	= "map-marker-end.png";	//$NON-NLS-1$
	private static final String	IMAGE_TOUR_MARKER	= "map-marker-tour.png";	//$NON-NLS-1$

	private static TourPainter	fInstance;

	private final Image			fImageStartMarker;
	private final Image			fImageEndMarker;
	private final Image			fImageTourMarker;
	private final Image			fPositionImage;
	private final Image			fMarkerImage;

	private int[]				fDataSerie;
	private ILegendProvider		fLegendProvider;

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
			legendPositionY = legendBounds.y + MapView.LEGEND_MARGIN_TOP_BOTTOM;
			legendWidth = 20;
			legendHeight = legendBounds.height - 2 * MapView.LEGEND_MARGIN_TOP_BOTTOM;

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
		final Color borderColor = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
//		final Color borderColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		gc.setForeground(borderColor);
		gc.drawRectangle(legendBorder);

		final Color legendTextColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		final Color legendTextBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);

		Color lineColor = null;
		int legendValue = 0;

		int unitLabelIndex = 0;

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
						final Point valueTextExtent = gc.textExtent(valueText);

						gc.setForeground(legendTextColor);
						gc.setBackground(legendTextBackgroundColor);

						gc.drawLine(legendWidth, // 
								valuePosition, //
								legendWidth + 5,
								valuePosition);

						// draw unit value and text
						if (unitLabels == null) {
//					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
							gc.fillRectangle(legendWidth + 5,
									valuePosition - valueTextExtent.y / 2,
									valueTextExtent.x,
									valueTextExtent.y);
						}

						gc.drawText(valueText, //
								legendWidth + 5, //
								valuePosition - valueTextExtent.y / 2, //
								true);

						// prevent to draw this unit again
						legendUnits.remove(unitValue);

						break;
					}
				}
			}

			/*
			 * draw legend color line
			 */

			lineColor = legendProvider.getValueColor(legendValue);

			if (lineColor != null) {
				gc.setForeground(lineColor);
			}

			if (isVertical) {

				// vertial legend

				gc.drawLine(legendPositionX, valuePosition, legendWidth, valuePosition);

			} else {

				// horizontal legend

				gc.drawLine(valuePosition, legendPositionY, valuePosition, legendHeight);
			}

			if (lineColor != null) {
				lineColor.dispose();
			}
		}
	}

	public static TourPainter getInstance() {

		if (fInstance == null) {
			fInstance = new TourPainter();
		}

		return fInstance;
	}

	/**
	 * @param legendConfig
	 * @param legendColor
	 * @param legendValue
	 * @return Returns a {@link Color} which corresponst to the legend value
	 */
	static Color getLegendColor(final LegendConfig legendConfig, final LegendColor legendColor, final int legendValue) {

		int red = 0;
		int green = 0;
		int blue = 0;

		final ValueColor[] valueColors = legendColor.valueColors;
		final float minBrightnessFactor = legendColor.minBrightnessFactor / (float) 100;
		final float maxBrightnessFactor = legendColor.maxBrightnessFactor / (float) 100;
		ValueColor valueColor;

		/*
		 * find the valueColor for the current value
		 */
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

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	public TourPainter() {

		super();

		fInstance = this;

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);
		final Color systemColorRed = display.getSystemColor(SWT.COLOR_RED);
		fPositionImage = createPositionImage(systemColorBlue);
		fMarkerImage = createPositionImage(systemColorRed);

		fImageStartMarker = TourbookPlugin.getImageDescriptor(IMAGE_START_MARKER).createImage();
		fImageEndMarker = TourbookPlugin.getImageDescriptor(IMAGE_END_MARKER).createImage();
		fImageTourMarker = TourbookPlugin.getImageDescriptor(IMAGE_TOUR_MARKER).createImage();
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

	/**
	 * create an image for the tour marker
	 * 
	 * @param labelExtent
	 * @param markerLabel
	 * @return
	 */
	private Image createTourMarkerImage(final String markerLabel, final Point labelExtent) {

		final int MARGIN = 5;
		final int MARKER_POLE = 26;

		final int bannerWidth = labelExtent.x + 2 * MARGIN;
		final int bannerHeight = labelExtent.y + 2 * MARGIN;
		final int bannerWidth2 = bannerWidth / 2;

		final int markerWidth = bannerWidth;
		final int markerHeight = bannerHeight + MARKER_POLE;
		final int arcSize = 5;

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(markerWidth, markerHeight, 24, //
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		final Image markerImage = new Image(display, overlayImageData);
		final Rectangle markerImageBounds = markerImage.getBounds();

		final Color transparentColor = new Color(display, rgbTransparent);
		final Color bannerColor = new Color(display, 0x65, 0xF9, 0x1F);
		final Color bannerBorderColor = new Color(display, 0x69, 0xAF, 0x3D);

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
//			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
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

	@Override
	protected void dispose() {

		disposeImage(fImageTourMarker);
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

		final ArrayList<TourData> tourDataList = paintManager.getTourData();
		if (tourDataList == null) {
			return false;
		}

		final boolean[] isInTile = new boolean[1];

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				boolean isTourInTile = false;

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

					getDataSerie(tourData);

					// draw tour into the tile
					final boolean isDrawTourInTile = drawTourInTile(gc, map, tile, tourData);

					isTourInTile = isTourInTile || isDrawTourInTile;

					// status if a marker is drawn
					boolean isMarkerInTile = false;

					// draw start/end marker
					if (paintManager.isShowStartEndInMap()) {

						// draw end marker
						isMarkerInTile = drawMarker(gc,
								map,
								tile,
								latitudeSerie[latitudeSerie.length - 1],
								longitudeSerie[longitudeSerie.length - 1],
								fImageEndMarker);

						isTourInTile = isTourInTile || isMarkerInTile;

						// draw start marker
						isMarkerInTile = drawMarker(gc,//
								map,
								tile,
								latitudeSerie[0],
								longitudeSerie[0],
								fImageStartMarker);
					}

					boolean isTourMarkerInTile = false;

					// draw tour marker
					if (paintManager.isShowTourMarker()) {

						boolean isTourMarkerInTile2 = false;

						for (final TourMarker tourMarker : tourData.getTourMarkers()) {

							final int serieIndex = tourMarker.getSerieIndex();

							// draw tour marker
							isTourMarkerInTile2 = drawTourMarker(gc,
									map,
									tile,
									latitudeSerie[serieIndex],
									longitudeSerie[serieIndex],
									fImageTourMarker,
									tourMarker);

							isTourMarkerInTile = isTourMarkerInTile || isTourMarkerInTile2;
						}

					}

					isTourInTile = isTourInTile || isMarkerInTile || isTourMarkerInTile;
				}

				isInTile[0] = isTourInTile;
			}
		});

		return isInTile[0];
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
		final java.awt.Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

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

	private void drawTourDot(final GC gc, final int serieIndex, final java.awt.Point devPosition) {

		if (fDataSerie == null) {

			// draw default dot when data are not available

//			gc.drawOval(devPosition.x, devPosition.y, LINE_WIDTH, LINE_WIDTH);
			gc.fillOval(devPosition.x, devPosition.y, LINE_WIDTH, LINE_WIDTH);

		} else {

			// draw dot with the color from the legend provider

			final Color lineColor = fLegendProvider.getValueColor(fDataSerie[serieIndex]);

			{
				if (LINE_WIDTH == 2) {
					// oval is not filled with width of 2
//					gc.setForeground(lineColor);
//					gc.drawOval(devPosition.x, devPosition.y, LINE_WIDTH, LINE_WIDTH);
					gc.setBackground(lineColor);
					gc.fillRectangle(devPosition.x, devPosition.y, LINE_WIDTH, LINE_WIDTH);
				} else {
					gc.setBackground(lineColor);
					gc.fillOval(devPosition.x, devPosition.y, LINE_WIDTH, LINE_WIDTH);
				}
			}

			lineColor.dispose();
		}
	}

	private boolean drawTourInTile(final GC gc, final Map map, final Tile tile, final TourData tourData) {

		boolean isTourInTile = false;

		final TileFactory tileFactory = map.getTileFactory();
		final int zoomLevel = map.getZoom();
		final int tileSize = tileFactory.getInfo().getTileSize();

		// get viewport for the current tile
		final int worldTileX = tile.getX() * tileSize;
		final int worldTileY = tile.getY() * tileSize;
		final Rectangle tileViewport = new Rectangle(worldTileX, worldTileY, tileSize, tileSize);

		java.awt.Point worldPosition;
		java.awt.Point devPosition;
		java.awt.Point devPreviousPosition = null;

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * world positions are cached to optimize performance when multiple tours are selected
		 */
		final String projectionId = tileFactory.getProjection().getId();
		java.awt.Point worldPositions[] = tourData.getWorldPosition(projectionId, zoomLevel);
		final boolean createWorldPosition = worldPositions == null;
		if (createWorldPosition) {

			worldPositions = new java.awt.Point[latitudeSerie.length];

			tourData.setWorldPosition(projectionId, worldPositions, zoomLevel);
		}

		final Display display = Display.getCurrent();
		final Color systemColorBlue = display.getSystemColor(SWT.COLOR_BLUE);

		gc.setForeground(systemColorBlue);
		gc.setBackground(systemColorBlue);

		int lastInsideIndex = -99;
		java.awt.Point lastInsidePosition = null;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final String drawSymbol = prefStore.getString(ITourbookPreferences.MAP_LAYOUT_SYMBOL);
		final boolean isDrawLine = drawSymbol.equals(PrefPageMapAppearance.MAP_TOUR_SYMBOL_LINE);
		final boolean isDrawSquare = drawSymbol.equals(PrefPageMapAppearance.MAP_TOUR_SYMBOL_SQUARE);

		LINE_WIDTH = prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
		LINE_WIDTH2 = LINE_WIDTH / 2;
		gc.setLineWidth(LINE_WIDTH);

		for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

			if (createWorldPosition) {

				// convert lat/long into world pixels which depends on the map projection

				worldPosition = tileFactory.geoToPixel(new GeoPosition(latitudeSerie[serieIndex],
						longitudeSerie[serieIndex]), zoomLevel);

				worldPositions[serieIndex] = worldPosition;

			} else {
				worldPosition = worldPositions[serieIndex];
			}

			if (isDrawLine) {

				// convert world position into device position
				devPosition = new java.awt.Point(worldPosition.x - worldTileX, worldPosition.y - worldTileY);

				if (tileViewport.contains(worldPosition.x, worldPosition.y)) {

					// current position is inside the tile

					// check if position is in the viewport or position has changed
					if (devPreviousPosition != null && devPosition.equals(devPreviousPosition) == false) {

						isTourInTile = true;

						drawTourLine(gc, serieIndex, devPosition, devPreviousPosition);

						// initialize previous pixel
//						if (devPreviousPosition == null) {
//							devPreviousPosition = devPosition;
//						}
					}

					lastInsideIndex = serieIndex;
					lastInsidePosition = devPosition;
				}

				// current position is outside the tile

				if (serieIndex == lastInsideIndex + 1 && lastInsidePosition != null) {

					/*
					 * this position is the first which is outside of the tile, draw a line from the
					 * last inside to the first outside position
					 */

					drawTourLine(gc, serieIndex, devPosition, lastInsidePosition);

					// initialize previous pixel
//					if (devPreviousPosition == null) {
//						devPreviousPosition = devPosition;
//					}
				}

				devPreviousPosition = devPosition;

			} else {

				// draw tour with dots/squares

				if (tileViewport.contains(worldPosition.x, worldPosition.y)) {

					// current position is inside the tile

					// convert world position into device position
					devPosition = new java.awt.Point(worldPosition.x - worldTileX, worldPosition.y - worldTileY);

					// check if position is in the viewport or position has changed
					if (devPosition.equals(devPreviousPosition) == false) {

						isTourInTile = true;

						if (isDrawSquare) {
							drawTourSquare(gc, serieIndex, devPosition);
						} else {
							drawTourDot(gc, serieIndex, devPosition);
						}

						// initialize previous pixel
						if (devPreviousPosition == null) {
							devPreviousPosition = devPosition;
						}
					}
				}
			}
		}

		return isTourInTile;
	}

	private void drawTourLine(	final GC gc,
								final int serieIndex,
								final java.awt.Point devPosition,
								final java.awt.Point devPreviousPosition) {

		if (fDataSerie == null) {

			// draw default line when data are not available

			gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);

		} else {

			// draw line with the color from the legend provider

			final Color lineColor = fLegendProvider.getValueColor(fDataSerie[serieIndex]);

			{
				gc.setForeground(lineColor);
				gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);
			}

			lineColor.dispose();
		}

	}

	private boolean drawTourMarker(	final GC gc,
									final Map map,
									final Tile tile,
									final double latitude,
									final double longitude,
									final Image markerImage,
									final TourMarker tourMarker) {

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
		final java.awt.Point worldMarkerPos = tileFactory.geoToPixel(new GeoPosition(latitude, longitude), zoomLevel);

		// convert world position into device position
		final int devMarkerPosX = worldMarkerPos.x - worldTileX;
		final int devMarkerPosY = worldMarkerPos.y - worldTileY;

		final boolean isMarkerInTile = isMarkerInTile(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);

		if (isMarkerInTile) {

			final String markerLabel = tourMarker.getLabel();
			final Point labelExtent = gc.textExtent(markerLabel);

			if (markerLabel.length() > 3) {

				// draw marker with more than 3 characters

				final Image tourMarkerImage = createTourMarkerImage(markerLabel, labelExtent);
				final Rectangle markerBounds = tourMarkerImage.getBounds();

				gc.drawImage(tourMarkerImage,//
						devMarkerPosX - markerBounds.width / 2,
						devMarkerPosY - markerBounds.height);

				tourMarkerImage.dispose();

			} else {

				// draw marker with 3 or less characters

				// get marker size
				final Rectangle bounds = markerImage.getBounds();
				final int markerWidth = bounds.width;

				final int markerWidth2 = markerWidth / 2;
				final int markerHeight = bounds.height;

				final int devPosX = devMarkerPosX - markerWidth2;
				final int devPosY = devMarkerPosY - markerHeight;

				gc.drawImage(markerImage, devPosX, devPosY);

				gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
				gc.drawText(markerLabel, //
						devPosX + markerWidth2 - (labelExtent.x / 2),
						devPosY + markerWidth2 - (labelExtent.y / 2),
						true);
			}
		}

		return isMarkerInTile;
	}

	private void drawTourSquare(final GC gc, final int serieIndex, final java.awt.Point devPosition) {

		if (fDataSerie == null) {

			// draw default square when data are not available

			gc.fillRectangle(devPosition.x - LINE_WIDTH2, devPosition.y - LINE_WIDTH2, LINE_WIDTH, LINE_WIDTH);

		} else {

			// draw square with the color from the legend provider

			final Color lineColor = fLegendProvider.getValueColor(fDataSerie[serieIndex]);

			{
				gc.setBackground(lineColor);
				gc.fillRectangle(devPosition.x - LINE_WIDTH2, devPosition.y - LINE_WIDTH2, LINE_WIDTH, LINE_WIDTH);
			}

			lineColor.dispose();
		}
	}

	private void getDataSerie(final TourData tourData) {

		fLegendProvider = PaintManager.getInstance().getLegendProvider();

		switch (fLegendProvider.getTourColorId()) {
		case MapView.TOUR_COLOR_ALTITUDE:

			final int[] altitudeSerie = tourData.getAltitudeSerie();
			if (altitudeSerie == null) {
				fDataSerie = null;
			} else {
				fDataSerie = altitudeSerie;
			}
			break;

		case MapView.TOUR_COLOR_GRADIENT:

			final int[] gradientSerie = tourData.getGradientSerie();
			if (gradientSerie == null) {
				fDataSerie = null;
			} else {
				fDataSerie = gradientSerie;
			}
			break;

		case MapView.TOUR_COLOR_PULSE:

			final int[] pulseSerie = tourData.pulseSerie;
			if (pulseSerie == null) {
				fDataSerie = null;
			} else {
				fDataSerie = pulseSerie;
			}
			break;

		case MapView.TOUR_COLOR_SPEED:

			final int[] speedSerie = tourData.getSpeedSerie();
			if (speedSerie == null) {
				fDataSerie = null;
			} else {
				fDataSerie = speedSerie;
			}
			break;

		case MapView.TOUR_COLOR_PACE:

			final int[] paceSerie = tourData.getPaceSerie();
			if (paceSerie == null) {
				fDataSerie = null;
			} else {
				fDataSerie = paceSerie;
			}
			break;

		default:
			break;
		}
	}

	/**
	 * @param legendBounds
	 * @param valueIndex
	 * @return Returns the position for the value according to the value index in the legend,
	 *         {@link Integer#MIN_VALUE} when data are not initialized
	 */
	public int getLegendValuePosition(final Rectangle legendBounds, final int valueIndex) {

		if (fDataSerie == null || valueIndex >= fDataSerie.length) {
			return Integer.MIN_VALUE;
		}

		/*
		 * ONLY VERTICAL LEGENDS ARE SUPPORTED
		 */

		final int dataValue = fDataSerie[valueIndex];

		int valuePosition = 0;

		final LegendConfig config = fLegendProvider.getLegendConfig();

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
			final int legendPositionY = legendBounds.y + MapView.LEGEND_MARGIN_TOP_BOTTOM;
//			int legendWidth = 20;
			final int legendHeight = legendBounds.height - 2 * MapView.LEGEND_MARGIN_TOP_BOTTOM;

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

}
