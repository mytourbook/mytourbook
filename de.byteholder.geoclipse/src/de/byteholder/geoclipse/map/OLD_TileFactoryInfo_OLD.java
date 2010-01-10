/*
 * TileFactoryInfo.java
 *
 * Created on June 26, 2006, 10:47 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import org.eclipse.core.runtime.IPath;

/**
 * A TileFactoryInfo encapsulates all information specific to a map server. This includes everything
 * from the url to load the map tiles from to the size and depth of the tiles. Theoretically any map
 * server can be used by installing a customized TileFactoryInfo. Currently
 * 
 * @author joshy
 */
public abstract class OLD_TileFactoryInfo_OLD {

	private String		factoryId;

	private int			minimumZoomLevel;
	private int			maximumZoomLevel;

//	private int			totalMapZoom;

	// the size of each tile (assumes they are square)
	private int			tileSize	= 256;

	/**
	 * The number of tiles wide at each zoom level
	 */
	private int[]		mapWidthInTilesAtZoom;

	/**
	 * An array of coordinates in <em>pixels</em> that indicates the center in the world map for the
	 * given zoom level.
	 */
	private Point2D[]	mapCenterInPixelsAtZoom;

	/**
	 * An array of doubles that contain the number of pixels per degree of longitude at a give zoom
	 * level.
	 */
	private double[]	longitudeDegreeWidthInPixels;

	/**
	 * An array of doubles that contain the number of radians per degree of longitude at a given
	 * zoom level (where longitudeRadianWidthInPixels[0] is the most zoomed out)
	 */
	private double[]	longitudeRadianWidthInPixels;

	/**
	 * base url for loading tiles
	 */
	private String		baseURL;

//	private String		xparam;
//	private String		yparam;
//	private String		zparam;

	/**
	 * A property indicating if the X coordinates of tiles go from right to left or left to right.
	 */
	private boolean		xr2l		= true;

	/**
	 * A property indicating if the Y coordinates of tiles go from right to left or left to right.
	 */
	private boolean		yt2b		= true;

	private int			defaultZoomLevel;

	private int			totalMapZoom;

	/**
	 * @returns Return <code>true</code> if this point in <em>tiles</em> is valid at this zoom
	 *          level. For example, if the zoom level is 0 (zoomed all the way out, there is only
	 *          one tile), x,y must be 0,0
	 */
	public static boolean isTileValid(final int x, final int y, final int zoomLevel, final TileFactoryInfo_OLD info) {
 
		//int x = (int)coord.getX();
		//int y = (int)coord.getY();

		// check if off the map to the top or left
		if (x < 0 || y < 0) {
			return false;
		}

		final int infoTileSize = info.getTileSize();

		// check if off the map to the right
		if (info.getMapCenterInPixelsAtZoom(zoomLevel).getX() * 2 <= x * infoTileSize) {
			return false;
		}

		// check if off the map to the bottom
		if (info.getMapCenterInPixelsAtZoom(zoomLevel).getY() * 2 <= y * infoTileSize) {
			return false;
		}

		// check if out of zoom bounds
		if (zoomLevel < info.getMinimumZoomLevel() || zoomLevel > info.getMaximumZoomLevel()) {
			return false;
		}

		return true;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TileFactoryInfo_OLD)) {
			return false;
		}
		final TileFactoryInfo_OLD other = (TileFactoryInfo_OLD) obj;
		if (factoryId == null) {
			if (other.factoryId != null) {
				return false;
			}
		} else if (!factoryId.equals(other.factoryId)) {
			return false;
		}
		return true;
	}

	public String getBaseURL() {
		return baseURL;
	}

	/**
	 * @return Returns a custom tile key, default returns <code>null</code>
	 */
	public String getCustomTileKey() {
		return null;
	}

	public int getDefaultZoomLevel() {
		return defaultZoomLevel;
	}

	/**
	 * @return Returns the factory id which is used to distinguish between different tile factorys
	 */
	public abstract String getFactoryID();

	/**
	 * @return Returns the name of the factory
	 */
	public abstract String getFactoryName();

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
		return maximumZoomLevel;
	}

	/**
	 * @return
	 */
	public int getMinimumZoomLevel() {
		return minimumZoomLevel;
	}

	/**
	 * @return Returns a tile loader which can load the tile images, the method
	 *         {@link #getTileUrl(int, int, int, Tile)} will be ignored when a tile loader is set
	 */
	public ITileLoader getTileLoader() {
		return null;
	}

	/**
	 * @return Returns the folder where tile files will be cached or <code>null</code> when tiles
	 *         cannot be cached
	 */
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
	public abstract IPath getTileOSPath(String fullPath, int x, int y, int zoomLevel, Tile tile);

	/**
	 * @param tileCacheOSPath
	 * @return Path where tile files will be cached or <code>null</code> when it's not defined in
	 *         the factory info
	 */
	public IPath getTileOSPathFolder(final String tileCacheOSPath) {
		return null;
	}

	/**
	 * @return Tile painter which is painting a tile or <code>null</code> when the tile is loaded
	 *         from a url
	 */
	public ITilePainter getTilePainter() {
		return null;
	}

	/**
	 * Get the tile size.
	 * 
	 * @return the tile size
	 */
	public int getTileSize() {
		return tileSize;
	}

