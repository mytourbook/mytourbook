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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.ITileLoader;
import de.byteholder.geoclipse.map.ITilePainter;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.Mercator;
import de.byteholder.geoclipse.map.Projection;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.TileCache;
import de.byteholder.geoclipse.map.TileImageCache;
import de.byteholder.geoclipse.map.TileImageLoader;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.util.Util;
import de.byteholder.gpx.GeoPosition;

/**
 * This is the base class for map providers (MP) which provides all data which are necessary to draw
 * a map.<br>
 * <br>
 * This is a new implementation of the previous TileFactory class
 */
public abstract class MP implements Cloneable, Comparable<Object> {

	public static final int							OFFLINE_INFO_NOT_READ			= -1;

	/**
	 * these zoom levels are displayed in the UI therefore they start with 1 instead of 0
	 */
	public static final int							UI_MIN_ZOOM_LEVEL				= 1;
	public static final int							UI_MAX_ZOOM_LEVEL				= 18;

	// loading tiles pool
	private static int								THREAD_POOL_SIZE				= 20;
	private static ExecutorService					fExecutorService;

	private static final ReentrantLock				EXECUTOR_LOCK					= new ReentrantLock();
	private static final ReentrantLock				RESET_LOCK						= new ReentrantLock();

	private int										fDimmingAlphaValue				= 0xFF;
	private RGB										fDimmingColor;

	private Projection								fProjection;

	/**
	 * image size in pixel for a square image
	 */
	private int										fTileSize						= Integer
																							.parseInt(MapProviderManager.DEFAULT_IMAGE_SIZE);
	// map min/max zoom level
	private int										fMinZoomLevel					= 0;
	private int										fMaxZoomLevel					= UI_MAX_ZOOM_LEVEL
																							- UI_MIN_ZOOM_LEVEL;

	private int										fDefaultZoomLevel				= 0;

	/**
	 * The number of tiles wide at each zoom level
	 */
	private int[]									mapWidthInTilesAtZoom;

	/**
	 * An array of coordinates in <em>pixels</em> that indicates the center in the world map for the
	 * given zoom level.
	 */
	private Point2D[]								mapCenterInPixelsAtZoom;

	/**
	 * An array of doubles that contain the number of pixels per degree of longitude at a give zoom
	 * level.
	 */
	private double[]								longitudeDegreeWidthInPixels;

	/**
	 * An array of doubles that contain the number of radians per degree of longitude at a given
	 * zoom level (where longitudeRadianWidthInPixels[0] is the most zoomed out)
	 */
	private double[]								longitudeRadianWidthInPixels;

	/**
	 * cache for tiles which do not have loading errors
	 */
	private final TileCache							fTileCache						= new TileCache();

	/**
	 * cache for tile images
	 */
	private final TileImageCache					fTileImageCache;

	/**
	 * contains tiles which are currently being loaded or which have loading errors when loading
	 * failed
	 */
	private final ConcurrentHashMap<String, Tile>	fLoadingTiles					= new ConcurrentHashMap<String, Tile>();

	/**
	 * This queue contains tiles which needs to be loaded, only the number of
	 * {@link #THREAD_POOL_SIZE} can be loaded at the same time, the other tiles are waiting in this
	 * queue. <br>
	 * <br>
	 * !!!!! THIS IS JDK 1.6 !!!!!!!
	 */
	private LinkedBlockingDeque<Tile>				fTileWaitingQueue				= new LinkedBlockingDeque<Tile>();

	private boolean									fUseOfflineImage				= true;

	private final static ListenerList				fTileListeners					= new ListenerList(
																							ListenerList.IDENTITY);

	/**
	 * This is the image shown as long as the real tile image is not yet fully loaded.
	 */
	private Image									fLoadingImage;

	/**
	 * This is the image displayed when the real tile image could not be loaded.
	 */
	private Image									fErrorImage;

	/**
	 * unique id to identify a map provider
	 */
	private String									fMapProviderId;

