/**
 * DefaultTileFactory.java
 *
 * Created on June 27, 2006, 2:20 PM
 *
 * 2007-05-01
 *  - reversed the zoom logic: now zoom level 0 is like zoom level 0 in most
 *    map servers the farest away; it means that the world fits completely
 *    on one tile
 *  - added methods zoomIn() and zoomOut()
 */

package de.byteholder.geoclipse.map;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.util.Util;
import de.byteholder.gpx.GeoPosition;

/**
 * A tile factory which configures itself using a TileFactoryInfo object and uses a Google Maps like
 * mercator projection.
 * 
 * @author joshy
 * @author Michael Kanis
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
public class TileFactoryImpl extends TileFactory {

	private static int								THREAD_POOL_SIZE	= 20;

	private static final ReentrantLock				RESET_LOCK			= new ReentrantLock();
	private static final ReentrantLock				EXECUTOR_LOCK		= new ReentrantLock();

	private static ExecutorService					fExecutorService;

	private TileFactoryInfo							fFactoryInfo;



	/**
	 * contains tiles which are currently being loaded or which have loading errors when loading
	 * failed
	 */
	private final ConcurrentHashMap<String, Tile>	fLoadingTiles		= new ConcurrentHashMap<String, Tile>();

	/**
	 * This queue contains tiles which needs to be loaded, only the number of
	 * {@link #THREAD_POOL_SIZE} can be loaded at the same time, the other tiles are waiting in this
	 * queue. <br>
	 * <br>
	 * !!!!! THIS IS JDK 1.6 !!!!!!!
	 */
	private LinkedBlockingDeque<Tile>				fTileWaitingQueue	= new LinkedBlockingDeque<Tile>();

	/**
	 * cache for tile images
	 */
	private TileImageCache							fTileImageCache;

	/**
	 * This is the image shown as long as the real tile image is not yet fully loaded.
	 */
	private Image									fLoadingImage;

	/**
	 * This is the image displayed when the real tile image could not be loaded.
	 */
	private Image									fErrorImage;

	/**
	 * when using the default constructor, the method {@link initializeTilefactory} must be called
	 * to initialize the tile factory
	 */
	public TileFactoryImpl() {}

	/**
	 * Creates a new instance of {@link TileFactoryImpl} using the spcified {@link TileFactoryInfo}
	 * 
	 * @param info
	 *            a TileFactoryInfo to configure this TileFactory
	 */
	public TileFactoryImpl(final TileFactoryInfo info) {
		initializeTileFactory(info);
	}

	private void createErrorImage() {

		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			public void run() {

				final int tileSize = getTileSize();

				fErrorImage = new Image(display, tileSize, tileSize);

				final Color bgColor = new Color(display, Map.DefaultBackgroundRGB);
				final GC gc = new GC(getErrorImage());
				{
					gc.setBackground(bgColor);
					gc.fillRectangle(0, 0, tileSize, tileSize);

					if (fFactoryInfo.isMapEmpty() == false) {
						gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
						gc.drawString(Messages.geoclipse_extensions_loading_failed, 5, 5);
					}
				}
				gc.dispose();
				bgColor.dispose();
			}
		});
	}

	private void createLoadingImage() {

		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			public void run() {

				final int tileSize = getTileSize();

				fLoadingImage = new Image(display, tileSize, tileSize);

				final Color bgColor = new Color(display, Map.DefaultBackgroundRGB);
				final GC gc = new GC(getLoadingImage());
				{
					gc.setBackground(bgColor);
					gc.fillRectangle(0, 0, tileSize, tileSize);

					if (fFactoryInfo.isMapEmpty() == false) {
						gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
						gc.drawString(Messages.geoclipse_extensions_loading, 5, 5);
					}
				}
				gc.dispose();
				bgColor.dispose();
			}
		});
	}

	@Override
	public void dispose() {

		fTileImageCache.dispose();
		fLoadingTiles.clear();

		if (fLoadingImage != null) {
			fLoadingImage.dispose();
		}

		if (fErrorImage != null) {
			fErrorImage.dispose();
		}
	}

	@Override
	public void disposeCachedImages() {
		fLoadingTiles.clear();
		fTileImageCache.dispose();
	}

	/**
	 * Convert a GeoPosition to a Point2D pixel coordinate in the world bitmap
	 * 
	 * @param geoPosition
	 *            a coordinate
	 * @param zoomLevel
	 *            the current zoom level
	 * @return a pixel location in the world bitmap
	 */
	@Override
	public Point geoToPixel(final GeoPosition geoPosition, final int zoomLevel) {
		return projection.geoToPixel(geoPosition, zoomLevel, fFactoryInfo);
	}

	@Override
	public double getDistance(final GeoPosition position1, final GeoPosition position2, final int zoom) {
		return projection.getHorizontalDistance(position1, position2, zoom, fFactoryInfo);
	}

	@Override
	public Image getErrorImage() {

		if (fErrorImage == null || fErrorImage.isDisposed()) {
			createErrorImage();
		}

		return fErrorImage;
	}

	/**
	 * @return Returns the {@link ExecutorService} which contains 20 threads to load or create map
	 *         images
	 */
	private ExecutorService getExecutor() {

		if (fExecutorService != null) {
			return fExecutorService;
		}

		/*
		 * create thread pool, this is synched only once until the executor is created
		 */
		EXECUTOR_LOCK.lock();
		{
			try {

				// check again
				if (fExecutorService != null) {
					return fExecutorService;
				}

				final ThreadFactory threadFactory = new ThreadFactory() {

					private int	fCount	= 0;

					public Thread newThread(final Runnable r) {

						final String threadName = "tile-pool-" + fCount++; //$NON-NLS-1$

						final Thread thread = new Thread(r, threadName);
						thread.setPriority(Thread.MIN_PRIORITY);
						thread.setDaemon(daemonize);

						return thread;
					}
				};

				fExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, threadFactory);

			} finally {
				EXECUTOR_LOCK.unlock();
			}
		}

		return fExecutorService;
	}

	/**
	 * Get the TileFactoryInfo describing this TileFactory
	 * 
	 * @return a TileFactoryInfo
	 */
	@Override
	public TileFactoryInfo getInfo() {
		return fFactoryInfo;
	}

	@Override
	public Image getLoadingImage() {

		if (fLoadingImage == null || fLoadingImage.isDisposed()) {
			createLoadingImage();
		}

		return fLoadingImage;
	}

	/**
	 * @return Returns a list with tiles which are currently being loaded or which have loading
	 *         errors when loading failed
	 */
	public ConcurrentHashMap<String, Tile> getLoadingTiles() {
		return fLoadingTiles;
	}

	/**
	 * Get <b>number of tiles</b> of the world bitmap at the current zoom level
	 * 
	 * @param zoom
	 *            the current zoom level
	 * @return size of the world bitmap in tiles
	 */
	@Override
	public Dimension getMapSize(final int zoom) {
		return fFactoryInfo.getMapSize(zoom);
	}

	/**
	 * Returns the tile that is located at the given tilePoint for this zoom. For example, if
	 * getMapSize() returns 10x20 for this zoom, and the tilePoint is (3,5), then the appropriate
	 * tile will be located and returned.<br>
	 * <br>
	 * The image for the tile is checked if it's available, if not the loading of the image is
	 * started.
	 * 
	 * @param tilePoint
	 * @param zoom
	 * @return
	 */
	@Override
	public Tile getTile(int tilePositionX, final int tilePositionY, final int zoom) {

		/*
		 * create tile key, wrap the tiles horizontally --> mod the x with the max width and use
		 * that
		 */
		final int numTilesWidth = (int) getMapSize(zoom).getWidth();
		if (tilePositionX < 0) {
			tilePositionX = numTilesWidth - (Math.abs(tilePositionX) % numTilesWidth);
		}
		tilePositionX = tilePositionX % numTilesWidth;

		final String tileKey = Tile.getTileKey(null, tilePositionX, tilePositionY, zoom, null, fFactoryInfo
				.getCustomTileKey(), projection.getId());

		/*
		 * check if tile is available in the tile cache and the tile image is available
		 */
		Tile tile = fTileCache.get(tileKey);

		if (tile != null) {

			// cache contains tile

			// check tile image
			if (tile.getCheckedMapImage() != null) {
				return tile;
			}

			// check loading state
			if (tile.isLoading()) {
				return tile;
			}

			// check loading error
			if (tile.isLoadingError()) {
				return tile;
			}

			// tile image is disposed
		}

		/*
		 * check if tile is being loaded, this list contains ALL tiles which have loading errors
		 */
		if (tile == null) {

			tile = fLoadingTiles.get(tileKey);
			if (tile == null) {

				// tile is not being loaded, create a new tile 

				tile = new Tile(this, tilePositionX, tilePositionY, zoom, null);
				tile.setBoundingBoxEPSG4326();

				doPostCreation(tile);

			} else {

				// tile is available in the loading map

				// check loading state
				if (tile.isLoading()) {
					return tile;
				}

				// check if loading failed
				if (tile.isLoadingError()) {
					return tile;
				}

				// when the tile has an image, use it
				final Image tileImage = tile.getCheckedMapImage();
				if (tileImage != null) {
					return tile;
				}

//				// this case should not happen
//				StatusUtil.showStatus(
//						"Tile is in loading map but has an invalid state: " + tileKey + "\t" + tile,
//						new Exception());
			}
		}

		/*
		 * now we have a tile, get tile image from the image cache
		 */
		Image cachedTileImage = null;

		final boolean useOfflineImage = isUseOfflineImage();
		if (useOfflineImage) {
			cachedTileImage = fTileImageCache.getTileImage(tile);
		}

		if (cachedTileImage == null) {

			// start loading the image

			if (TileFactoryInfo.isTileValid(tilePositionX, tilePositionY, zoom, fFactoryInfo)) {

				if (useOfflineImage) {
					// set state if an offline image for the current tile is available
					fTileImageCache.setOfflineImageAvailability(tile);
				}

				// LOAD/CREATE image
				putTilesInWaitingQueue(tile);
			}

		} else {

			// set image from the cache into the tile

			tile.setMapImage(cachedTileImage);
		}

		return tile;
	}

	public TileImageCache getTileImageCache() {
		return fTileImageCache;
	}

	/**
	 * Gets the size of an edge of a tile in pixels. Tiles must be square.
	 * 
	 * @return the size of an edge of a tile in pixels
	 */
	@Override
	public int getTileSize() {
		return fFactoryInfo.getTileSize();
	}

	public LinkedBlockingDeque<Tile> getTileWaitingQueue() {
		return fTileWaitingQueue;
	}

	/**
	 * Gets the URL of a tile.
	 * 
	 * @param tile
	 * @throws java.net.URISyntaxException
	 * @return
	 * @throws Exception
	 */
	URL getURL(final Tile tile) throws Exception {

		final String urlString = tile.getTileFactory().getInfo().getTileUrl(
				tile.getX(),
				tile.getY(),
				tile.getZoom(),
				tile);

		if (urlString == null) {
			final Exception e = new Exception();
			StatusUtil.log(NLS.bind(Messages.DBG041_Error_InvalidUrlNull, tile.getTileKey()), e);
			throw e;
		}

		final String encodedUrl;

		URL url;

		try {
			encodedUrl = Util.encodeSpace(urlString);

			// keep url for logging
			tile.setUrl(urlString);

			url = new URL(encodedUrl);

		} catch (final MalformedURLException e) {
			StatusUtil.log(NLS.bind(Messages.DBG042_Error_InvalidUrl, urlString, tile.getTileKey()), e);
			throw e;
		}

		return url;
	}

	public void initializeTileFactory(final TileFactoryInfo info) {

		fFactoryInfo = info;

		projection = new Mercator();
		fTileImageCache = new TileImageCache(this, info, Display.getDefault());
	}

	/**
	 * Converts a pixel coordinate in the world bitmap to a GeoPosition
	 * 
	 * @param pixelCoordinate
	 *            a point in the world bitmap at the current zoom level
	 * @param zoom
	 *            the current zoom level
	 * @return the point in lat/long coordinates
	 */
	@Override
	public GeoPosition pixelToGeo(final Point2D pixelCoordinate, final int zoom) {
		return projection.pixelToGeo(pixelCoordinate, zoom, fFactoryInfo);
	}

	/**
	 * Put one tile into the tile image waiting queue
	 * 
	 * @param tile
	 * @throws InterruptedException
	 */
	private void putOneTileInWaitingQueue(final Tile tile) throws InterruptedException {

		tile.setLoading(true);

		fLoadingTiles.put(tile.getTileKey(), tile);

		fTileWaitingQueue.add(tile);

		// create loading task
		final Future<?> future = getExecutor().submit(new TileImageLoader(this));

		// keep loading task
		tile.setFuture(future);

		fireTileEvent(TileEventId.TILE_IS_QUEUED, tile);
	}

	/**
	 * Put all tiles into a queue to load/create the tile image
	 * 
	 * @param tile
	 */
	private void putTilesInWaitingQueue(final Tile tile) {

		// prevent to start loading when loading is already started
		if (tile.isLoading()) {
			return;
		}

		final String tileKey = tile.getTileKey();
		final Tile loadingTile = fLoadingTiles.get(tileKey);

		// check if the tile is available in the loading queue
		if (loadingTile != null) {

			/*
			 * tile is corrupt, when tile.isLoading() == false, it should NOT be in loadingTiles
			 */

			// remove invalid tile
			fLoadingTiles.remove(tileKey);

			// log disabled: happened too often
// 			StatusUtil.log("Loading Tile Map contains an invalid tile: " + tileKey, new Exception());

			return;
		}

		try {

			putOneTileInWaitingQueue(tile);

			if (tile.isOfflimeImageAvailable() == false) {

				final ArrayList<Tile> tileChildren = tile.createTileChildren(fLoadingTiles);
				if (tileChildren != null) {

					// this is a parent child, put all child tiles into the loading queue

					if (tileChildren.size() == 0) {

						/*
						 * there are no child tiles available, this can happen when the zoom factor
						 * does not support the map providers or when child tiles have an loading
						 * error
						 */

						// set loading error for the parent tile
						tile.setLoadingError(Messages.Tile_Error_NoMapProvider);
					}

					for (final Tile tileChild : tileChildren) {

						/*
						 * check if a tile is already in the loading queue, this can be the case
						 * when a child has a loading error
						 */
						boolean isLoadChild = true;
						final Tile loadingChild = fLoadingTiles.get(tileChild.getTileKey());

						if (loadingChild != null) {
							if (loadingChild.isLoadingError()) {
								isLoadChild = false;
							}
						}

						if (isLoadChild) {
							putOneTileInWaitingQueue(tileChild);
						}
					}
				}
			}

		} catch (final Exception ex) {
			StatusUtil.log(ex.getMessage(), ex);
		}
	}

	@Override
	public void resetAll(final boolean keepTilesWithLoadingError) {

		RESET_LOCK.lock();
		{
			try {

				stopLoadingTiles();

				fTileWaitingQueue.clear();

				// clear loading map
				if (keepTilesWithLoadingError) {

					// remove tiles which has no error
					final Collection<Tile> loadingTiles = fLoadingTiles.values();

					for (final Tile tile : loadingTiles) {
						if (tile.isLoadingError() == false) {
							fLoadingTiles.remove(tile.getTileKey());
						}
					}

				} else {
					fLoadingTiles.clear();
				}

				fTileCache.clear();
				fTileImageCache.dispose();

			} finally {
				RESET_LOCK.unlock();
			}
		}

		fireTileEvent(TileEventId.TILE_RESET_QUEUES, null);
	}

	@Override
	public synchronized void resetOverlays() {

		stopLoadingTiles();

		fTileWaitingQueue.clear();
		fLoadingTiles.clear();

		fTileCache.resetOverlays();

		fireTileEvent(TileEventId.TILE_RESET_QUEUES, null);
	}

	@Override
	public void resetParentTiles() {

		RESET_LOCK.lock();
		{
			try {

				stopLoadingTiles();

				fTileWaitingQueue.clear();

				// remove parent tiles
				for (final Tile tile : fLoadingTiles.values()) {

					/*
					 * check if this is a parent tile, child tiles are not removed to prevent
					 * loading them again
					 */
					if (tile.getChildren() != null) {
						fLoadingTiles.remove(tile.getTileKey());
					}
				}

				fTileCache.clear();
				fTileImageCache.dispose();

				fTileCache.clear();
				fTileImageCache.dispose();

			} finally {
				RESET_LOCK.unlock();
			}
		}
	}

	@Override
	public void resetTileImageAvailability() {
		fTileCache.resetTileImageAvailability();
	}

	@Override
	public synchronized void resetTileQueue() {

// this propably caused loading of invalid images		
//		stopLoadingTiles();

//		fTileWaitingQueue.clear();
//		fLoadingTiles.clear();
//
//		fireTileEvent(TileEventId.TILE_SET_RESET, null);
	}

	public void setThreadPoolSize(final int size) {
		if (size <= 0) {
			throw new IllegalArgumentException("size invalid: " //$NON-NLS-1$
					+ size
					+ ". The size of the threadpool must be greater than 0."); //$NON-NLS-1$
		}
		THREAD_POOL_SIZE = size;
	}

	/**
	 * stop downloading tiles
	 */
	private void stopLoadingTiles() {

		for (final Tile tile : fLoadingTiles.values()) {

			if (tile.isLoading()) {

				// reset loading state
				tile.setLoading(false);

				final Future<?> future = tile.getFuture();

				if (future != null) {

					if (future.isCancelled() == false) {
						future.cancel(true);
					}
				}
			}
		}
	}
}
