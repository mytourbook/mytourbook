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
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;

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

	private static final String	SPACER				= " ";
	private static final String	IMAGE_START_MARKER	= "map-marker-start.png";	//$NON-NLS-1$
	private static final String	IMAGE_END_MARKER	= "map-marker-end.png";	//$NON-NLS-1$

	private static TourPainter	fInstance;

	private static LegendColor	fAltitudeLegendColor;
	private static LegendColor	fGradientLegendColor;
	private static LegendColor	fPulseLegendColor;

	private LegendConfig		fGradientLegendConfig;

	private final Image			fImageStartMarker;
	private final Image			fImageEndMarker;
	private final Image			fPositionImage;
	private final Image			fMarkerImage;

	private int					fTourColorId;
	private int[]				fDataSerie;

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

		// altitude legend color, min/max values will be set when a new tour is displayed
		fAltitudeLegendColor = new LegendColor();
		fAltitudeLegendColor.maxColor1 = 255;
		fAltitudeLegendColor.maxColor2 = 220;

		// pulse legend color
		fPulseLegendColor = new LegendColor();

		fPulseLegendColor.maxColor2 = 220;

		fPulseLegendColor.minValue = 50;
		fPulseLegendColor.lowValue = 70;
		fPulseLegendColor.midValue = 125;
		fPulseLegendColor.highValue = 150;
		fPulseLegendColor.maxValue = 200;

	}

	/**
	 * Draw legend colors into the legend bounds
	 * 
	 * @param gc
	 * @param legendBounds
	 * @param colorId
	 */
	public static void drawLegendColors(final GC gc,
										final Rectangle legendBounds,
										final LegendConfig config,
										final int colorId) {

		// get configuration for the legend 
		final ArrayList<Integer> legendUnits = new ArrayList<Integer>(config.units);
		final Integer unitFactor = config.unitFactor;
		final int legendMaxValue = config.legendMaxValue;
		final int legendMinValue = config.legendMinValue;
		final String unitText = config.unitText;

		final int legendWidth = 20;
		final int legendHeight = legendBounds.height - MappingView.LEGEND_MARGIN;
		final int legendPositionX = legendBounds.x + 1;
		final int legendPositionY = legendBounds.y + 10;

		// draw border around the colors
		final Color borderColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
		gc.setForeground(borderColor);
		gc.drawRectangle(legendPositionX - 1, legendPositionY - 1, legendWidth + 1, legendHeight + 2);

		final Color legendTextColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		final Color legendTextBackgroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
		Color lineColor = null;

		int legendValue = 0;

		final int legendDiffValue = legendMaxValue - legendMinValue;

		// pixelValue contains the value for ONE pixel
		final float pixelValue = (float) legendDiffValue / legendHeight;

		for (int heightIndex = 0; heightIndex <= legendHeight; heightIndex++) {

			legendValue = (int) (legendMinValue + pixelValue * heightIndex);

			final int legendValuePositionY = legendPositionY + legendHeight - heightIndex;

			/*
			 * draw legend unit
			 */

			// find a unit which corresponds to the current legend value
			for (final Integer unitValue : legendUnits) {
				if (legendValue >= unitValue) {

					final int unit = unitValue / unitFactor;
					final String valueText = Integer.toString(unit) + SPACER + unitText;
					final org.eclipse.swt.graphics.Point valueTextExtent = gc.textExtent(valueText);

					gc.setForeground(legendTextColor);
					gc.setBackground(legendTextBackgroundColor);

					gc.drawLine(legendWidth, // 
							legendValuePositionY, //
							legendWidth + 5,
							legendValuePositionY);

					// draw unit value and text
//					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					gc.fillRectangle(legendWidth + 5,
							legendValuePositionY - valueTextExtent.y / 2,
							valueTextExtent.x,
							valueTextExtent.y);

					gc.drawText(valueText, //
							legendWidth + 5, //
							legendValuePositionY - valueTextExtent.y / 2, //
							true);

					// prevent to draw this unit again
					legendUnits.remove(unitValue);

					break;
				}
			}

			/*
			 * draw legend color line
			 */
			switch (colorId) {
			case MappingView.TOUR_COLOR_ALTITUDE:
				lineColor = getLegendColor(fAltitudeLegendColor, legendValue);
				break;

			case MappingView.TOUR_COLOR_GRADIENT:
//				lineColor = getGradientColor(legendValue);
				lineColor = getLegendColor(fGradientLegendColor, legendValue);
				break;

			case MappingView.TOUR_COLOR_PULSE:
				lineColor = getLegendColor(fPulseLegendColor, legendValue);
				break;

			case MappingView.TOUR_COLOR_SPEED:
				lineColor = getSpeedColor(legendValue);
				break;

			case MappingView.TOUR_COLOR_PACE:
				lineColor = getPaceColor(legendValue);
				break;
			default:
				break;
			}

			if (lineColor != null) {
				gc.setForeground(lineColor);
			}

			gc.drawLine(legendPositionX, legendValuePositionY, legendWidth, legendValuePositionY);

			if (lineColor != null) {
				lineColor.dispose();
			}
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
	private static Color getGradientColor(final int value) {

		final int highValue = 100;
		final int highValue2 = 2 * highValue;

		final int maxRed = 255;
		final int maxGreen = 200;
		final int maxBlue = 255;

		final int absValue = Math.min(255, Math.abs((int) (value)));

		// red
		// -10% = 0  
		//   0% = 0
		//  10% = 255
		//  20% = 255
		//  25% = 128
		//
		int red = //
		value > highValue2 ? maxRed - (value - highValue2) : //
				value < 0 ? 0 : value > highValue ? maxRed : //
						Math.min(maxRed, absValue * maxRed / highValue)//
		;

		// green
		// -25% = 0
		// -20% = 0
		// -10% = 200
		//  10% = 200
		//  16% = 80 = 200 - 0.6*200 (120)
		//  20% = 0
		//  25% = 0

		final float green2 = maxGreen - ((absValue - highValue) / (float) highValue * maxGreen);
		int green = //
		value > -highValue && value < highValue ? maxGreen : //
				value < -2 * highValue || value > highValue2 ? 0 : //
						(int) green2;

		// blue
		// -20% = 0
		//   0% = 0
		//   6% = 0.6 * 255
		//  10% = 255
		//  20% = 255
		final float blue2 = absValue * maxBlue / highValue;
		int blue = //
		value < -highValue2 ? maxBlue - (-value - highValue2) : //
				value > 0 ? 0 : //
						value < -highValue ? maxBlue : //
								(int) blue2;

		red = Math.min(255, Math.abs(red));
		green = Math.min(255, Math.abs(green));
		blue = Math.min(255, Math.abs(blue));

		//		System.out.println(value + "\t" + absValue + "\t" + red + "\t" + green + "\t" + blue);

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	public static TourPainter getInstance() {
		return fInstance;
	}

	private static Color getLegendColor(LegendColor legendColor, final int value) {

		// 50, 75, 100, 125, 150, 175, 200

		final int maxColor1 = legendColor.maxColor1;
		final int maxColor2 = legendColor.maxColor2;
		final int maxColor3 = legendColor.maxColor3;

		final int minValue = legendColor.minValue;
		final int lowValue = legendColor.lowValue;
		final int midValue = legendColor.midValue;
		final int highValue = legendColor.highValue;
		final int maxValue = legendColor.maxValue;

		final float dimmFactor = legendColor.dimmFactor;

		// red
		// minValue	  50 bpm = 0
		// value     100 bpm = 170 = 50/75 * 255 
		// midValue	 125 bpm = 255
		// highValue 150 bpm = 255
		// value     170 bpm = 153 = 255 -(255 * 20/50)
		// maxValue	 200 bpm = 0

		final float color1_min = (value - minValue) / (float) (midValue - minValue);
		int color1 = //
		value > highValue
				? maxColor1 - (int) (dimmFactor * (maxColor1 * (value - highValue) / (maxValue - highValue)))
				: //
				value > midValue ? maxColor1 : //
						(int) (maxColor1 * color1_min);

//		int color1 = //
//		value < lowValue ? maxColor1 - (int) (dimmFactor * (maxColor1 * (lowValue - value) / (lowValue - minValue))) : //
//				value < midValue ? maxColor1 : //
//						value > highValue ? 0 : //
//								maxColor1 - (int) (maxColor1 * (value - midValue) / (highValue - midValue));

		// minValue  50  bpm = 0
		// value     70  bpm = ? = 255 - (255 * 10/30)
		// lowValue  80  bpm = 255
		// midValue 125  bpm = 255
		// value    180  bpm = 68 = 255 - (255 * 55/75)  
		// highValue 190 bpm = 0
		// maxValue 200  bpm = 0

		int color2 = //
		value < lowValue ? maxColor2 - (int) (dimmFactor * (maxColor2 * (lowValue - value) / (lowValue - minValue))) : //
				value < midValue ? maxColor2 : //
						value > highValue ? 0 : //
								maxColor2 - (int) (maxColor2 * (value - midValue) / (highValue - midValue));

		int color3 = //
		value < lowValue ? maxColor3 - (int) (dimmFactor * (maxColor3 * (lowValue - value) / (lowValue - minValue))) : //
				value < midValue ? maxColor3 : //
						value > highValue ? 0 : //
								maxColor3 - (int) (maxColor3 * (value - midValue) / (highValue - midValue));

//		System.out.println(value + "\t" + red + "\t" + green + "\t" + blue + "\t" + green2);

		color1 = Math.min(255, Math.abs(color1));
		color2 = Math.min(255, Math.abs(color2));
		color3 = Math.min(255, Math.abs(color3));

		final int red = //
		legendColor.color1 == LegendColor.COLOR_RED ? color1 : //
				legendColor.color1 == LegendColor.COLOR_GREEN ? color2 : //
						color3;

		final int green = //
		legendColor.color2 == LegendColor.COLOR_RED ? color1 : //
				legendColor.color2 == LegendColor.COLOR_GREEN ? color2 : //
						color3;

		final int blue = //
		legendColor.color3 == LegendColor.COLOR_BLUE ? color3 : //
				legendColor.color3 == LegendColor.COLOR_RED ? color1 : //
						color2;

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private static Color getPaceColor(final int paceValue) {

		final int red = 0;
		final int green = 0;
		final int blue = 0;

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
	}

	private static Color getSpeedColor(final int speedValue) {

		final int red = 0;
		final int green = 0;
		final int blue = 0;

		return new Color(Display.getCurrent(), new RGB(red, green, blue));
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

		if (fDataSerie == null) {

			gc.drawLine(devPreviousPosition.x, devPreviousPosition.y, devPosition.x, devPosition.y);

		} else {

			Color lineColor = null;
			switch (fTourColorId) {
			case MappingView.TOUR_COLOR_ALTITUDE:
				lineColor = getLegendColor(fAltitudeLegendColor, fDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_GRADIENT:
//				lineColor = getGradientColor(fDataSerie[serieIndex]);
				lineColor = getLegendColor(fGradientLegendColor, fDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_PULSE:
				lineColor = getLegendColor(fPulseLegendColor, fDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_SPEED:
				lineColor = getSpeedColor(fDataSerie[serieIndex]);
				break;

			case MappingView.TOUR_COLOR_PACE:
				lineColor = getPaceColor(fDataSerie[serieIndex]);
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

	LegendColor getAltitudeLegendColor() {
		return fAltitudeLegendColor;
	}

	LegendConfig getGradientLegendConfig() {

//		if (fGradientLegendConfig != null) {
//			return fGradientLegendConfig;
//		}

		fGradientLegendConfig = new LegendConfig();

		fGradientLegendConfig.units = Arrays.asList(-200, -100, 0, 100, 200);
		fGradientLegendConfig.legendMinValue = -250;
		fGradientLegendConfig.legendMaxValue = 250;
		fGradientLegendConfig.unitFactor = 10;
		fGradientLegendConfig.unitText = Messages.graph_label_gradiend_unit;

		/*
		 * gradient legend color
		 */
		fGradientLegendColor = new LegendColor();

		fGradientLegendColor.minValue = -300;
		fGradientLegendColor.lowValue = -100;
		fGradientLegendColor.midValue = 0;
		fGradientLegendColor.highValue = 100;
		fGradientLegendColor.maxValue = 300;

		fGradientLegendColor.dimmFactor = 0.5F;

		fGradientLegendColor.maxColor1 = 255;
		fGradientLegendColor.maxColor2 = 255;
		fGradientLegendColor.maxColor3 = 255;
		fGradientLegendColor.color1 = LegendColor.COLOR_RED;
		fGradientLegendColor.color2 = LegendColor.COLOR_GREEN;
		fGradientLegendColor.color3 = LegendColor.COLOR_BLUE;

		return fGradientLegendConfig;
	}

	LegendColor getPulseLegendColor() {
		return fPulseLegendColor;
	}

	private void inizializeTourColor(final TourData tourData) {

		fTourColorId = PaintManager.getInstance().getTourColorId();

		switch (fTourColorId) {
		case MappingView.TOUR_COLOR_ALTITUDE:

			final int[] altitudeSerie = tourData.getAltitudeSerie();
			if (altitudeSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_ALTITUDE;
				fDataSerie = null;
			} else {
				fDataSerie = altitudeSerie;
			}
			break;

		case MappingView.TOUR_COLOR_GRADIENT:

			final int[] gradientSerie = tourData.getGradientSerie();
			if (gradientSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_ALTITUDE;
				fDataSerie = null;
			} else {
				fDataSerie = gradientSerie;
			}
			break;

		case MappingView.TOUR_COLOR_PULSE:

			final int[] pulseSerie = tourData.pulseSerie;
			if (pulseSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_ALTITUDE;
				fDataSerie = null;
			} else {
				fDataSerie = pulseSerie;
			}
			break;

		case MappingView.TOUR_COLOR_SPEED:

			final int[] speedSerie = tourData.getSpeedSerie();
			if (speedSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_ALTITUDE;
				fDataSerie = null;
			} else {
				fDataSerie = speedSerie;
			}
			break;

		case MappingView.TOUR_COLOR_PACE:

			final int[] paceSerie = tourData.getPaceSerie();
			if (paceSerie == null) {
				fTourColorId = MappingView.TOUR_COLOR_ALTITUDE;
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