	/**
	 * mime image format which is currently used
	 */
	private String									fImageFormat					= MapProviderManager.DEFAULT_IMAGE_FORMAT;

	private int										fFavoriteZoom					= 0;
	private GeoPosition								fFavoritePosition				= new GeoPosition(0.0, 0.0);

	private int										fLastUsedZoom					= 0;
	private GeoPosition								fLastUsedPosition				= new GeoPosition(0.0, 0.0);

	/**
	 * name of the map provider which is displayed in the UI
	 */
	private String									fMapProviderName;

	/**
	 * map provider description
	 */
	private String									fDescription					= UI.EMPTY_STRING;

//	/**
//	 * OS folder to save offline images
//	 */
//	private String									fOfflineFolder;

	/**
	 * number of files in the offline cache
	 */
	private int										fOfflineFileCounter				= -1;

	/**
	 * size in Bytes for the offline images
	 */
	private long									fOfflineFileSize				= -1;

	private static final ListenerList				fOfflineReloadEventListeners	= new ListenerList(
																							ListenerList.IDENTITY);

	/**
	 * State if the map provider can be toggled in the map
	 */
	private boolean									fCanBeToggled;

	public static void addOfflineInfoListener(final IOfflineInfoListener listener) {
		fOfflineReloadEventListeners.add(listener);
	}

	public static void addTileListener(final ITileListener tileListener) {
		fTileListeners.add(tileListener);
	}

	public static ListenerList getTileListeners() {
		return fTileListeners;
	}

	public static void removeOfflineInfoListener(final IOfflineInfoListener listener) {
		if (listener != null) {
			fOfflineReloadEventListeners.remove(listener);
		}
	}

	public static void removeTileListener(final ITileListener tileListener) {
		if (tileListener != null) {
			fTileListeners.remove(tileListener);
		}
	}

	/**
	 * <pre>
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * </pre>
	 */
	public MP() {

		fProjection = new Mercator();

		fTileImageCache = new TileImageCache(this);

		initializeMapSize(fMaxZoomLevel, fTileSize);

	}

	public boolean canBeToggled() {
		return fCanBeToggled;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MP mapProvider = (MP) super.clone();

		if (this instanceof MPProfile) {

			/*
			 * a map profile contains all map providers which are not a map profile, clone all of
			 * them in the clone constructor
			 */

		} else {

			mapProvider.fImageFormat = new String(fImageFormat);

			mapProvider.fFavoritePosition = new GeoPosition(fFavoritePosition == null
					? new GeoPosition(0.0, 0.0)
					: fFavoritePosition);

			mapProvider.fLastUsedPosition = new GeoPosition(fLastUsedPosition == null
					? new GeoPosition(0.0, 0.0)
					: fLastUsedPosition);
		}

		return mapProvider;
	}

