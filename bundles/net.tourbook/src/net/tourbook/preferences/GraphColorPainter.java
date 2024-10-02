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
package net.tourbook.preferences;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.map2.view.TourMapPainter;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

public class GraphColorPainter {

   static final int                     GRAPH_COLOR_SPACING = 5;

   private final IColorTreeViewer       _colorTreeViewer;

   private final HashMap<String, Image> _imageCache         = new HashMap<>();
   private final HashMap<String, Color> _colorCache         = new HashMap<>();

   private final int                    _itemHeight;

   private String                       _recreateColorId;
   private String                       _recreateColorDefinitionId;

   /**
    * @param colorTree
    */
   GraphColorPainter(final IColorTreeViewer colorTreeViewer) {

      _colorTreeViewer = colorTreeViewer;

      final Tree tree = _colorTreeViewer.getTreeViewer().getTree();

      _itemHeight = tree.getItemHeight();
   }

   void disposeAllResources() {

      for (final Image image : _imageCache.values()) {
         image.dispose();
      }
      _imageCache.clear();

      _colorCache.clear();
   }

   /**
    * Draw graph and map colors into the defintion image.
    *
    * @param colorDefinition
    * @param numHorizontalImages
    * @param isRecreateTourTypeImages
    * @param defaultBackgroundColor
    *
    * @return
    */
   Image drawColorDefinitionImage(final ColorDefinition colorDefinition,
                                  final int numHorizontalImages,
                                  final boolean isRecreateTourTypeImages,
                                  final Color defaultBackgroundColor) {

      final String colorDefinitionId = colorDefinition.getColorDefinitionId();

      if (isRecreateTourTypeImages || colorDefinitionId.equals(_recreateColorDefinitionId)) {

         /*
          * Dispose image for the color definition
          */

         _recreateColorDefinitionId = null;

         final Image image = _imageCache.remove(colorDefinitionId);
         if (image != null && !image.isDisposed()) {
            image.dispose();
         }
      }

      Image swtImage = _imageCache.get(colorDefinitionId);

      if (swtImage == null || swtImage.isDisposed()) {

         final GraphColorItem[] graphColors = colorDefinition.getGraphColorItems();

         final int imageSpacing = GRAPH_COLOR_SPACING;
         final int iconSize = _itemHeight - 2;

         final int imageWidth = (numHorizontalImages * iconSize) + ((numHorizontalImages - 1) * imageSpacing);
         final int imageHeight = iconSize;

         final int imageWidthScaled = (int) (imageWidth * UI.HIDPI_SCALING);
         final int imageHeightScaled = (int) (imageHeight * UI.HIDPI_SCALING);

         final BufferedImage awtImage = new BufferedImage(imageWidthScaled, imageHeightScaled, BufferedImage.TYPE_4BYTE_ABGR);
         final Graphics2D g2d = awtImage.createGraphics();

         try {

            // fill background
            g2d.setColor(ColorUtil.convertSWTColor_into_AWTColor(defaultBackgroundColor));
            g2d.fillRect(0, 0, imageWidthScaled, imageHeightScaled);

            for (int colorIndex = 0; colorIndex < graphColors.length; colorIndex++) {

               final int colorX = (int) (colorIndex * (iconSize + imageSpacing) * UI.HIDPI_SCALING);

               final int contentWidth = (int) (iconSize * UI.HIDPI_SCALING);
               final int contentHeight = (int) (iconSize * UI.HIDPI_SCALING);

               final GraphColorItem graphColorItem = graphColors[colorIndex];

               if (graphColorItem.isMapColor()) {

                  // draw 2D map color

                  final Rectangle iconBounds = new Rectangle(
                        colorX,
                        0,
                        contentWidth,
                        contentHeight);

                  // tell the legend provider how to draw the legend
                  final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
                  colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

                  TourMapPainter.drawMap2_Legend_GradientColors_AWT(
                        g2d,
                        iconBounds,
                        colorProvider);

               } else {

                  // draw graph color

                  final Color graphColor = getGraphColor(graphColorItem);

                  // draw graph color
                  g2d.setColor(ColorUtil.convertSWTColor_into_AWTColor(graphColor));
                  g2d.fillRect(
                        colorX,
                        0,
                        contentWidth,
                        contentHeight);
               }
            }

         } finally {

            g2d.dispose();
         }

         swtImage = new Image(Display.getCurrent(), new NoAutoScalingImageDataProvider(awtImage));

         _imageCache.put(colorDefinitionId, swtImage);
      }

      return swtImage;
   }

