/*******************************************************************************
 * Copyright (C) 2012, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.manager;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.Image;

/**
 * This wrapper tries to store a minimum of data
 */
public class ImageCacheWrapper {

   public String        imageKey;

   public BufferedImage awtImage;
   public Image         swtImage;

   public String        originalImagePathName;

   public ImageCacheWrapper(final BufferedImage awtImage,
                            final String originalImagePathName,
                            final String imageKey) {

      this.awtImage = awtImage;

      this.originalImagePathName = originalImagePathName;
      this.imageKey = imageKey;
   }

   public ImageCacheWrapper(final Image swtImage,
                            final String originalImagePathName,
                            final String imageKey) {

      this.swtImage = swtImage;

      this.originalImagePathName = originalImagePathName;
      this.imageKey = imageKey;
   }

}
