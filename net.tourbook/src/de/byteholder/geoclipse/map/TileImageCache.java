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

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

/**
 * This cache manages map images by caching and saving the images for the offline mode. The cached
 * images can be dimmed, saved offline image are not dimmed.
 * 
 * @author joshua.marinacci@sun.com
 * @author Michael Kanis
 * @author Wolfgang Schramm
 */
public class TileImageCache {


	/**
	 * relative OS path for storing offline map image files
	 */
	public static final String								TILE_OFFLINE_CACHE_OS_PATH		= "offline-map";							//$NON-NLS-1$

	private static final ConcurrentHashMap<String, Image>	_imageCache						= new ConcurrentHashMap<String, Image>();
	private static final ConcurrentLinkedQueue<String>		_imageCacheFifo					= new ConcurrentLinkedQueue<String>();

	/**
	 * Path from user preferences where tile images are stored
	 */
	private static String									_osTileCachePath;

	private static boolean									_useOffLineCache;

	private static final ReentrantLock						CREATE_DIR_LOCK					= new ReentrantLock();
	private static final ReentrantLock						CACHE_LOCK						= new ReentrantLock();

	/**
	 * This display is used because {@link Display#getDefault()} is synchronized which propably
	 * causes the UI to be not smooth when images are loaded and the map is dragged at the same time
	 */
	private Display											_display;

	private int												_maxCacheSize					= 10;

	/**
	 * @param factoryInfo
	 * @param display
	 */
	public TileImageCache(final int maxCacheSize) {

		_maxCacheSize = maxCacheSize;

		_display = Display.getDefault();

		setTileCachePath();
	}

