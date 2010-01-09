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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import de.byteholder.geoclipse.Messages;
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

	private static final String				COLUMN_2			= "  ";								//$NON-NLS-1$
	private static final String				COLUMN_4			= "    ";								//$NON-NLS-1$
	private static final String				COLUMN_5			= "     ";								//$NON-NLS-1$

	private OverlayTourState				fOverlayTourState	= OverlayTourState.TILE_IS_NOT_CHECKED;
	private OverlayImageState				fOverlayImageState	= OverlayImageState.NOT_SET;
	private int								fOverlayContent		= 0;

//	/**
//	 * <pre>
//	 * 
//	 * y,x
//	 * 
//	 * 0,0		0,1		0,2
//	 * 1,0		1,1		1,2
//	 * 2,0		2,1		2,2
//	 * 
//	 * </pre>
//	 */
//
//	public static int						OVERLAY_CONTENT_0_0			= 0x0001;							// 0000 0000 0000 0001
//	public static int						OVERLAY_CONTENT_0_1			= 0x0002;							// 0000 0000 0000 0010
//	public static int						OVERLAY_CONTENT_0_2			= 0x0004;							// 0000 0000 0000 0100
//	public static int						OVERLAY_CONTENT_1_0			= 0x0008;							// 0000 0000 0000 1000
//	public static int						OVERLAY_CONTENT_1_1			= 0x0010;							// 0000 0000 0001 0000
//	public static int						OVERLAY_CONTENT_1_2			= 0x0020;							// 0000 0000 0010 0000
//	public static int						OVERLAY_CONTENT_2_0			= 0x0040;							// 0000 0000 0100 0000
//	public static int						OVERLAY_CONTENT_2_1			= 0x0080;							// 0000 0000 1000 0000
//	public static int						OVERLAY_CONTENT_2_2			= 0x0100;							// 0000 0001 0000 0000

	private boolean							isLoading			= false;

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
	private Image							mapImage			= null;

	/**
	 * Image for the overlay tile, NOT the surrounding part tiles
	 */
	private Image							fOverlayImage;

	private boolean							fIsOfflineError		= false;

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
	private String							fLoadingError		= null;

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

	private ImageData[]						fChildTileImageData;

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

		fTileKey = getTileKey(tileFactory, x, y, zoom, tileCreatorId, null, tileFactory.getProjection().getId());
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

		fChildTileImageData = childImageData;

		if (fParentTile == null) {
			StatusUtil.showStatus(NLS.bind(Messages.DBG057_MapProfile_NoParentTile, getTileKey()), new Exception());
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
		return fChildTileImageData;
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

	public int getOverlayContent() {
		return fOverlayContent;
	}

	public Image getOverlayImage() {
		return fOverlayImage;
	}

	public OverlayImageState getOverlayImageState() {
		return fOverlayImageState;
	}

	public OverlayTourState getOverlayTourStatus() {
		return fOverlayTourState;
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

	public String getTileKey(final int xOffset, final int yOffset, final String projectionId) {
		return getTileKey(fTileFactory, x + xOffset, y + yOffset, zoom, tileCreatorId, null, projectionId);
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
	 * Increments the overlay content counter
	 */
	public void incrementOverlayContent() {
		fOverlayContent++;
	}

//	/**
//	 * @return Returns <code>true</code> when this tile is a child of another tile
//	 */
//	public boolean isChildTile() {
//		return fParentTile != null;
//	}

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

	/**
	 * notify image observers that the image has changed
	 */
	void notifyImageObservers() {

		setChanged();
		notifyObservers();
	}

	/**
	 * reset overlay in this tile, by resetting the status state
	 */
	public void resetOverlay() {

		fOverlayTourState = OverlayTourState.TILE_IS_NOT_CHECKED;
		fOverlayImageState = OverlayImageState.NOT_SET;

		fOverlayContent = 0;
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

//		if (newImage != null) {
//			int a = 0;
//			a++;
//		}

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

	public void setOverlayImage(final Image partImage) {
		fOverlayImage = partImage;
	}

	public void setOverlayImageState(final OverlayImageState overlayImageState) {
		fOverlayImageState = overlayImageState;
	}

	public void setOverlayTourStatus(final OverlayTourState overlayTourStatus) {
		fOverlayTourState = overlayTourStatus;
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

		return (" z=" + Integer.toString(zoom).concat(COLUMN_2).substring(0, 2)) // //$NON-NLS-1$
				+ (" x=" + Integer.toString(x).concat(COLUMN_5).substring(0, 5)) //$NON-NLS-1$
				+ (" y=" + Integer.toString(y).concat(COLUMN_5).substring(0, 5)) //$NON-NLS-1$
				+ (isLoading ? " LOAD" : COLUMN_5) //$NON-NLS-1$
				+ (" img=" + (isImageOK ? "OK" : COLUMN_2)) //$NON-NLS-1$ //$NON-NLS-2$
				+ (isLoadingError() ? " ERR" : COLUMN_4) //$NON-NLS-1$
				//
				//                        0123456789012345678901234567890123456789		
				+ (" " + fTileKey.concat("                                        ").substring(0, 40)) //$NON-NLS-1$ //$NON-NLS-2$
		//
		;
	}

}
