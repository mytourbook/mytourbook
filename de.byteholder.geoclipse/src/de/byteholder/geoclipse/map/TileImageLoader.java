package de.byteholder.geoclipse.map;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.event.TileEventId;

/**
 * This class loads the tile images. The run method is called from the executer in the thread queue.
 */
class TileImageLoader implements Runnable {

	private final TileFactoryImpl	fTileFactoryImpl;

	/**
	 * @param defaultTileFactory
	 */
	TileImageLoader(final TileFactoryImpl defaultTileFactory) {
		fTileFactoryImpl = defaultTileFactory;
	}

	private void finalizeTile(final Tile tile, final boolean isNotifyObserver) {

		final String tileKey = tile.getTileKey();

		if (tile.isLoadingError()) {

			// keep tile with loading error in the loading queue to prevent loading it again

		} else {

			// keep tiles in the cache
			fTileFactoryImpl.getTileCache().add(tileKey, tile);

			// remove from loading map
			fTileFactoryImpl.getLoadingTiles().remove(tileKey);
		}

		// set tile state, notify observer (Map is an observer)
		tile.setLoading(false);

		if (isNotifyObserver) {
			tile.notifyImageObservers();
		}

		fTileFactoryImpl.fireTileEvent(TileEventId.TILE_END_LOADING, tile);
	}

	/**
	 * load tile from a url
	 */
	private void loadTileImage(final Tile tile) {

		String loadingError = null;
		Tile parentTile = null;
		boolean isNotifyObserver = true;
		boolean isParentFinal = false;

		try {

			boolean isSaveImage = false;

			final TileImageCache tileImageCache = fTileFactoryImpl.getTileImageCache();

			final TileFactory tileFactory = tile.getTileFactory();
			final TileFactoryInfo factoryInfo = tileFactory.getInfo();
			final boolean useOfflineImage = tileFactory.isUseOfflineImage();

			// load image from offline cache
			ImageData[] tileImageData = null;
			if (useOfflineImage) {
				tileImageData = tileImageCache.getOfflineTileImageData(tile);
			}

			if (tileImageData == null) {

				/*
				 * offline image is not available, load tile image from a url
				 */

				isSaveImage = true;
				InputStream inputStream = null;

				try {

					final ITileLoader tileLoader = factoryInfo.getTileLoader();

					if (tileLoader instanceof ITileLoader) {

						/*
						 * get image from a tile loader (this feature is used to load images
						 * from a wms server)
						 */

						try {
							inputStream = tileLoader.getTileImageStream(tile);
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

							url = fTileFactoryImpl.getURL(tile);

						} catch (final Exception e) {
							loadingError = e.getMessage();
							throw e;
						}

						try {

							inputStream = url.openStream();

						} catch (final UnknownHostException e) {

							loadingError = NLS.bind(
									Messages.DBG053_Loading_Error_UnknownHostException,
									tile.getUrl(),
									e.getMessage());

// this is hidden because it can happen very often
//							StatusUtil.log(loadingError, e);
							throw e;

						} catch (final FileNotFoundException e) {

							loadingError = NLS.bind(
									Messages.DBG052_Loading_Error_FileNotFoundException,
									tile.getUrl(),
									e.getMessage());

// this is hidden because it can happen very often
//							StatusUtil.logStatus(loadingError, e);
							throw e;

						} catch (final Exception e) {

							loadingError = NLS.bind(//
									Messages.DBG054_Loading_Error_FromUrl,
									tile.getUrl(),
									e.getMessage());
// this is hidden because it can happen very often
//							StatusUtil.logStatus(loadingError, e);
							throw e;
						}
					}

					tileImageData = new ImageLoader().load(inputStream);

				} catch (final Exception e) {

					/*
					 * exception occures when loading the image, don't remove them from the
					 * loading list, so that the tiles don't get reloaded
					 */

					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (final IOException e1) {
						StatusUtil.log(e.getMessage(), e);
					}

					fTileFactoryImpl.fireTileEvent(TileEventId.TILE_ERROR_LOADING, tile);
				}
			}

			/*
			 * tile image is loaded from a url or from a offline file or is not availabel
			 */

			boolean isCreateImage = true;

			// set tile where the tile image is stored
			Tile imageTile = tile;
			String imageTileKey = tile.getTileKey();

			if (tileImageData == null) {

				// image data is empty, set error

				tile
						.setLoadingError(loadingError == null
								? Messages.DBG051_Loading_Error_EmptyImageData
								: loadingError);

				isCreateImage = false;
			}

			if (tile.isChildTile()) {

				/*
				 * the current tile is a child of a parent tile, create the parent image with
				 * this child image data
				 */

				isNotifyObserver = false;

				// save child image
				if (tileImageData != null && isSaveImage) {
					tileImageCache.saveOfflineImage(tile, tileImageData);
				}

				parentTile = tile.getParentTile();

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
						isCreateImage = true;
						isSaveImage = parentImageStatus.isSaveImage;

					} else {

						/*
						 * disable image creation, the image is created only when the parent is
						 * final
						 */

						isCreateImage = false;
					}
				}
			}

