/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.render.TextRenderer;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLTextRenderer;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.util.WWUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapUnits;
import net.tourbook.map.MapUtils;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.view.Map3Manager;

/**
 * Part of this code is copied from: gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend
 */
public class TourLegendLayer extends RenderableLayer {

   public static final String             MAP3_LAYER_ID = "TourLegendLayer"; //$NON-NLS-1$

   private static final Font              DEFAULT_FONT  = UI.AWT_DIALOG_FONT;
   private static final Color             DEFAULT_COLOR = Color.WHITE;

   private IMapColorProvider              _colorProvider;

   private Renderable                     _legendRenderable;
   private ScreenImage                    _legendImage;
   private Iterable<? extends Renderable> _labels;

   private Point                          _legendImageLocation;

   private boolean                        _isVisible    = false;
   private boolean                        _isTourAvailable;
   private boolean                        _canDisplayLegend;

   public interface LabelAttributes {

      Color getColor();

      Font getFont();

      Point2D getOffset();

      String getText();

      double getValue();
   }

   protected static class LabelRenderable implements Renderable {

      protected final OrderedLabel orderedLabel;

      public LabelRenderable(final TourLegendLayer legend,
                             final LabelAttributes attr,
                             final double x,
                             final double y,
                             final String halign,
                             final String valign) {

         this.orderedLabel = new OrderedLabel(legend, attr, x, y, halign, valign);
      }

      @Override
      public void render(final DrawContext dc) {
         dc.addOrderedRenderable(this.orderedLabel);
      }
   }

   protected static class OrderedLabel implements OrderedRenderable {

      protected final TourLegendLayer legend;
      protected final LabelAttributes attr;
      protected final double          x;
      protected final double          y;
      protected final String          halign;
      protected final String          valign;

      public OrderedLabel(final TourLegendLayer legend,
                          final LabelAttributes attr,
                          final double x,
                          final double y,
                          final String halign,
                          final String valign) {
         this.legend = legend;
         this.attr = attr;
         this.x = x;
         this.y = y;
         this.halign = halign;
         this.valign = valign;
      }

      @Override
      public double getDistanceFromEye() {
         return 0;
      }

      @Override
      public void pick(final DrawContext dc, final Point pickPoint) {
         // Intentionally left blank.
      }

      @Override
      public void render(final DrawContext dc) {
         this.legend.drawLabel(dc, this.attr, this.x, this.y, this.halign, this.valign);
      }
   }

   public TourLegendLayer() {

      _legendImage = new ScreenImage();

      _legendRenderable = createRenderable(this);

      setPickEnabled(false);
   }

   private static TourLegendLayer.LabelAttributes createLegendLabelAttributes(final double value,
                                                                              final String text,
                                                                              final Font font,
                                                                              final Color color,
                                                                              final double xOffset,
                                                                              final double yOffset) {
      return new TourLegendLayer.LabelAttributes() {

         @Override
         public Color getColor() {
            return color;
         }

         @Override
         public Font getFont() {
            return font;
         }

         @Override
         public Point2D getOffset() {
            return new Point2D.Double(xOffset, yOffset);
         }

         @Override
         public String getText() {
            return text;
         }

         @Override
         public double getValue() {
            return value;
         }
      };
   }

   private Iterable<? extends Renderable> createColorGradientLegendLabels(final int width,
                                                                          final int height,
                                                                          final double minValue,
                                                                          final double maxValue,
                                                                          final Iterable<? extends LabelAttributes> labels) {
      final ArrayList<Renderable> list = new ArrayList<>();

      if (labels != null) {
         for (final LabelAttributes attr : labels) {

            if (attr == null) {
               continue;
            }

            final double factor = WWMath.computeInterpolationFactor(attr.getValue(), minValue, maxValue);

            final double y = (1d - factor) * (height - 1);

            list.add(new LabelRenderable(this, attr, width, y, AVKey.LEFT, AVKey.CENTER));
         }
      }

      return list;
   }

   private Renderable createRenderable(final TourLegendLayer tourLegendLayer) {

      return new Renderable() {

         @Override
         public void render(final DrawContext dc) {
            tourLegendLayer.render(dc);
         }
      };
   }

   private void drawLabel(final DrawContext dc,
                          final LabelAttributes attr,
                          double x,
                          double y,
                          final String halign,
                          final String valign) {

      final String text = attr.getText();
      if (WWUtil.isEmpty(text)) {
         return;
      }

      Font font = attr.getFont();
      if (font == null) {
         font = DEFAULT_FONT;
      }

      Color color = DEFAULT_COLOR;
      if (attr.getColor() != null) {
         color = attr.getColor();
      }

      final Point location = this.getScreenLocation(dc);
      if (location != null) {
         x += location.getX() - _legendImage.getImageWidth(dc) / 2;
         y += location.getY() - _legendImage.getImageHeight(dc) / 2;
      }

      final Point2D offset = attr.getOffset();
      if (offset != null) {
         x += offset.getX();
         y += offset.getY();
      }

      final TextRenderer tr = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font);
      if (tr == null) {
         return;
      }

      final Rectangle2D bounds = tr.getBounds(text);
      if (bounds != null) {
         if (AVKey.CENTER.equals(halign)) {
            x += -(bounds.getWidth() / 2d);
         }
         if (AVKey.RIGHT.equals(halign)) {
            x += -bounds.getWidth();
         }

         if (AVKey.CENTER.equals(valign)) {
            y += (bounds.getHeight() + bounds.getY());
         }
         if (AVKey.TOP.equals(valign)) {
            y += bounds.getHeight();
         }
      }

