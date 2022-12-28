/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common;

public class DPITools {

   /**
    * Currently only .png files are supported for 4k images !!!
    */
   private static final String IMAGE_NAME_EXTENSION_PNG = ".png";                           //$NON-NLS-1$

   private static final int    PNG_EXTENSION_LENGTH     = IMAGE_NAME_EXTENSION_PNG.length();

   /**
    * All images for the 4k display have this postfix before the file extension. 4k image files are
    * scaled by 2 compared with normal images.
    */
   public static final String  POSTFIX_4K               = "-4k";                            //$NON-NLS-1$

   /**
    * @param imageName
    * @return Returns the 4k image name when the device zoom is >= 140
    */
   public static String get4kImageName(final String imageName) {

      String imageName4k = imageName;

      final int deviceZoom = org.eclipse.swt.internal.DPIUtil.getDeviceZoom();
      if (deviceZoom >= 140) {

         final String imageNameWithoutExtension = imageName.substring(0, imageName.length() - PNG_EXTENSION_LENGTH);

         imageName4k = imageNameWithoutExtension + POSTFIX_4K + IMAGE_NAME_EXTENSION_PNG;
      }

      return imageName4k;
   }
}