			/*
			 * create tile image
			 */
			if (isCreateImage) {

				// create/save image
				final Image tileImage = tileImageCache.createImage(tileImageData, imageTile, imageTileKey, isSaveImage);

				if (imageTile.setMapImage(tileImage) == false) {

					// keep image in loading list and set an error to prevent it loading a second time
					tile.setLoadingError(Messages.DBG049_Loading_Error_ImageIsInvalid);
				}
			}

		} catch (final Exception e) {

			// this should not happen
			StatusUtil.log(Messages.DBG048_Loading_Error_DefaultException, e);

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
	private void paintTileImage(final Tile tile, final ITilePainter tilePainter) {

		fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_START, tile);

		try {

			final Display display = Display.getDefault();

			// create RGB data for the tile
			final RGB[][] rgbData = tilePainter.drawTile(tile);

			// needs to run in the UI thread!
			display.asyncExec(new Runnable() {
				public void run() {

					/*
					 * create tile image from RGB data
					 */
					final ImageData[] paintedImageData = new ImageData[1];

					final int tileSize = rgbData[0].length;
					final Image paintedImage = new Image(display, tileSize, tileSize);

					final HashMap<Integer, Color> usedMapColors = new HashMap<Integer, Color>();

					final GC gc = new GC(paintedImage);
					{
						for (int drawX = 0; drawX < rgbData.length; drawX++) {
							final RGB[] rgbX = rgbData[drawX];

							for (int drawY = 0; drawY < rgbX.length; drawY++) {

								final RGB rgb = rgbX[drawY];

								Color mapColor = usedMapColors.get(rgb.hashCode());
								if (mapColor == null) {
									mapColor = new Color(display, rgb);
									usedMapColors.put(rgb.hashCode(), mapColor);
								}

								gc.setForeground(mapColor);
								gc.drawPoint(drawX, drawY);
							}
						}
					}
					gc.dispose();

					paintedImageData[0] = paintedImage.getImageData();
					paintedImage.dispose();

					// dispose all used colors
					for (final Color color : usedMapColors.values()) {
						color.dispose();
					}

					if (paintedImageData == null) {

						tile.setLoadingError(Messages.DBG047_Loading_Error_InvalidPaintingData);

					} else {

						/*
						 * tile image is painted
						 */

						final String tileKey = tile.getTileKey();

						final Image tileImage = fTileFactoryImpl.getTileImageCache().createImage(
								paintedImageData,
								tile,
								tileKey,
								true);

						if (tile.setMapImage(tileImage)) {

							// image is valid
							fTileFactoryImpl.getLoadingTiles().remove(tileKey);

						} else {

							// keep invalid tiles in the list of loaded images, that they are not loaded again
							tile.setLoadingError(Messages.DBG046_Loading_Error_InvalidPaintingImage);
						}
					}

					fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_END, tile);
				}
			});

		} catch (final Exception e) {

			tile.setLoadingError(Messages.DBG045_Loading_Error_PaintingError + e.getMessage());

			fTileFactoryImpl.fireTileEvent(TileEventId.SRTM_PAINTING_ERROR, tile);

			StatusUtil.log(e.getMessage(), e);

		} finally {

			finalizeTile(tile, true);
		}

	}

	public void run() {

		/*
		 * load/create tile image
		 */
		// get tile from queue
		final LinkedBlockingDeque<Tile> tileWaitingQueue = fTileFactoryImpl.getTileWaitingQueue();

		final Tile tile = tileWaitingQueue.pollLast();

		if (tile == null) {
			// waiting queue is reset
			return;
		}

		final boolean isChildTile = tile.isChildTile();
		final boolean isParentTile = fTileFactoryImpl instanceof ITileChildrenCreator && isChildTile == false;

		fTileFactoryImpl.fireTileEvent(TileEventId.TILE_START_LOADING, tile);

		if (isParentTile) {

			/*
			 * current tile is a parent tile, keep it in the loading queue until all children
			 * are
			 * loaded
			 */

			if (tile.isLoadingError()) {

				/*
				 * parent tile has a loading error, this can happen when it contains no child
				 * because it does not support the zooming level
				 */

				finalizeTile(tile, true);

			} else if (tile.isOfflimeImageAvailable()) {

				// this parent tile has no chilren which needs to be loaded, behave as a normal tile

				loadTileImage(tile);

			} else {

				// a parent is loaded and finalized when the last child was loaded
			}

		} else {

			final ITilePainter tilePainter = tile.getTileFactory().getInfo().getTilePainter();

			if (tilePainter != null) {

				paintTileImage(tile, tilePainter);

			} else {

				loadTileImage(tile);
			}
		}

		// loading has finished
		tile.setFuture(null);
	}
}
