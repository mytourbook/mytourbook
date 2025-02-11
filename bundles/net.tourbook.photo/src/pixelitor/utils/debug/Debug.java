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
      case BufferedImage.TYPE_3BYTE_BGR      -> "3BYTE_BGR"; //$NON-NLS-1$
      case BufferedImage.TYPE_4BYTE_ABGR     -> "4BYTE_ABGR"; //$NON-NLS-1$
      case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE"; //$NON-NLS-1$
      case BufferedImage.TYPE_BYTE_BINARY    -> "BYTE_BINARY"; //$NON-NLS-1$
      case BufferedImage.TYPE_BYTE_GRAY      -> "BYTE_GRAY"; //$NON-NLS-1$
      case BufferedImage.TYPE_BYTE_INDEXED   -> "BYTE_INDEXED"; //$NON-NLS-1$
      case BufferedImage.TYPE_CUSTOM         -> "CUSTOM"; //$NON-NLS-1$
      case BufferedImage.TYPE_INT_ARGB       -> "INT_ARGB"; //$NON-NLS-1$
      case BufferedImage.TYPE_INT_ARGB_PRE   -> "INT_ARGB_PRE"; //$NON-NLS-1$
      case BufferedImage.TYPE_INT_BGR        -> "INT_BGR"; //$NON-NLS-1$
      case BufferedImage.TYPE_INT_RGB        -> "INT_RGB"; //$NON-NLS-1$
      case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB"; //$NON-NLS-1$
      case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB"; //$NON-NLS-1$
      case BufferedImage.TYPE_USHORT_GRAY    -> "USHORT_GRAY"; //$NON-NLS-1$
      default                                -> "unrecognized (" + type + ")";
      };
   }

   static String colorSpaceTypeToString(final int type) {
      return switch (type) {
      case ColorSpace.TYPE_2CLR  -> "2CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_3CLR  -> "3CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_4CLR  -> "4CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_5CLR  -> "5CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_6CLR  -> "6CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_7CLR  -> "7CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_8CLR  -> "8CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_9CLR  -> "9CLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_ACLR  -> "ACLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_BCLR  -> "BCLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_CCLR  -> "CCLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_CMY   -> "CMY"; //$NON-NLS-1$
      case ColorSpace.TYPE_CMYK  -> "CMYK"; //$NON-NLS-1$
      case ColorSpace.TYPE_DCLR  -> "DCLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_ECLR  -> "ECLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_FCLR  -> "FCLR"; //$NON-NLS-1$
      case ColorSpace.TYPE_GRAY  -> "GRAY"; //$NON-NLS-1$
      case ColorSpace.TYPE_HLS   -> "HLS"; //$NON-NLS-1$
      case ColorSpace.TYPE_HSV   -> "HSV"; //$NON-NLS-1$
      case ColorSpace.TYPE_Lab   -> "Lab"; //$NON-NLS-1$
      case ColorSpace.TYPE_Luv   -> "Luv"; //$NON-NLS-1$
      case ColorSpace.TYPE_RGB   -> "RGB"; //$NON-NLS-1$
      case ColorSpace.TYPE_XYZ   -> "XYZ"; //$NON-NLS-1$
      case ColorSpace.TYPE_YCbCr -> "YCbCr"; //$NON-NLS-1$
      case ColorSpace.TYPE_Yxy   -> "Yxy"; //$NON-NLS-1$
      default                    -> "unrecognized (" + type + ")";
      };
   }

   public static String dataBufferTypeToString(final int type) {
      return switch (type) {
      case DataBuffer.TYPE_BYTE      -> "BYTE"; //$NON-NLS-1$
      case DataBuffer.TYPE_USHORT    -> "USHORT"; //$NON-NLS-1$
      case DataBuffer.TYPE_SHORT     -> "SHORT"; //$NON-NLS-1$
      case DataBuffer.TYPE_INT       -> "INT"; //$NON-NLS-1$
      case DataBuffer.TYPE_FLOAT     -> "FLOAT"; //$NON-NLS-1$
      case DataBuffer.TYPE_DOUBLE    -> "DOUBLE"; //$NON-NLS-1$
      case DataBuffer.TYPE_UNDEFINED -> "UNDEFINED"; //$NON-NLS-1$
      default                        -> "unrecognized (" + type + ")";
      };
   }

   // Color's toString doesn't include the alpha
   public static String debugColor(final Color c) {
      return String.format("r=%d,g=%d,b=%d,a=%d", //$NON-NLS-1$
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
                           """, //$NON-NLS-1$
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
      return d.width + "x" + d.height; //$NON-NLS-1$
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
      case PageFormat.LANDSCAPE         -> "Landscape"; //$NON-NLS-1$
      case PageFormat.PORTRAIT          -> "Portrait"; //$NON-NLS-1$
      case PageFormat.REVERSE_LANDSCAPE -> "Reverse Landscape"; //$NON-NLS-1$
      default                           -> "Unexpected orientation " + orientation;
      };
      final String paperString = paperAsString(pageFormat.getPaper());
      return "PageFormat[" + orientationString + ", " + paperString + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

   public static String paperAsString(final Paper paper) {
      return String.format("Paper[%.1fx%.1f, area = %.0f, %.0f, %.0f, %.0f]", //$NON-NLS-1$
            paper.getWidth(),
            paper.getHeight(),
            paper.getImageableX(),
            paper.getImageableY(),
            paper.getImageableWidth(),
            paper.getImageableHeight());
   }

   static String transparencyToString(final int transparency) {
      return switch (transparency) {
      case Transparency.OPAQUE      -> "OPAQUE"; //$NON-NLS-1$
      case Transparency.BITMASK     -> "BITMASK"; //$NON-NLS-1$
      case Transparency.TRANSLUCENT -> "TRANSLUCENT"; //$NON-NLS-1$
      default                       -> "unrecognized (" + transparency + ")";
      };
   }

}
