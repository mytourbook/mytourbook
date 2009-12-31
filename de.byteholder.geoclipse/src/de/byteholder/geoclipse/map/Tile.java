/*
 * Tile.java
 *
 * Created on March 14, 2006, 4:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package de.byteholder.geoclipse.map;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import de.byteholder.geoclipse.logging.StatusUtil;

/**
 * The Tile class represents a particular square image piece of the world bitmap at a particular
 * zoom level.
 * 
 * @author Joshua Marinacci
 * @author Michael Kanis
 * @author Wolfgang
 */

public class Tile extends Observable {

//	private static final double				MAX_LATITUDE_85_05112877	= 85.05112877;

	private static final String				COLUMN_2		= "  ";
	private static final String				COLUMN_4		= "    ";
	private static final String				COLUMN_5		= "     ";

	private OverlayStatus					overlayStatus	= OverlayStatus.NOT_SET;

	private boolean							isLoading		= false;

	/**
	 * If an error occurs while loading a tile, store the exception here.
	 */
	private Throwable						error;

	/**
	 * The zoom level, x, and y values this tile is for
	 */
	private int								zoom, x, y;

	/**
	 * Map image for this tile
	 */
	private Image							mapImage		= null;

	/**
	 * Overlay image for this tile
	 */
	private Image							overlayImage	= null;

	private boolean							fIsOfflineError	= false;

	private String							fTileKey;

	private Object							fCustomData;

	// bbox coordinates
	private BoundingBoxEPSG4326				fBoundingBox;

	private Future<?>						fFuture;

	/**
	 * url which is used to load the tile
	 */
	private String							fUrl;

	/**
	 * contains the error message when loading of the image fails
	 */
	private String							fLoadingError	= null;

	private boolean							fIsOfflineImageAvailable;

	/**
	 * path which was used to load the offline image
	 */
	private String							fOfflinePath;

	/**
	 * When set, this {@link Tile} is a part of the parent tile
	 */
	private Tile							fParentTile;

	/**
	 * When set, this is a parent tile which has children tiles
	 */
	private ArrayList<Tile>					fTileChildren;

	private ReentrantLock					PARENT_LOCK;

	private ImageData[]						fChildIileImageData;

	private TileFactory						fTileFactory;

	/**
	 * custom part for the tile image file path
	 */
	private String							fTileCustomPath;

	// time for statistics
	private long							fTimeIsQueued;
	private long							fTimeStartLoading;
	private long							fTimeEndLoading;

	/**
	 * contains children which contains loading errors
	 */
	private ConcurrentHashMap<String, Tile>	fChildrenWithErrors;
	private String							tileCreatorId;

	public static enum OverlayStatus {
		NOT_SET,
		//
		IN_QUEUE,
		//		
		IMAGE_AVAILABLE,
		//
		NO_IMAGE,
		//
		IS_PART_IMAGE,
	}

	/**
	 * create a key for a tile
	 * 
	 * @param tileFactory
	 * @param x
	 * @param y
	 * @param zoom
	 * @param tileCreatorId
	 * @param customTileKey
	 *            custom tile key which can be <code>null</code> when it's not set
	 * @param projectionId
	 * @return
	 */
	public static String getTileKey(final TileFactory tileFactory,
									final int x,
									final int y,
									final int zoom,
									final String tileCreatorId,
									String customTileKey,
									String projectionId) {

		// get tile parameter from the tile factory when they are not yet set
		if (tileFactory != null) {

			if (customTileKey == null) {
				customTileKey = tileFactory.getInfo().getCustomTileKey();
			}

			if (projectionId == null) {
				projectionId = tileFactory.getProjection().getId();
			}
		}

		final StringBuilder sb = new StringBuilder();

		if (tileCreatorId != null) {
			sb.append(tileCreatorId);
			sb.append('-');
		}

		if (customTileKey != null) {
			sb.append(customTileKey);
			sb.append('-');
		}

		if (projectionId != null) {
			sb.append(projectionId);
			sb.append('-');
		}

		sb.append(zoom);
		sb.append('-');
		sb.append(x);
		sb.append('-');
		sb.append(y);

		return sb.toString();
	}

	/**
	 * Create a new Tile at the specified tile point and zoom level
	 * 
	 * @param tileFactory
	 *            contains tile factory or <code>null</code> when an empty tile is created
	 * @param x
	 * @param y
	 * @param zoom
	 * @param tileCreatorId
	 */
	public Tile(final TileFactory tileFactory, final int x, final int y, final int zoom, final String tileCreatorId) {

		fTileFactory = tileFactory;

		this.x = x;
		this.y = y;
		this.zoom = zoom;
		this.tileCreatorId = tileCreatorId;

		fTileKey = getTileKey(tileFactory, x, y, zoom, tileCreatorId, null, null);
	}