	/**
	 * @return OS path for the tile cache or <code>null</code> when offline cache is not used or
	 *         otherwise
	 */
	public static String getTileCacheOSPath() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE)) {
			return _osTileCachePath;
		}

		return null;
	}

	private static void setTileCachePath() {

		// get status if the tile is offline cache is activated
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		_useOffLineCache = prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE);

		if (_useOffLineCache) {

			/*
			 * check and create tile cache path
			 */
			String workingDirectory;

			final boolean useDefaultLocation = prefStore
					.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION);
			if (useDefaultLocation) {
				workingDirectory = Platform.getInstanceLocation().getURL().getPath();
			} else {
				workingDirectory = prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH);
			}

			if (new File(workingDirectory).exists() == false) {
				System.err.println("working directory is not available: " + workingDirectory); //$NON-NLS-1$
				_useOffLineCache = false;
				return;
			}

			final IPath tileCachePath = new Path(workingDirectory).append(TILE_OFFLINE_CACHE_OS_PATH);

			if (tileCachePath.toFile().exists() == false) {
				tileCachePath.toFile().mkdirs();
			}

			_osTileCachePath = tileCachePath.toOSString();
		}
	}

	public boolean contains(final URI uri) {
		return _imageCache.containsKey(uri);
	}

	/**
	 * Dispose all cached images and clear the cache.
	 */
	public synchronized void dispose() {

		final Collection<Image> images = _imageCache.values();
		for (final Image image : images) {
			if (image != null) {
				try {
					image.dispose();
				} catch (final Exception e) {
					// ignore, another thread can have set the image to null
				}
			}
		}

		_imageCache.clear();
		_imageCacheFifo.clear();
	}

	/**
	 * @param tileImagePath
	 * @return Returns the path for the offline image or <code>null</code> when the image is not
	 *         available
	 */
	private IPath getCheckedOfflineImagePath(final IPath tileImagePath) {

		File tileImageFile = tileImagePath.toFile();

		if (tileImageFile.exists()) {

			return tileImagePath;

		} else {

			// test a part image

			final String fileExt = tileImagePath.getFileExtension();
			final IPath pathWithoutExt = tileImagePath.removeFileExtension();

			final String partFileName = pathWithoutExt.lastSegment() + MapProviderManager.PART_IMAGE_FILE_NAME_SUFFIX;

			final IPath partFilePath = pathWithoutExt
					.removeLastSegments(1)
					.append(partFileName)
					.addFileExtension(fileExt);

			tileImageFile = partFilePath.toFile();

			if (tileImageFile.exists()) {
				return partFilePath;
			}
		}

		return null;
	}

	/**
	 * Loads the tile image from the offline image file
	 * 
	 * @param tile
	 * @return Returns image from offline image file or <code>null</code> when loading fails
	 */
	Image getOfflineTileImageData(final Tile tile) {

		if (_useOffLineCache == false) {
			return null;
		}

		/*
		 * try to get the image from the offline image files
		 */
		try {

			final IPath tileImagePath = getTileImagePath(tile);
			if (tileImagePath == null) {
				// offline image path is not available
				return null;
			}

			final IPath offlineImagePath = getCheckedOfflineImagePath(tileImagePath);
			if (offlineImagePath != null) {

				// get image for this tile

				final String osTileImagePath = offlineImagePath.toOSString();

				tile.setOfflinePath(osTileImagePath);

				try {

					/*
					 * load image with the constructor which is 20 times faster than loading the
					 * image with an imageloader
					 */

					return new Image(_display, osTileImagePath);

				} catch (final Exception e) {

					// this file seems to be corrupted

					tile.setOfflineError(true);

					/*
					 * it happened too often when zooming fast in/out
					 */
//					StatusUtil.logStatus("Error loading image from file: " + osTilePath, e);//$NON-NLS-1$
				}
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return null;
	}

//	/**
//	 * Loads the tile image from the offline image file
//	 *
//	 * @param tile
//	 * @return Returns image data from the offline image file or <code>null</code> otherwise
//	 */
//	ImageData getOfflineTileImageData_OLD(final Tile tile) {
//
//		if (_useOffLineCache == false) {
//			return null;
//		}
//
//		/*
//		 * try to get the image from the offline image files
//		 */
//		try {
//
//			final IPath tileImagePath = getTileImagePath(tile);
//			if (tileImagePath == null) {
//				// offline image path is not available
//				return null;
//			}
//
//			final File tileImageFile = tileImagePath.toFile();
//			if (tileImageFile.exists()) {
//
//				// get image for this tile
//
//				final String osTilePath = tileImagePath.toOSString();
//
//				tile.setOfflinePath(osTilePath);
//
//				try {
//
//					// load image
//
//					final ImageData[] loadedImageData = new ImageLoader().load(osTilePath);
//
//					if (loadedImageData != null && loadedImageData.length > 0 && loadedImageData[0] != null) {
//
//						// loading image data was successful
//
//						return loadedImageData[0];
//					}
//
//				} catch (final Exception e) {
//
//					// this file seems to be corrupted
//
//					tile.setOfflineError(true);
//
//					/*
//					 * it happened too often when zooming fast in/out
//					 */
////					StatusUtil.logStatus("Error loading image from file: " + osTilePath, e);//$NON-NLS-1$
//				}
//			}
//
//		} catch (final Exception e) {
//			StatusUtil.log(e);
//		}
//
//		return null;
//	}

	/**
	 * @param tile
	 * @return Returns the tile image from the cache, returns <code>null</code> when the image is
	 *         not available in the cache or is disposed
	 */
	public Image getTileImage(final Tile tile) {

		// get image from the cache

		final Image cachedImage = _imageCache.get(tile.getTileKey());

		if (cachedImage != null && cachedImage.isDisposed() == false) {
			return cachedImage;
		}

		if (tile.isLoading()) {
			return null;
		}

		setOfflineImageAvailability(tile);

		return null;
	}

	/**
	 * @param tile
	 * @return Returns the tile image os file path or <code>null</code> when the path is not
	 *         available
	 */
	private IPath getTileImagePath(final Tile tile) {

		// get tile image cache path
		IPath tileCachePath = new Path(_osTileCachePath);

		// append tile custom path
		final String tileCustomPath = tile.getTileCustomPath();
		if (tileCustomPath != null) {
			tileCachePath = tileCachePath.append(tileCustomPath);
		}

		// append tile path
		final IPath tilePath = tile.getMP().getTileOSPath(tileCachePath.toOSString(), tile);

		return tilePath;
	}

	/**
	 * Put tile image into the image cache
	 * 
	 * @param tileKey
	 * @param tileImage
	 */
	private void putIntoImageCache(final String tileKey, final Image tileImage) {

		/*
		 * check if the max number of images is reached, remove oldest image from the cache
		 */
		final int cacheSize = _imageCacheFifo.size();
		if (cacheSize >= _maxCacheSize + 10) {

			CACHE_LOCK.lock();
			{
				try {

					// remove cache items
					for (int cacheIndex = _maxCacheSize; cacheIndex < cacheSize; cacheIndex++) {

						// remove and dispose oldest image

						final String headTileKey = _imageCacheFifo.poll();
						if (headTileKey == null) {
							// queue is empty -> nothing more to do
							break;
						}

						final Image oldestImage = _imageCache.remove(headTileKey);

						if (oldestImage != null) {
							try {
								oldestImage.dispose();
							} catch (final Exception e) {
								// it is possible that the image is already disposed by another thread
							}
						}
					}

				} finally {
					CACHE_LOCK.unlock();
				}
			}
		}

		try {

			/*
			 * check if the image is already in the cache
			 */
			final Image cachedImage = _imageCache.get(tileKey);
			if (cachedImage == null) {

				// put a new image into the cache

				_imageCache.put(tileKey, tileImage);
				_imageCacheFifo.add(tileKey);

				return;

			} else {

				// image is already in the cache

				if (cachedImage == tileImage) {

					// keep image in the cache, it is the same as the new image

					return;

				} else {

					// dispose cached image which has the same key but is another image

					if (cachedImage != null) {
						try {
							cachedImage.dispose();
						} catch (final Exception e) {
							// it is possible that the image is already disposed by another thread
						}
					}

					// replace existing image, the image must be already in the fifo queue
					_imageCache.put(tileKey, tileImage);

					return;
				}
			}

		} catch (final Exception e) {
			StatusUtil.log(e.getMessage(), e);
		}
	}

	/**
	 * @param tile
	 * @param tileImageData
	 *            {@link ImageData} which is loaded from the internet
	 * @param isChildError
	 */
	void saveOfflineImage(final Tile tile, final ImageData tileImageData, final boolean isChildError) {

		if (_useOffLineCache == false) {
			return;
		}

		final MP mp = tile.getMP();
		final IPath tileImageFilePath = getTileImagePath(tile);

		if (tileImageFilePath == null) {
			StatusUtil.log("a tile path is not available in the map provider: " // $NON-NLS-1$ //$NON-NLS-1$
					+ mp.getName(), new Exception());
			return;
		}

		final String imageOSFilePath = tileImageFilePath.toOSString();

		IPath tilePathWithoutExt = tileImageFilePath.removeFileExtension();

		// check tile directory
		final File tileDir = tilePathWithoutExt.removeLastSegments(1).toFile();
		if (tileDir.exists() == false) {

			/*
			 * create tile directory
			 */

			CREATE_DIR_LOCK.lock();
			{
				IStatus returnStatus = null;

				try {

					// check again, it could be created in another thread
					if (tileDir.exists() == false) {

						if (tileDir.mkdirs() == false) {

							/*
							 * disabled because it happened too often
							 */
							StatusUtil
									.log("tile path cannot be created: " + tileDir.getAbsolutePath(), new Exception());//$NON-NLS-1$

							returnStatus = Status.OK_STATUS;
						}
					}
				} finally {
					CREATE_DIR_LOCK.unlock();
				}

				if (returnStatus != null) {
					return;
				}
			}
		}

		int imageType = 0;

		try {

			final ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { tileImageData };

			imageType = tileImageData.type;

			final String fileExtension = MapProviderManager.getImageFileExtension(imageType);
			final String extension;
			if (fileExtension == null) {
				extension = MapProviderManager.FILE_EXTENSION_PNG;
				imageType = SWT.IMAGE_PNG;
			} else {
				extension = fileExtension;
			}

			if (isChildError) {

				/*
				 * create a part image which is an image where not all children are loaded
				 */

				final String fileName = tilePathWithoutExt.lastSegment()
						+ MapProviderManager.PART_IMAGE_FILE_NAME_SUFFIX;

				tilePathWithoutExt = tilePathWithoutExt.removeLastSegments(1).append(fileName);
			}

			final IPath fullImageFilePath = tilePathWithoutExt.addFileExtension(extension);

			imageLoader.save(fullImageFilePath.toOSString(), imageType);

			// update map provider with the image format
			mp.setImageFormat(MapProviderManager.getImageMimeType(imageType));

		} catch (final Exception e) {

			/*
			 * disabled because it happened to often
			 */
			StatusUtil.log("cannot save tile image:" + imageOSFilePath + " - image type:" + imageType, e);//$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Checks if the offline image is available in the file system and set's the state into the tile
	 * which can be retrieved with {@link Tile#isOfflimeImageAvailable()}
	 * 
	 * @param tile
	 *            the tile which is checked
	 */
	public void setOfflineImageAvailability(final Tile tile) {

		if (_useOffLineCache == false) {
			return;
		}

		// don't check again when it's already available
		if (tile.isOfflimeImageAvailable()) {
			return;
		}

		// check if the image is available as offlime image
		try {

			final IPath tileImagePath = tile.getMP().getTileOSPath(_osTileCachePath, tile);

			if (tileImagePath == null) {
				return;
			}

			final IPath offlineImagePath = getCheckedOfflineImagePath(tileImagePath);
			if (offlineImagePath != null) {

				// offline image is available

				tile.setIsOfflineImageAvailable(true);

				return;
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return;
	}

	/**
	 * Creates the map image and add it into the image cache. When loadedImageData is set, it is
	 * saved as an offline image.
	 * 
	 * @param loadedImageData
	 * @param tileOfflineImage
	 * @param tile
	 * @param tileKey
	 * @param isSaveImage
	 * @param isChildError
	 * @return
	 */
	Image setupImage(	final ImageData loadedImageData,
						final Image tileOfflineImage,
						final Tile tile,
						final String tileKey,
						final boolean isSaveImage,
						final boolean isChildError) {

		if (loadedImageData != null && isSaveImage && _useOffLineCache) {
			saveOfflineImage(tile, loadedImageData, isChildError);
		}

		return setupImageInternal(tile, tileKey, loadedImageData, tileOfflineImage);
	}

	/**
	 * dim tile image, this must be synchronized because other threads could call this method at the
	 * same time and the map tiles are not drawn
	 * 
	 * @param tile
	 * @param tileKey
	 *            tile key which is used to keep the image in the cache
	 * @param loadedImageData
	 * @param tileOfflineImage
	 * @return
	 */
	private Image setupImageInternal(	final Tile tile,
										final String tileKey,
										final ImageData loadedImageData,
										final Image tileOfflineImage) {

		final MP mp = tile.getMP();

		final int dimmingAlphaValue = mp.getDimLevel();
		if (dimmingAlphaValue == 0xFF) {

			// tile image is not dimmed

			final Image tileImage = tileOfflineImage != null ? //
					tileOfflineImage
					: new Image(_display, loadedImageData);

			putIntoImageCache(tileKey, tileImage);

			return tileImage;

		} else {

			// tile image is dimmed

			final Image tileImage = tileOfflineImage != null ? //
					tileOfflineImage
					: new Image(_display, loadedImageData);

			final Rectangle imageBounds = tileImage.getBounds();

			final Image dimmedImage = new Image(_display, imageBounds);

			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			//
			// run in the UI thread
			//
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			_display.syncExec(new Runnable() {
				public void run() {

					final GC gcTileImage = new GC(dimmedImage);
					final Color dimColor = new Color(_display, mp.getDimColor());
					{
						gcTileImage.setBackground(dimColor);
						gcTileImage.fillRectangle(imageBounds);

						gcTileImage.setAlpha(dimmingAlphaValue);
						{
							gcTileImage.drawImage(tileImage, 0, 0);
						}
						gcTileImage.setAlpha(0xff);
					}
					dimColor.dispose();
					gcTileImage.dispose();

					tileImage.dispose();
				}
			});

			putIntoImageCache(tileKey, dimmedImage);

			return dimmedImage;
		}
	}

}
