/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.Map2Painter;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.mapprovider.MP;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Util;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.color.MapUnits;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.ImageConverter;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.map2.Messages;
import net.tourbook.map3.layer.TourLegendLabel;
import net.tourbook.photo.PhotoUI;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.Map2_Appearance;
import net.tourbook.ui.views.referenceTour.ReferenceTourManager;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Paints a tour into the 2D map.
 */
public class TourMapPainter extends Map2Painter {

   private static final Font              DEFAULT_FONT      = net.tourbook.common.UI.AWT_DIALOG_FONT;

   private static final IPreferenceStore  _prefStore        = TourbookPlugin.getPrefStore();

   private static IPropertyChangeListener _prefChangeListener;

   private static float                   _borderBrightness;

   private static RGB                     _prefBorderRGB;
   private static int                     _prefBorderType;
   private static int                     _prefBorderWidth;
   private static boolean                 _prefIsAntialiasPainting;
   private static boolean                 _prefIsDrawLine;
   private static boolean                 _prefIsDrawSquare;
   private static boolean                 _prefIsWithBorder;
   private static int                     _prefLineWidth;

   private static int                     _prefGeoCompare_LineWidth;
   private static RGB                     _prefGeoCompare_RefTour_RGB;
   private static RGB                     _prefGeoCompare_CompartTourPart_RGB;

   private static boolean                 _isImageAvailable = false;
   private static boolean                 _isErrorLogged;

   /**
    * Tour start/end image
    */
   private static Image                   _tourStartMarker;
   private static Image                   _tourEndMarker;

   private static final NumberFormat      _nf1              = NumberFormat.getNumberInstance();
   static {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private static final ColorCacheSWT _colorCache = new ColorCacheSWT();

   /*
    * None static fields
    */
   private float[]           _dataSerie;
   private IMapColorProvider _legendProvider;

   // painting parameter
   private int     _symbolSize;
   private int     _symbolSize2;
   private int     _symbolHoveredMargin;
   private int     _symbolHoveredMargin2;

   private boolean _isFastPainting;
   private int     _fastPainting_SkippedValues;

   public TourMapPainter() {

      super();

      /*
       * I've not yet understood to manage this problem because TourPainter() is created from an
       * extension point but setting the instance in the constructor is not valid according to
       * FindBugs
       */

      initPainter();
   }

   /**
    * Creates a legend image with AWT framework. This image must be disposed who created it.
    *
    * @param display
    * @param colorProvider
    * @param imageWidth
    * @param imageHeight
    * @param isDarkBackground
    * @param isDrawVertical
    * @param isDarkBackground
    * @param isDrawUnitShadow
    * @param isDrawLegendText
    *
    * @return
    */
   public static Image createMap2_LegendImage_AWT(final IGradientColorProvider colorProvider,
                                                  final int imageWidth,
                                                  final int imageHeight,
                                                  final boolean isDarkBackground,
                                                  final boolean isDrawUnitShadow) {

      final BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);

      final Graphics2D g2d = image.createGraphics();
      try {

         drawMap_Legend_AWT(
               g2d,
               colorProvider,
               ColorProviderConfig.MAP2,
               imageWidth,
               imageHeight,
               true, // isVertical
               true, // isDrawUnits
               isDarkBackground,
               isDrawUnitShadow);

      } finally {
         g2d.dispose();
      }

      return ImageConverter.convertIntoSWT(image);

   }

   public static Image createMap3_LegendImage(final IGradientColorProvider colorProvider,
                                              final ColorProviderConfig config,
                                              final int imageWidth,
                                              final int imageHeight,
                                              final boolean isVertical,
                                              final boolean isDrawUnits,
                                              final boolean isDarkBackground,
                                              final boolean isDrawUnitShadow) {

      final BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);

      final Graphics2D g2d = image.createGraphics();
      try {

         drawMap_Legend_AWT(
               g2d,
               colorProvider,
               config,
               imageWidth,
               imageHeight,
               isVertical,
               isDrawUnits,
               isDarkBackground,
               isDrawUnitShadow);

      } finally {
         g2d.dispose();
      }

