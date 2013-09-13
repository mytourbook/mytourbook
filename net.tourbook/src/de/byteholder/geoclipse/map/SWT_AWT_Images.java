package de.byteholder.geoclipse.map;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * clase auxiliar para la escritura a disco de las imágenes swt mediante mecanismos swing. ¿por qué?
 * porque la clase swt encargada de esto (ImageLoader) no trabaja bien con algunos formatos como
 * png. Mucho de este código fue extraído de un snippet de la paina swt. this is free (non
 * restrictive) software. Use as you wish
 * <p>
 * Translation
 * <p>
 * helper class for writing to disk images swing swt through mechanisms. Why? because this charge
 * swt class (ImageLoader) does not work well with some formats like png. Much of this code was
 * taken from a snippet of paina swt. this is free (non restrictive) software. Use as you wish
 * 
 * @author sgurin (sgurin at montevideo com uy)
 */
public class SWT_AWT_Images {

	/**
	 * Founde here (12.9.2013): <a href=
	 * "http://m4tx.pl/en/2013/01/01/java-swt-to-awt-and-vice-versa-image-conversion-with-transparency-support/"
	 * >http://m4tx.pl/en/2013/01/01/java-swt-to-awt-and-vice-versa-image-conversion-with-
	 * transparency-support/</a>
	 * 
	 * @param data
	 * @return
	 */
	public static BufferedImage convertToAWT(final ImageData data) {

		ColorModel colorModel = null;
		final PaletteData palette = data.palette;
		if (palette.isDirect) {
			final BufferedImage bufferedImage = new BufferedImage(data.width, data.height, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					final int pixel = data.getPixel(x, y);
					final RGB rgb = palette.getRGB(pixel);
					bufferedImage.setRGB(x, y, data.getAlpha(x, y) << 24 | rgb.red << 16 | rgb.green << 8 | rgb.blue);
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

	/**
	 * Alpha is not working !!!
	 * <p>
	 * Translation
	 * <p>
	 * to see .... This method was taken from one of the snippets swt for transforming an image into
	 * a BufferedImage swt of awt. However, this code was improved by sgurin to handle
	 * transparencies. Its mission is to use:
	 * <p>
	 * a ver.... este método fue extraído de uno de los snippets de swt para la transformación de
	 * una imágen swt en una bufferedImage de awt. Sin embargo, este código fue mejorado por sgurin
	 * para el manejo de las transparencias. Su misión es que el uso: Image img = new Image(null,
	 * "pepe"); BufferedImage bimg = convertToAWT(img.getImageData()) funcione correctamente para
	 * "pepe" con formato png, jpg, gif
	 * <p>
	 * <code>
		Image img = new Image (null, "foo");
		BufferedImage BIMG = convertToAWT (img.getImageData ())
		</code>
	 * <p>
	 * work correctly for "pepe" format png, jpg, gif
	 * 
	 * @param data
	 *            la ImageData (image swt) a convertir
	 * @return la conversión de data a una BufferedImage swt
	 */
	public static BufferedImage convertToAWT_NoAlpha(final ImageData data) {

		ColorModel colorModel = null;
		final PaletteData palette = data.palette;

		if (palette.isDirect) {
//			System.out.println("image is direct");

			// no tenemos canal alfa
			if (data.alphaData == null) {
//				System.out.println("data.alphaData==null");
				colorModel = new DirectColorModel(24, palette.redMask, palette.greenMask, palette.blueMask);
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

			//tenemos canal alfa
			else {

//				System.out.println("data.alphaData!=null");

				colorModel = new DirectColorModel(32, palette.redMask, palette.greenMask, palette.blueMask, 0xff000000);

				final BufferedImage bufferedImage = new BufferedImage(
						colorModel,
						colorModel.createCompatibleWritableRaster(data.width, data.height),
						false,
						null);

				final WritableRaster raster = bufferedImage.getRaster();
				final int[] pixelArray = new int[4];

				for (int y = 0; y < data.height; y++) {
					for (int x = 0; x < data.width; x++) {

						final int pixel = data.getPixel(x, y);
						final RGB rgb = palette.getRGB(pixel);

						pixelArray[0] = rgb.red;
						pixelArray[1] = rgb.green;
						pixelArray[2] = rgb.blue;
						pixelArray[3] = data.getAlpha(x, y);

						raster.setPixels(x, y, 1, 1, pixelArray);

//						System.out.println(x + "\t" + y + "\tpixelArray"
//								+ ("\t" + pixelArray[0])
//								+ ("\t" + pixelArray[1])
//								+ ("\t" + pixelArray[2])
//								+ ("\t" + pixelArray[3])
//						//
//								);
//						// TODO remove SYSTEM.OUT.PRINTLN

					}
				}
				return bufferedImage;
			}

			//la paleta swt no es directa ¿?¿?¿?

		} else { //no sabemos qué pasa acá

//			System.out.println("image is indirecto");
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

	public static void convertTransparentPixelToTransparentData(final ImageData imageData, final RGB rgbTransparent) {

		final int width = imageData.width;
		final int height = imageData.height;

		final int srcBytesPerLine = imageData.bytesPerLine;

		final byte transRed = (byte) rgbTransparent.red;
		final byte transGreen = (byte) rgbTransparent.green;
		final byte transBlue = (byte) rgbTransparent.blue;

		final byte[] rgbData = imageData.data;
		final byte[] alphaData = imageData.alphaData = new byte[width * height];

//		int opaque = 0;
//		int trans = 0;
//		int first = -1;

		for (int y = 0; y < height; y++) {

			int srcIndex = srcBytesPerLine * y;

			int destAlphaIndex = width * y;

			for (int x = 0; x < width; x++) {

				if (rgbData[srcIndex + 0] == transRed
						&& rgbData[srcIndex + 1] == transGreen
						&& rgbData[srcIndex + 2] == transBlue) {

					// transparent

//					trans++;

					alphaData[destAlphaIndex] = (byte) 0x0;

				} else {

					// opaque

//					if (first == -1) {
//						first = srcIndex;
//					}
//
//					opaque++;

					alphaData[destAlphaIndex] = (byte) 0xff;
				}

				srcIndex += 3;

				destAlphaIndex++;
			}
		}

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "] \topaque: "
//				+ opaque
//				+ "\ttrans: "
//				+ trans
//				+ "\tfirst: "
//				+ first
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	/**
	 * escribir una imágen swt a disco utilizando utilidades swing
	 * 
	 * @param imageData
	 *            la imagen swt a escribir
	 * @param fileName
	 *            el nombre del archivo que se creará
	 * @param format
	 *            la extensión correspondiente al formato con el que se quiere escribir la imágen.
	 *            por ejemplo png, jpg, gif
	 * @throws IOException
	 *             en caso de error al escribir
	 */
	public static void write(final ImageData imageData, final String fileName, final String format) throws IOException {
		final BufferedImage buffi = convertToAWT(imageData);
		final File outputFile = new File(fileName);
		ImageIO.write(buffi, format, outputFile);
	}

	/**
	 * write a swt Image into a png file
	 * 
	 * @param imageData
	 *            the swt Image to write
	 * @param filename
	 *            name of the png file that will be created
	 */
	public static void writeJPG(final ImageData imageData, final String filename) throws IOException {
		write(imageData, filename, "jpg"); //$NON-NLS-1$
	}

	/**
	 * write a swt Image into a png file<br>
	 * <br>
	 * Eclipse 3.3.1.1 is buggy saving png files with {@link ImageLoader#save()} it took me several
	 * days to figure this out <br>
	 * <br>
	 * 29.1.2008 Wolfgang
	 * 
	 * @param imgData
	 *            the swt Image to write
	 * @param filename
	 *            name of the png file that will be created
	 */
	public static void writePNG(final ImageData imgData, final String filename) throws IOException {
		write(imgData, filename, "png"); //$NON-NLS-1$
	}

}