	@Override
	public void addObserver(final Observer o) {
		super.addObserver(o);
	}

	/**
	 * @param tileChildren
	 * @return Returns <code>true</code> when all children are loaded, otherwise <code>false</code>
	 */
	private boolean areAllChildrenLoaded(final ArrayList<Tile> tileChildren) {

		for (final Tile childTile : tileChildren) {

			final ImageData[] childImageData = childTile.getChildImageData();

			if (childImageData == null || childImageData[0] == null) {

				// child image data are not available

				if (childTile.isLoadingError()) {

					// loading of the tile is done with an error

					continue;
				}

				return false;
			}
		}

		return true;
	}

	/**
	 * Set child image data into the parent tile and create the parent image when all child images
	 * are available
	 * 
	 * @param childImageData
	 * @return
	 */
	public ParentImageStatus createParentImage(final ImageData[] childImageData) {

		fChildIileImageData = childImageData;

		if (fParentTile == null) {
			StatusUtil.showStatus("parent tile is not set", new Exception());
			return null;
		}

		final ArrayList<Tile> tileChildren = fParentTile.getChildren();
		if (tileChildren != null) {

			final ReentrantLock parentLock = fParentTile.PARENT_LOCK;
			parentLock.lock();
			{
				try {

					// check if the parent is already created
					final Image parentImage = fParentTile.mapImage;
					if (parentImage != null && !parentImage.isDisposed()) {
						// parent image is already created
						return new ParentImageStatus(null, false, false);

					}

					// check if all children are loaded
					if (areAllChildrenLoaded(tileChildren)) {

						// create parent image when all childs are loaded
						final TileFactory parentTileFactory = fParentTile.fTileFactory;
						if (parentTileFactory instanceof ITileChildrenCreator) {

							return ((ITileChildrenCreator) parentTileFactory).getParentImage(fParentTile, this);
						}

					} else {

						// all children are not yet loaded, create return status
						return new ParentImageStatus(null, false, false);
					}

				} finally {
					parentLock.unlock();
				}
			}
		}

		return null;
	}

	/**
	 * @param loadingTiles
	 * @return Returns the tile children or <code>null</code> when the tile has no children
	 */
	public ArrayList<Tile> createTileChildren(final ConcurrentHashMap<String, Tile> loadingTiles) {

		if (fTileFactory instanceof ITileChildrenCreator) {

			if (fTileChildren == null) {

				PARENT_LOCK = new ReentrantLock();
				fTileChildren = ((ITileChildrenCreator) fTileFactory).createTileChildren(this, loadingTiles);
			}

			return fTileChildren;
		}

		return null;
	}

	public BoundingBoxEPSG4326 getBbox() {
		return fBoundingBox;
	}

	/**
	 * Check if the new image is valid
	 * 
	 * @param newImage
	 * @return Returns a valid image or <code>null</code> when the image is invald
	 */
	private Image getCheckedImage(Image image) {

		// ckeck if available or disposed
		if (image == null || image.isDisposed()) {
			image = null;
			return null;
		}

		// check image bounds
		final Rectangle imageBounds = image.getBounds();
		if (imageBounds.width <= 0 || imageBounds.height <= 0) {
			image.dispose();
			image = null;
			return null;
		}

		return image;
	}

	/**
	 * @return Returns the map image for this tile or <code>null</code> when the image is not
	 *         available or is disposed
	 */
	public Image getCheckedMapImage() {
		return getCheckedImage(mapImage);
	}

	public ImageData[] getChildImageData() {
		return fChildIileImageData;
	}

	/**
	 * @return Returns children tiles when this tile is a parent, otherwise <code>null</code>
	 */
	public ArrayList<Tile> getChildren() {
		return fTileChildren;
	}

	public ConcurrentHashMap<String, Tile> getChildrenWithErrors() {
		return fChildrenWithErrors;
	}

	/**
	 * @return custom data which are set with {@link #setData(Object)}
	 */
	public Object getData() {
		return fCustomData;
	}

	/**
	 * Returns the Throwable tied to any error that may have ocurred while loading the tile. This
	 * error may change several times if multiple errors occur
	 * 
	 * @return
	 */
	public Throwable getError() {
		return error;
	}

	public Future<?> getFuture() {
		return fFuture;
	}

