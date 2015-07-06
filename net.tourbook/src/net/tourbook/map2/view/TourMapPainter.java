/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Util;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapUnits;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.ImageConverter;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.map2.Messages;
import net.tourbook.map3.layer.TourLegendLabel;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.PhotoUI;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
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

/**
 * Paints a tour into the 2D map.
 */
public class TourMapPainter extends MapPainter {

	private static final int				MARKER_MARGIN		= 2;
	private static final int				MARKER_POLE			= 16;

	private static final Font				DEFAULT_FONT		= net.tourbook.common.UI.AWT_FONT_ARIAL_12;

	final static IPreferenceStore			_prefStore			= TourbookPlugin.getPrefStore();

	private static IPropertyChangeListener	_prefChangeListener;

	private float[]							_dataSerie;

	private IMapColorProvider				_legendProvider;

	// painting parameter
	private int								_lineWidth;
	private int								_lineWidth2;
	private static float					_borderBrightness;

	private static RGB						_prefBorderRGB;
	private static int						_prefBorderType;
	private static int						_prefBorderWidth;
	private static boolean					_prefIsDrawLine;
	private static boolean					_prefIsDrawSquare;
	private static boolean					_prefIsWithBorder;
	private static int						_prefLineWidth;

	private static boolean					_isImageAvailable	= false;
	private static boolean					_isErrorLogged;

	/**
	 * Tour start/end image
	 */
	private static Image					_tourStartMarker;

	private static Image					_tourEndMarker;

	private static Rectangle				_twpImageBounds;
	private static TourPainterConfiguration	_tourPaintConfig;

	private final static NumberFormat		_nf1				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/*
	 * UI resources
	 */
	private static Color					_bgColor;

	/**
	 * Tour Way Point image
	 */
	private static Image					_twpImage;

	private final static ColorCacheSWT		_colorCache			= new ColorCacheSWT();

	private class LoadCallbackImage implements ILoadCallBack {

		private Map		__map;
		private Tile	__tile;

		public LoadCallbackImage(final Map map, final Tile tile) {
			__map = map;
			__tile = tile;
		}

		@Override
		public void callBackImageIsLoaded(final boolean isUpdateUI) {

			if (isUpdateUI == false) {
				return;
			}

			__map.queueOverlayPainting(__tile);
//			__map.paint();
		}
	}

	public TourMapPainter() {

		super();

		/*
		 * I've not yet understood to manage this problem because TourPainter() is created from an
		 * extension point but setting the instance in the constructor is not valid according to
		 * FindBugs
		 */

		init();
	}

	/**
	 * Creates an legend image, this image must be disposed who created it.
	 * 
	 * @param display
	 * @param colorProvider
	 * @param imageWidth
	 * @param imageHeight
	 * @param isDrawVertical
	 * @param isDrawLegendText
	 * @return
	 */
	public static Image createMapLegendImage(	final Display display,
												final IGradientColorProvider colorProvider,
												final int imageWidth,
												final int imageHeight,
												final boolean isDrawVertical) {

		/*
		 * Use a color which is likely not used, the previous color 0xfefefe was used and had bad
		 * effects.
		 */
		final RGB rgbTransparent = new RGB(0xfa, 0xfb, 0xfc);

		final ImageData imageData = new ImageData(//
				imageWidth,
				imageHeight,
				24,
				new PaletteData(0xff, 0xff00, 0xff0000));

		imageData.transparentPixel = imageData.palette.getPixel(rgbTransparent);

		final Image image = new Image(display, imageData);
		final Rectangle legendImageBounds = image.getBounds();

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(image);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(legendImageBounds);

			drawMap2Legend(gc, legendImageBounds, colorProvider, isDrawVertical);
		}
		gc.dispose();
		transparentColor.dispose();

