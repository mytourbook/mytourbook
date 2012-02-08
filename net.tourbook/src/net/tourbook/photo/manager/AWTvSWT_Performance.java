package net.tourbook.photo.manager;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class AWTvSWT_Performance {

	static BufferedImage convertToAWT(final ImageData data) {

		ColorModel colorModel = null;
		final PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			final BufferedImage bufferedImage = new BufferedImage(
					colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height),
					false,
					null);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					final int pixel = data.getPixel(x, y);
					final RGB rgb = palette.getRGB(pixel);
					bufferedImage.setRGB(x, y, rgb.red << 16 | rgb.green << 8 | rgb.blue);
				}
			}
			return bufferedImage;

		} else {

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
			final BufferedImage bufferedImage = new BufferedImage(
					colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height),
					false,
					null);
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
	}

//	/**
//	 * ORIGINAL
//	 * @param bufferedImage
//	 * @return
//	 */
//	static ImageData convertToSWT(final BufferedImage bufferedImage) {
//
//		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
//			final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
//			final PaletteData palette = new PaletteData(
//					colorModel.getRedMask(),
//					colorModel.getGreenMask(),
//					colorModel.getBlueMask());
//			final ImageData data = new ImageData(
//					bufferedImage.getWidth(),
//					bufferedImage.getHeight(),
//					colorModel.getPixelSize(),
//					palette);
//			for (int y = 0; y < data.height; y++) {
//				for (int x = 0; x < data.width; x++) {
//					final int rgb = bufferedImage.getRGB(x, y);
//					final int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
//					data.setPixel(x, y, pixel);
//					if (colorModel.hasAlpha()) {
//						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
//					}
//				}
//			}
//			return data;
//
//		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
//
//			final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
//			final int size = colorModel.getMapSize();
//			final byte[] reds = new byte[size];
//			final byte[] greens = new byte[size];
//			final byte[] blues = new byte[size];
//			colorModel.getReds(reds);
//			colorModel.getGreens(greens);
//			colorModel.getBlues(blues);
//			final RGB[] rgbs = new RGB[size];
//			for (int i = 0; i < rgbs.length; i++) {
//				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
//			}
//			final PaletteData palette = new PaletteData(rgbs);
//			final ImageData data = new ImageData(
//					bufferedImage.getWidth(),
//					bufferedImage.getHeight(),
//					colorModel.getPixelSize(),
//					palette);
//			data.transparentPixel = colorModel.getTransparentPixel();
//			final WritableRaster raster = bufferedImage.getRaster();
//			final int[] pixelArray = new int[1];
//			for (int y = 0; y < data.height; y++) {
//				for (int x = 0; x < data.width; x++) {
//					raster.getPixel(x, y, pixelArray);
//					data.setPixel(x, y, pixelArray[0]);
//				}
//			}
//			return data;
//		}
//		return null;
//	}

	/**
	 * Converts a Swing BufferedImage into a lightweight ImageData object for SWT
	 * 
	 * @param bufferedImage
	 *            the image to be converted
	 * @return An ImageData that represents the same image as bufferedImage
	 */
	public static ImageData convertToSWT(final BufferedImage bufferedImage) {

		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
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
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
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
		} else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
			final ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();

			//ASSUMES: 3 BYTE BGR IMAGE TYPE

			final PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
			final ImageData data = new ImageData(
					bufferedImage.getWidth(),
					bufferedImage.getHeight(),
					colorModel.getPixelSize(),
					palette);

			//This is valid because we are using a 3-byte Data model with no transparent pixels
			data.transparentPixel = -1;

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
		}
		return null;
	}
	private static void copyStripesAWT(final BufferedImage source, final BufferedImage target) {

		assert (source.getWidth() == target.getWidth());

		final int stripeCount = source.getWidth();
		final int sourceHeight = source.getHeight();
		final int targetHeight = target.getHeight();
		final Graphics g = target.getGraphics();

		for (int x = 0; x < stripeCount; x++) {
			g.drawImage(source, x, 0, // upper left corner of destination
					x + 1,
					targetHeight, // lower right corner of destination
					x,
					0, // upper left corner of source
					x + 1,
					sourceHeight, // lower right corner of source
					null);
		}

		g.dispose();
	}

	private static void copyStripesSWT(final Image source, final Image target) {

		assert (source.getBounds().width == target.getBounds().width);

		final int stripeCount = source.getBounds().width;
		final int sourceHeight = source.getBounds().height;
		final int targetHeight = target.getBounds().height;
		final GC gc = new GC(target);

		for (int x = 0; x < stripeCount; x++) {
			gc.drawImage(source, x, 0, // upper left corner of source
					1,
					sourceHeight, // size of source
					x,
					0, // upper left corner of destination
					1,
					targetHeight // size of desination
					);
		}

		gc.dispose();
	}

	public static void main(final String[] args) throws IOException {

		final int runs = 1;

		// create test images
		long start = System.currentTimeMillis();

		final BufferedImage awtSource = ImageIO.read(new File(args[0]));
//		final BufferedImage awtTarget = new BufferedImage(
//				awtSource.getWidth(),
//				awtSource.getHeight() / 2,
//				awtSource.getType());
		System.out.println("AWT load: " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		final Image swtSource = new Image(Display.getDefault(), args[0]);
//		final Image swtTarget = new Image(
//				Display.getDefault(),
//				swtSource.getBounds().width,
//				swtSource.getBounds().height / 2);
		System.out.println("SWT load: " + (System.currentTimeMillis() - start));

		// run AWT performance test
		final long awtStartTime = System.currentTimeMillis();
		for (int i = 0; i < runs; i++) {
//			copyStripesAWT(awtSource, awtTarget);
			convertToSWT(awtSource);
		}
		final long awtEndTime = System.currentTimeMillis();
		System.out.println("convertToSWT: " + (awtEndTime - awtStartTime) /* / runs */);

		// run SWT performance test
		final long swtStartTime = System.currentTimeMillis();
		for (int i = 0; i < runs; i++) {
//			copyStripesSWT(swtSource, swtTarget);
//			convertToAWT(swtSource.getImageData());
		}
		final long swtEndTime = System.currentTimeMillis();
		System.out.println("convertToAWT: " + (swtEndTime - swtStartTime) /* / runs */);
	}
}
