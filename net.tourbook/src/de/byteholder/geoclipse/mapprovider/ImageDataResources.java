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

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.UI;

/**
 */
public class ImageDataResources {

	private static final ReentrantLock	TILE_LOCK			= new ReentrantLock();
	private static final ReentrantLock	NEIGHBOR_LOCK		= new ReentrantLock();

	/**
	 * contains image data which are drawn into this tile
	 */
	private ImageData					_tileImageData		= null;

	/**
	 * contains image data which are drawn from neighbor tiles
	 */
	private ImageData					_neighborImageData	= null;

	private final int					_tileSize;

	public ImageDataResources(final int tileSize) {
		_tileSize = tileSize;
	}

	/**
	 * this is sysnchonized because multiple threads can call this method
	 * 
	 * @param dst
	 * @param src
	 * @param srcXStart
	 * @param srcYStart
	 * @param width
	 * @param height
	 */
	public synchronized void drawImageData(	final ImageData dst,
											final ImageData src,
											final int srcXStart,
											final int srcYStart,
											final int width,
											final int height) {

		// source data
		final byte[] srcData = src.data;
		final PaletteData srcPalette = src.palette;
		final byte[] srcAlphaData = src.alphaData;
		final int srcBytesPerLine = src.bytesPerLine;
		int srcIndex;
		int srcPixel = 0;

		// destination data
		final byte[] dstData = dst.data;
		byte[] dstAlphaData = dst.alphaData;
		final int dstBytesPerLine = dst.bytesPerLine;

		int srcRed, srcGreen, srcBlue;

		// create alpha data
		if (dstAlphaData == null) {
			dst.alphaData = dstAlphaData = new byte[width * height];
		}

		final int srcMaxX = srcXStart + width;
		final int srcMaxY = srcYStart + height;

		for (int srcY = srcYStart, dstY = 0; srcY < srcMaxY; srcY++, dstY++) {

			final int srcYBytesPerLine = srcY * srcBytesPerLine;
			final int dstYBytesPerLine = dstY * dstBytesPerLine;

			for (int srcX = srcXStart, dstX = 0; srcX < srcMaxX; srcX++, dstX++) {

				final int dataIndex = dstYBytesPerLine + (dstX * 3);
				final int alphaIndex = dstY * width + dstX;

				// check bounds
				if (!(0 <= dstX && dstX < dst.width && 0 <= dstY && dstY < dst.height)) {
					continue;
				}

				// get pixel value
				srcIndex = srcYBytesPerLine + (srcX * 3);
				srcPixel = ((srcData[srcIndex] & 0xFF) << 16)
						+ ((srcData[srcIndex + 1] & 0xFF) << 8)
						+ (srcData[srcIndex + 2] & 0xFF);

				int srcAlpha = 255;

				if (src.transparentPixel != -1) {
					if (src.transparentPixel == srcPixel) {
						srcAlpha = 0;
				}
				} else if (src.alpha != -1) {
					srcAlpha = src.alpha;
				} else if (src.alphaData != null) {
					srcAlpha = srcAlphaData[alphaIndex] & 0xFF;
				}

				if (srcAlpha == 0) {
					// pixel is transparent
					continue;
				}

				// get rgb values
				srcRed = srcPixel & srcPalette.redMask;
				srcRed = (srcPalette.redShift < 0) ? srcRed >>> -srcPalette.redShift : srcRed << srcPalette.redShift;

				srcGreen = srcPixel & srcPalette.greenMask;
				srcGreen = (srcPalette.greenShift < 0)
						? srcGreen >>> -srcPalette.greenShift
						: srcGreen << srcPalette.greenShift;

				srcBlue = srcPixel & srcPalette.blueMask;
				srcBlue = (srcPalette.blueShift < 0)
						? srcBlue >>> -srcPalette.blueShift
						: srcBlue << srcPalette.blueShift;

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

				dstData[dataIndex] = (byte) (dstBlue & 0xff);
				dstData[dataIndex + 1] = (byte) (dstGreen & 0xff);
				dstData[dataIndex + 2] = (byte) (dstRed & 0xff);

				dstAlphaData[alphaIndex] = (byte) (dstAlpha & 0xff);
			}
		}
	}

	/**
	 * Draws image data from src into the neighbor image data
	 * 
	 * @param src
	 *            the source image data
	 * @param srcXStart
	 * @param srcYStart
	 * @param width
	 * @param height
	 */
	public void drawNeighborImageData(	final ImageData src,
										final int srcXStart,
										final int srcYStart,
										final int width,
										final int height) {
		if (_neighborImageData == null) {

			NEIGHBOR_LOCK.lock();
			{
				try {

					// check again
					if (_neighborImageData == null) {
						_neighborImageData = UI.createTransparentImageData(_tileSize, Map.getTransparentRGB());
					}

				} finally {
					NEIGHBOR_LOCK.unlock();
				}
			}
		}

		drawImageData(_neighborImageData, src, srcXStart, srcYStart, width, height);
	}

	/**
	 * Draws image data from src into the tile image data
	 * 
	 * @param src
	 *            the source image data
	 * @param srcXStart
	 * @param srcYStart
	 * @param width
	 * @param height
	 */
	public void drawTileImageData(	final ImageData src,
									final int srcXStart,
									final int srcYStart,
									final int width,
									final int height) {

		if (_tileImageData == null) {

			TILE_LOCK.lock();
			{
				try {

					// check again
					if (_tileImageData == null) {
						_tileImageData = UI.createTransparentImageData(_tileSize, Map.getTransparentRGB());
					}

				} finally {
					TILE_LOCK.unlock();
				}
			}
		}

		drawImageData(_tileImageData, src, srcXStart, srcYStart, width, height);
	}

	/**
	 * @return Returns the neighbor image data or <code>null</code> when there is no image data for
	 *         the
	 *         neighbor tiles
	 */
	public ImageData getNeighborImageData() {
		return _neighborImageData;
	}

	/**
	 * @return Returns the tile image data or <code>null</code> when there is no image data for the
	 *         tile
	 */
	public ImageData getTileImageData() {
		return _tileImageData;
	}

}