//	public String getTileUrl(final int x, final int y, final int zoom) {
//
//		//System.out.println("getting tile at zoom: " + zoom);
//		//System.out.println("map width at zoom = " + getMapWidthInTilesAtZoom(zoom));
//		String ypart = "&" + yparam + "=" + y; //$NON-NLS-1$ //$NON-NLS-2$
//		//System.out.println("ypart = " + ypart);
//
//		if (!yt2b) {
//			final int tilemax = getMapWidthInTilesAtZoom(zoom);
//			//int y = tilePoint.getY();
//			ypart = "&" + yparam + "=" + (tilemax / 2 - y - 1); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		//System.out.println("new ypart = " + ypart);
//		final String url = getBaseURL() + "&" + xparam + "=" + x + ypart + //$NON-NLS-1$ //$NON-NLS-2$
//				//"&" + yparam + "=" + tilePoint.getY() +
//				"&" //$NON-NLS-1$
//				+ zparam
//				+ "=" //$NON-NLS-1$
//				+ zoom;
//		return url;
//	}

//	/**
//	 * @return
//	 */
//	public int getTotalMapZoom() {
//		return totalMapZoom;
//	}

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
	public abstract String getTileUrl(final int x, final int y, final int zoom, Tile tile);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((factoryId == null) ? 0 : factoryId.hashCode());
		return result;
	}

	/**
	 * @param factoryId
	 * @param minimumZoomLevel
	 * @param maximumZoomLevel
	 * @param totalMapZoom
	 * @param tileSize
	 */
	public void initializeInfo(	final String factoryId,
								final int minimumZoomLevel,
								final int maximumZoomLevel,
								final int totalMapZoom,
								final int tileSize) {
		initializeInfo(
				factoryId,
				minimumZoomLevel,
				maximumZoomLevel,
				totalMapZoom,
				tileSize,
				true,
				true,
				null,
				null,
				null,
				null);
	}

	/**
	 * @param factoryId
	 * @param minimumZoomLevel
	 * @param maximumZoomLevel
	 * @param totalMapZoom
	 * @param tileSize
	 * @param xr2l
	 * @param yt2b
	 * @param baseURL
	 * @param xparam
	 * @param yparam
	 * @param zparam
	 */
	private void initializeInfo(final String factoryId,
								final int minimumZoomLevel,
								final int maximumZoomLevel,
								final int totalMapZoom,
								final int tileSize,
								final boolean xr2l,
								final boolean yt2b,
								final String baseURL,
								final String xparam,
								final String yparam,
								final String zparam) {

		this.factoryId = factoryId;

		this.minimumZoomLevel = minimumZoomLevel;
		this.maximumZoomLevel = maximumZoomLevel;
		this.totalMapZoom = totalMapZoom;

		this.baseURL = baseURL;

		// these parameters are not used anymore 9.9.2009
		//		this.xparam = xparam;
		//		this.yparam = yparam;
		//		this.zparam = zparam;

		this.xr2l = xr2l;
		this.yt2b = yt2b;

		initializeMapSize(totalMapZoom, tileSize);
	}

	public void initializeMapSize(final int tileSize) {
		initializeMapSize(totalMapZoom, tileSize);
	}

	private void initializeMapSize(final int totalMapZoom, final int tileSize) {

		this.tileSize = tileSize;

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
		
		minimumZoomLevel = minZoom;
		maximumZoomLevel = maxZoom;

		totalMapZoom = maxZoom;

		initializeMapSize(totalMapZoom, tileSize);
	}

	/**
	 * @return Returns the <code>true</code> when the factory contains content, <code>false</code>
	 *         when no content is loaded and displayed
	 */
	public boolean isMapEmpty() {
		return false;
	}

	/**
	 * A property indicating if the X coordinates of tiles go from right to left or left to right.
	 * 
	 * @return
	 */
	public boolean isXr2l() {
		return xr2l;
	}

	/**
	 * A property indicating if the Y coordinates of tiles go from right to left or left to right.
	 * 
	 * @return
	 */
	public boolean isYt2b() {
		return yt2b;
	}

	public void setBaseURL(final String baseURL) {
		this.baseURL = baseURL;
	}

	public void setDefaultZoomLevel(final int defaultZoomLevel) {
		this.defaultZoomLevel = defaultZoomLevel;
	}

	public void setFactoryId(final String factoryId) {
		this.factoryId = factoryId;
	}

	public TileFactoryInfo_OLD() {}

	/**
	 * Creates a new instance of TileFactoryInfo. Note that TileFactoryInfo should be considered
	 * invariate, meaning that subclasses should ensure all of the properties stay the same after
	 * the class is constructed. Returning different values of getTileSize() for example is
	 * considered an error and may result in unexpected behavior.
	 * 
	 * @param factoryId
	 * @param minimumZoomLevel
	 *            The minimum zoom level
	 * @param maximumZoomLevel
	 *            the maximum zoom level
	 * @param totalMapZoom
	 *            the top zoom level, essentially the height of the pyramid
	 * @param tileSize
	 *            the size of the tiles in pixels (must be square)
	 * @param xr2l
	 *            if the x goes r to l (is this backwards?)
	 * @param yt2b
	 *            if the y goes top to bottom
	 * @param baseURL
	 *            the base url for grabbing tiles
	 * @param xparam
	 *            the x parameter for the tile url
	 * @param yparam
	 *            the y parameter for the tile url
	 * @param zparam
	 *            the z parameter for the tile url
	 */
	/*
	 * @param xr2l true if tile x is measured from the far left of the map to the far right, or else
	 * false if based on the center line. @param yt2b true if tile y is measured from the top (north
	 * pole) to the bottom (south pole) or else false if based on the equator.
	 */
	public TileFactoryInfo_OLD(	final String factoryId,
							final int minimumZoomLevel,
							final int maximumZoomLevel,
							final int totalMapZoom,
							final int tileSize,
							final boolean xr2l,
							final boolean yt2b,
							final String baseURL,
							final String xparam,
							final String yparam,
							final String zparam) {

		initializeInfo(
				factoryId,
				minimumZoomLevel,
				maximumZoomLevel,
				totalMapZoom,
				tileSize,
				xr2l,
				yt2b,
				baseURL,
				xparam,
				yparam,
				zparam);
	}

}
