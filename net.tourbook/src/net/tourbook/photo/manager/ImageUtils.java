/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.manager;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Original code: org.sharemedia.utils.ImageUtils
 */
public class ImageUtils {

	public static int getBestQuality(final Photo photo, final int width, final int height, final int maxDef) {

		final int imageQuality = PhotoManager.IMAGE_QUALITY_THUMB_160;

		for (int qualityIndex = PhotoManager.IMAGE_QUALITY_THUMB_160; qualityIndex <= maxDef; qualityIndex++) {

			if (qualityIndex < PhotoManager.IMAGE_QUALITY_ORIGINAL) {

				final int qualitySize = PhotoManager.IMAGE_SIZE[qualityIndex];

				if (qualitySize > width || qualitySize > height) {
					return imageQuality;
				}
			}
		}

		return imageQuality;
	}

// ORIGINAL
//
//	public static int getBestDefinition(IMedia media, int width, int height, int maxDef) {
//		int def = IConstants.IMAGE_THUMB;
//
//		for (int i = IConstants.IMAGE_THUMB; i <= maxDef; i++) {
//			if (media.hasImage(i)) {
//				def = i;
//			}
//
//			if (i < IConstants.IMAGE_ORIGINAL)
//				if (IConstants.IMAGE_WIDTH[i] > width || IConstants.IMAGE_HEIGHT[i] > height)
//					return def;
//
//		}
//		return def;
//	}

	public static double getBestRatio(final int originalX, final int originalY, final int maxX, final int maxY) {

		final double widthRatio = (double) originalX / (double) maxX;
		final double heightRatio = (double) originalY / (double) maxY;

		final double bestRatio = widthRatio > heightRatio ? widthRatio : heightRatio;

		return bestRatio;
	}

	public static Point getBestSize(final int originalX, final int originalY, final int maxX, final int maxY) {

		final double bestRatio = getBestRatio(originalX, originalY, maxX, maxY);

		final int newWidth = (int) (originalX / bestRatio);
		final int newHeight = (int) (originalY / bestRatio);
		// logger.debug("newWidth " + newWidth + " newHeight " + newHeight);

		return new Point(newWidth, newHeight);
	}

	public static Point getBestSize(final Point original, final Point max) {
		return getBestSize(original.x, original.y, max.x, max.y);
	}

	public static boolean isResizeRequired(final Image image, final int width, final int height) {
		final Rectangle bounds = image.getBounds();
		return !(bounds.width == width && bounds.height == height);
	}

	public static boolean isResizeRequiredAWT(final BufferedImage img, final int width, final int height) {
		return !(img.getWidth() == width && img.getHeight() == height);
	}

	/**
	 * Returns a new scaled image. new Image must be disposed after use.
	 * 
	 * @param image
	 * @param width
	 * @param height
	 * @return
	 */
	public static Image resize(final Image image, final int width, final int height) {
		return resize(image, width, height, SWT.ON, SWT.HIGH);
	}

//	public Image resize(int w, int h, Image img) {
//		Image newImage = new Image(Display.getDefault(), w, h);
//		GC gc = new GC(newImage);
//		gc.setAntialias(SWT.ON);
//		gc.setInterpolation(SWT.HIGH);
//		gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, 0, 0, w, h);
//		gc.dispose();
//		img.dispose();
//		return newImage;
//	}

	public static Image resize(	final Image image,
								final int width,
								final int height,
								final int antialias,
								final int interpolation) {

		if (image == null) {
			return null;
		}

		final Image scaledImage = new Image(Display.getDefault(), width, height);
		final GC gc = new GC(scaledImage);
		{
			// Linux-GTK Bug 174932
//			if (!SWT.getPlatform().equals("gtk")) {
//			}
			// this bug is solved
			gc.setAdvanced(true);

			if (gc.getAdvanced()) {
				gc.setAntialias(antialias);
				gc.setInterpolation(interpolation);
			}

			final Rectangle originalImageBounds = image.getBounds();

//			System.out.println("resize()\tfrom  "
//					+ originalImageBounds.width
//					+ "\t"
//					+ originalImageBounds.height
//					+ "\tto  "
//					+ width
//					+ "\t"
//					+ height);
//			// TODO remove SYSTEM.OUT.PRINTLN

			gc.drawImage(image, //
					0,
					0,
					originalImageBounds.width,
					originalImageBounds.height,
					//
					0,
					0,
					width,
					height);
		}
		gc.dispose();

		return scaledImage;
	}

	public static ImageData resize(	final ImageData imageData,
									final int width,
									final int height,
									final boolean antiAliasing) {

		if (imageData == null) {
			return null;
		}

		if (imageData.width == width && imageData.height == height) {
			return imageData;
		}

		if (antiAliasing) {
			Image tmpImage = null;
			final Image fullImage = new Image(Display.getCurrent(), imageData);
			ImageData result = null;
			tmpImage = resize(fullImage, width, height);

			result = tmpImage.getImageData();
			tmpImage.dispose();
			fullImage.dispose();
			return result;
		}
		return imageData.scaledTo(width, height);
	}

	/**
	 * Resize an image to the best fitting size. Old and new Image (result)must be disposed after
	 * use.
	 * 
	 * @param img
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	public static Image resizeBestSize(final Image img, final int maxWidth, final int maxHeight) {
		// Check for null
		if (img == null) {
			return null;
		}

		// Calculate best size
		final Point newSize = getBestSize(img.getBounds().width, img.getBounds().height, maxWidth, maxHeight);

		// Resize image
		return ImageUtils.resize(img, newSize.x, newSize.y);
	}

	public static byte[] saveImage(final Image image, final int format) {
		if (image == null) {
			return null;
		}

		final ImageLoader il = new ImageLoader();
		il.data = new ImageData[] { image.getImageData() };
		final ByteArrayOutputStream bas = new ByteArrayOutputStream();
		il.save(bas, format);
		return bas.toByteArray();
	}

}
