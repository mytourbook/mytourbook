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
package net.tourbook.tourType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.ImageConverter;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

/**
 * Tour type images are painted with AWT because it has better antialiasing with transparency than
 * SWT.
 */
public class TourTypeImage {

   private static final String                           TOUR_TYPE_PREFIX        = "tourType";                //$NON-NLS-1$
   private static final String                           POSTFIX_TO_FORCE_UPDATE = "POSTFIX_TO_FORCE_UPDATE"; //$NON-NLS-1$

   private final static HashMap<String, Image>           _imageCache             = new HashMap<>();
   private final static HashMap<String, ImageDescriptor> _imageCacheDescriptor   = new HashMap<>();
   private final static HashMap<String, Boolean>         _dirtyImages            = new HashMap<>();

   private static String createImageCacheKey(final long typeId) {

      return TOUR_TYPE_PREFIX + typeId;
   }

   private static Image createTourTypeImage(final long typeId, final String colorId, final Image existingImage) {

      final Image swtTourTypeImage = createTourTypeImage_Create(typeId, existingImage);

      // keep image in cache
      _imageCache.put(colorId, swtTourTypeImage);

      return swtTourTypeImage;
   }

   private static Image createTourTypeImage_Create(final long typeId, final Image existingImageSWT) {

      final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;

      final BufferedImage awtImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_4BYTE_ABGR);

      final Graphics2D g2d = awtImage.createGraphics();

// SET_FORMATTING_OFF

      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,          RenderingHints.VALUE_ANTIALIAS_ON);
//      g2d.setRenderingHint(RenderingHints.KEY_DITHERING,             RenderingHints.VALUE_DITHER_ENABLE);
//      g2d.setRenderingHint(RenderingHints.KEY_RENDERING,             RenderingHints.VALUE_RENDER_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST,     100);
//      g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,     RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//      g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,   RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,       RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//      g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,        RenderingHints.VALUE_STROKE_PURE);

