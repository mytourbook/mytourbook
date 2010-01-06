/**
 * TileFactory.java
 *
 * Created on March 17, 2006, 8:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import de.byteholder.geoclipse.map.event.ITileListener;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.gpx.GeoPosition;

/**
 * A class that can produce tiles and convert coordinates to pixels
 * 
 * @author joshy
 */
public abstract class TileFactory {

	protected boolean					daemonize				= true;

	protected Projection				projection;

	private int							dimmingAlphaValue		= 0xFF;
	private RGB							dimmingColor;

	private int							fProfileAlpha;
	private boolean						fIsProfileBrightness;
	private int							fProfileBrightness;

	private boolean						fIsTransparentColors	= false;
	private boolean						fIsTransparentBlack		= false;
	private int[]						fTransparentColors		= null;

	private boolean						fUseOfflineImage		= true;

	private final static ListenerList	fTileListeners			= new ListenerList(ListenerList.IDENTITY);

	/**
	 * cache for tiles which do not have loading errors
	 */
	protected final TileCache			fTileCache				= new TileCache();

	/**
	 * cache for tile images
	 */
	protected TileImageCache			fTileImageCache;

	public static void addTileListener(final ITileListener tileListener) {
		fTileListeners.add(tileListener);
	}

	public static ListenerList getTileListeners() {
		return fTileListeners;
	}

	public static void removeTileListener(final ITileListener tileListener) {
		if (tileListener != null) {
			fTileListeners.remove(tileListener);
		}
	}

	/** Creates a new instance of TileFactory */
	protected TileFactory() {}

	/**
	 * In this method the implementing Factroy can dispose all of its temporary images and other SWT
	 * objects that need to be disposed.
	 */
	public abstract void dispose();

	/**
	 * Dispose all cached images
	 */
	public abstract void disposeCachedImages();

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

	protected void fireTileEvent(final TileEventId tileEventId, final Tile tile) {
		for (final Object listener : fTileListeners.getListeners()) {
			final ITileListener tileListener = (ITileListener) listener;
			tileListener.tileEvent(tileEventId, tile);
		}
	}

	/**
	 * Convert a GeoPosition to a pixel position in the world bitmap a the specified zoom level.
	 * 
	 * @param c
	 *            a GeoPosition
	 * @param zoom
	 *            the zoom level to extract the pixel coordinate for
	 * @return the pixel point
	 */
	public abstract Point geoToPixel(GeoPosition c, int zoom);

	/**
	 * @return Returns the color which is used to dim the map images
	 */
	public RGB getDimColor() {
		return dimmingColor;
	}

	/**
	 * @return Returns the alpha value which is used to dim the map images, default value is not to
	 *         dim the map.
	 */
	public int getDimLevel() {
		return dimmingAlphaValue;
	}

	/**
	 * @param zoom
	 * @param position2
	 * @param position1
	 * @return Returns the distance between two coordinates in pixel
	 */
	public abstract double getDistance(GeoPosition position1, GeoPosition position2, int zoom);

	public abstract Image getErrorImage();

	/**
	 * Return the TileFactoryInfo containing map metrics information
	 * 
	 * @return the TileFactoryInfo for this TileFactory
	 */
	public abstract TileFactoryInfo getInfo();

	/**
	 * A property for an image which will be display when an image is still loading.
	 * 
	 * @return the current property value
	 */
	public abstract Image getLoadingImage();

	public abstract MP getMapProvider();

	/**
	 * Returns a Dimension containing the width and height of the map, in tiles at the current zoom
	 * level. So a Dimension that returns 10x20 would be 10 tiles wide and 20 tiles tall. These
	 * values can be multipled by getTileSize() to determine the pixel width/height for the map at
	 * the given zoom level
	 * 
	 * @return the size of the world bitmap in tiles
	 * @param zoom
	 *            the current zoom level
	 */
	public abstract Dimension getMapSize(int zoom);