      return ImageConverter.convertIntoSWT(image);
   }

   /**
    * @param g2d
    * @param colorProvider
    * @param config
    * @param legendWidth
    * @param legendHeight
    * @param isDrawVertical
    * @param isDrawUnits
    * @param isDarkBackground
    * @param isDrawUnitShadow
    */
   private static void drawMap_Legend_AWT(final Graphics2D g2d,
                                          final IGradientColorProvider colorProvider,
                                          final ColorProviderConfig config,
                                          final int legendWidth,
                                          final int legendHeight,
                                          final boolean isDrawVertical,
                                          final boolean isDrawUnits,
                                          final boolean isDarkBackground,
                                          final boolean isDrawUnitShadow) {

// SET_FORMATTING_OFF

      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

// sometimes it looks better with this parameter but sometimes not
//      g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,     RenderingHints.VALUE_FRACTIONALMETRICS_ON);

//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,          RenderingHints.VALUE_ANTIALIAS_ON);
//      g2d.setRenderingHint(RenderingHints.KEY_DITHERING,             RenderingHints.VALUE_DITHER_ENABLE);
//      g2d.setRenderingHint(RenderingHints.KEY_RENDERING,             RenderingHints.VALUE_RENDER_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST,     100);
//      g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,   RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,       RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,        RenderingHints.VALUE_STROKE_PURE);

// SET_FORMATTING_ON

      final MapUnits mapUnits = colorProvider.getMapUnits(config);

      // ensure units are available
      Assert.isNotNull(mapUnits.units);

      /*
       * Setup units
       */
      final ArrayList<Float> legendUnits = new ArrayList<>(mapUnits.units);
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
      g2d.setFont(font);
//      final Font largerFont = font.deriveFont(font.getSize() * 1.1f);
//      g2d.setFont(largerFont);

      // Measure the font and the message
      final FontRenderContext fontRenderContext = g2d.getFontRenderContext();
      final LineMetrics metrics = font.getLineMetrics(unitText, fontRenderContext);
      final float lineheight = metrics.getHeight(); // Total line height

      /*
       * Setup legend image
       */
      final int borderSize = 1;
      final int borderSize2 = 2 * borderSize;

      int availableLegendPixels;
      int contentX;
      int contentY;
      int graphWidth;
      int graphHeight;

      if (isDrawVertical) {

         // vertical

         if (isDrawUnits) {

            contentX = borderSize;
            contentY = borderSize + IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

            graphWidth = IMapColorProvider.DEFAULT_LEGEND_GRAPHIC_WIDTH;
            graphHeight = legendHeight - borderSize2 - 1 - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

         } else {

            contentX = borderSize;
            contentY = borderSize;

            graphWidth = legendWidth - borderSize2;
            graphHeight = legendHeight - borderSize2 - 1;
         }

         availableLegendPixels = graphHeight;

      } else {

         // horizontal

         contentX = borderSize;
         contentY = borderSize;

         graphWidth = legendWidth - borderSize2;
         graphHeight = legendHeight - borderSize2 - 1;

         availableLegendPixels = graphWidth;
      }

      // pixelValue contains the value for ONE pixel
      final float pixelValue = legendDiffValue / availableLegendPixels;
      final float roundingValue = pixelValue / 100;

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

                           final float unitDecimals = unitValue.floatValue() - unitValue.intValue();

                           if (Math.abs(unitDecimals) < 0.1) {

                              // hide ".0" decimals

                              valueText = UI.EMPTY_STRING

                                    // add space to align values in a column, it't not perfect but better than nothing
                                    + UI.SPACE3

                                    + Integer.toString(unitValue.intValue()) + UI.SPACE + unitText;

                           } else {

                              valueText = _nf1.format(unitValue) + UI.SPACE + unitText;
                           }
                        }
                     }

                  } else {

                     // when unitLabels are available, they will overwrite the default labeling
                     valueText = unitLabels.get(unitLabelIndex++);
                  }
                  final int devXText = graphWidth + 7;
                  final int devYText = (int) (devValue + lineheight / 2);

                  if (isDarkBackground) {

                     // dark background

                     if (isDrawUnitShadow) {
                        g2d.setColor(java.awt.Color.DARK_GRAY);
                        g2d.drawString(valueText, devXText + 1, devYText + 1);
                        g2d.drawString(valueText, devXText + 1, devYText - 1);
                        g2d.drawString(valueText, devXText - 1, devYText + 1);
                        g2d.drawString(valueText, devXText - 1, devYText - 1);
                     }

                     g2d.setColor(java.awt.Color.WHITE);
                     g2d.drawString(valueText, devXText, devYText);

                  } else {

                     // bright background

                     if (isDrawUnitShadow) {
                        g2d.setColor(java.awt.Color.WHITE);
                        g2d.drawString(valueText, devXText + 1, devYText + 1);
                        g2d.drawString(valueText, devXText + 1, devYText - 1);
                        g2d.drawString(valueText, devXText - 1, devYText + 1);
                        g2d.drawString(valueText, devXText - 1, devYText - 1);
                     }

                     g2d.setColor(java.awt.Color.BLACK);
                     g2d.drawString(valueText, devXText, devYText);
                  }

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
         g2d.setColor(
               new java.awt.Color(
                     (rgba & 0xFF) >>> 0,
                     (rgba & 0xFF00) >>> 8,
                     (rgba & 0xFF0000) >>> 16,
                     (rgba & 0xFF000000) >>> 24));

         if (isDrawVertical) {

            // vertical legend
            g2d.drawLine(contentX, devValue, graphWidth, devValue);

         } else {

            // horizontal legend
            g2d.drawLine(devValue, contentY, devValue, graphHeight);
         }

      }
   }

   /**
    * Draws map legend colors into the legend bounds.
    *
    * @param gc
    * @param legendImageBounds
    * @param colorProvider
    * @param isDrawVertical
    * @param isDrawVertical
    *           When <code>true</code> the legend is drawn vertically otherwise it's drawn
    *           horizontally.
    * @param isDarkBackground
    * @param isDrawLegendText
    */
   public static void drawMap2_Legend(final GC gc,
                                      final Rectangle legendImageBounds,
                                      final IMapColorProvider colorProvider) {

      if (colorProvider instanceof IGradientColorProvider) {

         drawMap2_Legend_GradientColors_SWT(
               gc,
               legendImageBounds,
               (IGradientColorProvider) colorProvider);
      }
   }

   private static void drawMap2_Legend_GradientColors_SWT(final GC gc,
                                                          final Rectangle imageBounds,
                                                          final IGradientColorProvider colorProvider) {

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
      final float legendMaxValue = mapUnits.legendMaxValue;
      final float legendMinValue = mapUnits.legendMinValue;
      final float legendDiffValue = legendMaxValue - legendMinValue;

      int contentX;
      int contentY;
      int contentWidth;
      int contentHeight;
      int availableLegendPixels;

      // horizontal legend
      contentX = imageBounds.x;
      contentY = imageBounds.y;

      contentWidth = imageBounds.width;
      contentHeight = imageBounds.height;

      availableLegendPixels = contentWidth;

      // pixelValue contains the value for ONE pixel
      final float pixelValue = legendDiffValue / availableLegendPixels;

      for (int pixelIndex = 0; pixelIndex <= availableLegendPixels; pixelIndex++) {

         final float legendValue = legendMinValue + pixelValue * pixelIndex;

         int devXorY_Value;

         devXorY_Value = contentX + pixelIndex;

         /*
          * Draw legend color line
          */

         final long valueRGB = colorProvider.getRGBValue(ColorProviderConfig.MAP2, legendValue);
         final Color valueColor = _colorCache.getColor((int) valueRGB);

         gc.setForeground(valueColor);

         // horizontal legend

         gc.drawLine(devXorY_Value, contentY, devXorY_Value, contentHeight);
      }

      _colorCache.dispose();
   }

   public static void drawMap3_Legend(final Graphics2D g2d,
                                      final IMapColorProvider colorProvider,
                                      final ColorProviderConfig config,
                                      final int legendWidth,
                                      final int legendHeight,
                                      final boolean isDarkBackground,
                                      final boolean isDrawUnitShadow) {

      if (colorProvider instanceof IGradientColorProvider) {

         drawMap_Legend_AWT(
               g2d,
               (IGradientColorProvider) colorProvider,
               config,
               legendWidth,
               legendHeight,
               true, // isVertical
               true, // isDrawUnits
               isDarkBackground,
               isDrawUnitShadow);
      }
   }

   public static List<TourLegendLabel> getMap3_LegendLabels(final int legendHeight,
                                                            final IGradientColorProvider colorProvider,
                                                            final ColorProviderConfig config) {

      final ArrayList<TourLegendLabel> allLegendLabels = new ArrayList<>();

      final MapUnits mapUnits = colorProvider.getMapUnits(config);

      // ensure units are available
      if (mapUnits.units == null) {
         return allLegendLabels;
      }

      // get configuration for the legend
      final ArrayList<Float> allLegendUnits = new ArrayList<>(mapUnits.units);

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

                     valueText = Util.format_mm_ss(unitValue.longValue() * 60) + UI.SPACE + unitText;

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

               allLegendLabels.add(new TourLegendLabel(unitValue, valueText, valuePositionY));

               // prevent to draw this unit again
               allLegendUnits.remove(unitValue);

               break;
            }
         }
      }

      return allLegendLabels;
   }

   private static void getTourPainterSettings() {

// SET_FORMATTING_OFF

      final String drawSymbol = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE);


      _prefIsDrawLine            = drawSymbol.equals(Map2_Appearance.PLOT_TYPE_LINE);
      _prefIsDrawSquare          = drawSymbol.equals(Map2_Appearance.PLOT_TYPE_SQUARE);

      _prefIsAntialiasPainting   = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_IS_ANTIALIAS_PAINTING);

      _prefLineWidth             = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
      _prefIsWithBorder          = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER);

      _prefBorderRGB             = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR);
      _prefBorderType            = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE);
      _prefBorderWidth           = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH);


      final int prefBorderDimmValue = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE);
      _borderBrightness          = (float) (1.0 - prefBorderDimmValue / 100.0);

      /*
       * Geo compare
       */
      _prefGeoCompare_LineWidth           = _prefStore.getInt(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH);
      _prefGeoCompare_RefTour_RGB         = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB);
      _prefGeoCompare_CompartTourPart_RGB = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB);