	/**
	 * @return Returns the loading error when loading fails or <code>null</code> when an error is
	 *         not set
	 */
	public String getLoadingError() {
		return fLoadingError == null ? null : fLoadingError.length() == 0 ? null : fLoadingError;
	}

	/**
	 * @return Returns the path which was used to load the offline image
	 */
	public String getOfflinePath() {
		return fOfflinePath;
	}

	/**
	 * @return Returns the overlay image for this tile or <code>null</code> when the image is not
	 *         available or invalid
	 */
	public Image getOverlayImage() {
		return getCheckedImage(overlayImage);
	}

	public OverlayStatus getOverlayStatus() {
		return overlayStatus;
	}

	public Tile getParentTile() {
		return fParentTile;
	}

	public String getTileCustomPath() {
		return fTileCustomPath;
	}

	public TileFactory getTileFactory() {
		return fTileFactory;
	}

	public String getTileKey() {
		return fTileKey;
	}

	public String getTileKey(final int xOffset, final int yOffset) {
		return getTileKey(fTileFactory, x + xOffset, y + yOffset, zoom, tileCreatorId, null, null);
	}

	public long getTimeEndLoading() {
		return fTimeEndLoading;
	}

	public long getTimeIsQueued() {
		return fTimeIsQueued;
	}

	public long getTimeStartLoading() {
		return fTimeStartLoading;
	}

	/**
	 * @return Returns the url which is used to load the tile, or null when it's not loaded
	 */
	public String getUrl() {
		return fUrl;
	}

	/**
	 * @return Returns the tile position for the x-axis
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return Returns the tile position for the y-axis
	 */
	public int getY() {
		return y;
	}

	/**
	 * @return the zoom level that this tile belongs in
	 */
	public int getZoom() {
		return zoom;
	}

	/**
	 * @return Returns <code>true</code> when this tile is a child of another tile
	 */
	public boolean isChildTile() {
		return fParentTile != null;
	}

	/**
	 * @return Returns <code>true</code> when the tile image is currently being loaded
	 */
	public boolean isLoading() {
		return isLoading;
	}

	/**
	 * @return Returns <code>true</code> when loading of the tile image from the internet failed
	 */
	public boolean isLoadingError() {
		return fLoadingError != null && fLoadingError.length() > 0;
	}

	/**
	 * @return Returns <code>true</code> when the offline image is available in the file system,
	 *         otherwise <code>false</code>
	 */
	public boolean isOfflimeImageAvailable() {

		if (fIsOfflineError) {
			return false;
		}

		return fIsOfflineImageAvailable;
	}

	public boolean isOfflineError() {
		return fIsOfflineError;
	}

//	public void setBboxOLD(final TileFactoryInfo info, final Projection projection) {
//
//		final int tileSize = info.getTileSize();
//
//		final int devX = x * tileSize;
//		final int devY = y * tileSize;
//
//		final Point2D.Double topLeft = new Point2D.Double(devX, devY);
//		final Point2D.Double bottomRight = new Point2D.Double(devX + tileSize, devY + tileSize);
//
////		bboxTopLeft = projection.pixelToGeo(topLeft, zoom, info);
////		bboxBottomRight = projection.pixelToGeo(bottomRight, zoom, info);
////
////		/*
////		 * adjust latitude to the north/south pole
////		 */
////		final double top = Math.abs(bboxTopLeft.getLatitude());
////		final double topDiff = top - MAX_LATITUDE_85_05112877;
////		if (topDiff > 0.0) {
////			bboxTopLeft.setLatitude(MAX_LATITUDE_85_05112877);
////		}
////		final double bottom = Math.abs(bboxBottomRight.getLatitude());
////		final double bottomDiff = bottom - MAX_LATITUDE_85_05112877;
////		if (bottomDiff > 0.0) {
////			bboxBottomRight.setLatitude(-MAX_LATITUDE_85_05112877);
////		}
//	}

	/**
	 * notify image observers that the image has changed
	 */
	void notifyImageObservers() {

		setChanged();
		notifyObservers();
	}

	/**
	 * reset overlay in this tile, by resetting the status status
	 */
	public void resetOverlay() {

// disabled image dispose to keep the image

//		if (overlayImage != null && !overlayImage.isDisposed()) {
//			overlayImage.dispose();
//		}

		overlayStatus = OverlayStatus.NOT_SET;
		overlayImage = null;
	}