		return image;
	}

	public static Image createMapLegendImage(	final Map3GradientColorProvider colorProvider,
												final ColorProviderConfig config,
												final int imageWidth,
												final int imageHeight,
												final boolean isVertical,
												final boolean isDrawUnits,
												final boolean isDrawBorder) {

		final BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);

		final Graphics2D g2d = image.createGraphics();
		try {

//			g2d.setColor(java.awt.Color.ORANGE);
//			g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

			drawMapLegend_GradientColors_AWT(
					g2d,
					colorProvider,
					config,
					imageWidth,
					imageHeight,
					isVertical,
					isDrawUnits,
					isDrawBorder);

		} finally {
			g2d.dispose();
		}

		return ImageConverter.convertIntoSWT(image);
	}

	/**
	 * Draws map legend colors into the legend bounds.
	 * 
	 * @param gc
	 * @param legendImageBounds
	 * @param colorProvider
	 * @param isDrawVertical
	 * @param isDrawVertical
	 *            When <code>true</code> the legend is drawn vertically otherwise it's drawn
	 *            horizontally.
	 * @param isDrawLegendText
	 */
	public static void drawMap2Legend(	final GC gc,
										final Rectangle legendImageBounds,
										final IMapColorProvider colorProvider,
										final boolean isDrawVertical) {

		if (colorProvider instanceof IGradientColorProvider) {

			drawMapLegend_GradientColors_SWT(//
					gc,
					legendImageBounds,
					(IGradientColorProvider) colorProvider,
					isDrawVertical);
		}
	}

	public static void drawMapLegend(	final Graphics2D g2d,
										final IMapColorProvider colorProvider,
										final ColorProviderConfig config,
										final int legendWidth,
										final int legendHeight) {

		if (colorProvider instanceof IGradientColorProvider) {

			drawMapLegend_GradientColors_AWT(
					g2d,
					(IGradientColorProvider) colorProvider,
					config,
					legendWidth,
					legendHeight,
					true,
					false,
					true);
		}
	}

	private static void drawMapLegend_GradientColors_AWT(	final Graphics2D g2,
															final IGradientColorProvider colorProvider,
															final ColorProviderConfig config,
															final int legendWidth,
															final int legendHeight,
															final boolean isDrawVertical,
															final boolean isDrawUnits,
															final boolean isDrawBorder) {

		final MapUnits mapUnits = colorProvider.getMapUnits(config);

		// ensure units are available
		Assert.isNotNull(mapUnits.units);
//		if (mapUnits.units == null) {
//			return;
//		}

		/*
		 * Setup units
		 */
		final ArrayList<Float> legendUnits = new ArrayList<Float>(mapUnits.units);
		final float legendMinValue = mapUnits.legendMinValue;
		final float legendMaxValue = mapUnits.legendMaxValue;
		final float legendDiffValue = legendMaxValue - legendMinValue;

		final List<String> unitLabels = mapUnits.unitLabels;
		final int legendFormatDigits = mapUnits.numberFormatDigits;
		final LegendUnitFormat unitFormat = mapUnits.unitFormat;

		final String unitText = UI.SPACE + mapUnits.unitText;

		/*
		 * Setup font
		 */
		final Font font = DEFAULT_FONT;

		// Measure the font and the message
		final FontRenderContext frc = g2.getFontRenderContext();
//		final Rectangle2D bounds = font.getStringBounds(unitText, frc);
		final LineMetrics metrics = font.getLineMetrics(unitText, frc);

//		final float width = (float) bounds.getWidth(); // The width of our text
		final float lineheight = metrics.getHeight(); // Total line height
//		final float ascent = metrics.getAscent(); // Top of text to baseline

		// Now display the message centered horizontally and vertically in box
//		final float x0 = (float) (box.getX() + (box.getWidth() - width) / 2);
//		final float y0 = (float) (box.getY() + (box.getHeight() - lineheight) / 2 + ascent);
//		g2.setFont(font);
//		g2.drawString(unitText, x0, y0);

		/*
		 * Setup legend image
		 */
		final int borderSize = 1;
		final int borderSize2 = 2 * borderSize;
		Rectangle borderRect;

		int availableLegendPixels;
		int contentX;
		int contentY;
		int contentWidth;
		int contentHeight;

		if (isDrawVertical) {

			// vertical

			if (isDrawUnits) {

				contentX = borderSize;
				contentY = borderSize + IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

				contentWidth = IMapColorProvider.DEFAULT_LEGEND_GRAPHIC_WIDTH;
				contentHeight = legendHeight - borderSize2 - 1 - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

			} else {

				contentX = borderSize;
				contentY = borderSize;

				contentWidth = legendWidth - borderSize2;
				contentHeight = legendHeight - borderSize2 - 1;
			}

			borderRect = new Rectangle(
					contentX - borderSize,
					contentY - borderSize,
					contentWidth + borderSize2 - 1,
					contentHeight + borderSize2);

			availableLegendPixels = contentHeight;

		} else {

			// horizontal

			contentX = borderSize;
			contentY = borderSize;

			contentWidth = legendWidth - borderSize2;
			contentHeight = legendHeight - borderSize2 - 1;

			borderRect = new Rectangle(contentX, contentY, contentWidth, contentHeight);

			availableLegendPixels = contentWidth;
		}

		// draw border
		if (isDrawBorder) {
			g2.setColor(java.awt.Color.WHITE);
			g2.drawRect(borderRect.x, borderRect.y, borderRect.width, borderRect.height);
		}

		// pixelValue contains the value for ONE pixel
		final float pixelValue = legendDiffValue / availableLegendPixels;
		final float roundingValue = pixelValue / 100;

		final java.awt.Color textBorderColor = new java.awt.Color(0xf1, 0xee, 0xe8);

		int unitLabelIndex = 0;

		for (int pixelIndex = 0; pixelIndex <= availableLegendPixels; pixelIndex++) {

			final float legendValue = legendMinValue + pixelValue * pixelIndex;

			int devValue;

			if (isDrawVertical) {
				devValue = contentY + availableLegendPixels - pixelIndex;
			} else {
				devValue = contentX + pixelIndex;
			}

			if (isDrawUnits) {

				// Rounding value is necessary otherwise the uppermost unit is sometimes not drawn.
				final float legendUnitValue = legendValue + roundingValue;

				for (final Float unitValue : legendUnits) {

					if (legendUnitValue >= unitValue) {

						/*
						 * get unit label
						 */
						String valueText;
						if (unitLabels == null) {

							// set default unit label

							if (unitFormat == LegendUnitFormat.Pace) {

								valueText = Util.format_mm_ss(unitValue.longValue()) + unitText;

							} else {

								if (legendFormatDigits == 0) {
									valueText = Integer.toString(unitValue.intValue()) + UI.SPACE + unitText;
								} else {
									// currently only 1 digit is supported
									valueText = _nf1.format(unitValue) + UI.SPACE + unitText;
								}
							}

						} else {
							// when unitLabels are available, they will overwrite the default labeling
							valueText = unitLabels.get(unitLabelIndex++);
						}

						final int devXText = contentWidth + 7;
						final int devYText = (int) (devValue + lineheight / 2);

						g2.setColor(textBorderColor);
//						g2.drawString(valueText, devXText + 1, devYText - 1);
//						gc.drawText(valueText, devXText - 1, devYText, true);
//						gc.drawText(valueText, devXText + 1, devYText, true);
//						gc.drawText(valueText, devXText, devYText - 1, true);
//						gc.drawText(valueText, devXText, devYText + 1, true);

						g2.setColor(java.awt.Color.BLACK);
						g2.drawString(valueText, devXText, devYText);

						// prevent to draw this unit again
						legendUnits.remove(unitValue);

						break;
					}
				}

			}

			/*
			 * draw color line
			 */
			final int rgba = colorProvider.getRGBValue(config, legendValue);
			g2.setColor(new java.awt.Color(
					(rgba & 0xFF) >>> 0,
					(rgba & 0xFF00) >>> 8,
					(rgba & 0xFF0000) >>> 16,
					(rgba & 0xFF000000) >>> 24));

			if (isDrawVertical) {

				// vertial legend
				g2.drawLine(contentX, devValue, contentWidth, devValue);

			} else {

				// horizontal legend
				g2.drawLine(devValue, contentY, devValue, contentHeight);
			}

		}
	}

	private static void drawMapLegend_GradientColors_SWT(	final GC gc,
															final Rectangle imageBounds,
															final IGradientColorProvider colorProvider,
															final boolean isDrawVertical) {

		final MapUnits mapUnits = colorProvider.getMapUnits(ColorProviderConfig.MAP2);

		// ensure units are available
		if (mapUnits.units == null) {

			if (!_isErrorLogged) {
				_isErrorLogged = true;
				StatusUtil.log(new Throwable("Color provider is not configured: " + colorProvider));//$NON-NLS-1$
			}

			return;
		}

		// get configuration for the legend
		final ArrayList<Float> legendUnits = new ArrayList<Float>(mapUnits.units);
		final float legendMaxValue = mapUnits.legendMaxValue;
		final float legendMinValue = mapUnits.legendMinValue;
		final float legendDiffValue = legendMaxValue - legendMinValue;
		final List<String> unitLabels = mapUnits.unitLabels;
		final int legendFormatDigits = mapUnits.numberFormatDigits;
		final LegendUnitFormat unitFormat = mapUnits.unitFormat;

		final String unitText = UI.SPACE + mapUnits.unitText;

		final int borderSize = 0;
		Rectangle contentBorder;

		int contentX;
		int contentY;
		int contentWidth;
		int contentHeight;
		int availableLegendPixels;

		if (isDrawVertical) {

			// vertical legend

			final int marginTopBottom = IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

			contentX = borderSize;
			contentY = marginTopBottom + borderSize;

			contentWidth = IMapColorProvider.DEFAULT_LEGEND_GRAPHIC_WIDTH;
			contentHeight = imageBounds.height - 2 * marginTopBottom - 2 * borderSize;

			availableLegendPixels = contentHeight - 0;

			contentBorder = new Rectangle(//
					contentX - borderSize,
					contentY - borderSize,
					contentWidth + borderSize,
					contentHeight + 2 * borderSize + 0);

		} else {

			// horizontal legend

			contentX = imageBounds.x + borderSize;
			contentY = imageBounds.y + borderSize;

			contentWidth = imageBounds.width - 2 * borderSize;
			contentHeight = imageBounds.height - 2 * borderSize;

			availableLegendPixels = contentWidth - borderSize;

			contentBorder = new Rectangle(//
					contentX - borderSize,
					contentY - borderSize,
					contentWidth + 2 * borderSize - 1,
					contentHeight + 2 * borderSize - 1);
		}

		// pixelValue contains the value for ONE pixel
		final float pixelValue = legendDiffValue / availableLegendPixels;

		final float roundingValue = pixelValue / 100;

		final Device display = gc.getDevice();

		// draw border around the colors
//		final Color borderColor = display.getSystemColor(SWT.COLOR_GRAY);
		final Color borderColor = display.getSystemColor(SWT.COLOR_RED);
		gc.setForeground(borderColor);
		gc.drawRectangle(contentBorder);

		final Color textBorderColor = _colorCache.getColor(0xF1EEE8);
		final Color unitTextColor = display.getSystemColor(SWT.COLOR_BLACK);

		int unitLabelIndex = 0;

		for (int pixelIndex = 0; pixelIndex <= availableLegendPixels; pixelIndex++) {

			final float legendValue = legendMinValue + pixelValue * pixelIndex;

			int devValue;

			if (isDrawVertical) {
				devValue = contentY + availableLegendPixels - pixelIndex;
			} else {
				devValue = contentX + pixelIndex;
			}

			/*
			 * draw legend unit
			 */

			if (isDrawVertical) {

				// find a unit which corresponds to the current legend value

				// Rounding value is necessary otherwise the uppermost unit is sometimes not drawn.
				final float legendUnitValue = legendValue + roundingValue;

				for (final Float unitValue : legendUnits) {

					if (legendUnitValue >= unitValue) {

						/*
						 * get unit label
						 */
						String valueText;
						if (unitLabels == null) {

							// set default unit label

							if (unitFormat == LegendUnitFormat.Pace) {

								valueText = Util.format_mm_ss(unitValue.longValue()) + unitText;

							} else {

								if (legendFormatDigits == 0) {
									valueText = Integer.toString(unitValue.intValue()) + UI.SPACE + unitText;
								} else {
									// currently only 1 digit is supported
									valueText = _nf1.format(unitValue) + UI.SPACE + unitText;
								}
							}

						} else {
							// when unitLabels are available, they will overwrite the default labeling
							valueText = unitLabels.get(unitLabelIndex++);
						}
						final org.eclipse.swt.graphics.Point valueTextExtent = gc.textExtent(valueText);

						final int devXText = contentWidth + 7;
						final int devYText = devValue - valueTextExtent.y / 2;

						gc.setForeground(textBorderColor);
						gc.drawText(valueText, devXText - 1, devYText, true);
						gc.drawText(valueText, devXText + 1, devYText, true);
						gc.drawText(valueText, devXText, devYText - 1, true);
						gc.drawText(valueText, devXText, devYText + 1, true);

						gc.setForeground(unitTextColor);
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

			final long valueRGB = colorProvider.getRGBValue(ColorProviderConfig.MAP2, legendValue);
			final Color valueColor = _colorCache.getColor((int) valueRGB);

			gc.setForeground(valueColor);

			if (isDrawVertical) {

				// vertial legend

				gc.drawLine(contentX, devValue, contentWidth, devValue);

			} else {

				// horizontal legend

				gc.drawLine(devValue, contentY, devValue, contentHeight);
			}
		}

		_colorCache.dispose();
	}

	public static ArrayList<TourLegendLabel> getMapLegendLabels(final int legendWidth,
																final int legendHeight,
																final IGradientColorProvider colorProvider,
																final ColorProviderConfig config) {

		final ArrayList<TourLegendLabel> legendLabels = new ArrayList<TourLegendLabel>();

		final MapUnits mapUnits = colorProvider.getMapUnits(config);

		// ensure units are available
		if (mapUnits.units == null) {
			return legendLabels;
		}

		// get configuration for the legend
		final ArrayList<Float> allLegendUnits = new ArrayList<Float>(mapUnits.units);

		final String unitText = mapUnits.unitText;
		final List<String> unitLabels = mapUnits.unitLabels;
		final int legendFormatDigits = mapUnits.numberFormatDigits;
		final LegendUnitFormat unitFormat = mapUnits.unitFormat;

		// get configuration for the legend
		final float legendMaxValue = mapUnits.legendMaxValue;
		final float legendMinValue = mapUnits.legendMinValue;
		final float legendDiffValue = legendMaxValue - legendMinValue;

		final int legendPositionY = 1;

		final int availableLegendPixels = legendHeight - 3;

		// pixelValue contains the value for ONE pixel
		final float pixelValue = legendDiffValue / availableLegendPixels;

		float legendValue = 0;
		int unitLabelIndex = 0;

		for (int pixelIndex = 0; pixelIndex <= availableLegendPixels; pixelIndex++) {

			legendValue = legendMinValue + pixelValue * pixelIndex;

			final int valuePositionY = legendPositionY + availableLegendPixels - pixelIndex;

			// find a unit which corresponds to the current legend value
			for (final Float unitValue : allLegendUnits) {

				if (legendValue >= unitValue) {

					/*
					 * get unit label
					 */
					String valueText;
					if (unitLabels == null) {

						// set default unit label

						if (unitFormat == LegendUnitFormat.Pace) {

							valueText = Util.format_mm_ss(unitValue.longValue()) + UI.SPACE + unitText;

						} else {

							if (legendFormatDigits == 0) {
								valueText = Integer.toString(unitValue.intValue()) + UI.SPACE + unitText;
							} else {
								// currently only 1 digit is supported
								valueText = _nf1.format(unitValue) + UI.SPACE + unitText;
							}
						}

					} else {
						// when unitLabels are available, they will overwrite the default labeling
						valueText = unitLabels.get(unitLabelIndex++);
					}

					legendLabels.add(new TourLegendLabel(unitValue, valueText, valuePositionY));

					// prevent to draw this unit again
					allLegendUnits.remove(unitValue);

					break;
				}
			}
		}

		return legendLabels;
	}

	private static void getTourPainterSettings() {

		final String drawSymbol = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE);

		_prefIsDrawLine = drawSymbol.equals(PrefPageMap2Appearance.PLOT_TYPE_LINE);
		_prefIsDrawSquare = drawSymbol.equals(PrefPageMap2Appearance.PLOT_TYPE_SQUARE);

		_prefLineWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
		_prefIsWithBorder = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER);

		_prefBorderRGB = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR);
		_prefBorderType = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE);
		_prefBorderWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH);

		final int prefBorderDimmValue = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE);
		_borderBrightness = (float) (1.0 - prefBorderDimmValue / 100.0);
	}

	private static void init() {

		if (_bgColor != null) {
			return;
		}

		// ensure color registry is setup
		PhotoUI.init();

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		_bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		_tourPaintConfig = TourPainterConfiguration.getInstance();

		/**
		 * this code optimizes the performance by reading from the pref store which is not very
		 * efficient
		 */
		getTourPainterSettings();

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {
					getTourPainterSettings();
				}
			}
		};

		// add pref listener, dispose is not removing it because it is static !!!
		TourbookPlugin.getDefault().getPreferenceStore()//
				.addPropertyChangeListener(_prefChangeListener);
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

		Util.disposeResource(_tourEndMarker);
		Util.disposeResource(_tourStartMarker);

		_isImageAvailable = false;
	}

	@Override
	protected void disposeTempResources() {
//		_colorCache.dispose();
	}

	@Override
	protected boolean doPaint(final GC gcTile, final Map map, final Tile tile, final int parts) {

		init();

		final ArrayList<TourData> tourDataList = _tourPaintConfig.getTourData();
		final ArrayList<Photo> photoList = _tourPaintConfig.getPhotos();

		if (tourDataList.size() == 0 && photoList.size() == 0) {
			return false;
		}

		boolean isContentInTile = false;

		if (_isImageAvailable == false) {
			createImages();
		}

		// first draw the tour, then the marker and photos
		if (_tourPaintConfig.isTourVisible) {

			final Color systemColorBlue = gcTile.getDevice().getSystemColor(SWT.COLOR_BLUE);

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

				final boolean isDrawTourInTile = drawTour_10_InTile(gcTile, map, tile, tourData, parts, systemColorBlue);

				isContentInTile = isContentInTile || isDrawTourInTile;

//				/**
//				 * DEBUG Start
//				 */
//				gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
//				gcTile.fillRectangle(0, 0, 2, 50);
//				gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//				gcTile.fillRectangle(2, 0, 2, 50);
//				gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//				gcTile.fillRectangle(4, 0, 2, 50);
//				gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//				gcTile.fillRectangle(6, 0, 2, 50);
//				isContentInTile = true;
//				/**
//				 * DEBUG End
//				 */

				// status if a marker is drawn
				int staticMarkerCounter = 0;

				// draw start/end marker
				if (_tourPaintConfig.isShowStartEndInMap) {

					// draw end marker first
					if (drawStaticMarker(
							gcTile,
							map,
							tile,
							latitudeSerie[latitudeSerie.length - 1],
							longitudeSerie[longitudeSerie.length - 1],
							_tourEndMarker,
							parts)) {

						staticMarkerCounter++;
					}

					// draw start marker above the end marker
					if (drawStaticMarker(//
							gcTile,
							map,
							tile,
							latitudeSerie[0],
							longitudeSerie[0],
							_tourStartMarker,
							parts)) {

						staticMarkerCounter++;
					}
				}

				isContentInTile = isContentInTile || staticMarkerCounter > 0;
			}

			_colorCache.dispose();
		}

		if (_tourPaintConfig.isShowTourMarker || _tourPaintConfig.isShowWayPoints) {

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

				if (_tourPaintConfig.isShowTourMarker) {

					isContentInTile = doPaint_Marker(
							gcTile,
							map,
							tile,
							parts,
							isContentInTile,
							tourData,
							latitudeSerie,
							longitudeSerie);
				}

				if (_tourPaintConfig.isShowWayPoints) {

					// ckeck if way points are available
					final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();
					if (wayPoints.size() > 0) {

						/*
						 * world positions are cached to optimize performance
						 */
						final MP mp = map.getMapProvider();
						final String projectionId = mp.getProjection().getId();
						final int mapZoomLevel = map.getZoom();

						TIntObjectHashMap<Point> allWayPointWorldPixel = tourData.getWorldPositionForWayPoints(
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

						int wayPointCounter = 0;
						for (final TourWayPoint tourWayPoint : wayPoints) {

							final Point twpWorldPixel = allWayPointWorldPixel.get(tourWayPoint.hashCode());

							if (drawTourWayPoint(gcTile, map, tile, tourWayPoint, twpWorldPixel, parts)) {
								wayPointCounter++;
							}
						}

						isContentInTile = isContentInTile || wayPointCounter > 0;
					}
				}
			}
		}

		if (_tourPaintConfig.isPhotoVisible && photoList.size() > 0) {

			/*
			 * world positions are cached to optimize performance
			 */
			final MP mp = map.getMapProvider();
			final String projectionId = mp.getProjection().getId();
			final int mapZoomLevel = map.getZoom();

			int photoCounter = 0;

//			System.out.println(net.tourbook.common.UI.timeStampNano() + " Photo\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			for (final Photo photo : photoList) {

//				System.out.println(net.tourbook.common.UI.timeStampNano() + "\t" + photo);
//				// TODO remove SYSTEM.OUT.PRINTLN

				final Point photoWorldPixel = photo.getWorldPosition(
						mp,
						projectionId,
						mapZoomLevel,
						_tourPaintConfig.isLinkPhotoDisplayed);
				if (photoWorldPixel == null) {
					continue;
				}

				if (drawPhoto(gcTile, map, tile, photo, photoWorldPixel, parts)) {
					photoCounter++;
				}
			}

			isContentInTile = isContentInTile || photoCounter > 0;
		}

		return isContentInTile;
	}

	private boolean doPaint_Marker(	final GC gcTile,
									final Map map,
									final Tile tile,
									final int parts,
									boolean isContentInTile,
									final TourData tourData,
									final double[] latitudeSerie,
									final double[] longitudeSerie) {

		if (tourData.isMultipleTours) {

			final int[] multipleStartTimeIndex = tourData.multipleTourStartIndex;
			final int[] multipleNumberOfMarkers = tourData.multipleNumberOfMarkers;

			int tourIndex = 0;
			int numberOfMultiMarkers = 0;
			int tourSerieIndex = 0;

			// setup first multiple tour
			tourSerieIndex = multipleStartTimeIndex[tourIndex];
			numberOfMultiMarkers = multipleNumberOfMarkers[tourIndex];

			final ArrayList<TourMarker> allTourMarkers = tourData.multiTourMarkers;

			// draw tour marker

			int markerCounter = 0;

			for (int markerIndex = 0; markerIndex < allTourMarkers.size(); markerIndex++) {

				if (markerIndex >= numberOfMultiMarkers) {

					// setup next tour

					tourIndex++;

					if (tourIndex <= multipleStartTimeIndex.length - 1) {

						tourSerieIndex = multipleStartTimeIndex[tourIndex];
						numberOfMultiMarkers += multipleNumberOfMarkers[tourIndex];
					}
				}

				final TourMarker tourMarker = allTourMarkers.get(markerIndex);

				// skip marker when hidden or not set
				if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
					continue;
				}

				final int markerSerieIndex = tourSerieIndex + tourMarker.getSerieIndex();

				tourMarker.setMultiTourSerieIndex(markerSerieIndex);

				// draw tour marker
				if (drawTourMarker(
						gcTile,
						map,
						tile,
						latitudeSerie[markerSerieIndex],
						longitudeSerie[markerSerieIndex],
						tourMarker,
						parts)) {

					markerCounter++;
				}
			}

			isContentInTile = isContentInTile || markerCounter > 0;

		} else {

			final ArrayList<TourMarker> sortedMarkers = tourData.getTourMarkersSorted();

			// ckeck if markers are available
			if (sortedMarkers.size() > 0) {

				// draw tour marker

				int markerCounter = 0;

				for (final TourMarker tourMarker : sortedMarkers) {

					// skip marker when hidden or not set
					if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
						continue;
					}

					final int serieIndex = tourMarker.getSerieIndex();

					/*
					 * check bounds because when a tour is split, it can happen that the marker
					 * serie index is out of scope
					 */
					if (serieIndex >= latitudeSerie.length) {
						continue;
					}

					// draw tour marker
					if (drawTourMarker(
							gcTile,
							map,
							tile,
							latitudeSerie[serieIndex],
							longitudeSerie[serieIndex],
							tourMarker,
							parts)) {

						markerCounter++;
					}
				}

				isContentInTile = isContentInTile || markerCounter > 0;
			}
		}
		return isContentInTile;
	}

	private boolean drawPhoto(	final GC gcTile,
								final Map map,
								final Tile tile,
								final Photo photo,
								final Point photoWorldPixel,
								final int parts) {

		final MP mp = map.getMapProvider();
		final int tileSize = mp.getTileSize();

		// get world viewport for the current tile
		final int tileWorldPixelX = tile.getX() * tileSize;
		final int tilwWorldPixelY = tile.getY() * tileSize;

		// convert world position into device position
		final int devXPhoto = photoWorldPixel.x - tileWorldPixelX;
		final int devYPhoto = photoWorldPixel.y - tilwWorldPixelY;

		final org.eclipse.swt.graphics.Point photoSize = photo.getMapImageSize();

		final boolean isPhotoInTile = isPhotoInTile(photoSize, devXPhoto, devYPhoto, tileSize);

		if (isPhotoInTile) {

//			final int zoomLevel = map.getZoom();
			final int devPartOffset = ((parts - 1) / 2) * tileSize;

			final Image image = getPhotoImage(photo, map, tile);

			if (image == null) {
				return false;
			}

			final Rectangle imageSize = image.getBounds();

			final int photoWidth = photoSize.x;
			final int photoHeight = photoSize.y;

			int devX = devXPhoto - photoWidth / 2;
			int devY = devYPhoto - photoHeight;

			devX += devPartOffset;
			devY += devPartOffset;

			gcTile.drawImage(image, //
					0,
					0,
					imageSize.width,
					imageSize.height,

					//
					devX,
					devY,
					photoWidth,
					photoHeight);

			gcTile.setForeground(_bgColor);
			gcTile.setLineWidth(1);
			gcTile.drawRectangle(devX, devY, photoWidth, photoHeight);

//			System.out.println(net.tourbook.common.UI.timeStampNano()
//					+ (" image: " + imageSize.width + "x" + imageSize.height)
//					+ ("\tphoto: " + photoWidth + " x " + photoHeight));
//			// TODO remove SYSTEM.OUT.PRINTLN
		}

		return isPhotoInTile;
	}

	private boolean drawStaticMarker(	final GC gcTile,
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

			gcTile.drawImage(markerImage, //
					devMarkerPosX - markerWidth2 + devPartOffset,
					devMarkerPosY - markerHeight + devPartOffset);
		}

		return isMarkerInTile;
	}

	private boolean drawTour_10_InTile(	final GC gcTile,
										final Map map,
										final Tile tile,
										final TourData tourData,
										final int parts,
										final Color systemColorBlue) {

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

		gcTile.setForeground(systemColorBlue);
		gcTile.setBackground(systemColorBlue);

		int lastInsideIndex = -99;
		boolean isBorder;

		// index == 0: paint border
		// index == 1: paint tour symbol
		for (int lineIndex = 0; lineIndex < 2; lineIndex++) {

			if (lineIndex == 0) {

				if (_prefIsWithBorder == false) {
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

			gcTile.setLineWidth(_lineWidth);

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

							color = getTourColor(tourData, serieIndex, isBorder, true);

							drawTour_20_Line(gcTile, //
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
						 * the last inside to the first outside position
						 */

						drawTour_20_Line(gcTile, //
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

							final Color color = getTourColor(tourData, serieIndex, isBorder, false);

							if (_prefIsDrawSquare) {
								drawTour_30_Square(gcTile, devX, devY, color);
							} else {
								drawTour_40_Dot(gcTile, devX, devY, color);
							}

							// set previous pixel
							devFromWithOffsetX = devX;
							devFromWithOffsetY = devY;
						}
					}
				}
			}
		}

		return isTourInTile;
	}

	private void drawTour_20_Line(	final GC gc,
									final int devXFrom,
									final int devYFrom,
									final int devXTo,
									final int devYTo,
									final Color color) {

		if (color != null) {
			gc.setForeground(color);
		}

		drawTour_40_Dot(gc, devXTo, devYTo, color);

		// draw line with the color from the legend provider
		gc.drawLine(devXFrom, devYFrom, devXTo, devYTo);

	}

	private void drawTour_30_Square(final GC gc, final int devX, final int devY, final Color color) {

		if (color != null) {
			gc.setBackground(color);
		}

		gc.fillRectangle(devX - _lineWidth2, devY - _lineWidth2, _lineWidth, _lineWidth);
	}

	private void drawTour_40_Dot(final GC gc, final int devX, final int devY, final Color color) {

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

	/**
	 * @param gcTile
	 * @param map
	 * @param tile
	 * @param latitude
	 * @param longitude
	 * @param tourMarker
	 * @param parts
	 * @return Returns <code>true</code> when marker has been painted
	 */
	private boolean drawTourMarker(	final GC gcTile,
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

			final org.eclipse.swt.graphics.Point labelExtent = gcTile.textExtent(tourMarker.getLabel());

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

			final Image tourMarkerImage = drawTourMarkerImage(gcTile.getDevice(), tourMarker.getLabel(), markerBounds);
			{
				devX = devMarkerPosX - markerBounds.width / 2;
				devY = devMarkerPosY - markerBounds.height;

				devX += devPartOffset;
				devY += devPartOffset;

				gcTile.drawImage(tourMarkerImage, devX, devY);
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

		final RGB rgbTransparent = Map.getTransparentRGB();

		final ImageData markerImageData = new ImageData(//
				markerImageWidth,
				markerImageHeight,
				24,
				new PaletteData(0xff, 0xff00, 0xff0000));

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

	/**
	 * @param gcTile
	 * @param map
	 * @param tile
	 * @param twp
	 * @param twpWorldPixel
	 * @param parts
	 * @return Returns <code>true</code> when way point has been painted
	 */
	private boolean drawTourWayPoint(	final GC gcTile,
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

			gcTile.drawImage(_twpImage, devX, devY);

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
	 * @param config
	 * @param legendBounds
	 * @param valueIndex
	 * @return Returns the position for the value according to the value index in the legend,
	 *         {@link Integer#MIN_VALUE} when data are not initialized
	 */
	public int getLegendValuePosition(	final ColorProviderConfig config,
										final Rectangle legendBounds,
										final int valueIndex) {

		if (_dataSerie == null || valueIndex >= _dataSerie.length || //
				// check legend provider type
				_legendProvider instanceof IGradientColorProvider == false//
		) {
			return Integer.MIN_VALUE;
		}

		/*
		 * ONLY VERTICAL LEGENDS ARE SUPPORTED
		 */

		final float dataValue = _dataSerie[valueIndex];

		int valuePosition = 0;

		final MapUnits mapUnits = ((IGradientColorProvider) _legendProvider).getMapUnits(config);

//		final Integer unitFactor = config.unitFactor;
//		dataValue /= unitFactor;

		final float legendMaxValue = mapUnits.legendMaxValue;
		final float legendMinValue = mapUnits.legendMinValue;
		final float legendDiffValue = legendMaxValue - legendMinValue;

		if (dataValue >= legendMaxValue) {

			// value >= max

		} else if (dataValue <= legendMinValue) {

			// value <= min

		} else {

			// min < value < max

			final int legendPositionY = legendBounds.y + IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;
			final int legendHeight = legendBounds.height - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

			final int pixelDiff = legendHeight - 1;

			final float dataValue0 = dataValue - legendMinValue;
			final float ratio = pixelDiff / legendDiffValue;

			valuePosition = legendPositionY + (int) (dataValue0 * ratio);
		}

		return valuePosition;
	}

	/**
	 * @param photo
	 * @param map
	 * @param tile
	 * @return Returns the photo image or <code>null</code> when image is not loaded.
	 */
	private Image getPhotoImage(final Photo photo, final Map map, final Tile tile) {

		Image photoImage = null;

		final ImageQuality requestedImageQuality = ImageQuality.THUMB;

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

		if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

			// image is not yet loaded

			// check if image is in the cache
			photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

			if ((photoImage == null || photoImage.isDisposed())
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				// the requested image is not available in the image cache -> image must be loaded

				final ILoadCallBack imageLoadCallback = new LoadCallbackImage(map, tile);

				PhotoLoadManager.putImageInLoadingQueueThumbMap(photo, requestedImageQuality, imageLoadCallback);
			}
		}

		return photoImage;
	}

	private Color getTourColor(	final TourData tourData,
								final int serieIndex,
								final boolean isBorder,
								final boolean isDrawLine) {

		if (_dataSerie == null) {
			return null;
		}

		/*
		 * Get border color.
		 */
		if (isBorder && _prefBorderType == PrefPageMap2Appearance.BORDER_TYPE_COLOR) {
			return _colorCache.getColor(_prefBorderRGB);
		}

		/*
		 * Get color from the color provider
		 */
		long colorValue = 0;
		if (_legendProvider instanceof IGradientColorProvider) {

			colorValue = ((IGradientColorProvider) _legendProvider).getRGBValue(
					ColorProviderConfig.MAP2,
					_dataSerie[serieIndex]);

		} else if (_legendProvider instanceof IDiscreteColorProvider) {

			colorValue = ((IDiscreteColorProvider) _legendProvider).getColorValue(tourData, serieIndex, isDrawLine);
		}

		if (isBorder) {

			// paint the border in a darker color

			final int red = (int) (((colorValue & 0xFF) >>> 0) * _borderBrightness);
			final int green = (int) (((colorValue & 0xFF00) >>> 8) * _borderBrightness);
			final int blue = (int) (((colorValue & 0xFF0000) >>> 16) * _borderBrightness);

			colorValue = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);
		}

		return _colorCache.getColor((int) colorValue);
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

	private TIntObjectHashMap<Point> initWorldPixelWayPoint(final TourData tourData,
															final Set<TourWayPoint> wayPoints,
															final MP mp,
															final String projectionId,
															final int mapZoomLevel) {
		// world pixels are not yet cached, create them now

		final TIntObjectHashMap<Point> allWayPointWorldPixel = new TIntObjectHashMap<Point>();

		for (final TourWayPoint twp : wayPoints) {

			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition geoPosition = new GeoPosition(twp.getLatitude(), twp.getLongitude());

			allWayPointWorldPixel.put(twp.hashCode(), mp.geoToPixel(geoPosition, mapZoomLevel));
		}

		tourData.setWorldPixelForWayPoints(allWayPointWorldPixel, mapZoomLevel, projectionId);

		return allWayPointWorldPixel;
	}

	/**
	 * Checks if an image bounds is within the tile. The image is above the image position and one
	 * half to the left and right side
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

		final ArrayList<TourData> tourDataList = _tourPaintConfig.getTourData();
		final ArrayList<Photo> photoList = _tourPaintConfig.getPhotos();

		if (tourDataList.size() == 0 && photoList.size() == 0) {
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

		if (_tourPaintConfig.isTourVisible && tourDataList.size() > 0) {

			if (isPaintingNeeded_Tours(
					tourDataList,
					mp,
					mapZoomLevel,
					projectionId,
					tileWorldPixelLeft,
					tileWorldPixelRight,
					tileWorldPixelTop,
					tileWorldPixelBottom)) {

				return true;
			}
		}

		if (_tourPaintConfig.isPhotoVisible && photoList.size() > 0) {

			if (isPaintingNeeded_Photos(
					photoList,
					mp,
					mapZoomLevel,
					projectionId,
					tileWorldPixelLeft,
					tileWorldPixelRight,
					tileWorldPixelTop,
					tileWorldPixelBottom)) {

				return true;
			}
		}

		return false;
	}

	private boolean isPaintingNeeded_Photos(final ArrayList<Photo> photoList,
											final MP mp,
											final int mapZoomLevel,
											final String projectionId,
											final int tileWorldPixelLeft,
											final int tileWorldPixelRight,
											final int tileWorldPixelTop,
											final int tileWorldPixelBottom) {
		/*
		 * check photos
		 */
		for (final Photo photo : photoList) {

			final Point photoWorldPixel = photo.getWorldPosition(
					mp,
					projectionId,
					mapZoomLevel,
					_tourPaintConfig.isLinkPhotoDisplayed);

			if (photoWorldPixel == null) {
				continue;
			}

			final org.eclipse.swt.graphics.Point photoSize = photo.getMapImageSize();
			final int tileSize = mp.getTileSize();

			// convert world position into tile position
			final int devXPhoto = photoWorldPixel.x - tileWorldPixelLeft;
			final int devYPhoto = photoWorldPixel.y - tileWorldPixelTop;

			final boolean isPhotoInTile = isPhotoInTile(photoSize, devXPhoto, devYPhoto, tileSize);

			if (isPhotoInTile) {
				return true;
			}
		}

		return false;
	}

	private boolean isPaintingNeeded_Tours(	final ArrayList<TourData> tourDataList,
											final MP mp,
											final int mapZoomLevel,
											final String projectionId,
											final int tileWorldPixelLeft,
											final int tileWorldPixelRight,
											final int tileWorldPixelTop,
											final int tileWorldPixelBottom) {
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

				TIntObjectHashMap<Point> allWayPointWorldPixel = tourData.getWorldPositionForWayPoints(
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

					if (twpWorldPixel == null) {
						// this happened but should not
						continue;
					}

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

	private boolean isPhotoInTile(	final org.eclipse.swt.graphics.Point photoSize,
									final int devXPhoto,
									final int devYPhoto,
									final int tileSize) {

		// get image size
		final int imageWidth = photoSize.x;
		final int imageWidth2 = imageWidth / 2;
		final int imageHeight = photoSize.y;

		final int devImagePosLeft = devXPhoto - imageWidth2;
		final int devImagePosRight = devXPhoto + imageWidth2;

		// image position top is in the opposite direction
		final int devImagePosTop = devYPhoto - imageHeight;

		if (((devImagePosLeft >= 0 && devImagePosLeft <= tileSize) || (devImagePosRight >= 0 && devImagePosRight <= tileSize))
				&& (devYPhoto >= 0 && devYPhoto <= tileSize || devImagePosTop >= 0 && devImagePosTop <= tileSize)) {
			return true;
		}

		return false;
	}

	/**
	 * Set the data serie which is painted
	 * 
	 * @param tourData
	 */
	private void setDataSerie(final TourData tourData) {

		final IMapColorProvider legendProvider = _tourPaintConfig.getMapColorProvider();

		if (legendProvider == null) {
			_dataSerie = null;
			return;
		}

		_legendProvider = legendProvider;

		switch (_legendProvider.getGraphId()) {
		case Altitude:
			_dataSerie = tourData.getAltitudeSerie();
			break;

		case Gradient:
			_dataSerie = tourData.getGradientSerie();
			break;

		case Pulse:
			_dataSerie = tourData.pulseSerie;
			break;

		case Speed:
			_dataSerie = tourData.getSpeedSerie();
			break;

		case Pace:
			_dataSerie = tourData.getPaceSerieSeconds();
			break;

		case HrZone:
			_dataSerie = tourData.pulseSerie;
			break;

		default:
			break;
		}
	}

}
