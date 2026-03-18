/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.common.graphics;

import static java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_COLOR_RENDERING;
import static java.awt.RenderingHints.KEY_DITHERING;
import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_DITHER_DISABLE;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;

import java.awt.Graphics2D;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import java.util.Map;

import net.tourbook.common.util.ImageUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

/**
 * A rasterizer implementation for converting SVG data into rasterized images.
 * This class uses the third party library JSVG for the raterization of SVG
 * images.
 *
 * Original Source: org.eclipse.swt.svg.JSVGRasterizer
 */
public class SVGRasterizerMT {

   private static final SVGLoader        SVG_LOADER      = new SVGLoader();

   private final static Map<Key, Object> RENDERING_HINTS = Map.of(

// SET_FORMATTING_OFF

         KEY_ANTIALIASING,          VALUE_ANTIALIAS_ON,
         KEY_ALPHA_INTERPOLATION,   VALUE_ALPHA_INTERPOLATION_QUALITY,
         KEY_COLOR_RENDERING,       VALUE_COLOR_RENDER_QUALITY,
         KEY_DITHERING,             VALUE_DITHER_DISABLE,
         KEY_FRACTIONALMETRICS,     VALUE_FRACTIONALMETRICS_ON,
         KEY_INTERPOLATION,         VALUE_INTERPOLATION_BICUBIC,
         KEY_RENDERING,             VALUE_RENDER_QUALITY,
         KEY_STROKE_CONTROL,        VALUE_STROKE_PURE,
         KEY_TEXT_ANTIALIASING,     VALUE_TEXT_ANTIALIAS_ON

// SET_FORMATTING_ON
   );

   private int calculateTargetHeight(final float scalingFactor, final FloatSize sourceImageSize) {

      final double sourceImageHeight = sourceImageSize.getHeight();

      return (int) Math.round(sourceImageHeight * scalingFactor);
   }

   private int calculateTargetWidth(final float scalingFactor, final FloatSize sourceImageSize) {

      final double sourceImageWidth = sourceImageSize.getWidth();

      return (int) Math.round(sourceImageWidth * scalingFactor);
   }

   private Graphics2D configureRenderingOptions(final float widthScalingFactor,
                                                final float heightScalingFactor,
                                                final BufferedImage image) {

      final Graphics2D g = image.createGraphics();

      g.setRenderingHints(RENDERING_HINTS);
      g.scale(widthScalingFactor, heightScalingFactor);

      return g;
   }

   private ImageData convertToSWTImageData(final BufferedImage rasterizedImage) {

      final int width = rasterizedImage.getWidth();
      final int height = rasterizedImage.getHeight();

      final int[] pixels = ((DataBufferInt) rasterizedImage.getRaster().getDataBuffer()).getData();

      final PaletteData paletteData = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
      final ImageData imageData = new ImageData(width, height, 24, paletteData);

      int index = 0;

      for (int y = 0; y < imageData.height; y++) {
         for (int x = 0; x < imageData.width; x++) {

            final int alpha = (pixels[index] >> 24) & 0xFF;

            imageData.setAlpha(x, y, alpha);
            imageData.setPixel(x, y, pixels[index++] & 0x00FFFFFF);
         }
      }

      return imageData;
   }

   private SVGDocument loadAndValidateSVG(final InputStream inputStream) {

      final SVGDocument svgDocument = SVG_LOADER.load(inputStream, null, LoaderContext.createDefault());

      if (svgDocument == null) {
         SWT.error(SWT.ERROR_INVALID_IMAGE);
      }

      return svgDocument;
   }

   public ImageData rasterizeSVG(final InputStream inputStream, final int zoom) {

      if (zoom < 0) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }

      final SVGDocument svgDocument = loadAndValidateSVG(inputStream);
      final BufferedImage rasterizedImage = renderSVG(svgDocument, zoom);

      return convertToSWTImageData(rasterizedImage);
   }

   public ImageData rasterizeSVG(final InputStream inputStream, final int width, final int height) {

      final SVGDocument svgDocument = loadAndValidateSVG(inputStream);
      final BufferedImage rasterizedImage = renderSVG(svgDocument, width, height);

      return convertToSWTImageData(rasterizedImage);
   }

   public ImageData rasterizeSVGWithMaxSize(final InputStream inputStream, final int maxSize) {

      final SVGDocument svgDocument = loadAndValidateSVG(inputStream);

      final FloatSize svgSize = svgDocument.size();
      
      final int imageWidth = (int) svgSize.width;
      final int imageHeight = (int) svgSize.height;

      final org.eclipse.swt.graphics.Point bestSize = ImageUtils.getBestSize(imageWidth, imageHeight, maxSize, maxSize, false);

      final BufferedImage rasterizedImage = renderSVG(svgDocument, bestSize.x, bestSize.y);

      return convertToSWTImageData(rasterizedImage);
   }

   private BufferedImage renderSVG(final SVGDocument svgDocument, final int zoom) {

      final FloatSize sourceImageSize = svgDocument.size();
      final float scalingFactor = zoom / 100.0f;

      final int targetImageWidth = calculateTargetWidth(scalingFactor, sourceImageSize);
      final int targetImageHeight = calculateTargetHeight(scalingFactor, sourceImageSize);

      return renderSVG(svgDocument, targetImageWidth, targetImageHeight);
   }

   private BufferedImage renderSVG(final SVGDocument svgDocument, final int width, final int height) {

      if (width <= 0 || height <= 0) {
         SWT.error(SWT.ERROR_INVALID_ARGUMENT);
      }

      final BufferedImage awtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      final FloatSize documentSize = svgDocument.size();

      final float widthScalingFactor = width / documentSize.width;
      final float heightScalingFactor = height / documentSize.height;

      final Graphics2D g = configureRenderingOptions(widthScalingFactor, heightScalingFactor, awtImage);
      {
         svgDocument.render(null, g);
      }
      g.dispose();

      return awtImage;
   }
}
