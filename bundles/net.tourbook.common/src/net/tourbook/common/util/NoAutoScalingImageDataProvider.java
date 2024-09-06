/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;

/**
 * With helpful hints from Heiko Klare
 * <p>
 * <i>A probably better option (and one that will also work when having the updateOnRuntime feature
 * enabled for better HiDPI support) would be to implement an ImageDataProvider
 * (org.eclipse.swt.graphics.ImageDataProvider), which only has a single method getImageData (int
 * zoom). In that implementation you can create your image with the zoom passed to the method of the
 * provider on demand. You can, of course, still cache the buffered image for a specific zoom value
 * inside, but this way you will be able to receive the zoom value and create a properly sized image
 * for that. You can pass this image data provider to the constructor of the image to be draw via
 * the GC in the UI. When painting that image in the UI, the image data provider will be requested
 * to deliver the image with the proper zoom. You will probably need to run your application with
 * -Dswt.autoScale=quarter then, as otherwise this zoom value will only be 100 or 200.</i>
 * <p>
 * https://github.com/eclipse-platform/eclipse.platform.swt/issues/1411
 */
public class NoAutoScalingImageDataProvider implements ImageDataProvider {

   private ImageData _imageData;

   public NoAutoScalingImageDataProvider(final BufferedImage awtImage) {

      _imageData = ImageConverter.convertIntoSWTImageData(awtImage);
   }

   public NoAutoScalingImageDataProvider(final ImageData imageData) {

      _imageData = imageData;
   }

   @Override
   public ImageData getImageData(final int zoom) {

      return _imageData;
   }
}
