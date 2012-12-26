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
package de.byteholder.geoclipse.map;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingDeque;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.mapprovider.MP;

/**
 * This class loads the tile images. The run method is called from the executer in the thread queue.
 */
public class TileImageLoader implements Runnable {

	/**
	 * Loads a tile image from a map provider which is contained in the tile. Tiles are retrieved
	 * from the tile waiting queue {@link MP#getTileWaitingQueue()}
	 */
	public TileImageLoader() {}

	private void finalizeTile(final Tile tile, final boolean isNotifyObserver) {

		final String tileKey = tile.getTileKey();

		if (tile.isLoadingError()) {

			// move tile from tile cache into the cache which contails tiles with errors

			MP.getErrorTiles().add(tileKey, tile);

			MP.getTileCache().remove(tileKey);
		}

		// set tile state, notify observer (Map is an observer)
		tile.setLoading(false);

		if (isNotifyObserver) {
			tile.notifyImageObservers();
		}

		MP.fireTileEvent(TileEventId.TILE_END_LOADING, tile);
	}

	/**
	 * Get tile tile image from offline file, url or tile painter
	 */
	private void getTileImage(final Tile tile) {

		String loadingError = null;
		boolean isNotifyObserver = true;
		boolean isParentFinal = false;

		Tile parentTile = null;

		try {

			boolean isSaveImage = false;

			final MP mp = tile.getMP();
			final TileImageCache tileImageCache = mp.getTileImageCache();

			final boolean useOfflineImage = mp.isUseOfflineImage();

			ImageData tileImageData = null;
			Image tileOfflineImage = null;

			// load image from offline cache
			if (useOfflineImage) {
				tileOfflineImage = tileImageCache.getOfflineImage(tile);
			}

			if (tileOfflineImage == null) {

				// offline image is not available

				isSaveImage = true;

				final ITilePainter tilePainter = mp.getTilePainter();

				if (tilePainter instanceof ITilePainter) {

					// paint tile image

					tileImageData = paintTileImage(tile, tilePainter);

				} else {

					// load tile image from a url

					InputStream inputStream = null;

					try {

						if (mp instanceof ITileLoader) {

							/*
							 * get image from a tile loader (this feature is used to load images
							 * from a wms server)
							 */

							try {
								inputStream = ((ITileLoader) mp).getTileImageStream(tile);
							} catch (final Exception e) {
								loadingError = e.getMessage();
								StatusUtil.log(loadingError, e);
								throw e;
							}

						} else {

							/*
							 * get image from a url (this was the behaviour before wms was
							 * supported)
							 */

							final URL url;

							try {

								url = mp.getTileURLEncoded(tile);

							} catch (final Exception e) {
								loadingError = e.getMessage();
								throw e;
							}

							try {

								inputStream = url.openStream();

							} catch (final FileNotFoundException e) {

								loadingError = NLS.bind(
										Messages.DBG052_Loading_Error_FileNotFoundException,
										tile.getUrl(),
										e.getMessage());

								// this is hidden because it can happen very often
								// StatusUtil.log(IMAGE_HAS_LOADING_ERROR, e);
								throw e;

							} catch (final UnknownHostException e) {

								loadingError = NLS.bind(
										Messages.DBG053_Loading_Error_UnknownHostException,
										tile.getUrl(),
										e.getMessage());

								// this is hidden because it can happen very often
								// StatusUtil.log(IMAGE_HAS_LOADING_ERROR, e);
								throw e;

							} catch (final Exception e) {

								loadingError = NLS.bind(//
										Messages.DBG054_Loading_Error_FromUrl,
										tile.getUrl(),
										e.getMessage());

								// this is hidden because it can happen very often
								// StatusUtil.log(IMAGE_HAS_LOADING_ERROR, e);
								throw e;
							}
						}

						final ImageData[] loadedImageData = new ImageLoader().load(inputStream);

						if (loadedImageData != null && loadedImageData.length > 0) {
							tileImageData = loadedImageData[0];
						}

					} catch (final Exception e) {

						/*
						 * exception occures when loading the image, don't remove them from the
						 * loading list, so that the tiles don't get reloaded
						 */

						try {
							if (inputStream != null) {

								// the stream can contain an error message from the wms server
								StatusUtil.showStatus(Util.convertStreamToString(inputStream), e);

								inputStream.close();

								isSaveImage = false;
							}
						} catch (final IOException e1) {
							StatusUtil.log(e.getMessage(), e);
						}

						MP.fireTileEvent(TileEventId.TILE_ERROR_LOADING, tile);
					}
				}
			}

			/**
			 * tile image is loaded from a url or from an offline file, is painted or is not
			 * available
			 */

			boolean isSetupImage = true;
			boolean isChildError = false;

			// set tile where the tile image is stored
			Tile imageTile = tile;
			String imageTileKey = tile.getTileKey();

			if (tileOfflineImage == null && tileImageData == null) {

				// image data is empty, set error

				tile
						.setLoadingError(loadingError == null
								? Messages.DBG051_Loading_Error_EmptyImageData
								: loadingError);

				isSetupImage = false;
			}

			parentTile = tile.getParentTile();
			if (parentTile != null) {

				/*
				 * the current tile is a child of a parent tile, create the parent image with
				 * this child image data
				 */

				isNotifyObserver = false;

				if (tileOfflineImage != null) {

					tileImageData = tileOfflineImage.getImageData();

					// when image data is used, the image is not needed any more
					tileOfflineImage.dispose();
					tileOfflineImage = null;
				}

				// save child image
				if (tileImageData != null && isSaveImage) {
					tileImageCache.saveOfflineImage(tile, tileImageData, false);
				}

				// set image into child
				final ParentImageStatus parentImageStatus = tile.createParentImage(tileImageData);

				if (parentImageStatus == null) {

					// set error into parent tile
					parentTile.setLoadingError(Messages.DBG050_Loading_Error_CannotCreateParent);

				} else {

					// check if the parent image is created
					if (parentImageStatus.isImageFinal) {

						// create image only when the parent is final

						// set image data from parent
						tileImageData = parentImageStatus.tileImageData;

						// use parent tile to store the image
						imageTile = parentTile;
						imageTileKey = parentTile.getTileKey();

						// parent is final
						isParentFinal = true;
						isSetupImage = true;
						isSaveImage = parentImageStatus.isSaveImage;
						isChildError = parentImageStatus.isChildError;

					} else {

						/*
						 * disable image creation, the image is created only when the parent is
						 * final
						 */

						isSetupImage = false;
					}
				}
			}

			/*
			 * create tile image
			 */
			if (isSetupImage) {

				// create/save image
				final Image tileImage = tileImageCache.setupImage(
						tileImageData,
						tileOfflineImage,
						imageTile,
						imageTileKey,
						isSaveImage,
						isChildError);

				if (imageTile.setMapImage(tileImage) == false) {

					// set an error to prevent it loading a second time
					tile.setLoadingError(Messages.DBG049_Loading_Error_ImageIsInvalid);
				}
			}

		} catch (final Exception e) {

			// this should not happen
			StatusUtil.log(NLS.bind(Messages.DBG048_Loading_Error_DefaultException, tile.getTileKey()), e);

		} finally {

			finalizeTile(tile, isNotifyObserver);

			if (isParentFinal) {
				finalizeTile(parentTile, true);
			}
		}
	}