// SET_FORMATTING_ON
   }

   private static void initPainter() {

      // setup only ONCE
      if (_prefChangeListener != null) {
         return;
      }

      // ensure color registry is setup
      PhotoUI.init();

      /**
       * this code optimizes the performance by reading from the pref store which is not very
       * efficient
       */
      getTourPainterSettings();

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // test if the color or statistic data have changed
         if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
               || property.equals(ITourbookPreferences.MAP2_OPTIONS_IS_MODIFIED)) {

            getTourPainterSettings();
         }
      };

      // add pref listener, dispose is not removing it because it is static !!!
      TourbookPlugin.getPrefStore().addPropertyChangeListener(_prefChangeListener);
   }

   private void createImages() {

      _tourStartMarker = TourbookPlugin.getImageDescriptor(Messages.Image_Map_TourStartMarker).createImage();
      _tourEndMarker = TourbookPlugin.getImageDescriptor(Messages.Image_Map_TourEndMarker).createImage();

      _isImageAvailable = true;
   }

   @Override
   public void dispose() {

      UI.disposeResource(_tourEndMarker);
      UI.disposeResource(_tourStartMarker);

      _isImageAvailable = false;
   }

   @Override
   protected void disposeTempResources() {
//    _colorCache.dispose();
   }

   @Override
   public boolean doPaint(final GC gcTile,
                          final Map2 map,
                          final Tile tile,
                          final int parts,
                          final boolean isFastPainting,
                          final int fastPainting_SkippedValues) {

      _isFastPainting = isFastPainting;
      _fastPainting_SkippedValues = fastPainting_SkippedValues;

      initPainter();

      final ArrayList<TourData> allTourData = Map2PainterConfig.getTourData();

      if (allTourData.isEmpty()) {
         return false;
      }

      boolean isContentInTile = false;

      if (_isImageAvailable == false) {
         createImages();
      }

      // first draw the tour, then the marker and photos
      if (Map2PainterConfig.isShowTours) {

         if (_prefIsAntialiasPainting) {
            gcTile.setAntialias(SWT.ON);
         } else {
            gcTile.setAntialias(SWT.OFF);
         }

         final Color systemColorBlue = gcTile.getDevice().getSystemColor(SWT.COLOR_BLUE);

         TourData prevTourData = null;
         final long geoCompareRefTourId = ReferenceTourManager.getGeoCompare_RefTour_TourId();

         final int numTours = allTourData.size();

         for (final TourData tourData : allTourData) {

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

            boolean isGeoCompareRefTour = geoCompareRefTourId >= 0

                  && geoCompareRefTourId == tourData.getTourId()

                  // when only 1 tour is displayed, do not show it as reference tour
                  && numTours > 1;

            int refTourStartIndex = 0;
            int refTourEndIndex = 0;

            if (isGeoCompareRefTour) {

               if (tourData == prevTourData) {

                  /*
                   * This can occur when a compared tour is compared with it's own reference tour
                   * -> paint 2nd time as normal tour
                   */
                  isGeoCompareRefTour = false;

               } else {

                  final TourReference refTour = ReferenceTourManager.getGeoCompare_RefTour();

                  refTourStartIndex = refTour.getStartValueIndex();
                  refTourEndIndex = refTour.getEndValueIndex();
               }
            }

            prevTourData = tourData;

            final boolean isDrawTourInTile = drawTour_10_InTile(
                  gcTile,
                  map,
                  tile,
                  tourData,
                  parts,
                  systemColorBlue,
                  isGeoCompareRefTour,
                  refTourStartIndex,
                  refTourEndIndex);

            isContentInTile = isContentInTile || isDrawTourInTile;

//          /**
//           * DEBUG Start
//           */
//          gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
//          gcTile.fillRectangle(0, 0, 2, 50);
//          gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//          gcTile.fillRectangle(2, 0, 2, 50);
//          gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//          gcTile.fillRectangle(4, 0, 2, 50);
//          gcTile.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//          gcTile.fillRectangle(6, 0, 2, 50);
//          isContentInTile = true;
//          /**
//           * DEBUG End
//           */

            // status if a marker is drawn
            int staticMarkerCounter = 0;

            // draw start/end marker
            if (Map2PainterConfig.isShowTourStartEnd) {

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
               if (drawStaticMarker(
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

      return isContentInTile;
   }

   private boolean drawStaticMarker(final GC gcTile,
                                    final Map2 map,
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

      final boolean isMarkerInTile = isInTile_Bounds(markerImage.getBounds(), devMarkerPosX, devMarkerPosY, tileSize);
      if (isMarkerInTile) {

         // get marker size
         final Rectangle bounds = markerImage.getBounds();
         final int markerWidth = bounds.width;
         final int markerWidth2 = markerWidth / 2;
         final int markerHeight = bounds.height;

         gcTile.drawImage(
               markerImage,
               devMarkerPosX - markerWidth2 + devPartOffset,
               devMarkerPosY - markerHeight + devPartOffset);
      }

      return isMarkerInTile;
   }

   private boolean drawTour_10_InTile(final GC gcTile,
                                      final Map2 map,
                                      final Tile tile,
                                      final TourData tourData,
                                      final int numParts,
                                      final Color systemColorBlue,
                                      final boolean isGeoCompareRefTour,
                                      final int refTourStartIndex,
                                      final int refTourEndIndex) {

      final MP mp = map.getMapProvider();
      final int projectionHash = mp.getProjection().getId().hashCode();
      final int mapZoomLevel = map.getZoom();

      if (numParts == 1) {

         // basic drawing method is used -> optimize performance

         if (isInTile_Tour(tourData, mp, mapZoomLevel, tile, projectionHash) == false) {
            return false;
         }
      }

      boolean isTourInTile = false;

      final int tileSize = mp.getTileSize();
      final int devPartOffset = ((numParts - 1) / 2) * tileSize;

      // get viewport for the current tile
      final int tileWorldPixelX = tile.getX() * tileSize;
      final int tileWorldPixelY = tile.getY() * tileSize;
      final int tileWidth = tileSize;
      final int tileHeight = tileSize;

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;
      final boolean[] visibleDataPointSerie = tourData.visibleDataPointSerie;

      final boolean isMultipleTours = tourData.isMultipleTours();
      final Long[] allMultipleTourIds = tourData.multipleTourIds;
      final int[] allMultipleTour_StartIndex = tourData.multipleTourStartIndex;

      final int numTimeSlices = latitudeSerie.length;
      final int numMultipleTours = isMultipleTours && allMultipleTourIds != null
            ? allMultipleTourIds.length
            : 0;

      final int nextTour_StartIndex = isMultipleTours

            ? numMultipleTours > 1
                  ? allMultipleTour_StartIndex[1]
                  : numTimeSlices

            : -1;

      int subTourIndex = 0;
      Long tourId = isMultipleTours
            ? allMultipleTourIds[0]
            : tourData.getTourId();

      boolean isVisibleDataPoint_AfterIsWasHidden = false;
      boolean isPreviousVisibleDataPoint = false;

      /*
       * World positions are cached to optimize performance when multiple tours are selected
       */
      Point[] allTour_WorldPixelPos = tourData.getWorldPositionForTour(projectionHash, mapZoomLevel);

      if ((allTour_WorldPixelPos == null)) {

         allTour_WorldPixelPos = setupWorldPixel_Tour(
               tourData,
               mp,
               mapZoomLevel,
               latitudeSerie,
               longitudeSerie,
               projectionHash);
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
            _symbolSize = _prefLineWidth + (_prefBorderWidth * 2);

         } else if (lineIndex == 1) {

            isBorder = false;

            // draw within the border
            _symbolSize = _prefLineWidth;

         } else {

            break;
         }

         if (isGeoCompareRefTour) {

            // draw it more visible

            _symbolSize = _prefGeoCompare_LineWidth;
         }

         _symbolSize2 = _symbolSize / 2;

         // ensure that the margin in not larger than a max size
         _symbolHoveredMargin = Map2.EXPANDED_HOVER_SIZE - _symbolSize > 0
               ? Map2.EXPANDED_HOVER_SIZE - _symbolSize
               : 0;
         _symbolHoveredMargin2 = _symbolHoveredMargin / 2;

         gcTile.setLineWidth(_symbolSize);

         int devFrom_WithOffsetX = 0;
         int devFrom_WithOffsetY = 0;

         Color lastVisibleColor = null;

         for (int serieIndex = 0; serieIndex < longitudeSerie.length; serieIndex++) {

            if (_isFastPainting) {

               serieIndex += _fastPainting_SkippedValues;

               if (serieIndex >= longitudeSerie.length) {
                  serieIndex = longitudeSerie.length - 1;
               }
            }

            final Point tourWorldPixel = allTour_WorldPixelPos[serieIndex];
            final int tourWorldPixelX = tourWorldPixel.x;
            final int tourWorldPixelY = tourWorldPixel.y;

            int devX = tourWorldPixelX - tileWorldPixelX;
            int devY = tourWorldPixelY - tileWorldPixelY;

            boolean isInRefTourPart = false;

            if (isGeoCompareRefTour) {

               // paint ref tour part with a different color

               isInRefTourPart = serieIndex >= refTourStartIndex && serieIndex <= refTourEndIndex;
            }

            if (_prefIsDrawLine && _isFastPainting == false) {

               // draw as a line

               // get positions with the part offset
               final int devTo_WithOffsetX = devX + devPartOffset;
               final int devTo_WithOffsetY = devY + devPartOffset;

               if (serieIndex == 0) {

                  // keep position
                  devFrom_WithOffsetX = devTo_WithOffsetX;
                  devFrom_WithOffsetY = devTo_WithOffsetY;

                  continue;
               }

               /*
                * Check visible points
                */
               boolean isVisibleDataPoint = true;
               if (visibleDataPointSerie != null) {

                  // visible data points are available -> use it

                  isVisibleDataPoint = visibleDataPointSerie[serieIndex];

                  if (isPreviousVisibleDataPoint == false && isVisibleDataPoint) {

                     isVisibleDataPoint_AfterIsWasHidden = true;
                  }
               }

               /*
                * Get sub tour id
                */
               if (isMultipleTours) {

                  if (serieIndex >= nextTour_StartIndex) {

                     // advance to the next sub tour

                     for (; subTourIndex < numMultipleTours; subTourIndex++) {

                        final int nextSubTour_StartIndex = allMultipleTour_StartIndex[subTourIndex];

                        if (serieIndex < nextSubTour_StartIndex) {
                           break;
                        }
                     }

                     tourId = subTourIndex >= numMultipleTours
                           ? allMultipleTourIds[numMultipleTours - 1]
                           : allMultipleTourIds[subTourIndex];
                  }
               }

//               if (serieIndex == 1146) {
//                  int a = 0;
//                  a++;
//               }

               Color color = null;

               /*
                * Check if position is in the viewport, this condition is an inline for:
                * -
                * tileViewport.contains(tileWorldPos.x, tileWorldPos.y)
                */
               if ((tourWorldPixelX >= tileWorldPixelX)
                     && (tourWorldPixelY >= tileWorldPixelY)
                     && tourWorldPixelX < (tileWorldPixelX + tileWidth)
                     && tourWorldPixelY < (tileWorldPixelY + tileHeight)) {

                  // current position is inside the tile

                  // check if position has changed
                  if (devTo_WithOffsetX != devFrom_WithOffsetX || devTo_WithOffsetY != devFrom_WithOffsetY) {

                     isTourInTile = true;

                     if (isVisibleDataPoint) {

                        color = getTourColor(
                              tourData,
                              serieIndex,
                              isBorder,
                              true,
                              isGeoCompareRefTour,
                              isInRefTourPart);

                        lastVisibleColor = color;

                        if (isVisibleDataPoint_AfterIsWasHidden) {

                           // draw starting point after a pause/break

                           drawTour_40_Dot(gcTile,
                                 devFrom_WithOffsetX,
                                 devFrom_WithOffsetY,
                                 color,
                                 tile,
                                 tourId,

                                 // adjust to the previous index otherwise the index is wrong
                                 serieIndex - 1);

                        }

                        drawTour_20_Line(
                              gcTile,
                              devFrom_WithOffsetX,
                              devFrom_WithOffsetY,
                              devTo_WithOffsetX,
                              devTo_WithOffsetY,
                              color,
                              tile,
                              tourId,
                              serieIndex);
                     }
                  }

                  lastInsideIndex = serieIndex;
               }

               // check first outside point
               if (isVisibleDataPoint && serieIndex == lastInsideIndex + 1) {

                  /*
                   * This position is the first which is outside of the tile, draw a line from
                   * the last inside to the first outside position
                   */

                  if (isVisibleDataPoint_AfterIsWasHidden) {

                     if (lastVisibleColor == null) {

                        lastVisibleColor = getTourColor(
                              tourData,
                              serieIndex,
                              isBorder,
                              true,
                              isGeoCompareRefTour,
                              isInRefTourPart);
                     }

                     drawTour_40_Dot(gcTile,
                           devFrom_WithOffsetX,
                           devFrom_WithOffsetY,
                           lastVisibleColor,
                           tile,
                           tourId,

                           // adjust to the previous index otherwise the index is wrong
                           serieIndex - 1);

                  }

                  drawTour_20_Line(
                        gcTile,
                        devFrom_WithOffsetX,
                        devFrom_WithOffsetY,
                        devTo_WithOffsetX,
                        devTo_WithOffsetY,
                        lastVisibleColor,
                        tile,
                        tourId,
                        serieIndex);
               }

               // keep positions
               devFrom_WithOffsetX = devTo_WithOffsetX;
               devFrom_WithOffsetY = devTo_WithOffsetY;

               isPreviousVisibleDataPoint = isVisibleDataPoint;

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
                  if (!(devX == devFrom_WithOffsetX && devY == devFrom_WithOffsetY)) {

                     /*
                      * Check visible points
                      */
                     boolean isVisibleDataPoint = true;
                     if (visibleDataPointSerie != null) {
                        isVisibleDataPoint = visibleDataPointSerie[serieIndex];
                     }

                     if (isVisibleDataPoint) {

                        isTourInTile = true;

                        /*
                         * Get sub tour
                         */
                        if (isMultipleTours) {

                           if (serieIndex >= nextTour_StartIndex) {

                              // advance to the next sub tour

                              for (; subTourIndex < numMultipleTours; subTourIndex++) {

                                 final int nextSubTour_StartIndex = allMultipleTour_StartIndex[subTourIndex];

                                 if (serieIndex < nextSubTour_StartIndex) {
                                    break;
                                 }
                              }

                              tourId = subTourIndex >= numMultipleTours
                                    ? allMultipleTourIds[numMultipleTours - 1]
                                    : allMultipleTourIds[subTourIndex];
                           }
                        }

                        // adjust positions with the part offset
                        devX += devPartOffset;
                        devY += devPartOffset;

                        final Color color = getTourColor(
                              tourData,
                              serieIndex,
                              isBorder,
                              false,
                              isGeoCompareRefTour,
                              isInRefTourPart);

                        if (_prefIsDrawSquare == false || _isFastPainting) {
                           drawTour_40_Dot(gcTile, devX, devY, color, tile, tourId, serieIndex);
                        } else {
                           drawTour_30_Square(gcTile, devX, devY, color, tile, tourId, serieIndex);
                        }

                        // set previous pixel
                        devFrom_WithOffsetX = devX;
                        devFrom_WithOffsetY = devY;
                     }

                  } else {

//                     System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()")
//                           + ("\tskipped: " + devX + " " + devY)
////                           + ("\t: " + )
//                           );
                  }
               }
            }
         }
      }

      return isTourInTile;
   }

   private void drawTour_20_Line(final GC gc,
                                 final int devXFrom,
                                 final int devYFrom,
                                 final int devXTo,
                                 final int devYTo,
                                 final Color color,
                                 final Tile tile,
                                 final Long tourId,
                                 final int serieIndex) {

      final int prime = 31;
      int paintingHash = 1;

      if (color != null) {
         paintingHash = prime * paintingHash + color.hashCode();
      }

      // create painting hash code
      paintingHash = prime * paintingHash + devXFrom;
      paintingHash = prime * paintingHash + devXTo;
      paintingHash = prime * paintingHash + devYFrom;
      paintingHash = prime * paintingHash + devYTo;

      final IntHashSet allPainted_DotsHash = tile.allPainted_Hash;
      if (allPainted_DotsHash.contains(paintingHash)) {

         // dot is already painted
         return;
      }
      allPainted_DotsHash.add(paintingHash);

      drawTour_40_Dot(gc, devXTo, devYTo, color, tile, tourId, serieIndex);

      // draw line with the color from the legend provider
      if (color != null) {
         gc.setForeground(color);
      }

      gc.drawLine(devXFrom, devYFrom, devXTo, devYTo);
   }

   private void drawTour_30_Square(final GC gc,
                                   final int devX,
                                   final int devY,
                                   final Color color,
                                   final Tile tile,
                                   final Long tourId,
                                   final int serieIndex) {

      final int prime = 31;
      int paintingHash = 1;

      if (color != null) {
         paintingHash = prime * paintingHash + color.hashCode();
      }

      final int paintedDevX = devX - _symbolSize2;
      final int paintedDevY = devY - _symbolSize2;

      // create painting hash code
      paintingHash = prime * paintingHash + paintedDevX;
      paintingHash = prime * paintingHash + paintedDevY;

      final IntHashSet allPainted_DotsHash = tile.allPainted_Hash;
      if (allPainted_DotsHash.contains(paintingHash)) {

         // dot is already painted
         return;
      }
      allPainted_DotsHash.add(paintingHash);

      if (color != null) {
         gc.setBackground(color);
      }
      gc.fillRectangle(paintedDevX, paintedDevY, _symbolSize, _symbolSize);

      /*
       * Keep area to detect the hovered tour and enlarge it with a margin to easier hit it
       */
      final Rectangle hoveredRect = new Rectangle(
            paintedDevX - _symbolHoveredMargin2,
            paintedDevY - _symbolHoveredMargin2,
            _symbolSize + _symbolHoveredMargin,
            _symbolSize + _symbolHoveredMargin);

      tile.allPainted_HoverRectangle.add(hoveredRect);
      tile.allPainted_HoverTourID.add(tourId);
      tile.allPainted_HoverSerieIndices.add(serieIndex);
   }

   private void drawTour_40_Dot(final GC gc,
                                final int devX,
                                final int devY,
                                final Color color,
                                final Tile tile,
                                final Long tourId,
                                final int serieIndex) {

      final int prime = 31;
      int paintingHash = 1;

      if (color != null) {
         paintingHash = prime * paintingHash + color.hashCode();
      }

      int paintedDevX;
      int paintedDevY;

      if (_symbolSize == 2) {

         // oval is not filled by a width of 2

         paintedDevX = devX;
         paintedDevY = devY;

      } else {

         paintedDevX = devX - _symbolSize2;
         paintedDevY = devY - _symbolSize2;
      }

      // create painting hash code
      paintingHash = prime * paintingHash + paintedDevX;
      paintingHash = prime * paintingHash + paintedDevY;

      final IntHashSet allPainted_DotsHash = tile.allPainted_Hash;
      if (allPainted_DotsHash.contains(paintingHash)) {

         // dot is already painted
         return;
      }
      allPainted_DotsHash.add(paintingHash);

      if (color != null) {
         gc.setBackground(color);
      }

      if (_symbolSize == 2) {
         gc.fillRectangle(paintedDevX, paintedDevY, _symbolSize, _symbolSize);
      } else {
         gc.fillOval(paintedDevX, paintedDevY, _symbolSize, _symbolSize);
      }

      /*
       * Keep painted area to detect the hovered tour and enlarge it with a margin to easier hit it
       */

      final Rectangle hoveredRect = new Rectangle(
            paintedDevX - _symbolHoveredMargin2,
            paintedDevY - _symbolHoveredMargin2,
            _symbolSize + _symbolHoveredMargin,
            _symbolSize + _symbolHoveredMargin);

      tile.allPainted_HoverRectangle.add(hoveredRect);
      tile.allPainted_HoverTourID.add(tourId);
      tile.allPainted_HoverSerieIndices.add(serieIndex);
   }

   /**
    * @param config
    * @param legendBounds
    * @param valueIndex
    *
    * @return Returns the position for the value according to the value index in the legend,
    *         {@link Integer#MIN_VALUE} when data are not initialized
    */
   public int getLegendValuePosition(final ColorProviderConfig config,
                                     final Rectangle legendBounds,
                                     final int valueIndex) {

      if (_dataSerie == null

            || valueIndex < 0
            || valueIndex >= _dataSerie.length

            // check legend provider type
            || _legendProvider instanceof IGradientColorProvider == false

      ) {
         return Integer.MIN_VALUE;
      }

      /*
       * ONLY VERTICAL LEGENDS ARE SUPPORTED
       */

      final float dataValue = _dataSerie[valueIndex];

      int valuePosition = 0;

      final MapUnits mapUnits = ((IGradientColorProvider) _legendProvider).getMapUnits(config);

//    final Integer unitFactor = config.unitFactor;
//    dataValue /= unitFactor;

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

   private Color getTourColor(final TourData tourData,
                              final int serieIndex,
                              final boolean isBorder,
                              final boolean isDrawLine,
                              final boolean isGeoCompareRefTour,
                              final boolean isInRefTourPart) {

      if (_dataSerie == null) {
         return null;
      }

      /*
       * Get border color
       */
      if (isBorder && _prefBorderType == Map2_Appearance.BORDER_TYPE_COLOR) {
         return _colorCache.getColor(_prefBorderRGB);
      }

      /*
       * Geo compare ref tour
       */
      if (isGeoCompareRefTour) {

         return _colorCache.getColor(isInRefTourPart
               ? _prefGeoCompare_CompartTourPart_RGB
               : _prefGeoCompare_RefTour_RGB);
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
    * Checks if an image bounds is within the tile. The image is above the image position and one
    * half to the left and right side
    *
    * @param imageBounds
    *           bounds of the image
    * @param devImagePosX
    *           x position for the image
    * @param devImagePosY
    *           y position for the image
    * @param tileSize
    *           width and height of the tile
    *
    * @return Returns <code>true</code> when the image is visible in the tile
    */
   private boolean isInTile_Bounds(final Rectangle imageBounds,
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

   private boolean isInTile_Tour(final TourData tourData,
                                 final MP mp,
                                 final int mapZoomLevel,
                                 final Tile tile,
                                 final int projectionHash) {

      // check if geo position is available
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      if (latitudeSerie != null && longitudeSerie != null) {

         // tiles are cached to optimize performance when multiple tours are selected
         IntHashSet tileHashes = tourData.getTileHashes_ForTours(projectionHash, mapZoomLevel);

         if (tileHashes == null) {

            // tile hashes are not yet cached, create them now

            tileHashes = setupTileHashes_Tour(
                  tourData,
                  mp,
                  mapZoomLevel,
                  latitudeSerie,
                  longitudeSerie,
                  projectionHash);
         }

         int tileHash = 15;
         tileHash = 35 * tileHash + tile.getX();
         tileHash = 35 * tileHash + tile.getY();

         if (tileHashes.contains(tileHash)) {

            // tour is in this tile

            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isPaintingNeeded(final Map2 map, final Tile tile) {

      final ArrayList<TourData> allTourData = Map2PainterConfig.getTourData();

      if (allTourData.isEmpty()) {
         return false;
      }

      if (_isImageAvailable == false) {
         createImages();
      }

      final MP mp = map.getMapProvider();
      final int mapZoomLevel = map.getZoom();
      final int projectionHash = mp.getProjection().getId().hashCode();

      if (Map2PainterConfig.isShowTours

            && allTourData.size() > 0

            && isPaintingNeeded_Tours(

                  allTourData,
                  mp,
                  mapZoomLevel,
                  tile,
                  projectionHash)) {

         return true;
      }

      return false;
   }

   private boolean isPaintingNeeded_Tours(final ArrayList<TourData> allTourData,
                                          final MP mp,
                                          final int mapZoomLevel,
                                          final Tile tile,
                                          final int projectionHash) {

      for (final TourData tourData : allTourData) {

         // check tour data
         if (tourData == null) {
            continue;
         }

         if (isInTile_Tour(tourData, mp, mapZoomLevel, tile, projectionHash)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Set the data serie which is painted
    *
    * @param tourData
    */
   private void setDataSerie(final TourData tourData) {

      final IMapColorProvider legendProvider = Map2PainterConfig.getMapColorProvider();

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

      case Power:
         _dataSerie = tourData.getPowerSerie();
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

      case RunDyn_StepLength:
         _dataSerie = tourData.getRunDyn_StepLength();
         break;

      case HrZone:
         _dataSerie = tourData.pulseSerie;
         break;

      default:
         break;
      }
   }

   /**
    * Create tour tile hashes for all geo positions and the current zoom level and projection
    *
    * @param tourData
    * @param mp
    * @param mapZoomLevel
    * @param latitudeSerie
    * @param longitudeSerie
    * @param projectionHash
    *
    * @return
    */
   private IntHashSet setupTileHashes_Tour(final TourData tourData,
                                           final MP mp,
                                           final int mapZoomLevel,
                                           final double[] latitudeSerie,
                                           final double[] longitudeSerie,
                                           final int projectionHash) {

      final IntHashSet tileHashes = new IntHashSet();

      final int numSlices = latitudeSerie.length;
      final int tileSize = mp.getTileSize();

      for (int serieIndex = 0; serieIndex < numSlices; serieIndex++) {

         // convert lat/long into world pixels which depends on the map projection

         final Point worldPixelPos = mp.geoToPixel(
               new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
               mapZoomLevel);

         final int tileX = worldPixelPos.x / tileSize;
         final int tileY = worldPixelPos.y / tileSize;

         // create painting hash code
         int tileHash = 15;
         tileHash = 35 * tileHash + tileX;
         tileHash = 35 * tileHash + tileY;

         tileHashes.add(tileHash);
      }

      tourData.setTileHashes_ForTours(tileHashes, mapZoomLevel, projectionHash);

      return tileHashes;
   }

   /**
    * world pixels are not yet cached, create them now
    *
    * @param tourData
    * @param mp
    * @param mapZoomLevel
    * @param latitudeSerie
    * @param longitudeSerie
    * @param projectionHash
    *
    * @return
    */
   private Point[] setupWorldPixel_Tour(final TourData tourData,
                                        final MP mp,
                                        final int mapZoomLevel,
                                        final double[] latitudeSerie,
                                        final double[] longitudeSerie,
                                        final int projectionHash) {

      final int numSlices = latitudeSerie.length;

      final Point[] allTour_WorldPixelPos = new Point[numSlices];

      for (int serieIndex = 0; serieIndex < numSlices; serieIndex++) {

         // convert lat/long into world pixels which depends on the map projection

         final GeoPosition geoPosition = new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);

         allTour_WorldPixelPos[serieIndex] = mp.geoToPixel(geoPosition, mapZoomLevel);
      }

      tourData.setWorldPixelForTour(allTour_WorldPixelPos, mapZoomLevel, projectionHash);

      return allTour_WorldPixelPos;
   }

}