   /**
    * Draw graph and map colors into the defintion image.
    *
    * @param colorDefinition
    * @param numHorizontalImages
    * @param isRecreateTourTypeImages
    * @param defaultBackgroundColor
    *
    * @return
    */
   Image drawGraphColorImage(final GraphColorItem graphColorItem,
                             final int numHorizontalImages,
                             final boolean isRecreateTourTypeImages,
                             final Color backgroundColor) {

      final String colorId = graphColorItem.getColorId();

      if (isRecreateTourTypeImages || colorId.equals(_recreateColorId)) {

         /*
          * Dispose graph color image/color
          */

         _recreateColorId = null;

         final Image image = _imageCache.remove(colorId);
         if (image != null && !image.isDisposed()) {
            image.dispose();
         }

         _colorCache.remove(colorId);
      }

      Image swtImage = _imageCache.get(colorId);

      if (swtImage == null || swtImage.isDisposed()) {

         final int imageSize = _itemHeight - 2;
         final int imageSpacing = GRAPH_COLOR_SPACING;

         final int imageWidth = (numHorizontalImages * imageSize) + ((numHorizontalImages - 1) * imageSpacing);
         final int imageHeight = imageSize;

         final int imageWidthScaled = (int) (imageWidth * UI.HIDPI_SCALING);
         final int imageHeightScaled = (int) (imageHeight * UI.HIDPI_SCALING);

         final BufferedImage awtImage = new BufferedImage(imageWidthScaled, imageHeightScaled, BufferedImage.TYPE_4BYTE_ABGR);

         final Graphics2D g2d = awtImage.createGraphics();
         try {

            if (graphColorItem.isMapColor()) {

               // draw map image

               /*
                * Tell the legend provider with which color the legend should be painted
                */
               final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
               colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

               TourMapPainter.drawMap_Legend_AWT(g2d,
                     colorProvider,
                     ColorProviderConfig.MAP2,
                     imageWidthScaled,
                     imageHeightScaled,
                     false, // isVertical
                     false, // isDrawUnits
                     false, // isDarkBackground,
                     false // isDrawUnitShadow
               );

            } else {

               // draw graph color image

               // fill with default background color that the dark theme
               // do not show a white rectangle before the graph color
               g2d.setColor(ColorUtil.convertSWTColor_into_AWTColor(backgroundColor));
               g2d.fillRect(0, 0, imageWidthScaled, imageHeightScaled);

               // draw graph color
               final Color graphColor = getGraphColor(graphColorItem);
               g2d.setColor(ColorUtil.convertSWTColor_into_AWTColor(graphColor));
               g2d.fillRect(0, 0, imageWidthScaled, imageHeightScaled);
            }

         } finally {
            g2d.dispose();
         }

         swtImage = new Image(Display.getCurrent(), new NoAutoScalingImageDataProvider(awtImage));

         _imageCache.put(colorId, swtImage);
      }

      return swtImage;
   }

   /**
    * @param display
    * @param graphColor
    *
    * @return return the {@link Color} for the graph
    */
   private Color getGraphColor(final GraphColorItem graphColor) {

      final String colorId = graphColor.getColorId();

      Color imageColor = _colorCache.get(colorId);

      if (imageColor == null) {

         imageColor = new Color(graphColor.getRGB());

         _colorCache.put(colorId, imageColor);
      }

      return imageColor;
   }

   public void invalidateResources(final String colorId, final String colorDefinitionId) {

      _recreateColorId = colorId;
      _recreateColorDefinitionId = colorDefinitionId;
   }

}