	public int getProfileAlpha() {
		return fProfileAlpha;
	}

	public int getProfileBrightness() {
		return fProfileBrightness;
	}

	public Projection getProjection() {
		return projection;
	}

	/**
	 * Return the Tile at a given TilePoint and zoom level
	 * 
	 * @return the tile that is located at the given tilePoint for this zoom level. For example, if
	 *         getMapSize() returns 10x20 for this zoom, and the tilePoint is (3,5), then the
	 *         appropriate tile will be located and returned. This method must not return null.
	 *         However, it can return dummy tiles that contain no data if it wants. This is
	 *         appropriate, for example, for tiles which are outside of the bounds of the map and if
	 *         the factory doesn't implement wrapping.
	 * @param tilePoint
	 *            the tilePoint
	 * @param zoom
	 *            the current zoom level
	 */
	public abstract Tile getTile(int x, int y, int zoom);

	public TileCache getTileCache() {
		return fTileCache;
	}

	public TileImageCache getTileImageCache() {
		return fTileImageCache;
	}

	/**
	 * The size of tiles for this factory. Tiles must be square.
	 * 
	 * @return the size of the tiles in pixels. All tiles must be square. A return value of 256, for
	 *         example, means that each tile will be 256 pixels wide and tall
	 */
	public abstract int getTileSize();

	public int[] getTransparentColors() {
		return fTransparentColors;
	}

	public boolean isProfileBrightness() {
		return fIsProfileBrightness;
	}

	public boolean isTransparentBlack() {
		return fIsTransparentBlack;
	}

	public boolean isTransparentColor() {
		return fIsTransparentColors;
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
	public abstract GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom);

	/**
	 * Resets all queues (tile) and caches (tile + image) and stops downloading the images
	 * 
	 * @param keepTilesWithLoadingError
	 *            when <code>true</code>, tiles with loading errors are not discarded
	 */
	public abstract void resetAll(boolean keepTilesWithLoadingError);

	/**
	 * Reset overlay information (status and image) from all tiles
	 */
	public abstract void resetOverlays();

	/**
	 * Removes all parent tiles from the loading queue
	 */
	public void resetParentTiles() {

	}

	public abstract void resetTileImageAvailability();

	/**
	 * Resets the queue which holds the tiles which are currently being loaded
	 */
	public abstract void resetTileQueue();

	public void setDimLevel(final int dimLevel, final RGB dimColor) {

		if (dimmingAlphaValue == dimLevel && dimmingColor == dimColor) {
			// dimming value is not modified
			return;
		}

		// set new dim level/color
		dimmingAlphaValue = dimLevel;
		dimmingColor = dimColor;

		// dispose all cached images
		disposeCachedImages();
	}

	public void setIsProfileBrightness(final boolean fProfileBrightness) {
		this.fIsProfileBrightness = fProfileBrightness;
	}

	public void setIsTransparentBlack(final boolean isTransparentBlack) {
		fIsTransparentBlack = isTransparentBlack;
	}

	public void setIsTransparentColors(final boolean isTransparentColors) {
		fIsTransparentColors = isTransparentColors;
	}

	public abstract void setMapProvider(final MP mapProvider);

	public void setProfileAlpha(final int alpha) {

		fProfileAlpha = alpha;

//		System.out.println(this
//				+ "-"
//				+ System.identityHashCode(this)
//				+ " alpha:"
//				+ getProfileAlpha()
//				+ " setProfileAlpha()"
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	public void setProfileBrightness(final int brightness) {
		fProfileBrightness = brightness;
	}

	public void setProjection(final Projection projection) {
		this.projection = projection;
	}

	public void setTransparentColors(final int[] transparentColors) {
		fTransparentColors = transparentColors;
	}

	public void setUseOfflineImage(final boolean useOfflineImage) {
		fUseOfflineImage = useOfflineImage;
	}

}
