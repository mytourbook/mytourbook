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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.ITileLoader;
import de.byteholder.geoclipse.map.ITilePainter;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapViewPortData;
import de.byteholder.geoclipse.map.Mercator;
import de.byteholder.geoclipse.map.OverlayTourState;
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

	private static final int						TILE_CACHE_SIZE						= 2000;													//2000;
	private static final int						ERROR_CACHE_SIZE					= 10000;													//10000;
	private static final int						IMAGE_CACHE_SIZE					= 100;

	public static final int							OFFLINE_INFO_NOT_READ				= -1;

	/**
	 * these zoom levels are displayed in the UI therefore they start with 1 instead of 0
	 */
	public static final int							UI_MIN_ZOOM_LEVEL					= 1;
	public static final int							UI_MAX_ZOOM_LEVEL					= 18;

	// loading tiles pool
	private static final int						THREAD_POOL_SIZE					= 20;
	private static ExecutorService					fExecutorService;

	private static final ReentrantLock				EXECUTOR_LOCK						= new ReentrantLock();
	private static final ReentrantLock				RESET_LOCK							= new ReentrantLock();

	/**
	 * Cache for tiles which do not have loading errors
	 */
	private static final TileCache					_tileCache							= new TileCache(TILE_CACHE_SIZE);

	/**
	 * Contains tiles which has loading errors, they are kept in this map that they are not loaded
	 * again
	 */
	private static final TileCache					_errorTiles							= new TileCache(
																								ERROR_CACHE_SIZE);

	/**
	 * Cache for tile images
	 */
	private static final TileImageCache				_tileImageCache						= new TileImageCache(
																								IMAGE_CACHE_SIZE);

	/**
	 * This queue contains tiles which needs to be loaded, only the number of
	 * {@link #THREAD_POOL_SIZE} can be loaded at the same time, the other tiles are waiting in this
	 * queue. <br>
	 * <br>
	 * TODO !!!!! THIS IS JDK 1.6 !!!!!!!
	 */
	private static final LinkedBlockingDeque<Tile>	_tileWaitingQueue					= new LinkedBlockingDeque<Tile>();

	/**
	 * Listener which throws {@link ITileListener} events
	 */
	private final static ListenerList				_tileListeners						= new ListenerList(
																								ListenerList.IDENTITY);

	private int										_dimmingAlphaValue					= 0xFF;
	private RGB										_dimmingColor;

	private final Projection						_projection;

	/**
	 * image size in pixel for a square image
	 */
	private int										_tileSize							= Integer
																								.parseInt(MapProviderManager.DEFAULT_IMAGE_SIZE);
	// map min/max zoom level
	private int										_minZoomLevel						= 0;
	private int										_maxZoomLevel						= UI_MAX_ZOOM_LEVEL
																								- UI_MIN_ZOOM_LEVEL;

	private int										_defaultZoomLevel					= 0;

	/**
	 * The number of tiles wide at each zoom level
	 */
	private int[]									_mapWidthInTilesAtZoom;

	/**
	 * An array of coordinates in <em>pixels</em> that indicates the center in the world map for the
	 * given zoom level.
	 */
	private Point2D[]								_mapCenterInPixelsAtZoom;

	/**
	 * An array of doubles that contain the number of pixels per degree of longitude at a give zoom
	 * level.
	 */
	private double[]								_longitudeDegreeWidthInPixels;

	/**
	 * An array of doubles that contain the number of radians per degree of longitude at a given
	 * zoom level (where longitudeRadianWidthInPixels[0] is the most zoomed out)
	 */
	private double[]								_longitudeRadianWidthInPixels;

	private boolean									_useOfflineImage					= true;

	/**
	 * This is the image shown as long as the real tile image is not yet fully loaded.
	 */
	private Image									_loadingImage;

	/**
	 * This is the image displayed when the real tile image could not be loaded.
	 */
	private Image									_errorImage;

	/**
	 * unique id to identify a map provider
	 */
	private String									_mapProviderId;

	/**
	 * mime image format which is currently used
	 */
	private String									_imageFormat						= MapProviderManager.DEFAULT_IMAGE_FORMAT;

	private int										_favoriteZoom						= 0;
	private GeoPosition								_favoritePosition					= new GeoPosition(0.0, 0.0);

	private int										_lastUsedZoom						= 0;
	private GeoPosition								_lastUsedPosition					= new GeoPosition(0.0, 0.0);

	/**
	 * name of the map provider which is displayed in the UI
	 */
	private String									_mapProviderName;

	/**
	 * map provider description
	 */
	private String									_description						= UI.EMPTY_STRING;

	/**
	 * OS folder to save offline images
	 */
	private String									_offlineFolder;

	/**
	 * number of files in the offline cache
	 */
	private int										_offlineFileCounter					= -1;

	/**
	 * size in Bytes for the offline images
	 */
	private long									_offlineFileSize					= -1;

	private static final ListenerList				_offlineReloadEventListeners		= new ListenerList(
																								ListenerList.IDENTITY);

	/**
	 * State if the map provider can be toggled in the map
	 */
	private boolean									_canBeToggled;

	//
	// Profile map provider values
	//

	/**
	 * alpha values for the map provider, 100 is opaque, 0 is transparent
	 */
	private int										_profileAlpha						= 100;

	private boolean									_isProfileTransparentColors			= false;
	private int[]									_profileTransparentColor			= null;

	/**
	 * when <code>true</code> the color black is transparent
	 */
	private boolean									_isProfileBlackTransparent;

	private boolean									_isProfileBrightnessForNextMp		= false;
	private int										_profileBrightnessValueForNextMp	= 77;

	private MapViewPortData							_mapViewPort;

	public static void addOfflineInfoListener(final IOfflineInfoListener listener) {
		_offlineReloadEventListeners.add(listener);
	}

	public static void addTileListener(final ITileListener tileListener) {
		_tileListeners.add(tileListener);
	}

	public static void fireTileEvent(final TileEventId tileEventId, final Tile tile) {
		for (final Object listener : _tileListeners.getListeners()) {
			final ITileListener tileListener = (ITileListener) listener;
			tileListener.tileEvent(tileEventId, tile);
		}
	}

	public static TileCache getErrorTiles() {
		return _errorTiles;
	}

	public static TileCache getTileCache() {
		return _tileCache;
	}

	public static ListenerList getTileListeners() {
		return _tileListeners;
	}

	public static LinkedBlockingDeque<Tile> getTileWaitingQueue() {
		return _tileWaitingQueue;
	}

	public static void removeOfflineInfoListener(final IOfflineInfoListener listener) {
		if (listener != null) {
			_offlineReloadEventListeners.remove(listener);
		}
	}

	public static void removeTileListener(final ITileListener tileListener) {
		if (tileListener != null) {
			_tileListeners.remove(tileListener);
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

		_projection = new Mercator();

		initializeMapWithZoomAndSize(_maxZoomLevel, _tileSize);

	}

	public boolean canBeToggled() {
		return _canBeToggled;
	}

	/**
	 * Checks if a tile is displayed in the map viewport.
	 * 
	 * @param tile
	 *            Tile which is checked
	 * @return Returns <code>true</code> when the tile is displayed in the current map viewport.
	 */
	public boolean checkViewPort(final Tile tile) {

		// check zoom level
		if (tile.getZoom() != _mapViewPort.mapZoomLevel) {
			return false;
		}

		// check position
		final int tileX = tile.getX();
		final int tileY = tile.getY();

		if (tileX >= _mapViewPort.tilePosMinX
				&& tileX <= _mapViewPort.tilePosMaxX
				&& tileY >= _mapViewPort.tilePosMinY
				&& tileY <= _mapViewPort.tilePosMaxY) {

			return true;
		}

		return false;
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

			mapProvider._imageFormat = new String(_imageFormat);

			mapProvider._favoritePosition = new GeoPosition(_favoritePosition == null
					? new GeoPosition(0.0, 0.0)
					: _favoritePosition);

			mapProvider._lastUsedPosition = new GeoPosition(_lastUsedPosition == null
					? new GeoPosition(0.0, 0.0)
					: _lastUsedPosition);
		}

		return mapProvider;
	}

	public int compareTo(final Object otherObject) {

		final MP otherMapProvider = (MP) otherObject;

		if (this instanceof MPPlugin && otherMapProvider instanceof MPPlugin) {

			return _mapProviderName.compareTo(otherMapProvider.getName());

		} else {

			if (this instanceof MPPlugin) {
				return -1;
			}
			if (otherMapProvider instanceof MPPlugin) {
				return 1;
			}

			return _mapProviderName.compareTo(otherMapProvider.getName());
		}
	}

	private void createErrorImage() {

		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			public void run() {

				final int tileSize = getTileSize();

				_errorImage = new Image(display, tileSize, tileSize);

				final Color bgColor = new Color(display, Map.DEFAULT_BACKGROUND_RGB);
				final GC gc = new GC(getErrorImage());
				{
					gc.setBackground(bgColor);
					gc.fillRectangle(0, 0, tileSize, tileSize);

					gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					gc.drawString(Messages.geoclipse_extensions_loading_failed, 5, 5);
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

				_loadingImage = new Image(display, tileSize, tileSize);

				final Color bgColor = new Color(display, Map.DEFAULT_BACKGROUND_RGB);
				final GC gc = new GC(getLoadingImage());
				{
					gc.setBackground(bgColor);
					gc.fillRectangle(0, 0, tileSize, tileSize);

					gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
					gc.drawString(Messages.geoclipse_extensions_loading, 5, 5);
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
	public void disposeAllImages() {

		if (_tileImageCache != null) {
			_tileImageCache.dispose();
		}

		if (_loadingImage != null) {
			_loadingImage.dispose();
		}

		if (_errorImage != null) {
			_errorImage.dispose();
		}
	}

	public void disposeTileImages() {
		_tileImageCache.dispose();
	}

	public void disposeTiles() {
		_tileCache.removeAll();
		_errorTiles.removeAll();
		_tileImageCache.dispose();
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
		if (_mapProviderId == null) {
			if (other._mapProviderId != null) {
				return false;
			}
		} else if (!_mapProviderId.equals(other._mapProviderId)) {
			return false;
		}

		return true;
	}

	private void fireOfflineReloadEvent(final MP mapProvider) {

		final Object[] allListeners = _offlineReloadEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((IOfflineInfoListener) listener).offlineInfoIsDirty(mapProvider);
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
		return _projection.geoToPixel(geoPosition, zoomLevel, this);
	}

	/**
	 * @return Returns a custom tile key, default returns <code>null</code>
	 */
	String getCustomTileKey() {
		return null;
	}

	public int getDefaultZoomLevel() {
		return _defaultZoomLevel;
	}

	public String getDescription() {
		return _description;
	}

	/**
	 * @return Returns the color which is used to dim the map images
	 */
	public RGB getDimColor() {
		return _dimmingColor;
	}

	/**
	 * @return Returns the alpha value which is used to dim the map images, default value is not to
	 *         dim the map.
	 */
	public int getDimLevel() {
		return _dimmingAlphaValue;
	}

	public double getDistance(final GeoPosition position1, final GeoPosition position2, final int zoom) {
		return _projection.getHorizontalDistance(position1, position2, zoom, this);
	}

	public Image getErrorImage() {

		if (_errorImage == null || _errorImage.isDisposed()) {
			createErrorImage();
		}

		return _errorImage;
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
		return _favoritePosition;
	}

	public int getFavoriteZoom() {
		return _favoriteZoom;
	}

	/**
	 * @return Returns a unique id for the map provider
	 */
	public String getId() {
		return _mapProviderId;
	}

	public String getImageFormat() {
		return _imageFormat;
	}

	public GeoPosition getLastUsedPosition() {
		return _lastUsedPosition;
	}

	public int getLastUsedZoom() {
		return _lastUsedZoom;
	}

	public Image getLoadingImage() {

		if (_loadingImage == null || _loadingImage.isDisposed()) {
			createLoadingImage();
		}

		return _loadingImage;
	}

	/**
	 * @param zoom
	 * @return
	 */
	public double getLongitudeDegreeWidthInPixels(final int zoom) {
		return _longitudeDegreeWidthInPixels[zoom];
	}

	/**
	 * @param zoom
	 * @return
	 */
	public double getLongitudeRadianWidthInPixels(final int zoom) {
		return _longitudeRadianWidthInPixels[zoom];
	}

	/**
	 * @param zoom
	 * @return
	 */
	public Point2D getMapCenterInPixelsAtZoom(final int zoom) {
		return _mapCenterInPixelsAtZoom[zoom];
	}

	/**
	 * @param zoom
	 * @return
	 */
	private int getMapSizeInTiles(int zoom) {

		// ensure array bounds, this is Math.min() inline
		final int b = _mapWidthInTilesAtZoom.length - 1;
		zoom = (zoom <= b) ? zoom : b;

		return _mapWidthInTilesAtZoom[zoom];
	}

	/**
	 * @return Returns the size of the map at the given zoom in tiles (num tiles tall by num tiles
	 *         wide)
	 */
	public Dimension getMapTileSize(final int zoom) {
		final int mapTileSize = getMapSizeInTiles(zoom);
		return new Dimension(mapTileSize, mapTileSize);
	}

	/**
	 * @return
	 */
	public int getMaximumZoomLevel() {
		return _maxZoomLevel;
	}

	public int getMaxZoomLevel() {
		return _maxZoomLevel;
	}

	/**
	 * @return
	 */
	public int getMinimumZoomLevel() {
		return _minZoomLevel;
	}

	public int getMinZoomLevel() {
		return _minZoomLevel;
	}

	/**
	 * @return Returns the name of the map provider which is displayed in the UI
	 */
	public String getName() {
		return _mapProviderName;
	}

	public int getOfflineFileCounter() {
		return _offlineFileCounter;
	}

	public long getOfflineFileSize() {
		return _offlineFileSize;
	}

	/**
	 * @return Returns the folder where tile files will be cached relativ to the common offline
	 *         image path
	 */
	public String getOfflineFolder() {
		return _offlineFolder;
	}

	int getProfileAlpha() {
		return _profileAlpha;
	}

	int getProfileBrightnessForNextMp() {
		return _profileBrightnessValueForNextMp;
	}

	int[] getProfileTransparentColors() {
		return _profileTransparentColor;
	}

	public Projection getProjection() {
		return _projection;
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
		final int numTilesWidth = getMapSizeInTiles(zoom);

		if (tilePositionX < 0) {
			tilePositionX = numTilesWidth - (Math.abs(tilePositionX) % numTilesWidth);
		}
		tilePositionX = tilePositionX % numTilesWidth;

		final String tileKey = Tile.getTileKey(//
				this,
				zoom,
				tilePositionX,
				tilePositionY,
				null,
				getCustomTileKey(),
				_projection.getId());

		/*
		 * check if tile is available in the tile cache and the tile image is available
		 */
		Tile tile = _tileCache.get(tileKey);

		if (tile != null) {

			// tile is available

			// check tile image
			if (tile.isImageValid()) {

				/*
				 * tile image is available, this is the shortest path to check if an image for a
				 * tile position is availabe
				 */

				return tile;
			}

			// check loading state
			if (tile.isLoading()) {
				return tile;
			}

			// check if the old implementation was not correctly transfered to the cache with error tiles
			if (tile.isLoadingError()) {
				StatusUtil.log("Internal error: Tile with loading error should not be in the tile cache 1: " //$NON-NLS-1$
						+ tile.getTileKey());

				// ensure the error do not occure again for this tile
				_tileCache.remove(tileKey);
				_errorTiles.add(tileKey, tile);

				return tile;
			}

			// tile image is not available until now
		}

		/*
		 * check if the tile has a loading error
		 */
		final Tile errorTile = _errorTiles.get(tileKey);
		if (errorTile != null) {

			// tiles with an error do not have an image

			// check if the old implementation was not correctly transfered to the cache with error tiles
			if (tile != null) {
				StatusUtil.log("Internal error: Tile with loading error should not be in the tile cache 2: " //$NON-NLS-1$
						+ tile.getTileKey());
			}

			// ensure the error do not occure again for this tile
			_tileCache.remove(tileKey);

			return errorTile;
		}

		/*
		 * create new tile
		 */
		if (tile == null) {

			// tile is not being loaded, create a new tile

			tile = new Tile(this, zoom, tilePositionX, tilePositionY, null);
			tile.setBoundingBoxEPSG4326();
			doPostCreation(tile);

			/*
			 * keep tiles in the cache, tiles with loading errors will be transferred to the tile
			 * cache with loading errors, this is done in the TileImageLoader
			 */
			_tileCache.add(tileKey, tile);
		}

		/*
		 * now we have a tile, get tile image from the image cache
		 */
		Image cachedTileImage = null;

		final boolean useOfflineImage = isUseOfflineImage();
		if (useOfflineImage) {
			cachedTileImage = _tileImageCache.getTileImage(tile);
		}

		if (cachedTileImage == null) {

			// an image is not available, start loading it

			if (isTileValid(tilePositionX, tilePositionY, zoom)) {

				// set state if an offline image for the current tile is available
				if (useOfflineImage) {
					_tileImageCache.setOfflineImageAvailability(tile);
				}

				// LOAD/CREATE image
				putTileInWaitingQueue(tile);
			}

		} else {

			// set image from the cache into the tile

			tile.setMapImage(cachedTileImage);
		}

		return tile;
	}

	public TileImageCache getTileImageCache() {
		return _tileImageCache;
	}

	/**
	 * @param fullPath
	 *            File system path on the local file system where the tile path is appended
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
		return _tileSize;
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
	 * This method will be ignored when the map provider is an instance of {@link ITileLoader}. <br>
	 * 
	 * @param tile
	 * @return a valid url to load the tile
	 */
	public String getTileUrl(final Tile tile) {
		return null;
	}

	/**
	 * Gets the URL of a tile.
	 * 
	 * @param tile
	 * @throws java.net.URISyntaxException
	 * @return
	 * @throws Exception
	 */
	public URL getTileURLEncoded(final Tile tile) throws Exception {

		final String urlString = getTileUrl(tile);

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_mapProviderId == null) ? 0 : _mapProviderId.hashCode());
		return result;
	}

	public void initializeMapSize(final int tileSize) {
		initializeMapWithZoomAndSize(_maxZoomLevel, tileSize);
	}

	private void initializeMapWithZoomAndSize(final int maxZoom, final int tileSize) {

		_tileSize = tileSize;

		// map width (in pixel) is one tile at zoomlevel 0
		int devMapSize = tileSize;

		final int mapArrayLength = maxZoom + 1;

		_longitudeDegreeWidthInPixels = new double[mapArrayLength];
		_longitudeRadianWidthInPixels = new double[mapArrayLength];

		_mapCenterInPixelsAtZoom = new Point2D.Double[mapArrayLength];
		_mapWidthInTilesAtZoom = new int[mapArrayLength];

		// get map values for each zoom level
		for (int z = 0; z <= maxZoom; ++z) {

			// how wide is each degree of longitude in pixels
			_longitudeDegreeWidthInPixels[z] = (double) devMapSize / 360;

			// how wide is each radian of longitude in pixels
			_longitudeRadianWidthInPixels[z] = devMapSize / (2.0 * Math.PI);

			final int devMapSize2 = devMapSize / 2;

			_mapCenterInPixelsAtZoom[z] = new Point2D.Double(devMapSize2, devMapSize2);
			_mapWidthInTilesAtZoom[z] = devMapSize / tileSize;

			devMapSize *= 2;
		}
	}

	private void initializeZoomLevel(final int minZoom, final int maxZoom) {

		_minZoomLevel = minZoom;
		_maxZoomLevel = maxZoom;

		initializeMapWithZoomAndSize(_maxZoomLevel, _tileSize);
	}

//	/**
//	 * @param offlineImagePath
//	 * @return Path where tile files will are cached relative to the offline image path
//	 */
//	public abstract IPath getTileOSPathFolder(final String offlineImagePath);

	boolean isProfileBrightnessForNextMp() {
		return _isProfileBrightnessForNextMp;
	}

	boolean isProfileTransparentBlack() {
		return _isProfileBlackTransparent;
	}

	boolean isProfileTransparentColors() {
		return _isProfileTransparentColors;
	}

	/**
	 * @returns Return <code>true</code> if this point in <em>tiles</em> is valid at this zoom
	 *          level. For example, if the zoom level is 0 (zoomed all the way out, there is only
	 *          one tile), x,y must be 0,0
	 */
	public boolean isTileValid(final int x, final int y, final int zoomLevel) {

		// check if off the map to the top or left
		if (x < 0 || y < 0) {
			return false;
		}

		// check if off the map to the right
		if (getMapCenterInPixelsAtZoom(zoomLevel).getX() * 2 <= x * _tileSize) {
			return false;
		}

		// check if off the map to the bottom
		if (getMapCenterInPixelsAtZoom(zoomLevel).getY() * 2 <= y * _tileSize) {
			return false;
		}

		// check if out of zoom bounds
		if (zoomLevel < getMinimumZoomLevel() || zoomLevel > getMaximumZoomLevel()) {
			return false;
		}

		return true;
	}

	public boolean isUseOfflineImage() {
		return _useOfflineImage;
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
		return _projection.pixelToGeo(pixelCoordinate, zoom, this);
	}

	/**
	 * Put one tile into the tile image waiting queue
	 * 
	 * @param tile
	 * @throws InterruptedException
	 */
	private void putOneTileInWaitingQueue(final Tile tile) throws InterruptedException {

		tile.setLoading(true);

		_tileWaitingQueue.add(tile);

		// create loading task
		final Future<?> future = getExecutor().submit(new TileImageLoader());

		// keep loading task
		tile.setFuture(future);

		fireTileEvent(TileEventId.TILE_IS_QUEUED, tile);
	}

	/**
	 * Put all tiles into a queue to load/create the tile image
	 * 
	 * @param tile
	 */
	public void putTileInWaitingQueue(final Tile tile) {

		// prevent to load it more than once
		if (tile.isLoading()) {
			return;
		}

		try {

			putOneTileInWaitingQueue(tile);

			if (tile.isOfflimeImageAvailable() == false) {

				final ArrayList<Tile> tileChildren = tile.createTileChildren();
				if (tileChildren != null) {

					// this is a parent child, put all child tiles into the loading queue

					if (tileChildren.size() == 0) {

						/*
						 * there are no child tiles available, this can happen when the zoom factor
						 * does not support the map providers or when child tiles have an loading
						 * error
						 */

						// set loading error into the parent tile
						tile.setLoadingError(Messages.TileInfo_Error_NoMapProvider);
					}

					for (final Tile tileChild : tileChildren) {
						putOneTileInWaitingQueue(tileChild);
					}
				}
			}

		} catch (final Exception ex) {
			StatusUtil.log(ex.getMessage(), ex);
		}
	}

	/**
	 * Reset all caches
	 * 
	 * @param keepTilesWithLoadingError
	 *            when <code>true</code> tiles with loading error are not removed
	 */
	public void resetAll(final boolean keepTilesWithLoadingError) {

		RESET_LOCK.lock();
		{
			try {

				_tileWaitingQueue.clear();
				_tileCache.stopLoadingTiles();

				if (keepTilesWithLoadingError == false) {
					_errorTiles.removeAll();
				}

				_tileCache.removeAll();
				_tileImageCache.dispose();

			} finally {
				RESET_LOCK.unlock();
			}
		}

		fireTileEvent(TileEventId.TILE_RESET_QUEUES, null);
	}

	/**
	 * Reset overlay information by setting the overlay status to
	 * {@link OverlayTourState#OVERLAY_NOT_CHECKED} in all tiles
	 */
	public synchronized void resetOverlays() {

		_tileWaitingQueue.clear();
		_tileCache.stopLoadingTiles();

		_tileCache.resetOverlays();
		_errorTiles.resetOverlays();

		fireTileEvent(TileEventId.TILE_RESET_QUEUES, null);
	}

	public void resetParentTiles() {

		RESET_LOCK.lock();
		{
			try {

				_tileWaitingQueue.clear();
				_tileCache.stopLoadingTiles();

				_errorTiles.removeParentTiles();

				_tileCache.removeAll();
				_tileImageCache.dispose();

			} finally {
				RESET_LOCK.unlock();
			}
		}
	}

	public void resetTileImageAvailability() {
		_tileCache.resetTileImageAvailability();
	}

	public void setCanBeToggled(final boolean canBeToggled) {
		_canBeToggled = canBeToggled;
	}

	public void setDefaultZoomLevel(final int defaultZoomLevel) {
		_defaultZoomLevel = defaultZoomLevel;
	}

	public void setDescription(final String fDescription) {
		this._description = fDescription;
	}

	public void setDimLevel(final int dimLevel, final RGB dimColor) {

		// check if dimming value is modified
		if (_dimmingAlphaValue == dimLevel && _dimmingColor == dimColor) {
			// dimming value is not modified
			return;
		}

		// set new dim level/color
		_dimmingAlphaValue = dimLevel;
		_dimmingColor = dimColor;

		// dispose all cached images
		disposeTileImages();
	}

	public void setFavoritePosition(final GeoPosition fFavoritePosition) {
		this._favoritePosition = fFavoritePosition;
	}

	public void setFavoriteZoom(final int favoriteZoom) {
		_favoriteZoom = favoriteZoom;
	}

	public void setId(final String mapProviderId) {

		_mapProviderId = mapProviderId;

		/*
		 * !!! very importand !!!
		 * the factory id must be set in the superclass to make the tile factory
		 * info unique, otherwise factorId is null and all created custom tile factory infos cannot
		 * be distinguished with the equals/hashcode methods
		 */
		//		super.setFactoryId(factoryId);
	}

	public void setImageFormat(final String imageFormat) {
		_imageFormat = imageFormat;
	}

	void setIsProfileBrightnessForNextMp(final boolean isBrightness) {
		_isProfileBrightnessForNextMp = isBrightness;
	}

	void setIsProfileTransparentBlack(final boolean isBlackTransparent) {
		_isProfileBlackTransparent = isBlackTransparent;
	}

	void setIsProfileTransparentColors(final boolean isTransColors) {
		_isProfileTransparentColors = isTransColors;
	}

	public void setLastUsedPosition(final GeoPosition position) {
		_lastUsedPosition = position;
	}

	public void setLastUsedZoom(final int zoom) {
		_lastUsedZoom = zoom;
	}

	public void setMapViewPort(final MapViewPortData mapViewPort) {
		_mapViewPort = mapViewPort;
	}

	public void setName(final String mapProviderName) {
		_mapProviderName = mapProviderName;
	}

	public void setOfflineFileCounter(final int offlineFileCounter) {
		_offlineFileCounter = offlineFileCounter;
	}

	public void setOfflineFileSize(final long offlineFileSize) {
		_offlineFileSize = offlineFileSize;
	}

	/**
	 * Sets the folder where offline images are saved, this folder is relativ to the common offline
	 * folder path
	 * 
	 * @param offlineFolder
	 */
	public void setOfflineFolder(final String offlineFolder) {
		_offlineFolder = offlineFolder;
	}

	void setProfileAlpha(final int alpha) {
		_profileAlpha = alpha;
	}

	void setProfileBrightnessForNextMp(final int brightnessValue) {
		_profileBrightnessValueForNextMp = brightnessValue;
	}

	void setProfileTransparentColors(final int[] transColors) {
		_profileTransparentColor = transColors;
	}

	public void setStateToReloadOfflineCounter() {

		if (_offlineFileCounter != OFFLINE_INFO_NOT_READ) {

			_offlineFileCounter = OFFLINE_INFO_NOT_READ;
			_offlineFileSize = OFFLINE_INFO_NOT_READ;

			fireOfflineReloadEvent(this);
		}
	}

	/**
	 * Sets the tile image size and updates the internal datastructures.
	 * 
	 * @param tileSize
	 */
	public void setTileSize(final int tileSize) {
		initializeMapSize(tileSize);
	}

	public void setUseOfflineImage(final boolean useOfflineImage) {
		_useOfflineImage = useOfflineImage;
	}

	/**
	 * Sets the min/max zoom levels which this map provider supports and updates the internal
	 * datastructures.
	 * 
	 * @param minZoom
	 * @param maxZoom
	 */
	public void setZoomLevel(final int minZoom, final int maxZoom) {
		initializeZoomLevel(minZoom, maxZoom);
	}

	@Override
	public String toString() {
		return _mapProviderName + "(" + _mapProviderId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
