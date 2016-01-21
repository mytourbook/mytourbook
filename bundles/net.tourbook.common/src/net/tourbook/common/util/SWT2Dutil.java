/***************************************************************************************************************************************************************
 * Copyright (c) 2004 Chengdong Li : cdli@ccs.uky.edu All rights reserved. This program and the accompanying materials are made available under the terms of the
 * Common Public License v1.0 which accompanies this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
 **************************************************************************************************************************************************************/
package net.tourbook.common.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Utility for Java2d transform
 * 
 * @author Chengdong Li: cli4@uky.edu
 */
public class SWT2Dutil {
	/**
	 * Given arbitrary rectangle, return a rectangle with upper-left start and positive width and
	 * height.
	 * 
	 * @param src
	 *            source rectangle
	 * @return result rectangle with positive width and height
	 */
	public static Rectangle absRect(final Rectangle src) {
		final Rectangle dest = new Rectangle(0, 0, 0, 0);
		if (src.width < 0) {
			dest.x = src.x + src.width + 1;
			dest.width = -src.width;
		} else {
			dest.x = src.x;
			dest.width = src.width;
		}
		if (src.height < 0) {
			dest.y = src.y + src.height + 1;
			dest.height = -src.height;
		} else {
			dest.y = src.y;
			dest.height = src.height;
		}
		return dest;
	}