	/**
	 * Sets the mercator bounding box for this tile
	 * 
	 * @param info
	 * @param projection
	 */
	public void setBoundingBoxEPSG4326() {
		fBoundingBox = BoundingBoxEPSG4326.tile2boundingBox(x, y, zoom);
	}

	public void setChildLoadingError(final Tile childTile) {

		if (fChildrenWithErrors == null) {

			PARENT_LOCK.lock();
			try {

				// check again
				if (fChildrenWithErrors == null) {

					fChildrenWithErrors = new ConcurrentHashMap<String, Tile>();
				}

			} finally {
				PARENT_LOCK.unlock();
			}
		}

		fChildrenWithErrors.put(childTile.getTileKey(), childTile);
	}

	/**
	 * Set custom data which can be retrieved with {@link #getData()}
	 * 
	 * @param customData
	 */
	public void setData(final Object customData) {
		fCustomData = customData;
	}

	public void setFuture(final Future<?> future) {
		fFuture = future;
	}

	public void setIsOfflineImageAvailable(final boolean isOfflineImageAvailable) {
		fIsOfflineImageAvailable = isOfflineImageAvailable;
	}

	/**
	 * Set loading status, when loading is set to <code>false</code> the oberservs (which is the
	 * Map) are notified
	 * 
	 * @param loading
	 */
	public void setLoading(final boolean loading) {

		isLoading = loading;

		if (loading == false) {

//			System.out.println(System.nanoTime()
//					+ "\t"
//					+ Thread.currentThread()
//					+ "\tsetLoading() is done\t\t"
//					+ getTileKey());
//			// TODO remove SYSTEM.OUT.PRINTLN

//			notifyImageObservers();
		}
	}

	/**
	 * Set error message when loading of the image failed
	 * 
	 * @param loadingError
	 */
	public void setLoadingError(final String loadingError) {

		fLoadingError = loadingError;

		if (mapImage != null && !mapImage.isDisposed()) {

			try {
				mapImage.dispose();
			} catch (final Exception e) {
				// ignore, this case happened that image was already disposed by another thread
			}

			mapImage = null;
		}

		if (fParentTile != null) {
			fParentTile.setChildLoadingError(this);
		}

	}

	/**
	 * Set the map image for this tile, the image is checked before it is set
	 * 
	 * @param newImage
	 * @return Returns <code>true</code> when the image was set, <code>false</code> when the image
	 *         is invalid
	 */
	public boolean setMapImage(final Image newImage) {

		if (newImage != null) {
			int a = 0;
			a++;
		}

		mapImage = getCheckedImage(newImage);

		return mapImage != null;
	}

	public void setOfflineError(final boolean isOfflineError) {
		fIsOfflineError = isOfflineError;
	}

	/**
	 * sets the path which was used to load the offline image
	 * 
	 * @param osTilePath
	 */
	public void setOfflinePath(final String osTilePath) {
		fOfflinePath = osTilePath;
	}

	public void setOverlayImage(final Image image) {
		overlayImage = image;
	}

	public void setOverlayStatus(final OverlayStatus newStatus) {
		overlayStatus = newStatus;
	}

	public void setParentTile(final Tile parentTile) {
		fParentTile = parentTile;
	}

	public void setTileCustomPath(final String tileCustomPath) {
		fTileCustomPath = tileCustomPath;
	}

	public void setTimeEndLoading(final long nanoTime) {
		fTimeEndLoading = nanoTime;
	}

	public void setTimeIsQueued(final long nanoTime) {
		fTimeIsQueued = nanoTime;
		fTimeStartLoading = 0;
		fTimeEndLoading = 0;
	}

	public void setTimeStartLoading(final long nanoTime) {
		fTimeStartLoading = nanoTime;
	}

	public void setUrl(final String tileUrl) {
		fUrl = tileUrl;
	}

	@Override
	public String toString() {

		final boolean isImageOK = mapImage == null ? //
				false
				: mapImage.isDisposed() ? //
						false
						: true;

		return (" z=" + Integer.toString(zoom).concat(COLUMN_2).substring(0, 2)) //
				+ (" x=" + Integer.toString(x).concat(COLUMN_5).substring(0, 5))
				+ (" y=" + Integer.toString(y).concat(COLUMN_5).substring(0, 5))
				+ (isLoading ? " LOAD" : COLUMN_5)
				+ (" img=" + (isImageOK ? "OK" : COLUMN_2))
				+ (isLoadingError() ? " ERR" : COLUMN_4)
				//
				//                        0123456789012345678901234567890123456789		
				+ (" " + fTileKey.concat("                                        ").substring(0, 40))
		//
		;
	}

}
