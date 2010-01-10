package de.byteholder.geoclipse.map;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

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

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.logging.StatusUtil;
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
	 * max. number of images in the image cache
	 */
	private static int								MAX_CACHE_ENTRIES			= 150;

	/**
	 * relative OS path for storing offline map image files
	 */
	public static final String						TILE_OFFLINE_CACHE_OS_PATH	= "offline-map";							//$NON-NLS-1$

	private final ConcurrentHashMap<String, Image>	fImageCache					= new ConcurrentHashMap<String, Image>();
	private final ConcurrentLinkedQueue<String>		fImageCacheFifo				= new ConcurrentLinkedQueue<String>();

	private final TileFactoryInfo_OLD					fFactoryInfo;

	/**
	 * Path from user preferences where tile images are stored
	 */
	private static String							fOSTileCachePath;

	private static boolean							fUseOffLineCache;

	private TileFactory_OLD								fTileFactory;

	/**
	 * This display is used because {@link Display#getDefault()} is synchronized which propably
	 * causes the UI to be not smooth when images are loaded and the map is dragged at the same time
	 */
	private Display									fDisplay;

	private static final ReentrantLock				CREATE_DIR_LOCK				= new ReentrantLock();

	/**
	 * @return OS path for the tile cache or null, when offline cache is not used and otherwise
	 */
	public static String getTileCacheOSPath() {

		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE)) {
			return fOSTileCachePath;
		}

		return null;
	}

	private static void setTileCachePath() {

		// get status if the tile is offline cache is activated
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		fUseOffLineCache = prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE);

		if (fUseOffLineCache) {

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
				fUseOffLineCache = false;
				return;
			}

			final IPath tileCachePath = new Path(workingDirectory).append(TILE_OFFLINE_CACHE_OS_PATH);

			if (tileCachePath.toFile().exists() == false) {
				tileCachePath.toFile().mkdirs();
			}

			fOSTileCachePath = tileCachePath.toOSString();
		}
	}

	/**
	 * @param tileFactory
	 * @param factoryInfo
	 * @param display
	 */
	public TileImageCache(final TileFactory_OLD tileFactory, final TileFactoryInfo_OLD factoryInfo, final Display display) {

		fTileFactory = tileFactory;
		fFactoryInfo = factoryInfo;
		fDisplay = display;

		setTileCachePath();
	}

	public boolean contains(final URI uri) {
		return fImageCache.containsKey(uri);
	}

	/**
	 * creates the map image, add it into the image cache and save the offline image
	 * 
	 * @param tileKey
	 * @param loadedImageData
	 * @param tile
	 * @param tileKey
	 * @param isSaveImage
	 * @return
	 */
	public Image createImage(	final ImageData[] loadedImageData,
								final Tile tile,
								final String tileKey,
								final boolean isSaveImage) {

		if (isSaveImage && fUseOffLineCache) {
			saveOfflineImage(tile, loadedImageData);
		}

		return createImageInternal(tileKey, loadedImageData[0]);
	}

	/**
	 * dim tile image, this must be synchronized because other threads could call this method at the
	 * same time and the map tiles are not drawn
	 * 
	 * @param tileKey
	 *            tile key which is used to keep the image in the cache
	 * @param loadedImageData
	 * @return
	 */
	private Image createImageInternal(final String tileKey, final ImageData loadedImageData) {

		final int dimmingAlphaValue = fTileFactory.getDimLevel();
		if (dimmingAlphaValue == 0xFF) {

			// tile image is not dimmed

			final Image loadedImage = new Image(fDisplay, loadedImageData);

			putIntoImageCache(tileKey, loadedImage);

			return loadedImage;

		} else {

			// tile image is dimmed

			final Image loadedImage = new Image(fDisplay, loadedImageData);
			final Rectangle imageBounds = loadedImage.getBounds();

			final Image tileImage = new Image(fDisplay, imageBounds);

			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			//
			// run in the UI thread
			//			
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			fDisplay.syncExec(new Runnable() {
				public void run() {

					final GC gcTileImage = new GC(tileImage);
					final Color dimColor = new Color(fDisplay, fTileFactory.getDimColor());
					{
						gcTileImage.setBackground(dimColor);
						gcTileImage.fillRectangle(imageBounds);

						gcTileImage.setAlpha(dimmingAlphaValue);
						{
							gcTileImage.drawImage(loadedImage, 0, 0);
						}
						gcTileImage.setAlpha(0xff);
					}
					dimColor.dispose();
					gcTileImage.dispose();

					loadedImage.dispose();
				}
			});

			putIntoImageCache(tileKey, tileImage);

			return tileImage;
		}
	}

	/**
	 * Dispose all cached images and clear the cache.
	 */
	public synchronized void dispose() {

		final Collection<Image> images = fImageCache.values();
		for (final Image image : images) {
			if (image != null) {
				try {
					image.dispose();
				} catch (final Exception e) {
					// ignore, another thread can have set the image to null
				}
			}
		}

		fImageCache.clear();
		fImageCacheFifo.clear();
	}

	/**
	 * Loads the tile image from the offline image file
	 * 
	 * @param tile
	 * @return Returns image data from the offline image file or <code>null</code> otherwise
	 */
	public ImageData[] getOfflineTileImageData(final Tile tile) {

		if (fUseOffLineCache == false) {
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

			final File tileImageFile = tileImagePath.toFile();
			if (tileImageFile.exists()) {

				// get image for this tile

				final String osTilePath = tileImagePath.toOSString();

				tile.setOfflinePath(osTilePath);

				try {

					// load image

					final ImageData[] loadedImageData = new ImageLoader().load(osTilePath);

					if (loadedImageData != null && loadedImageData.length > 0 && loadedImageData[0] != null) {

						// loading image data was successful

						return loadedImageData;
					}

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

	/**
	 * @param tile
	 * @return Returns the tile image from the cache, returns <code>null</code> when the image is
	 *         not available in the cache
	 */
	public Image getTileImage(final Tile tile) {

		// get image from the cache

		final Image cachedImage = fImageCache.get(tile.getTileKey());

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
		IPath tileCachePath = new Path(fOSTileCachePath);

		// append tile custom path 
		final String tileCustomPath = tile.getTileCustomPath();
		if (tileCustomPath != null) {
			tileCachePath = tileCachePath.append(tileCustomPath);
		}

		// append tile path
		final IPath tilePath = tile.getTileFactory().getInfo().getTileOSPath(
				tileCachePath.toOSString(),
				tile.getX(),
				tile.getY(),
				tile.getZoom(),
				tile);

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
		final int cacheSize = fImageCacheFifo.size();
		if (cacheSize >= MAX_CACHE_ENTRIES) {

			// remove cache items 
			for (int cacheIndex = MAX_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				// remove and dispose oldest image

				final String headTileKey = fImageCacheFifo.poll();
				final Image oldestImage = fImageCache.remove(headTileKey);

				if (oldestImage != null) {
					try {
						oldestImage.dispose();
					} catch (final Exception e) {
						// it is possible that the image is already disposed by another thread
					}
				}
			}
		}

		try {

			/*
			 * check if the image is already in the cache
			 */
			final Image cachedImage = fImageCache.get(tileKey);
			if (cachedImage != null) {

				if (cachedImage == tileImage) {

					// image is already in the cache

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
					fImageCache.put(tileKey, tileImage);

					return;
				}

			} else {

				// put a new image into the cache
				fImageCache.put(tileKey, tileImage);
				fImageCacheFifo.add(tileKey);

				return;
			}

		} catch (final Exception e) {
			StatusUtil.log(e.getMessage(), e);
		}

	}

	/**
	 * @param tile
	 * @param tileImageData
	 *            {@link ImageData} which is loaded from the internet
	 */
	void saveOfflineImage(final Tile tile, final ImageData[] tileImageData) {

		if (fUseOffLineCache == false) {
			return;
		}

		final TileFactory_OLD tileFactory = tile.getTileFactory();
		final IPath tileImageFilePath = getTileImagePath(tile);

		if (tileImageFilePath == null) {
			StatusUtil.log("a tile path is not available in the tile factory: " // $NON-NLS-1$
					+ tileFactory.getInfo().getFactoryName(), new Exception());
			return;
		}

		final String imageOSFilePath = tileImageFilePath.toOSString();

		final IPath tilePathWithoutExt = tileImageFilePath.removeFileExtension();

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
			imageLoader.data = tileImageData;

			imageType = tileImageData[0].type;

			final String fileExtension = MapProviderManager.getImageFileExtension(imageType);
			final String extension;
			if (fileExtension == null) {
				extension = MapProviderManager.FILE_EXTENSION_PNG;
				imageType = SWT.IMAGE_PNG;
			} else {
				extension = fileExtension;
			}

			final IPath fullImageFilePath = tilePathWithoutExt.addFileExtension(extension);

			imageLoader.save(fullImageFilePath.toOSString(), imageType);

			// update map provider with the image format
			tileFactory.getMapProvider().setImageFormat(MapProviderManager.getImageMimeType(imageType));

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
	void setOfflineImageAvailability(final Tile tile) {

		if (fUseOffLineCache == false) {
			return;
		}

		if (tile.isOfflimeImageAvailable()) {
			return;
		}

		/*
		 * check if the image is available as offlime image
		 */
		try {

			final IPath tilePath = fFactoryInfo.getTileOSPath(//
					fOSTileCachePath,
					tile.getX(),
					tile.getY(),
					tile.getZoom(),
					tile);

			if (tilePath == null) {
				return;
			}

			final File tileFile = tilePath.toFile();
			if (tileFile.exists()) {

				// offline image is available

				tile.setIsOfflineImageAvailable(true);

				return;
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return;
	}

}
