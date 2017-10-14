/******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 ****************************************************************************/
package net.tourbook.common.util;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * This is a helper class used to convert an SWT Image into an AWT BufferedImage.
 * 
 * @author Jody Schofield / sshaw
 */
public class ImageConverter {

	private static final PaletteData	PALETTE_DATA	= new PaletteData(0xFF0000, 0xFF00, 0xFF);

	/**
	 * Converts an swt based image into an AWT <code>BufferedImage</code>. This will always return a
	 * <code>BufferedImage</code> that is of type <code>BufferedImage.TYPE_INT_ARGB</code>
	 * regardless of the type of swt image that is passed into the method.
	 * 
	 * @param srcImage
	 *            the {@link org.eclipse.swt.graphics.Image} to be converted to a
	 *            <code>BufferedImage</code>
	 * @return a <code>BufferedImage</code> that represents the same image data as the swt
	 *         <code>Image</code>
	 */
	public static BufferedImage convertIntoAWT(final Image srcImage) {

		final ImageData imageData = srcImage.getImageData();
		final int width = imageData.width;
		final int height = imageData.height;
		ImageData maskData = null;
		final int alpha[] = new int[1];

		if (imageData.alphaData == null) {
			maskData = imageData.getTransparencyMask();
		}

		// now we should have the image data for the bitmap, decompressed in imageData[0].data.
		// Convert that to a Buffered Image.
		final BufferedImage image = new BufferedImage(imageData.width, imageData.height, BufferedImage.TYPE_INT_ARGB);

		final WritableRaster alphaRaster = image.getAlphaRaster();

		// loop over the imagedata and set each pixel in the BufferedImage to the appropriate color.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final RGB color = imageData.palette.getRGB(imageData.getPixel(x, y));
				image.setRGB(x, y, new java.awt.Color(color.red, color.green, color.blue).getRGB());

				// check for alpha channel
				if (alphaRaster != null) {
					if (imageData.alphaData != null) {
						alpha[0] = imageData.getAlpha(x, y);
						alphaRaster.setPixel(x, y, alpha);
					} else {
						// check for transparency mask
						if (maskData != null) {
							alpha[0] = maskData.getPixel(x, y) == 0 ? 0 : 255;
							alphaRaster.setPixel(x, y, alpha);
						}
					}
				}
			}
		}

		return image;
	}

	/**
	 * Converts an AWT based buffered image into an SWT <code>Image</code>. This will always return
	 * an <code>Image</code> that has 24 bit depth regardless of the type of AWT buffered image that
	 * is passed into the method.
	 * 
	 * @param awtImage
	 *            the {@link java.awt.image.BufferedImage} to be converted to an <code>Image</code>
	 * @return an <code>Image</code> that represents the same image data as the AWT
	 *         <code>BufferedImage</code> type.
	 */
	public static Image convertIntoSWT(final BufferedImage awtImage) {

		final int imageWidth = awtImage.getWidth();
		final int imageHeight = awtImage.getHeight();

		// We can force bitdepth to be 24 bit because BufferedImage getRGB allows us to always
		// retrieve 24 bit data regardless of source color depth.
		final ImageData swtImageData = new ImageData(imageWidth, imageHeight, 24, PALETTE_DATA);

		// ensure scansize is aligned on 32 bit.
		final int scansize = (((imageWidth * 3) + 3) * 4) / 4;

		final WritableRaster alphaRaster = awtImage.getAlphaRaster();
		final byte[] alphaBytes = new byte[imageWidth];

		for (int y = 0; y < imageHeight; y++) {

			final int[] buff = awtImage.getRGB(0, y, imageWidth, 1, null, 0, scansize);

			swtImageData.setPixels(0, y, imageWidth, buff, 0);

			// check for alpha channel
			if (alphaRaster != null) {

				final int[] alpha = alphaRaster.getPixels(0, y, imageWidth, 1, (int[]) null);

				for (int i = 0; i < imageWidth; i++) {
					alphaBytes[i] = (byte) alpha[i];
				}

				swtImageData.setAlphas(0, y, imageWidth, alphaBytes, 0);
			}
		}

		return new Image(Display.getCurrent(), swtImageData);
	}

}
