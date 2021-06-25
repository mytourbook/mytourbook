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

import java.util.HashMap;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.map2.view.TourMapPainter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
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
    * @return
    */
   Image drawColorDefinitionImage(final ColorDefinition colorDefinition,
                                  final int numHorizontalImages,
                                  final boolean isRecreateTourTypeImages,
                                  final Color defaultBackgroundColor) {

      final Display display = Display.getCurrent();

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

      Image colorDefinitionImage = _imageCache.get(colorDefinitionId);

      if (colorDefinitionImage == null || colorDefinitionImage.isDisposed()) {

         final GraphColorItem[] graphColors = colorDefinition.getGraphColorItems();

         final int borderSize = 0;
         final int imageSpacing = GRAPH_COLOR_SPACING;
         final int imageSize = _itemHeight - 2;

         colorDefinitionImage = new Image(
               display,
               (numHorizontalImages * imageSize) + ((numHorizontalImages - 1) * imageSpacing),
               imageSize);

         final GC gc = new GC(colorDefinitionImage);
         {
            // fill background
            gc.setBackground(defaultBackgroundColor);
            gc.fillRectangle(colorDefinitionImage.getBounds());

            for (int colorIndex = 0; colorIndex < graphColors.length; colorIndex++) {

               final int colorX = colorIndex * (imageSize + imageSpacing);
               final int colorY = 0;

               final int contentWidth = imageSize - 2 * borderSize;
               final int contentHeight = imageSize - 2 * borderSize;

               final Rectangle imageBounds = new Rectangle(
                     colorX,
                     colorY,
                     imageSize,
                     imageSize);

               final GraphColorItem graphColorItem = graphColors[colorIndex];

               if (graphColorItem.isMapColor()) {

                  // draw 2D map color

                  // tell the legend provider how to draw the legend
                  final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
                  colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

                  TourMapPainter.drawMap2_Legend(
                        gc,
                        imageBounds,
                        colorProvider);

               } else {

                  // draw graph color

                  final Color graphColor = getGraphColor(graphColorItem);

                  // draw border
//                  gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                  gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
                  gc.drawRectangle(
                        colorX,
                        colorY,
                        imageBounds.width - 1,
                        imageBounds.height - 1);

                  // draw graph color
                  gc.setBackground(graphColor);
                  gc.fillRectangle(
                        colorX + borderSize,
                        colorY + borderSize,
                        contentWidth,
                        contentHeight);
               }
            }
         }
         gc.dispose();

         _imageCache.put(colorDefinitionId, colorDefinitionImage);
      }

      return colorDefinitionImage;
   }

   /**
    * Draw graph and map colors into the defintion image.
    *
    * @param colorDefinition
    * @param numHorizontalImages
    * @param isRecreateTourTypeImages
    * @param defaultBackgroundColor
    * @return
    */
   Image drawGraphColorImage(final GraphColorItem graphColorItem,
                             final int numHorizontalImages,
                             final boolean isRecreateTourTypeImages,
                             final Color backgroundColor) {

      final Display display = Display.getCurrent();

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

      Image colorImage = _imageCache.get(colorId);

      if (colorImage == null || colorImage.isDisposed()) {

         final int borderSize = 0;

         final int imageSize = _itemHeight - 2;
         final int imageSpacing = GRAPH_COLOR_SPACING;

         final int imageWidth = (numHorizontalImages * imageSize) + ((numHorizontalImages - 1) * imageSpacing);
         final int imageHeight = imageSize;

         colorImage = new Image(
               display,
               imageWidth,
               imageHeight);

         final Rectangle drawableBounds = new Rectangle(
               0,
               0,
               imageWidth,
               imageHeight);

         final GC gc = new GC(colorImage);
         {
            if (graphColorItem.isMapColor()) {

               // draw map image

               /*
                * tell the legend provider with which color the legend should be painted
                */
               final IGradientColorProvider colorProvider = _colorTreeViewer.getMapLegendColorProvider();
               colorProvider.setColorProfile(graphColorItem.getColorDefinition().getMap2Color_New());

               TourMapPainter.drawMap2_Legend(
                     gc,
                     drawableBounds,
                     colorProvider);

            } else {

               // draw graph color image

               // fill with default background color that the dark theme
               // do not show a white rectangle before the graph color
               gc.setBackground(backgroundColor);
               gc.fillRectangle(colorImage.getBounds());

               final Color graphColor = getGraphColor(graphColorItem);

               // draw border
//               gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
               gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
               gc.drawRectangle(
                     drawableBounds.x,
                     drawableBounds.y,
                     drawableBounds.width - 1,
                     drawableBounds.height - 1);

               // draw graph color
               gc.setBackground(graphColor);
               gc.fillRectangle(
                     drawableBounds.x + borderSize,
                     drawableBounds.y + borderSize,
                     drawableBounds.width - 2 * borderSize - 0,
                     drawableBounds.height - 2 * borderSize - 0);

            }
         }
         gc.dispose();

         _imageCache.put(colorId, colorImage);
      }

      return colorImage;
   }

   /**
    * @param display
    * @param graphColor
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