	/**
	 * This method converts an SWT image to an AWT image
	 * 
	 * @param data
	 * @return BufferedImage
	 */
	public static BufferedImage convertToAWT(final ImageData data) {
		ColorModel colorModel = null;
		final PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			final BufferedImage bufferedImage = new BufferedImage(
					colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height),
					false,
					null);
			final WritableRaster raster = bufferedImage.getRaster();
			final int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					final int pixel = data.getPixel(x, y);
					final RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		}
		final RGB[] rgbs = palette.getRGBs();
		final byte[] red = new byte[rgbs.length];
		final byte[] green = new byte[rgbs.length];
		final byte[] blue = new byte[rgbs.length];
		for (int i = 0; i < rgbs.length; i++) {
			final RGB rgb = rgbs[i];
			red[i] = (byte) rgb.red;
			green[i] = (byte) rgb.green;
			blue[i] = (byte) rgb.blue;
		}
		if (data.transparentPixel != -1) {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
		} else {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
		}
		final BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(
				data.width,
				data.height), false, null);
		final WritableRaster raster = bufferedImage.getRaster();
		final int[] pixelArray = new int[1];
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				final int pixel = data.getPixel(x, y);
				pixelArray[0] = pixel;
				raster.setPixel(x, y, pixelArray);
			}
		}
		return bufferedImage;
	}

	/**
	 * This method converts a AWT image to an SWT image
	 * 
	 * @param bufferedImage
	 * @param imageFilePath
	 * @return ImageData
	 */
	public static ImageData convertToSWT(final BufferedImage bufferedImage, final String imageFilePath) {

		try {
			final ColorModel cm = bufferedImage.getColorModel();

// with Alpha from http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java
//
//			if (cm instanceof DirectColorModel) {
//
//				final DirectColorModel colorModel = (DirectColorModel) cm;
//
//				final PaletteData palette = new PaletteData(
//						colorModel.getRedMask(),
//						colorModel.getGreenMask(),
//						colorModel.getBlueMask());
//				final ImageData data = new ImageData(
//						bufferedImage.getWidth(),
//						bufferedImage.getHeight(),
//						colorModel.getPixelSize(),
//						palette);
//				for (int y = 0; y < data.height; y++) {
//					for (int x = 0; x < data.width; x++) {
//
//						final int rgb = bufferedImage.getRGB(x, y);
//
//						final int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
//						data.setPixel(x, y, pixel);
//
//						if (colorModel.hasAlpha()) {
//							data.setAlpha(x, y, (rgb >> 24) & 0xFF);
//						}
//					}
//				}
//
//				return data;
//
//			} else

			if (cm instanceof DirectColorModel) {

				final DirectColorModel colorModel = (DirectColorModel) cm;

				final PaletteData palette = new PaletteData(
						colorModel.getRedMask(),
						colorModel.getGreenMask(),
						colorModel.getBlueMask());

				final ImageData data = new ImageData(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						colorModel.getPixelSize(),
						palette);

				final WritableRaster raster = bufferedImage.getRaster();
				final int[] pixelArray = new int[3];

				for (int y = 0; y < data.height; y++) {
					for (int x = 0; x < data.width; x++) {
						raster.getPixel(x, y, pixelArray);
						final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
						data.setPixel(x, y, pixel);
					}
				}

				return data;

			} else if (cm instanceof ComponentColorModel) {

				final ComponentColorModel colorModel = (ComponentColorModel) cm;

				final PaletteData palette = new PaletteData(0xff0000, 0x00ff00, 0x0000ff);
				final ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), 24, palette);

				final WritableRaster raster = bufferedImage.getRaster();
				final int size = colorModel.getNumComponents();
				final int[] pixelArray = new int[size];

				if ((size == 1) && (colorModel.getPixelSize() == 48)) {
					for (int y = 0; y < data.height; y++) {
						for (int x = 0; x < data.width; x++) {
							raster.getPixel(x, y, pixelArray);
							// 16 bit greyscale
							data.setPixel(x, y, palette.getPixel(new RGB(
									pixelArray[0] / 256,
									pixelArray[0] / 256,
									pixelArray[0] / 256)));
						}
					}
				} else if (size == 1) {
					for (int y = 0; y < data.height; y++) {
						for (int x = 0; x < data.width; x++) {
							raster.getPixel(x, y, pixelArray);
							// 8 bit greyscale
							data.setPixel(x, y, palette.getPixel(new RGB(pixelArray[0], pixelArray[0], pixelArray[0])));
						}
					}
				} else if (((size == 3) || (size == 4)) && (colorModel.getPixelSize() == 48)) {
					for (int y = 0; y < data.height; y++) {
						for (int x = 0; x < data.width; x++) {
							raster.getPixel(x, y, pixelArray);
							// 16 bit color components
							data.setPixel(x, y, palette.getPixel(new RGB(
									pixelArray[0] / 256,
									pixelArray[1] / 256,
									pixelArray[2] / 256)));
						}
					}

				} else if ((size == 3) || (size == 4)) {

					final boolean hasAlpha = colorModel.hasAlpha();

					for (int y = 0; y < data.height; y++) {
						for (int x = 0; x < data.width; x++) {

							raster.getPixel(x, y, pixelArray);

							// 8 bit color components
							data.setPixel(x, y, palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2])));

							if (hasAlpha) {
								data.setAlpha(x, y, pixelArray[3]);
							}
						}
					}
				}

				return data;

			} else if (cm instanceof IndexColorModel) {
				final IndexColorModel colorModel = (IndexColorModel) cm;
				final int size = colorModel.getMapSize();
				final byte[] reds = new byte[size];
				final byte[] greens = new byte[size];
				final byte[] blues = new byte[size];
				colorModel.getReds(reds);
				colorModel.getGreens(greens);
				colorModel.getBlues(blues);
				final RGB[] rgbs = new RGB[size];
				for (int i = 0; i < rgbs.length; i++) {
					rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
				}
				final PaletteData palette = new PaletteData(rgbs);
				final ImageData data = new ImageData(
						bufferedImage.getWidth(),
						bufferedImage.getHeight(),
						colorModel.getPixelSize(),
						palette);
				data.transparentPixel = colorModel.getTransparentPixel();
				final WritableRaster raster = bufferedImage.getRaster();
				final int[] pixelArray = new int[1];
				for (int y = 0; y < data.height; y++) {
					for (int x = 0; x < data.width; x++) {
						raster.getPixel(x, y, pixelArray);
						data.setPixel(x, y, pixelArray[0]);
					}
				}
				return data;
			}

		} catch (final Exception e) {
			System.out.println("cannot convert AWT into SWT: " + imageFilePath + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Given an arbitrary point, get the point with the inverse given transform.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param pt
	 *            source point
	 * @return point after transform
	 */
	public static Point inverseTransformPoint(final AffineTransform af, final Point pt) {
		final Point2D src = new Point2D.Float(pt.x, pt.y);
		try {
			final Point2D dest = af.inverseTransform(src, null);
			return new Point((int) Math.floor(dest.getX()), (int) Math.floor(dest.getY()));
		} catch (final Exception e) {
			e.printStackTrace();
			return new Point(0, 0);
		}
	}

	/**
	 * Given an arbitrary rectangle, get the rectangle with the inverse given transform. The result
	 * rectangle is positive width and positive height.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param pSrc
	 *            source rectangle
	 * @return rectangle after transform with positive width and height
	 */
	public static Rectangle inverseTransformRect(final AffineTransform af, final Rectangle pSrc) {
		final Rectangle dest = new Rectangle(0, 0, 0, 0);
		final Rectangle src = absRect(pSrc);
		Point p1 = new Point(src.x, src.y);
		p1 = inverseTransformPoint(af, p1);
		dest.x = p1.x;
		dest.y = p1.y;
		dest.width = (int) (src.width / af.getScaleX());
		dest.height = (int) (src.height / af.getScaleY());
		return dest;
	}

	/**
	 * Given an arbitrary point, get the point with the given transform.
	 * 
	 * @param af
	 *            affine transform
	 * @param pt
	 *            point to be transformed
	 * @return point after tranform
	 */
	public static Point transformPoint(final AffineTransform af, final Point pt) {
		final Point2D src = new Point2D.Float(pt.x, pt.y);
		final Point2D dest = af.transform(src, null);
		final Point point = new Point((int) Math.floor(dest.getX()), (int) Math.floor(dest.getY()));
		return point;
	}

	/**
	 * Given an arbitrary rectangle, get the rectangle with the given transform. The result
	 * rectangle is positive width and positive height.
	 * 
	 * @param af
	 *            AffineTransform
	 * @param pSrc
	 *            source rectangle
	 * @return rectangle after transform with positive width and height
	 */
	public static Rectangle transformRect(final AffineTransform af, final Rectangle pSrc) {
		final Rectangle dest = new Rectangle(0, 0, 0, 0);
		final Rectangle src = absRect(pSrc);
		Point p1 = new Point(src.x, src.y);
		p1 = transformPoint(af, p1);
		dest.x = p1.x;
		dest.y = p1.y;
		dest.width = (int) (src.width * af.getScaleX());
		dest.height = (int) (src.height * af.getScaleY());
		return dest;
	}
}
