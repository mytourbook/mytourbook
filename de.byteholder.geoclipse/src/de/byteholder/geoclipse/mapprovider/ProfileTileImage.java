/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.mapprovider;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import de.byteholder.geoclipse.map.Tile;

/**
 * Creates the tile image for the profile map provider {@link MPProfile} <br>
 * <br>
 * Part of this class is copied from {@link CompositeImageDescriptor} and heavily modified
 */
final class ProfileTileImage {

	private ImageData	_tileImageData;

	ProfileTileImage() {
		_tileImageData = new ImageData(256, 256, 24, new PaletteData(0xFF, 0xFF00, 0xFF0000));
	}

	/**
	 * Draws the given source image data into this composite image at the given
	 * position.
	 * <p>
	 * Call this internal framework method to superimpose another image atop this composite image.
	 * </p>
	 * <p>
	 * This method is optimized from 724 ms to 5 ms by removing all image data methods
	 * </p>
	 * 
	 * @param src
	 *            the source image data
	 * @param srcTile
	 * @param brt
	 *            brightness image data
	 * @param brtTile
	 */
	final void drawImage(final ImageData src, final Tile srcTile, final ImageData brt, final Tile brtTile) {

		final MP srcMp = srcTile.getMP();

		final float externalAlpha = (float) srcMp.getProfileAlpha() / 100;
		int[] transparentPixel = null;

		/*
		 * set transparent pixel
		 */
		if (srcMp.isProfileTransparentColors()) {

			// transparent colors are set

			final int[] transparentColors = srcMp.getProfileTransparentColors();

			if (src.palette.isDirect) {

				transparentPixel = transparentColors;

			} else {

				// get index in the palette, a color with the same value can be multiple times in the palette

				final RGB[] paletteColors = src.palette.colors;
				transparentPixel = new int[paletteColors.length];

				int transIndex = 0;
				int numTransColors = 0;

				// set default to -1
				for (int defaultIndex = 0; defaultIndex < transparentPixel.length; defaultIndex++) {
					transparentPixel[defaultIndex] = -1;
				}

				// loop: all defined transparent colors
				for (final int transparentColor : transparentColors) {

					// ignore invalid values
					if (transparentColor < 0) {
						continue;
					}

					final int transRed = (transparentColor & 0xFF) >>> 0;
					final int transGreen = (transparentColor & 0xFF00) >>> 8;
					final int transBlue = (transparentColor & 0xFF0000) >>> 16;

					// loop: all palette colors, find transparent color in palette
					for (int paletteIndex = 0; paletteIndex < paletteColors.length; paletteIndex++) {

						final RGB rgb = paletteColors[paletteIndex];

						if (rgb.red == transRed && rgb.green == transGreen && rgb.blue == transBlue) {
							transparentPixel[paletteIndex] = paletteIndex;
							numTransColors++;
						}
					}

					transIndex++;
				}

				// remove unnecessary colors
				final int[] availableColors = transparentPixel;
				transparentPixel = new int[numTransColors];
				int colorIndex = 0;
				for (final int color : availableColors) {
					if (color > -1) {
						transparentPixel[colorIndex++] = color;
					}
				}

			}
		}

		final ImageData dst = _tileImageData;

		// brightness data
		byte[] brtData = null;
		byte[] brtAlphaData = null;
		PaletteData brtPalette = null;
		RGB[] brtColors = null;

		int brtBytesPerLine = 0;
		int brtPixel = 0;
		int brtDepth = 0;
		float brtAlpha = 1.0f;

		// selected brightness value: 0 ... 1.0
		float selectedBrightness = 0;
		float rgbBrightness = -1;

		// brightness image must have the same image size
		if (brt != null && src.width == brt.width && src.height == brt.height) {

			final MP brtMp = brtTile.getMP();

			brtData = brt.data;
			brtAlphaData = brt.alphaData;
			brtPalette = brt.palette;
			brtColors = brtPalette.colors;
			brtBytesPerLine = brt.bytesPerLine;
			brtDepth = brt.depth;

//			brtAlpha = brtTileFactory.getProfileAlpha();

			// adjust brightness to 0.0 ... 1.0
			selectedBrightness = brtMp.getProfileBrightness() / 100f;
		}

		// source data
		final byte[] srcData = src.data;
		final PaletteData srcPalette = src.palette;
		final RGB[] srcColors = srcPalette.colors;
		final byte[] srcAlphaData = src.alphaData;
		final int srcBytesPerLine = src.bytesPerLine;
		final int srcDepth = src.depth;
		int srcIndex;
		int srcPixel = 0;

		// destination data
		final byte[] dstData = dst.data;
		byte[] dstAlphaData = dst.alphaData;
		final int dstWidth = dst.width;
		final int dstHeight = dst.height;
		final int dstBytesPerLine = dst.bytesPerLine;

		int theByte;
		int mask;

		int srcRed, srcGreen, srcBlue;
		int brtRed, brtGreen, brtBlue;

		// create alpha data
		if (dstAlphaData == null) {
			dst.alphaData = dstAlphaData = new byte[dstWidth * dstHeight];
		}

		for (int srcY = 0, dstY = srcY; srcY < src.height; srcY++, dstY++) {

			final int brtYBytesPerLine = srcY * brtBytesPerLine;
			final int srcYBytesPerLine = srcY * srcBytesPerLine;
			final int dstYBytesPerLine = dstY * dstBytesPerLine;

			for (int srcX = 0, dstX = srcX; srcX < src.width; srcX++, dstX++) {

				final int dataIndex = dstYBytesPerLine + (dstX * 3);
				final int alphaIndex = dstY * dstWidth + dstX;

				// check bounds
				if (!(0 <= dstX && dstX < dst.width && 0 <= dstY && dstY < dst.height)) {
					continue;
				}

				/*
				 * this is inline code for src.getPixel() to optimize performance
				 */
				{
					switch (srcDepth) {
					case 32:
						srcIndex = srcYBytesPerLine + (srcX * 4);
						srcPixel = ((srcData[srcIndex] & 0xFF) << 24)
								+ ((srcData[srcIndex + 1] & 0xFF) << 16)
								+ ((srcData[srcIndex + 2] & 0xFF) << 8)
								+ (srcData[srcIndex + 3] & 0xFF);
						break;
					case 24:
						srcIndex = srcYBytesPerLine + (srcX * 3);
						srcPixel = ((srcData[srcIndex] & 0xFF) << 16)
								+ ((srcData[srcIndex + 1] & 0xFF) << 8)
								+ (srcData[srcIndex + 2] & 0xFF);
						break;
					case 16:
						srcIndex = srcYBytesPerLine + (srcX * 2);
						srcPixel = ((srcData[srcIndex + 1] & 0xFF) << 8) + (srcData[srcIndex] & 0xFF);
						break;
					case 8:
						srcIndex = srcYBytesPerLine + srcX;
						srcPixel = srcData[srcIndex] & 0xFF;
						break;
					case 4:
						srcIndex = srcYBytesPerLine + (srcX >> 1);
						theByte = srcData[srcIndex] & 0xFF;
						if ((srcX & 0x1) == 0) {
							srcPixel = theByte >> 4;
						} else {
							srcPixel = theByte & 0x0F;
						}
						break;
					case 2:
						srcIndex = srcYBytesPerLine + (srcX >> 2);
						theByte = srcData[srcIndex] & 0xFF;
						final int offset = 3 - (srcX % 4);
						mask = 3 << (offset * 2);
						srcPixel = (theByte & mask) >> (offset * 2);
						break;
					case 1:
						srcIndex = srcYBytesPerLine + (srcX >> 3);
						theByte = srcData[srcIndex] & 0xFF;
						mask = 1 << (7 - (srcX & 0x7));
						if ((theByte & mask) == 0) {
							srcPixel = 0;
						} else {
							srcPixel = 1;
						}
						break;
					}

					if (brtData != null) {

						switch (brtDepth) {
						case 32:
							srcIndex = brtYBytesPerLine + (srcX * 4);
							brtPixel = ((brtData[srcIndex] & 0xFF) << 24)
									+ ((brtData[srcIndex + 1] & 0xFF) << 16)
									+ ((brtData[srcIndex + 2] & 0xFF) << 8)
									+ (brtData[srcIndex + 3] & 0xFF);
							break;
						case 24:
							srcIndex = brtYBytesPerLine + (srcX * 3);
							brtPixel = ((brtData[srcIndex] & 0xFF) << 16)
									+ ((brtData[srcIndex + 1] & 0xFF) << 8)
									+ (brtData[srcIndex + 2] & 0xFF);
							break;
						case 16:
							srcIndex = brtYBytesPerLine + (srcX * 2);
							brtPixel = ((brtData[srcIndex + 1] & 0xFF) << 8) + (brtData[srcIndex] & 0xFF);
							break;
						case 8:
							srcIndex = brtYBytesPerLine + srcX;
							brtPixel = brtData[srcIndex] & 0xFF;
							break;
						case 4:
							srcIndex = brtYBytesPerLine + (srcX >> 1);
							theByte = brtData[srcIndex] & 0xFF;
							if ((srcX & 0x1) == 0) {
								brtPixel = theByte >> 4;
							} else {
								brtPixel = theByte & 0x0F;
							}
							break;
						case 2:
							srcIndex = brtYBytesPerLine + (srcX >> 2);
							theByte = brtData[srcIndex] & 0xFF;
							final int offset = 3 - (srcX % 4);
							mask = 3 << (offset * 2);
							brtPixel = (theByte & mask) >> (offset * 2);
							break;
						case 1:
							srcIndex = brtYBytesPerLine + (srcX >> 3);
							theByte = brtData[srcIndex] & 0xFF;
							mask = 1 << (7 - (srcX & 0x7));
							if ((theByte & mask) == 0) {
								brtPixel = 0;
							} else {
								brtPixel = 1;
							}
							break;
						}
					}
				}

				int srcAlpha = 255;

				if (transparentPixel != null) {
					for (final int transPixel : transparentPixel) {
						if (transPixel == srcPixel) {
							srcAlpha = 0;
						}
					}
				} else if (src.transparentPixel != -1) {
					if (src.transparentPixel == srcPixel) {
						srcAlpha = 0;
					}
				} else if (src.alpha != -1) {
					srcAlpha = src.alpha;
				} else if (src.alphaData != null) {
					srcAlpha = srcAlphaData[alphaIndex] & 0xFF;
				}

				srcAlpha = (int) (srcAlpha * externalAlpha);

				if (srcAlpha == 0) {
					// pixel is transparent
					continue;
				}

				if (srcPalette.isDirect) {

					srcRed = srcPixel & srcPalette.redMask;
					srcRed = (srcPalette.redShift < 0)
							? srcRed >>> -srcPalette.redShift
							: srcRed << srcPalette.redShift;

					srcGreen = srcPixel & srcPalette.greenMask;
					srcGreen = (srcPalette.greenShift < 0)
							? srcGreen >>> -srcPalette.greenShift
							: srcGreen << srcPalette.greenShift;

					srcBlue = srcPixel & srcPalette.blueMask;
					srcBlue = (srcPalette.blueShift < 0)
							? srcBlue >>> -srcPalette.blueShift
							: srcBlue << srcPalette.blueShift;

				} else {

					final RGB rgb = srcColors[srcPixel];

					srcRed = rgb.red;
					srcGreen = rgb.green;
					srcBlue = rgb.blue;
				}

				if (brtData != null) {

					// get brightness value

					if (brtPalette.isDirect) {

						brtRed = brtPixel & brtPalette.redMask;
						brtRed = (brtPalette.redShift < 0)
								? brtRed >>> -brtPalette.redShift
								: brtRed << brtPalette.redShift;

						brtGreen = brtPixel & brtPalette.greenMask;
						brtGreen = (brtPalette.greenShift < 0)
								? brtGreen >>> -brtPalette.greenShift
								: brtGreen << brtPalette.greenShift;

						brtBlue = brtPixel & brtPalette.blueMask;
						brtBlue = (brtPalette.blueShift < 0)
								? brtBlue >>> -brtPalette.blueShift
								: brtBlue << brtPalette.blueShift;

					} else {

						final RGB rgb = brtColors[brtPixel];

						brtRed = rgb.red;
						brtGreen = rgb.green;
						brtBlue = rgb.blue;
					}

					// average brightness: 0 ... 1.0
					rgbBrightness = (brtRed + brtGreen + brtBlue) / 765f; // 3 * 255;
				}

				int dstRed, dstGreen, dstBlue, dstAlpha;

				if (srcAlpha == 255) {

					// opaque

					dstRed = srcRed;
					dstGreen = srcGreen;
					dstBlue = srcBlue;

					dstAlpha = srcAlpha;

				} else {

					// transparent

					dstRed = dstData[dataIndex + 2] & 0xff;
					dstGreen = dstData[dataIndex + 1] & 0xff;
					dstBlue = dstData[dataIndex] & 0xff;

					dstRed += (srcRed - dstRed) * srcAlpha / 255;
					dstGreen += (srcGreen - dstGreen) * srcAlpha / 255;
					dstBlue += (srcBlue - dstBlue) * srcAlpha / 255;

					dstAlpha = dstAlphaData[alphaIndex] & 0xFF;
					dstAlpha += (srcAlpha - dstAlpha) * srcAlpha / 255;
				}

				if (brtData != null) {

					/*
					 * adjust brightness from the brightness data, this feature is used to apply the
					 * brightness of a relief to a map
					 */

					if (brtAlphaData != null) {
						brtAlpha = (float) (brtAlphaData[alphaIndex] & 0xFF) / 255;
					}

					final float noBrightness = 1 - selectedBrightness;
					final float alphaNoBrightness = (1 - brtAlpha) * selectedBrightness;
					final float brightnessYes = brtAlpha * selectedBrightness;

					final float adjBrightness = brightnessYes * rgbBrightness;

					dstRed = (int) ((dstRed * noBrightness) + (dstRed * alphaNoBrightness) + (dstRed * adjBrightness));
					dstGreen = (int) ((dstGreen * noBrightness) + (dstGreen * alphaNoBrightness) + (dstGreen * adjBrightness));
					dstBlue = (int) ((dstBlue * noBrightness) + (dstBlue * alphaNoBrightness) + (dstBlue * adjBrightness));
				}

				dstData[dataIndex] = (byte) (dstBlue & 0xff);
				dstData[dataIndex + 1] = (byte) (dstGreen & 0xff);
				dstData[dataIndex + 2] = (byte) (dstRed & 0xff);

				dstAlphaData[alphaIndex] = (byte) (dstAlpha & 0xff);
			}
		}
	}

	/**
	 * @return Returns image data of an image which has no transparency, these images are faster
	 *         painted
	 */
	public ImageData getImageData() {

		// no transparency 
		_tileImageData.alphaData = null;
		_tileImageData.transparentPixel = -1;

		return _tileImageData;
	}

	public void setBackgroundColor(final int backgroundColor) {

		final byte blue = (byte) ((backgroundColor & 0xFF0000) >> 16);
		final byte green = (byte) ((backgroundColor & 0xFF00) >> 8);
		final byte red = (byte) ((backgroundColor & 0xFF) >> 0);

		final ImageData dst = _tileImageData;

		final byte[] dstData = dst.data;
		final int dstWidth = dst.width;
		final int dstHeight = dst.height;
		final int dstBytesPerLine = dst.bytesPerLine;

		for (int dstY = 0; dstY < dstHeight; dstY++) {

			final int dstYBytesPerLine = dstY * dstBytesPerLine;

			for (int dstX = 0; dstX < dstWidth; dstX++) {

				final int dataIndex = dstYBytesPerLine + (dstX * 3);

				dstData[dataIndex] = blue;
				dstData[dataIndex + 1] = green;
				dstData[dataIndex + 2] = red;
			}
		}

	}
}