      final Rectangle viewport = dc.getView().getViewport();
      tr.beginRendering(viewport.width, viewport.height);
      try {
         final double yInGLCoords = viewport.height - y - 1;

         // Draw the text outline, in a contrasting color.
         tr.setColor(WWUtil.computeContrastingColor(color));
         tr.draw(text, (int) x + 1, (int) yInGLCoords - 1);

         // Draw the text over its outline, in the specified color.
         tr.setColor(color);
         tr.draw(text, (int) x, (int) yInGLCoords);

      } finally {
         tr.endRendering();
      }
   }

   private Point getScreenLocation(final DrawContext dc) {
      return _legendImage.getScreenLocation(dc);
   }

   private void hideLegend() {

      _isVisible = false;

      removeRenderable(_legendRenderable);
   }

   @Override
   public void render(final DrawContext dc) {

      if (dc == null) {
         final String message = Logging.getMessage("nullValue.DrawContextIsNull"); //$NON-NLS-1$
         Logging.logger().severe(message);
         throw new IllegalArgumentException(message);
      }

      if (_isVisible == false) {
         return;
      }

      if (isEnabled() == false) {
         return;
      }

      /*
       * Start rendering
       */

      _legendImage.render(dc);

      if (!dc.isPickingMode() && _labels != null) {

         for (final Renderable renderable : _labels) {
            if (renderable != null) {
               renderable.render(dc);
            }
         }
      }
   }

   public void resizeLegendImage() {

      /*
       * check if the legend size must be adjusted
       */
      final Object legendImage = _legendImage.getImageSource();
      if (legendImage == null) {
         // legend image is not yet created
         return;
      }

      // check if legend is displayed
//		if ((_isTourOrWayPoint == false) || (_isShowTour == false) || (_isShowLegend == false)) {
//			return;
//		}

      updateLegendImage(_isTourAvailable);
   }

   public void setColorProvider(final IMapColorProvider colorProvider) {

      _colorProvider = colorProvider;

      if (_colorProvider instanceof IGradientColorProvider) {

         _canDisplayLegend = true;

         showLegend();

      } else {

         // other color providers are not yet supported

         _canDisplayLegend = false;

         hideLegend();
      }
   }

   @Override
   public void setOpacity(final double opacity) {

      _legendImage.setOpacity(opacity);

      super.setOpacity(opacity);
   }

   private void showLegend() {

      // prevent to add legend image more than once
      if (_canDisplayLegend && _isVisible == false) {

         _isVisible = true;

         addRenderable(_legendRenderable);
      }
   }

   /**
    * Creates a new legend image.
    *
    * @param isTourAvailable
    * @param isUpdateMinMax
    * @param mapColorProvider
    */
   public void updateLegendImage(final boolean isTourAvailable) {

      _isTourAvailable = isTourAvailable;

      if (isTourAvailable) {

         showLegend();

      } else {

         // a tour is not displayed, hide legend

         hideLegend();
      }

      IGradientColorProvider gradientColorProvider;

      if (_colorProvider instanceof IGradientColorProvider) {

         gradientColorProvider = (IGradientColorProvider) _colorProvider;

      } else {

         // other color provider are not yet supported

         return;
      }

      final int mapHeight = Map3Manager.getMap3View().getMapSize().height;

      final int legendMinHeight = Math.min(IMapColorProvider.DEFAULT_LEGEND_HEIGHT, mapHeight - 40);
      final int legendHeight = Math.max(40, legendMinHeight);

      final BufferedImage image = new BufferedImage(
            IMapColorProvider.DEFAULT_LEGEND_GRAPHIC_WIDTH,
            legendHeight,
            BufferedImage.TYPE_4BYTE_ABGR);

      final int legendWidth = image.getWidth();

      final Graphics2D g2d = image.createGraphics();

      try {

         final boolean isDataAvailable = MapUtils.configureColorProvider(
               Map3Manager.getMap3View().getAllTours(),
               gradientColorProvider,
               ColorProviderConfig.MAP3_TOUR,
               legendHeight);

         if (isDataAvailable) {

            TourMapPainter.drawMap3_Legend(
                  g2d,
                  gradientColorProvider,
                  ColorProviderConfig.MAP3_TOUR,
                  legendWidth,
                  legendHeight,
                  UI.IS_DARK_THEME,
                  false // no unit shadow
            );
         }

      } finally {
         g2d.dispose();
      }

      _legendImage.setOpacity(getOpacity());
      _legendImage.setImageSource(image);

      /*
       * set legend position at left/bottom
       */
      final int devXCenter = 5 + (int) (IMapColorProvider.DEFAULT_LEGEND_GRAPHIC_WIDTH / 2.0);
      final int devYCenter = (int) (mapHeight - (legendHeight / 2.0)) - 30;

      _legendImageLocation = new Point(devXCenter, devYCenter);
      _legendImage.setScreenLocation(_legendImageLocation);

      final List<TourLegendLabel> legendLabels = TourMapPainter.getMap3_LegendLabels(
            legendHeight,
            gradientColorProvider,
            ColorProviderConfig.MAP3_TOUR);

      final ArrayList<LabelAttributes> labelAttributes = new ArrayList<>();

      for (final TourLegendLabel mapLegendLabel : legendLabels) {

         labelAttributes.add(createLegendLabelAttributes(
               mapLegendLabel.legendValue,
               mapLegendLabel.legendText,
               DEFAULT_FONT,
               Color.WHITE,
               5d,
               0d));
      }

      final MapUnits mapUnits = gradientColorProvider.getMapUnits(ColorProviderConfig.MAP3_TOUR);

      _labels = createColorGradientLegendLabels(
            legendWidth,
            legendHeight,
            mapUnits.legendMinValue,
            mapUnits.legendMaxValue,
            labelAttributes);
   }
}