	public int compareTo(final Object otherObject) {

		final MP otherMapProvider = (MP) otherObject;

		if (this instanceof MPPlugin && otherMapProvider instanceof MPPlugin) {

			return fMapProviderName.compareTo(otherMapProvider.getName());

		} else {

			if (this instanceof MPPlugin) {
				return -1;
			}
			if (otherMapProvider instanceof MPPlugin) {
				return 1;
			}

			return fMapProviderName.compareTo(otherMapProvider.getName());
		}
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

// mp2					
//					if (fFactoryInfo.isMapEmpty() == false) {
//						gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
//						gc.drawString(Messages.geoclipse_extensions_loading_failed, 5, 5);
//					}
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

// mp2					
//					if (fFactoryInfo.isMapEmpty() == false) {
//						gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
//						gc.drawString(Messages.geoclipse_extensions_loading, 5, 5);
//					}
				}
				gc.dispose();
				bgColor.dispose();
			}
		});
	}

	/**
	 * In this method the implementing Factroy can dispose all of its temporary images and other SWT
	 * objects that need to be disposed.
	 */
	public void dispose() {

		if (fTileImageCache != null) {
			fTileImageCache.dispose();
		}

		fLoadingTiles.clear();

		if (fLoadingImage != null) {
			fLoadingImage.dispose();
		}

		if (fErrorImage != null) {
			fErrorImage.dispose();
		}
	}

	public void disposeCachedImages() {
		fLoadingTiles.clear();
		fTileImageCache.dispose();
	}

	public void disposeTiles() {
		fLoadingTiles.clear();
		fTileCache.clear();
		fTileImageCache.dispose();
	}

	/**
	 * Is called directly after the tile was created and before other tile action are done.<br>
	 * <br>
	 * Default implementation do nothing but can be overwritten to do additional initialization like
	 * setting custom data with {@link Tile#setData(Object)}
	 * 
	 * @param tile
	 */
	public void doPostCreation(final Tile tile) {
	// default does nothing
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MP)) {
			return false;
		}

		final MP other = (MP) obj;
		if (fMapProviderId == null) {
			if (other.fMapProviderId != null) {
				return false;
			}
		} else if (!fMapProviderId.equals(other.fMapProviderId)) {
			return false;
		}

		return true;
	}

	private void fireOfflineReloadEvent(final MP mapProvider) {

		final Object[] allListeners = fOfflineReloadEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((IOfflineInfoListener) listener).offlineInfoIsDirty(mapProvider);
		}
	}

	public void fireTileEvent(final TileEventId tileEventId, final Tile tile) {
		for (final Object listener : fTileListeners.getListeners()) {
			final ITileListener tileListener = (ITileListener) listener;
			tileListener.tileEvent(tileEventId, tile);
		}
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
	public Point geoToPixel(final GeoPosition geoPosition, final int zoomLevel) {
		return fProjection.geoToPixel(geoPosition, zoomLevel, this);
	}

	/**
	 * @return Returns a custom tile key, default returns <code>null</code>
	 */
	public String getCustomTileKey() {
		return null;
	}

	public int getDefaultZoomLevel() {
		return fDefaultZoomLevel;
	}

	public String getDescription() {
		return fDescription;
	}

	/**
	 * @return Returns the color which is used to dim the map images
	 */
	public RGB getDimColor() {
		return fDimmingColor;
	}

	/**
	 * @return Returns the alpha value which is used to dim the map images, default value is not to
	 *         dim the map.
	 */
	public int getDimLevel() {
		return fDimmingAlphaValue;
	}

	public double getDistance(final GeoPosition position1, final GeoPosition position2, final int zoom) {
		return fProjection.getHorizontalDistance(position1, position2, zoom, this);
	}

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
						thread.setDaemon(true);

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

	public GeoPosition getFavoritePosition() {
		return fFavoritePosition;
	}

	public int getFavoriteZoom() {
		return fFavoriteZoom;
	}

	/**
	 * @return Returns a unique id for the map provider
	 */
	public String getId() {
		return fMapProviderId;
	}

	public String getImageFormat() {
		return fImageFormat;
	}

	public GeoPosition getLastUsedPosition() {
		return fLastUsedPosition;
	}

	public int getLastUsedZoom() {
		return fLastUsedZoom;
	}

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
	 * @param zoom
	 * @return
	 */
	public double getLongitudeDegreeWidthInPixels(final int zoom) {
		return longitudeDegreeWidthInPixels[zoom];
	}

	/**
	 * @param zoom
	 * @return
	 */
	public double getLongitudeRadianWidthInPixels(final int zoom) {
		return longitudeRadianWidthInPixels[zoom];
	}

	/**
	 * @param zoom
	 * @return
	 */
	public Point2D getMapCenterInPixelsAtZoom(final int zoom) {
		return mapCenterInPixelsAtZoom[zoom];
	}

	/**
	 * @return the size of the map at the given zoom, in tiles (num tiles tall by num tiles wide)
	 */
	public Dimension getMapSize(final int zoom) {
		return new Dimension(getMapWidthInTilesAtZoom(zoom), getMapWidthInTilesAtZoom(zoom));
	}

	/**
	 * @param zoom
	 * @return
	 */
	public int getMapWidthInTilesAtZoom(int zoom) {

		// ensure array bounds
		zoom = Math.min(zoom, mapWidthInTilesAtZoom.length - 1);

		return mapWidthInTilesAtZoom[zoom];
	}

	/**
	 * @return
	 */
	public int getMaximumZoomLevel() {
		return fMaxZoomLevel;
	}

	public int getMaxZoomLevel() {
		return fMaxZoomLevel;
	}

	/**
	 * @return
	 */
	public int getMinimumZoomLevel() {
		return fMinZoomLevel;
	}

	public int getMinZoomLevel() {
		return fMinZoomLevel;
	}

	/**
	 * @return Returns the name of the map provider which is displayed in the UI
	 */
	public String getName() {
		return fMapProviderName;
	}

	public int getOfflineFileCounter() {
		return fOfflineFileCounter;
	}

	public long getOfflineFileSize() {
		return fOfflineFileSize;
	}

	public Projection getProjection() {
		return fProjection;
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

		final String tileKey = Tile.getTileKey(//
				null,
				tilePositionX,
				tilePositionY,
				zoom,
				null,
				getCustomTileKey(),
				fProjection.getId());

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

				// keep all tiles in a cache
				fTileCache.add(tileKey, tile);

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

			if (isTileValid(tilePositionX, tilePositionY, zoom)) {

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

	public TileCache getTileCache() {
		return fTileCache;
	}

//	public String getOfflineFolder() {
//		return fOfflineFolder;
//	}

	public TileImageCache getTileImageCache() {
		return fTileImageCache;
	}

	/**
	 * @return Returns a tile loader which can load the tile images, the method
	 *         {@link #getTileUrl(int, int, int, Tile)} will be ignored when a tile loader is set
	 */
	public ITileLoader getTileLoader() {
		return null;
	}

	/**
	 * @return Returns the folder where tile files will be cached relativ to the common offline
	 *         image path
	 */
	//	 this is the same as: getOfflineFolder()
	public abstract String getTileOSFolder();

	/**
	 * @param fullPath
	 *            File system path on the local file system where the tile path is appended
	 * @param zoomLevel
	 * @param y
	 * @param x
	 * @param tile
	 * @return Returns the path for a tile when it's saved in the file system or <code>null</code>
	 *         when this features is not supported
	 */
	public abstract IPath getTileOSPath(String fullPath, Tile tile);

	/**
	 * @return Tile painter which is painting a tile or <code>null</code> when the tile is loaded
	 *         from a url
	 */
	public ITilePainter getTilePainter() {
		return null;
	}

	/**
	 * The size of tiles for this factory. Tiles must be square.
	 * 
	 * @return the size of the tiles in pixels. All tiles must be square. A return value of 256, for
	 *         example, means that each tile will be 256 pixels wide and tall
	 */
	public int getTileSize() {
		return fTileSize;
	}

	/**
	 * Returns the tile url for the specified tile at the specified zoom level. By default it will
	 * generate a tile url using the base url and parameters specified in the constructor. Thus if
	 * 
	 * <PRE>
	 * baseURl = http://www.myserver.com/maps?version=0.1 
	 * xparam = x 
	 * yparam = y 
	 * zparam = z 
	 * tilepoint = [1,2]
	 * zoom level = 3
	 * </PRE>
	 * 
	 * then the resulting url would be:
	 * 
	 * <pre>
	 * http://www.myserver.com/maps?version=0.1&amp;x=1&amp;y=2&amp;z=3
	 * </pre>
	 * 
	 * Note that the URL can be a <CODE>file:</CODE> url.<br>
	 * <br>
	 * This method will be ignored when {@link #getTileLoader()} returns a tile loader.
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 *            the zoom level
	 * @param tile
	 * @return a valid url to load the tile
	 */
	public String getTileUrl(final int x, final int y, final int zoom, final Tile tile) {
		return null;
	}

//	/**
//	 * @param offlineImagePath
//	 * @return Path where tile files will are cached relative to the offline image path
//	 */
//	public abstract IPath getTileOSPathFolder(final String offlineImagePath);

	/**
	 * Gets the URL of a tile.
	 * 
	 * @param tile
	 * @throws java.net.URISyntaxException
	 * @return
	 * @throws Exception
	 */
	public URL getTileURL(final Tile tile) throws Exception {

// mp2		
//		final String urlString = tile.getMP().getTileUrl(tile.getX(), tile.getY(), tile.getZoom(), tile);
		final String urlString = getTileUrl(tile.getX(), tile.getY(), tile.getZoom(), tile);

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

	public LinkedBlockingDeque<Tile> getTileWaitingQueue() {
		return fTileWaitingQueue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fMapProviderId == null) ? 0 : fMapProviderId.hashCode());
		return result;
	}

	public void initializeMapSize(final int tileSize) {
		initializeMapSize(fMaxZoomLevel, tileSize);
	}

	private void initializeMapSize(final int totalMapZoom, final int tileSize) {

		fTileSize = tileSize;

		// map width (in pixel) is one tile at zoomlevel 0
		int devMapSize = tileSize;

		final int mapArrayLength = totalMapZoom + 1;

		longitudeDegreeWidthInPixels = new double[mapArrayLength];
		longitudeRadianWidthInPixels = new double[mapArrayLength];

		mapCenterInPixelsAtZoom = new Point2D.Double[mapArrayLength];
		mapWidthInTilesAtZoom = new int[mapArrayLength];

		// get map values for each zoom level
		for (int z = 0; z <= totalMapZoom; ++z) {

			// how wide is each degree of longitude in pixels
			longitudeDegreeWidthInPixels[z] = (double) devMapSize / 360;

			// how wide is each radian of longitude in pixels
			longitudeRadianWidthInPixels[z] = devMapSize / (2.0 * Math.PI);

			final int devMapSize2 = devMapSize / 2;

			mapCenterInPixelsAtZoom[z] = new Point2D.Double(devMapSize2, devMapSize2);
			mapWidthInTilesAtZoom[z] = devMapSize / tileSize;

			devMapSize *= 2;
		}
	}

	public void initializeZoomLevel(final int minZoom, final int maxZoom) {

		fMinZoomLevel = minZoom;
		fMaxZoomLevel = maxZoom;

		initializeMapSize(fMaxZoomLevel, fTileSize);
	}

	/**
	 * @returns Return <code>true</code> if this point in <em>tiles</em> is valid at this zoom
	 *          level. For example, if the zoom level is 0 (zoomed all the way out, there is only
	 *          one tile), x,y must be 0,0
	 */
	public boolean isTileValid(final int x, final int y, final int zoomLevel) {

		//int x = (int)coord.getX();
		//int y = (int)coord.getY();

		// check if off the map to the top or left
		if (x < 0 || y < 0) {
			return false;
		}

		// check if off the map to the right
		if (getMapCenterInPixelsAtZoom(zoomLevel).getX() * 2 <= x * fTileSize) {
			return false;
		}

		// check if off the map to the bottom
		if (getMapCenterInPixelsAtZoom(zoomLevel).getY() * 2 <= y * fTileSize) {
			return false;
		}

		// check if out of zoom bounds
		if (zoomLevel < getMinimumZoomLevel() || zoomLevel > getMaximumZoomLevel()) {
			return false;
		}

		return true;
	}

	public boolean isUseOfflineImage() {
		return fUseOfflineImage;
	}

	/**
	 * Convert a pixel in the world bitmap at the specified zoom level into a GeoPosition
	 * 
	 * @param pixelCoordinate
	 *            a Point2D representing a pixel in the world bitmap
	 * @param zoom
	 *            the zoom level of the world bitmap
	 * @return the converted GeoPosition
	 */
	public GeoPosition pixelToGeo(final Point2D pixelCoordinate, final int zoom) {
		return fProjection.pixelToGeo(pixelCoordinate, zoom, this);
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

	public synchronized void resetOverlays() {

		stopLoadingTiles();

		fTileWaitingQueue.clear();
		fLoadingTiles.clear();

		fTileCache.resetOverlays();

		fireTileEvent(TileEventId.TILE_RESET_QUEUES, null);
	}

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

			} finally {
				RESET_LOCK.unlock();
			}
		}
	}

	public void resetTileImageAvailability() {
		fTileCache.resetTileImageAvailability();
	}

	public void setCanBeToggled(final boolean canBeToggled) {
		fCanBeToggled = canBeToggled;
	}

	public void setDefaultZoomLevel(final int defaultZoomLevel) {
		fDefaultZoomLevel = defaultZoomLevel;
	}

	public void setDescription(final String fDescription) {
		this.fDescription = fDescription;
	}

	public void setDimLevel(final int dimLevel, final RGB dimColor) {

		if (fDimmingAlphaValue == dimLevel && fDimmingColor == dimColor) {
			// dimming value is not modified
			return;
		}

		// set new dim level/color
		fDimmingAlphaValue = dimLevel;
		fDimmingColor = dimColor;

		// dispose all cached images
		disposeCachedImages();
	}

	public void setFavoritePosition(final GeoPosition fFavoritePosition) {
		this.fFavoritePosition = fFavoritePosition;
	}

	public void setFavoriteZoom(final int favoriteZoom) {
		fFavoriteZoom = favoriteZoom;
	}

	public void setImageFormat(final String imageFormat) {
		fImageFormat = imageFormat;
	}

	public void setLastUsedPosition(final GeoPosition position) {
		fLastUsedPosition = position;
	}

	public void setLastUsedZoom(final int zoom) {
		fLastUsedZoom = zoom;
	}

	public void setMapProviderId(final String mapProviderId) {

		fMapProviderId = mapProviderId;

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		//		super.setFactoryId(factoryId);
	}

	public void setName(final String mapProviderName) {
		fMapProviderName = mapProviderName;
	}

	public void setOfflineFileCounter(final int offlineFileCounter) {
		fOfflineFileCounter = offlineFileCounter;
	}

	public void setOfflineFileSize(final long offlineFileSize) {
		fOfflineFileSize = offlineFileSize;
	}

	public void setStateToReloadOfflineCounter() {

		if (fOfflineFileCounter != OFFLINE_INFO_NOT_READ) {

			fOfflineFileCounter = OFFLINE_INFO_NOT_READ;
			fOfflineFileSize = OFFLINE_INFO_NOT_READ;

			fireOfflineReloadEvent(this);
		}
	}

//	public void setOfflineFolder(final String offlineFolder) {
//		fOfflineFolder = offlineFolder;
//	}

	/**
	 * Sets the folder where offline images are saved, this folder is relativ to the offline folder
	 * path
	 * 
	 * @param offlineFolder
	 */
	public abstract void setTileOSFolder(String offlineFolder);

	public void setTileSize(final int tileSize) {
		fTileSize = tileSize;
	}

	public void setUseOfflineImage(final boolean useOfflineImage) {
		fUseOfflineImage = useOfflineImage;
	}

	/**
	 * Sets the min/max zoom levels which this map provider supports
	 * 
	 * @param minZoom
	 * @param maxZoom
	 */
	public void setZoomLevel(final int minZoom, final int maxZoom) {
		fMinZoomLevel = minZoom;
		fMaxZoomLevel = maxZoom;
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

	@Override
	public String toString() {
		return fMapProviderName + "(" + fMapProviderId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
