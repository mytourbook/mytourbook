/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils.debug;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

import javax.swing.JComponent;

/**
 * Debugging-related static utility methods
 */
public class Debug {
   private Debug() {
      // shouldn't be instantiated
   }

   public static String bufferedImageTypeToString(final int type) {
      return switch (type) {
      case BufferedImage.TYPE_3BYTE_BGR      -> "3BYTE_BGR";
      case BufferedImage.TYPE_4BYTE_ABGR     -> "4BYTE_ABGR";
      case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE";
      case BufferedImage.TYPE_BYTE_BINARY    -> "BYTE_BINARY";
      case BufferedImage.TYPE_BYTE_GRAY      -> "BYTE_GRAY";
      case BufferedImage.TYPE_BYTE_INDEXED   -> "BYTE_INDEXED";
      case BufferedImage.TYPE_CUSTOM         -> "CUSTOM";
      case BufferedImage.TYPE_INT_ARGB       -> "INT_ARGB";
      case BufferedImage.TYPE_INT_ARGB_PRE   -> "INT_ARGB_PRE";
      case BufferedImage.TYPE_INT_BGR        -> "INT_BGR";
      case BufferedImage.TYPE_INT_RGB        -> "INT_RGB";
      case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB";
      case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB";
      case BufferedImage.TYPE_USHORT_GRAY    -> "USHORT_GRAY";
      default                                -> "unrecognized (" + type + ")";
      };
   }

   static String colorSpaceTypeToString(final int type) {
      return switch (type) {
      case ColorSpace.TYPE_2CLR  -> "2CLR";
      case ColorSpace.TYPE_3CLR  -> "3CLR";
      case ColorSpace.TYPE_4CLR  -> "4CLR";
      case ColorSpace.TYPE_5CLR  -> "5CLR";
      case ColorSpace.TYPE_6CLR  -> "6CLR";
      case ColorSpace.TYPE_7CLR  -> "7CLR";
      case ColorSpace.TYPE_8CLR  -> "8CLR";
      case ColorSpace.TYPE_9CLR  -> "9CLR";
      case ColorSpace.TYPE_ACLR  -> "ACLR";
      case ColorSpace.TYPE_BCLR  -> "BCLR";
      case ColorSpace.TYPE_CCLR  -> "CCLR";
      case ColorSpace.TYPE_CMY   -> "CMY";
      case ColorSpace.TYPE_CMYK  -> "CMYK";
      case ColorSpace.TYPE_DCLR  -> "DCLR";
      case ColorSpace.TYPE_ECLR  -> "ECLR";
      case ColorSpace.TYPE_FCLR  -> "FCLR";
      case ColorSpace.TYPE_GRAY  -> "GRAY";
      case ColorSpace.TYPE_HLS   -> "HLS";
      case ColorSpace.TYPE_HSV   -> "HSV";
      case ColorSpace.TYPE_Lab   -> "Lab";
      case ColorSpace.TYPE_Luv   -> "Luv";
      case ColorSpace.TYPE_RGB   -> "RGB";
      case ColorSpace.TYPE_XYZ   -> "XYZ";
      case ColorSpace.TYPE_YCbCr -> "YCbCr";
      case ColorSpace.TYPE_Yxy   -> "Yxy";
      default                    -> "unrecognized (" + type + ")";
      };
   }

   public static String dataBufferTypeToString(final int type) {
      return switch (type) {
      case DataBuffer.TYPE_BYTE      -> "BYTE";
      case DataBuffer.TYPE_USHORT    -> "USHORT";
      case DataBuffer.TYPE_SHORT     -> "SHORT";
      case DataBuffer.TYPE_INT       -> "INT";
      case DataBuffer.TYPE_FLOAT     -> "FLOAT";
      case DataBuffer.TYPE_DOUBLE    -> "DOUBLE";
      case DataBuffer.TYPE_UNDEFINED -> "UNDEFINED";
      default                        -> "unrecognized (" + type + ")";
      };
   }

   // Color's toString doesn't include the alpha
   public static String debugColor(final Color c) {
      return String.format("r=%d,g=%d,b=%d,a=%d",
            c.getRed(),
            c.getGreen(),
            c.getBlue(),
            c.getAlpha());
   }

   public static String debugJComponent(final JComponent c) {
      return String.format("""
                           size = %s
                           preferredSize = %s
                           minimumSize = %s
                           maximumSize = %s
                           insets = %s
                           border = %s
                           border insets = %s
                           doubleBuffered = %s
                           """,
            dimensionAsString(c.getSize()),
            dimensionAsString(c.getPreferredSize()),
            dimensionAsString(c.getMinimumSize()),
            dimensionAsString(c.getMaximumSize()),
            c.getInsets().toString(),
            c.getBorder().toString(),
            c.getBorder().getBorderInsets(c).toString(),
            c.isDoubleBuffered());
   }

   private static String dimensionAsString(final Dimension d) {
      return d.width + "x" + d.height;
   }

   public static boolean isBGR(final ColorModel cm) {
      if (cm instanceof final DirectColorModel dcm &&
            cm.getTransferType() == DataBuffer.TYPE_INT) {

         return dcm.getRedMask() == 0x00_00_00_FF
               && dcm.getGreenMask() == 0x00_00_FF_00
               && dcm.getBlueMask() == 0x00_FF_00_00
               && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF_00_00_00);
      }

      return false;
   }

   public static boolean isRGB(final ColorModel cm) {
      if (cm instanceof final DirectColorModel dcm
            && cm.getTransferType() == DataBuffer.TYPE_INT) {

         return dcm.getRedMask() == 0x00_FF_00_00
               && dcm.getGreenMask() == 0x00_00_FF_00
               && dcm.getBlueMask() == 0x00_00_00_FF
               && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF_00_00_00);
      }

      return false;
   }

   public static String pageFormatAsString(final PageFormat pageFormat) {
      final int orientation = pageFormat.getOrientation();
      final String orientationString = switch (orientation) {
      case PageFormat.LANDSCAPE         -> "Landscape";
      case PageFormat.PORTRAIT          -> "Portrait";
      case PageFormat.REVERSE_LANDSCAPE -> "Reverse Landscape";
      default                           -> "Unexpected orientation " + orientation;
      };
      final String paperString = paperAsString(pageFormat.getPaper());
      return "PageFormat[" + orientationString + ", " + paperString + "]";
   }

   public static String paperAsString(final Paper paper) {
      return String.format("Paper[%.1fx%.1f, area = %.0f, %.0f, %.0f, %.0f]",
            paper.getWidth(),
            paper.getHeight(),
            paper.getImageableX(),
            paper.getImageableY(),
            paper.getImageableWidth(),
            paper.getImageableHeight());
   }

   static String transparencyToString(final int transparency) {
      return switch (transparency) {
      case Transparency.OPAQUE      -> "OPAQUE";
      case Transparency.BITMASK     -> "BITMASK";
      case Transparency.TRANSLUCENT -> "TRANSLUCENT";
      default                       -> "unrecognized (" + transparency + ")";
      };
   }

}