	/**
	 * paint tile based on SRTM data
	 */
	private ImageData paintTileImage(final Tile tile, final ITilePainter tilePainter) {

		MP.fireTileEvent(TileEventId.SRTM_PAINTING_START, tile);

		ImageData tileImageData = null;

		try {

			/*
			 * create tile image data from RGB data
			 */

			// create RGB data for the tile
			final int[][] rgbData = tilePainter.drawTile(tile);

			final int tileSize = rgbData[0].length;

			tileImageData = new ImageData(//
					tileSize,
					tileSize,
					24,
					new PaletteData(0xFF, 0xFF00, 0xFF0000));

			final byte[] pixelData = tileImageData.data;
			final int bytesPerLine = tileImageData.bytesPerLine;

			for (int drawX = 0; drawX < rgbData.length; drawX++) {

				final int xBytesPerLine = drawX * bytesPerLine;
				final int[] rgbX = rgbData[drawX];

				for (int drawY = 0; drawY < rgbX.length; drawY++) {

					final int dataIndex = xBytesPerLine + (drawY * 3);

					final int rgb = rgbX[drawY];

					final int blue = (byte) ((rgb & 0xFF0000) >> 16);
					final int green = (byte) ((rgb & 0xFF00) >> 8);
					final int red = (byte) ((rgb & 0xFF) >> 0);

					pixelData[dataIndex] = (byte) (blue & 0xff);
					pixelData[dataIndex + 1] = (byte) (green & 0xff);
					pixelData[dataIndex + 2] = (byte) (red & 0xff);
				}
			}

			MP.fireTileEvent(TileEventId.SRTM_PAINTING_END, tile);

		} catch (final Exception e) {

			tile.setLoadingError(Messages.DBG045_Loading_Error_PaintingError + e.getMessage());

			MP.fireTileEvent(TileEventId.SRTM_PAINTING_ERROR, tile);

			StatusUtil.log(e.getMessage(), e);
		}

		return tileImageData;
	}

	public void run() {

		/*
		 * load/create tile image
		 */
		// get tile from queue
		final LinkedBlockingDeque<Tile> tileWaitingQueue = MP.getTileWaitingQueue();

		final Tile tile = tileWaitingQueue.pollFirst();

		if (tile == null) {
			// it's possible that the waiting queue was reset
			return;
		}

		final MP mp = tile.getMP();
		final boolean isParentTile = mp instanceof ITileChildrenCreator;
		{
			// current tile is in the viewport of the map

			MP.fireTileEvent(TileEventId.TILE_START_LOADING, tile);

			if (isParentTile) {

				// current tile is a parent tile

				if (tile.isLoadingError()) {

					/*
					 * parent tile has a loading error, this can happen when it contains no child
					 * because it does not support the zooming level
					 */

					finalizeTile(tile, true);

				} else if (tile.isOfflimeImageAvailable()) {

					// parent tile has no chilren which needs to be loaded, behave as a normal tile

					getTileImage(tile);

				} else {

					// a parent gets finalized when the last child is loaded
				}

			} else {

				// tile is 'normal' or a children

				getTileImage(tile);
			}
		}

		// loading has finished
		tile.setFuture(null);
	}
}
