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
 * 
 * @author sgurin (sgurin at montevideo com uy)
 */
public class SWT_AWT_Images {

	/**
	 * write a swt Image into a png file<br>
	 * <br>
	 * Eclipse 3.3.1.1 is buggy saving png files with {@link ImageLoader#save()} it took me several
	 * days to figure this out <br>
	 * <br>
	 * 29.1.2008 Wolfgang
	 * 
	 * @param imgData
	 *        the swt Image to write
	 * @param filename
	 *        name of the png file that will be created
	 */
	public static void writePNG(ImageData imgData, String filename) throws IOException {
		write(imgData, filename, "png"); //$NON-NLS-1$
	}

	/**
	 * write a swt Image into a png file
	 * 
	 * @param imageData
	 *        the swt Image to write
	 * @param filename
	 *        name of the png file that will be created
	 */
	public static void writeJPG(ImageData imageData, String filename) throws IOException {
		write(imageData, filename, "jpg"); //$NON-NLS-1$
	}

	/**
	 * escribir una imágen swt a disco utilizando utilidades swing
	 * 
	 * @param imageData
	 *        la imagen swt a escribir
	 * @param fileName
	 *        el nombre del archivo que se creará
	 * @param format
	 *        la extensión correspondiente al formato con el que se quiere escribir la imágen. por
	 *        ejemplo png, jpg, gif
	 * @throws IOException
	 *         en caso de error al escribir
	 */
	public static void write(ImageData imageData, String fileName, String format) throws IOException {
		BufferedImage buffi = convertToAWT(imageData);
		File outputFile = new File(fileName);
		ImageIO.write(buffi, format, outputFile);
	}

	/**
	 * a ver.... este método fue extraído de uno de los snippets de swt para la transformación de
	 * una imágen swt en una bufferedImage de awt. Sin embargo, este código fue mejorado por sgurin
	 * para el manejo de las transparencias. Su misión es que el uso: Image img = new Image(null,
	 * "pepe"); BufferedImage bimg = convertToAWT(img.getImageData()) funcione correctamente para
	 * "pepe" con formato png, jpg, gif
	 * 
	 * @param data
	 *        la ImageData (image swt) a convertir
	 * @return la conversión de data a una BufferedImage swt
	 */
	static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;

		if (palette.isDirect) {
//			System.out.println("image is direct");

			// no tenemos canal alfa
			if (data.alphaData == null) {
//				System.out.println("data.alphaData==null");
				colorModel = new DirectColorModel(24, palette.redMask, palette.greenMask, palette.blueMask);
				BufferedImage bufferedImage = new BufferedImage(colorModel,
						colorModel.createCompatibleWritableRaster(data.width, data.height),
						false,
						null);
				WritableRaster raster = bufferedImage.getRaster();
				int[] pixelArray = new int[3];
				for (int y = 0; y < data.height; y++) {
					for (int x = 0; x < data.width; x++) {
						int pixel = data.getPixel(x, y);
						RGB rgb = palette.getRGB(pixel);
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
				BufferedImage bufferedImage = new BufferedImage(colorModel,
						colorModel.createCompatibleWritableRaster(data.width, data.height),
						false,
						null);
				WritableRaster raster = bufferedImage.getRaster();
				int[] pixelArray = new int[4];
				for (int y = 0; y < data.height; y++) {
					for (int x = 0; x < data.width; x++) {
						int pixel = data.getPixel(x, y);
						RGB rgb = palette.getRGB(pixel);
						pixelArray[0] = rgb.red;
						pixelArray[1] = rgb.green;
						pixelArray[2] = rgb.blue;
						pixelArray[3] = data.getAlpha(x, y);
						raster.setPixels(x, y, 1, 1, pixelArray);
					}
				}
				return bufferedImage;
			}

			//la paleta swt no es directa ¿?¿?¿?

		} else { //no sabemos qué pasa acá

//			System.out.println("image is indirecto");
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height),
					false,
					null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

}