// SET_FORMATTING_ON

      try {

         drawTourTypeImage(typeId, g2d);

         final Image newImageSWT = ImageConverter.convertIntoSWT(awtImage);

         if (existingImageSWT == null) {

            return newImageSWT;

         } else {

            // draw into existing image -> return old SWT image

            final GC gc = new GC(existingImageSWT);
            {
               // cleanup old image -> this do not change the alpha data
               // a workaround may be to use AWT to draw the image
               gc.setBackground(ThemeUtil.getDefaultBackgroundColor_Table());
               gc.fillRectangle(existingImageSWT.getBounds());

               // draw new image into the old image
               gc.drawImage(newImageSWT, 0, 0);
            }
            newImageSWT.dispose();
            gc.dispose();

            return existingImageSWT;
         }

      } finally {
         g2d.dispose();
      }
   }

   /**
    * Dispose images
    */
   public static void dispose() {

      for (final Image image : _imageCache.values()) {
         image.dispose();
      }

      _imageCache.clear();
      _imageCacheDescriptor.clear();
   }

   /**
    * Dispose tour type images which were create to show the modified layout and border in
    * {@link #getTourTypeImage_New(long)}.
    */
   public static void disposeRecreatedImages() {

      // get all image id's which should be disposed
      final ArrayList<String> allDisposedImageIds = new ArrayList<>();

      for (final String imageId : _imageCache.keySet()) {

         if (imageId.endsWith(POSTFIX_TO_FORCE_UPDATE)) {
            allDisposedImageIds.add(imageId);
         }
      }

      // remove and dispose image
      for (final String imageId : allDisposedImageIds) {

         final Image image = _imageCache.remove(imageId);

         if (image != null) {
            image.dispose();
         }
      }
   }

   private static void drawTourTypeImage(final long typeId, final Graphics2D g2d) {

      if (typeId == TourType.IMAGE_KEY_DIALOG_SELECTION) {

         // create a default image

      } else if (typeId == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // make the image invisible
         return;
      }

      final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;
      final DrawingColorsAWT drawingColors = getTourTypeColors(typeId);

      drawTourTypeImage_Background(g2d, imageSize, drawingColors);
      drawTourTypeImage_Border(g2d, imageSize, drawingColors);
   }

   private static void drawTourTypeImage_Background(final Graphics2D g2d,
                                                    final int imageSize,
                                                    final DrawingColorsAWT drawingColors) {

      final TourTypeImageConfig imageConfig = TourTypeManager.getImageConfig();

      if (imageConfig.imageLayout == TourTypeLayout.NOTHING) {
         // nothing to do
         return;
      }

      final int imageSize2 = imageSize / 2;

      boolean isRectangle = false;
      boolean isOval = false;
      boolean isGradient = false;
      boolean isHorizontal = false;
      boolean isVertical = false;

      switch (imageConfig.imageLayout) {

      case RECTANGLE:
         isRectangle = true;
         break;

      case CIRCLE:
         isOval = true;
         break;

      case GRADIENT_HORIZONTAL:
         isRectangle = true;
         isGradient = true;
         isHorizontal = true;
         break;

      case GRADIENT_VERTICAL:
         isRectangle = true;
         isGradient = true;
         isVertical = true;
         break;

      case NOTHING:
         break;
      }

      if (isGradient) {

         final Color color1 = getColor(imageConfig.imageColor1, drawingColors);
         final Color color2 = getColor(imageConfig.imageColor2, drawingColors);

         if (isHorizontal) {

            g2d.setPaint(new GradientPaint(0, imageSize2, color1, imageSize, imageSize2, color2));

         } else if (isVertical) {

            g2d.setPaint(new GradientPaint(imageSize2, 0, color1, imageSize2, imageSize, color2));
         }

      } else {

         // no gradient

         final Color color = getColor(imageConfig.imageColor1, drawingColors);
         g2d.setColor(color);
      }

      if (isRectangle) {

         g2d.fillRect(0, 0, imageSize, imageSize);

      } else if (isOval) {

         g2d.fillOval(0, 0, imageSize, imageSize);
      }
   }

   private static void drawTourTypeImage_Border(final Graphics2D g2d,
                                                final int imageSize,
                                                final DrawingColorsAWT drawingColors) {

      final TourTypeImageConfig imageConfig = TourTypeManager.getImageConfig();
      final int borderWidth = imageConfig.borderWidth;

      if (borderWidth == 0) {
         // nothing to do
         return;
      }

      boolean isCircle = false;

      boolean isLeft = false;
      boolean isRight = false;
      boolean isTop = false;
      boolean isBottom = false;

      switch (imageConfig.borderLayout) {

      case BORDER_RECTANGLE:
         isLeft = true;
         isRight = true;
         isTop = true;
         isBottom = true;
         break;

      case BORDER_CIRCLE:
         isCircle = true;
         break;

      case BORDER_LEFT:
         isLeft = true;
         break;
      case BORDER_RIGHT:
         isRight = true;
         break;
      case BORDER_LEFT_RIGHT:
         isLeft = true;
         isRight = true;
         break;

      case BORDER_TOP:
         isTop = true;
         break;
      case BORDER_BOTTOM:
         isBottom = true;
         break;
      case BORDER_TOP_BOTTOM:
         isTop = true;
         isBottom = true;
         break;

      default:
         break;
      }

      final Color color1 = getColor(imageConfig.borderColor, drawingColors);

      if (isCircle) {

//         // draw debug border
//         g2d.setStroke(new BasicStroke(1));
//         g2d.setColor(Color.GRAY);
//         g2d.drawRect(0, 0, imageSize - 1, imageSize - 1);

         g2d.setStroke(new BasicStroke(borderWidth));
         g2d.setColor(color1);

         /*
          * Highly complicated formula that the oval outline has the correct size, needed some
          * experimenting time to get it.
          */
         final float ovalPos = (borderWidth / 2f) + 0.5f;
         final int ovalPosInt = (int) ovalPos;
         final float ovalSize = imageSize - ovalPosInt - borderWidth / 2f;
         final int ovalSizeInt = (int) ovalSize - 2;


         g2d.drawOval(
               ovalPosInt,
               ovalPosInt,
               ovalSizeInt,
               ovalSizeInt);

      } else if (isLeft || isRight || isTop || isBottom) {

         g2d.setColor(color1);

         if (isLeft) {

            g2d.fillRect(0, 0, borderWidth, imageSize);
         }

         if (isRight) {

            g2d.fillRect(imageSize - borderWidth, 0, borderWidth, imageSize);
         }

         if (isTop) {

            g2d.fillRect(0, 0, imageSize, borderWidth);
         }

         if (isBottom) {

            g2d.fillRect(0, imageSize - borderWidth, imageSize, borderWidth);
         }
      }

   }

   private static Color getColor(final TourTypeColor imageColor, final DrawingColorsAWT drawingColors) {

      switch (imageColor) {
      case COLOR_BRIGHT:
         return drawingColors.colorBright;

      case COLOR_LINE:
         return drawingColors.colorLine;

      case COLOR_DARK:
      default:
         return drawingColors.colorDark;
      }
   }

   /**
    * @param graphColor
    * @return return the color for the graph
    */
   private static DrawingColorsAWT getTourTypeColors(final long tourTypeId) {

      final DrawingColorsAWT drawingColors = new DrawingColorsAWT();
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      TourType colorTourType = null;

      for (final TourType tourType : tourTypes) {
         if (tourType.getTypeId() == tourTypeId) {
            colorTourType = tourType;
            break;
         }
      }

      if (tourTypeId == TourType.IMAGE_KEY_DIALOG_SELECTION
            || colorTourType == null
            || colorTourType.getTypeId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // tour type was not found use default color

         drawingColors.colorBright = java.awt.Color.YELLOW;
         drawingColors.colorDark = java.awt.Color.PINK;
         drawingColors.colorLine = java.awt.Color.DARK_GRAY;

      } else {

         final RGB rgbBright = colorTourType.getRGB_Gradient_Bright();
         final RGB rgbDark = colorTourType.getRGB_Gradient_Dark();
         final RGB rgbLine = colorTourType.getRGB_Line_Themed();

         drawingColors.colorBright = new java.awt.Color(rgbBright.red, rgbBright.green, rgbBright.blue);
         drawingColors.colorDark = new java.awt.Color(rgbDark.red, rgbDark.green, rgbDark.blue);
         drawingColors.colorLine = new java.awt.Color(rgbLine.red, rgbLine.green, rgbLine.blue);
      }

      return drawingColors;
   }

   /**
    * @param typeId
    * @return Returns an image which represents the tour type. This image must not be disposed,
    *         this is done when the app closes.
    */
   public static Image getTourTypeImage(final long typeId) {

      return getTourTypeImage(typeId, createImageCacheKey(typeId), false);
   }

   /**
    * @param typeId
    * @param imageCacheKey
    * @param isDisposeCachedImage
    * @return
    */
   public static Image getTourTypeImage(final long typeId, final String imageCacheKey, final boolean isDisposeCachedImage) {

      final Image cachedImage = _imageCache.get(imageCacheKey);

      // check if image is available
      if (cachedImage != null && cachedImage.isDisposed() == false) {

         // check if the image is dirty
         if (_dirtyImages.containsKey(imageCacheKey) == false) {

            // image is available and not dirty -> return valid image

            return cachedImage;

         } else {

            // image is available and dirty

            if (isDisposeCachedImage) {

               cachedImage.dispose();
            }
         }
      }

      // create image for the tour type

      if (cachedImage == null || cachedImage.isDisposed()) {

         return createTourTypeImage(typeId, imageCacheKey, null);

      } else {

         // old tour type image is available and not disposed but needs to be updated

         return createTourTypeImage(typeId, imageCacheKey, cachedImage);
      }
   }

   /**
    * Creates a new tour type image every time and disposes the old image.
    * <p>
    * With {@link #disposeRecreatedImages()} the newly created images can be disposed.
    *
    * @param typeId
    * @return
    */
   public static Image getTourTypeImage_New(final long typeId) {

      final String imageCacheKey = createImageCacheKey(typeId) + POSTFIX_TO_FORCE_UPDATE;

      return getTourTypeImage(typeId, imageCacheKey, true);
   }

   /**
    * The image descriptor is cached because the creation takes system resources and it's called
    * very often
    *
    * @param tourTypeId
    *           Tour type id
    * @return Returns image descriptor for the tour type id
    */
   public static ImageDescriptor getTourTypeImageDescriptor(final long tourTypeId) {

      final String imageCacheKey = createImageCacheKey(tourTypeId);
      final ImageDescriptor existingDescriptor = _imageCacheDescriptor.get(imageCacheKey);

      if (existingDescriptor != null) {
         return existingDescriptor;
      }

      final Image tourTypeImage = getTourTypeImage(tourTypeId);
      final ImageDescriptor newImageDesc = ImageDescriptor.createFromImage(tourTypeImage);

      _imageCacheDescriptor.put(imageCacheKey, newImageDesc);

      return newImageDesc;
   }

   /**
    * Set dirty state for all tour type images, images cannot be disposed because they are
    * displayed in the UI
    */
   public static void setTourTypeImagesDirty() {

      for (final String imageId : _imageCache.keySet()) {

         if (imageId.startsWith(TOUR_TYPE_PREFIX)) {
            _dirtyImages.put(imageId, true);
         }
      }

      _imageCacheDescriptor.clear();
   }

}
