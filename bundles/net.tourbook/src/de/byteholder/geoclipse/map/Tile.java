/**
 * Tile.java
 *
 * Created on March 14, 2006, 4:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package de.byteholder.geoclipse.map;

import de.byteholder.geoclipse.mapprovider.ImageDataResources;
import de.byteholder.geoclipse.mapprovider.MP;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourWayPoint;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

/**
 * The Tile class represents a particular square image piece of the world bitmap at a particular
 * zoom level.
 *
 * @author Joshua Marinacci
 * @author Michael Kanis
 * @author Wolfgang
 */

public class Tile extends Observable {

//   private static final double            MAX_LATITUDE_85_05112877   = 85.05112877;

   private static final String             NL                 = "\n";                                //$NON-NLS-1$
   private static final String             COLUMN_2           = "  ";                                //$NON-NLS-1$
   private static final String             COLUMN_4           = "    ";                              //$NON-NLS-1$
   private static final String             COLUMN_5           = "     ";                             //$NON-NLS-1$

   private static final ReentrantLock      TILE_LOCK          = new ReentrantLock();
   private static final int                MAX_BOUNDS         = Map.MAP_MAX_ZOOM_LEVEL + 1;

   private OverlayTourState                _overlayTourState  = OverlayTourState.TILE_IS_NOT_CHECKED;

   /**
    * <pre>
    *
    * y,x
    *
    * 0,0      0,1      0,2
    * 1,0      1,1      1,2
    * 2,0      2,1      2,2
    * </pre>
    */

   private OverlayImageState               _overlayImageState = OverlayImageState.NOT_SET;

   private int                             _overlayContent    = 0;

   /**
    * Map zoom level
    */
   private final int                       _zoom;

   /**
    * Horizontal tile position within the map
    */
   private final int                       _x;

   /**
    * Vertical tile position within the map
    */
   private final int                       _y;

   /**
    * Map provider which provides the tile image
    */
   private final MP                        _mp;

   /**
    * Map image for this tile
    */
   private Image                           _mapImage          = null;

   /**
    * Image for the overlay tile, NOT the surrounding part tiles
    */
   private Image                           _overlayImage;
   /**
    * contains overlay the image data for this tile
    */
   private ImageDataResources              _overlayImageDataResources;

   private final String                    _tileKey;

   private final String                    _tileKeyCreatorId;

   private Object                          _customData;

   /**
    * bbox coordinates
    */
   private BoundingBoxEPSG4326             _boundingBox;

   private Future<?>                       _future;

   private boolean                         _isLoading         = false;

   private boolean                         _isOfflineError    = false;

   /**
    * contains the error message when loading of the image fails
    */
   private String                          _loadingError      = null;

   /**
    * url which is used to load the tile
    */
   private String                          _url;

   private boolean                         _isOfflineImageAvailable;

   /**
    * path which was used to load the offline image
    */
   private String                          _offlinePath;

   private ReentrantLock                   PARENT_LOCK;

   /**
    * Contains the parent tile when this tile is a child tile. This field can be null to preserve
    * the child. The field {@link #_isChild} determines if this tile was a child of a parent tile.
    */
   private Tile                            _parentTile;

   /**
    * Is <code>true</code> when this is is a child tile. It is possible that the parent tile field
    * {@link #_parentTile} is set to <code>null</code> to keep the tile in a cache when the tile has
    * loading errors
    */
   private boolean                         _isChild           = false;

   /**
    * When set, this is a parent tile which has children tiles
    */
   private ArrayList<Tile>                 _tileChildren;

   private ImageData                       _childTileImageData;
   /**
    * Custom part for the tile image file path
    */
   private String                          _tileCustomPath;
   // times for the statistics
   private long                            _timeIsQueued;

   private long                            _timeStartLoading;
   private long                            _timeEndLoading;

   /**
    * contains children which contains loading errors
    */
   private ConcurrentHashMap<String, Tile> _childrenWithErrors;

   @SuppressWarnings("unchecked")
   private final ArrayList<Rectangle>[]    _markerBounds      = new ArrayList[MAX_BOUNDS];
   @SuppressWarnings("unchecked")
   private final ArrayList<Rectangle>[]    _markerPartBounds  = new ArrayList[MAX_BOUNDS];

   /**
    * Contains the {@link TourWayPoint}'s which are displayed in this tile.
    * <p>
    * {@link #_twpSimpleBounds} and {@link #_twpEnhancedBounds} contains the rectangles in the same
    * sequence as {@link #_twp}.
    */
   @SuppressWarnings("unchecked")
   private final ArrayList<TourWayPoint>[] _twp               = new ArrayList[MAX_BOUNDS];

   @SuppressWarnings("unchecked")
   private final ArrayList<Rectangle>[]    _twpSimpleBounds   = new ArrayList[MAX_BOUNDS];

   @SuppressWarnings("unchecked")
   private final ArrayList<Rectangle>[]    _twpEnhancedBounds = new ArrayList[MAX_BOUNDS];

   /**
    * The hover rectangles will be set when a tile is painted, the rectangle position is relative to
    * the tile
    */
   public ArrayList<Rectangle>             allPainted_HoverRectangle  = new ArrayList<>();
   public TLongArrayList                   allPainted_HoverTourID     = new TLongArrayList();

   /**
    * Create a new Tile at the specified tile point and zoom level
    *
    * @param mp
    *           map provider which creates the tile image
    * @param zoom
    * @param x
    * @param y
    * @param tileCreatorId
    */
   public Tile(final MP mp, final int zoom, final int x, final int y, final String tileCreatorId) {

      _mp = mp;

      _zoom = zoom;
      _x = x;
      _y = y;

      _tileKeyCreatorId = tileCreatorId;

      _tileKey = getTileKey(mp, zoom, x, y, tileCreatorId, null, mp.getProjection().getId());
   }

   /**
    * create a key for a tile
    *
    * @param mp
    * @param zoom
    * @param x
    * @param y
    * @param tileCreatorId
    * @param customTileKey
    *           custom tile key which can be <code>null</code> when it's not set
    * @param projectionId
    * @return
    */
   public static String getTileKey(final MP mp,
                                   final int zoom,
                                   final int x,
                                   final int y,
                                   final String tileCreatorId,
                                   final String customTileKey,
                                   final String projectionId) {

      final StringBuilder sb = new StringBuilder(100);

      sb.append(mp.getId());
      sb.append('-');

      sb.append(zoom);
      sb.append('-');
      sb.append(x);
      sb.append('-');
      sb.append(y);

      if (tileCreatorId != null) {
         sb.append('-');
         sb.append(tileCreatorId);
      }

      if (customTileKey != null) {
         sb.append('-');
         sb.append(customTileKey);
      }

      if (projectionId != null) {
         sb.append('-');
         sb.append(projectionId);
      }

      return sb.toString();
   }

   public void addMarkerBounds(final int x, //
                               final int y,
                               final int width,
                               final int height,
                               final int zoomLevel) {

      if (_markerBounds[zoomLevel] == null) {
         initBounds(zoomLevel);
      }

      _markerBounds[zoomLevel].add(new Rectangle(x, y, width, height));
   }

   /**
    * @param x
    *           left position relative to the tile image
    * @param y
    *           top position relative to the tile image
    * @param width
    * @param height
    * @param zoomLevel
    * @param parts
    *           number of parts for which the marker is painted
    */
   public void addMarkerBounds(final int x,
                               final int y,
                               final int width,
                               final int height,
                               final int zoomLevel,
                               final int parts) {

      if (_markerBounds[zoomLevel] == null) {
         initBounds(zoomLevel);
      }

//      final Rectangle markerBounds = new Rectangle(x < 0 ? 0 : x, y < 0 ? 0 : y, width, height);
      final Rectangle markerBounds = new Rectangle(x, y, width, height);

      if (parts == 1) {
         _markerBounds[zoomLevel].add(markerBounds);
      } else {
         _markerPartBounds[zoomLevel].add(markerBounds);
      }
   }

   @Override
   public void addObserver(final Observer o) {
      super.addObserver(o);
   }

   /**
    * @param twp
    * @param twpBounds
    *           Hovered area for the tour way point.
    *           <p>
    *           <i>x/y</i> is the top left corner within the control, <br>
    *           <i>width/height</i> is the dimension for the hovered area which is the size of the
    *           painted image
    * @param zoomLevel
    * @param parts
    */
   public void addTourWayPointBounds(final TourWayPoint twp,
                                     final Rectangle twpBounds,
                                     final int zoomLevel,
                                     final int parts) {

      if (_markerBounds[zoomLevel] == null) {
         initBounds(zoomLevel);
      }

      final ArrayList<TourWayPoint> twpList = _twp[zoomLevel];

      if (twpList.contains(twp) == false) {

         /*
          * The way point is set at the beginning of the list because further way point could
          * overpaint the current. So the later painted way point can be not covered by another and
          * the tooltips are displayed accordingly.
          */
         twpList.add(0, twp);

         if (parts == 1) {
            _twpSimpleBounds[zoomLevel].add(0, twpBounds);
         } else {
            _twpEnhancedBounds[zoomLevel].add(0, twpBounds);
         }
      }
   }

   /**
    * @param tileChildren
    * @return Returns <code>true</code> when all children are loaded, otherwise <code>false</code>
    */
   private boolean areAllChildrenLoaded(final ArrayList<Tile> tileChildren) {

      for (final Tile childTile : tileChildren) {

         final ImageData childImageData = childTile.getChildImageData();

         if (childImageData == null) {

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

   public synchronized Image createOverlayImage(final Device display) {

      if (_overlayImageDataResources == null) {
         return null;
      }

      try {

         // it is synchronized because this object can be set to null in another thread
         synchronized (_overlayImageDataResources) {

            // check _overlayImageDataResources again, it could be null at this time
            if (_overlayImageDataResources == null) {
               return null;
            }

            final ImageData tileImageData = _overlayImageDataResources.getTileImageData();
            final ImageData neighborImageData = _overlayImageDataResources.getNeighborImageData();

            if ((tileImageData == null) && (neighborImageData == null)) {
               return null;
            }

            final int tileSize = _mp.getTileSize();
            final ImageData finalImageData = UI.createTransparentImageData(tileSize);

            // draw neighbor first
            if (neighborImageData != null) {

               // check _overlayImageDataResources again, it could be null at this time
               if (_overlayImageDataResources == null) {
                  return null;
               }
               _overlayImageDataResources.drawImageData(
                     finalImageData,
                     neighborImageData,
                     0,
                     0,
                     tileSize,
                     tileSize);
            }

            // draw tile last to overwrite neighbor image data,
            if (tileImageData != null) {

               // check _overlayImageDataResources again, it could be null at this time
               if (_overlayImageDataResources == null) {
                  return null;
               }
               _overlayImageDataResources.drawImageData(finalImageData, tileImageData, 0, 0, tileSize, tileSize);
            }

            // create image from image data
            _overlayImage = new Image(display, finalImageData);

            return _overlayImage;
         }

      } catch (final Exception e) {
         // log it but don't show because it happened too often
         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * Set child image data into the parent tile and create the parent image when all child images
    * are available
    *
    * @param childImageData
    * @return
    */
   public ParentImageStatus createParentImage(final ImageData childImageData) {

      _childTileImageData = childImageData;

      // create a copy because parent tile can be set to null
      final Tile parentTile = _parentTile;

      if (parentTile == null) {
         // this happens often -> disabled log
//         StatusUtil.log(NLS.bind(Messages.DBG057_MapProfile_NoParentTile, getTileKey()), new Exception());
         return null;
      }

      final ArrayList<Tile> tileChildren = parentTile.getChildren();
      if (tileChildren != null) {

         final ReentrantLock parentLock = parentTile.PARENT_LOCK;
         parentLock.lock();
         {
            try {

               // check if the parent is already created
               final Image parentImage = parentTile._mapImage;
               if ((parentImage != null) && !parentImage.isDisposed()) {
                  // parent image is already created
                  return new ParentImageStatus(null, false, false, false);

               }

               // check if all children are loaded
               if (areAllChildrenLoaded(tileChildren)) {

                  // create parent image when all childs are loaded
                  final MP parentMp = parentTile._mp;
                  if (parentMp instanceof ITileChildrenCreator) {

                     final ParentImageStatus parentImageStatus = ((ITileChildrenCreator) parentMp)
                           .getParentImage(parentTile);

                     // prevent memory leaks: remove image data in the chilren tiles
                     for (final Tile childTile : tileChildren) {
                        childTile._childTileImageData = null;
                     }

                     return parentImageStatus;
                  }

               } else {

                  // all children are not yet loaded, create return status
                  return new ParentImageStatus(null, false, false, false);
               }

            } finally {
               parentLock.unlock();
            }
         }
      }

      return null;
   }

   /**
    * Creates tile children for all mp wrapper which are displayed in one tile
    *
    * @return Returns the tile children or <code>null</code> when the tile has no children. A list
    *         is returned with children which are not yet available in the tile cache or error
    *         cache, children are skipped when they already exist and have loading errord
    */
   public ArrayList<Tile> createTileChildren() {

      if (_mp instanceof ITileChildrenCreator) {

         if (_tileChildren == null) {

            PARENT_LOCK = new ReentrantLock();
            _tileChildren = ((ITileChildrenCreator) _mp).createTileChildren(this);
         }

         return _tileChildren;
      }

      return null;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof Tile)) {
         return false;
      }
      final Tile other = (Tile) obj;
      if (_tileKey == null) {
         if (other._tileKey != null) {
            return false;
         }
      } else if (!_tileKey.equals(other._tileKey)) {
         return false;
      }
      return true;
   }

   public BoundingBoxEPSG4326 getBbox() {
      return _boundingBox;
   }

   /**
    * Check if the new image is valid
    *
    * @param newImage
    * @return Returns a valid image or <code>null</code> when the image is invald
    */
   private Image getCheckedImage(Image image) {

      // ckeck if available or disposed
      if ((image == null) || image.isDisposed()) {
         image = null;
         return null;
      }

//      // check image bounds
//      final Rectangle imageBounds = image.getBounds();
//      if (imageBounds.width <= 0 || imageBounds.height <= 0) {
//         image.dispose();
//         image = null;
//         return null;
//      }

      return image;
   }

   /**
    * @return Returns the map image for this tile or <code>null</code> when the image is not
    *         available or is disposed
    */
   public Image getCheckedMapImage() {
      return getCheckedImage(_mapImage);
   }

   public ImageData getChildImageData() {
      return _childTileImageData;
   }

   /**
    * @return Returns children tiles when this tile is a parent, otherwise <code>null</code>
    */
   public ArrayList<Tile> getChildren() {
      return _tileChildren;
   }

   public ConcurrentHashMap<String, Tile> getChildrenWithErrors() {
      return _childrenWithErrors;
   }

   /**
    * @return custom data which are set with {@link #setData(Object)}
    */
   public Object getData() {
      return _customData;
   }

   public Future<?> getFuture() {
      return _future;
   }

   /**
    * @return Returns the loading error when loading fails or <code>null</code> when an error is not
    *         set
    */
   public String getLoadingError() {
      return _loadingError == null ? null : _loadingError.length() == 0 ? null : _loadingError;
   }

   /**
    * @return Returns the map provider for this tile
    */
   public MP getMP() {
      return _mp;
   }

   /**
    * @return Returns the path which was used to load the offline image
    */
   public String getOfflinePath() {
      return _offlinePath;
   }

   public int getOverlayContent() {
      return _overlayContent;
   }

   public Image getOverlayImage() {
      return _overlayImage;
   }

   public ImageDataResources getOverlayImageDataResources() {

      if (_overlayImageDataResources != null) {
         return _overlayImageDataResources;
      }

      TILE_LOCK.lock();
      {
         try {

            // check again
            if (_overlayImageDataResources != null) {
               return _overlayImageDataResources;
            }

            _overlayImageDataResources = new ImageDataResources(_mp.getTileSize());

         } finally {
            TILE_LOCK.unlock();
         }
      }

      return _overlayImageDataResources;
   }

   public OverlayImageState getOverlayImageState() {
      return _overlayImageState;
   }

   public OverlayTourState getOverlayTourStatus() {
      return _overlayTourState;
   }

   /**
    * @return Return the parent tile when this tile is a child tile or <code>null</code> when this
    *         is NOT a child tile.
    */
   public Tile getParentTile() {
      return _parentTile;
   }

   /**
    * @param zoomLevel
    * @return Returns marker bounds which are set for a part or <code>null</code> when there are no
    *         part marker bounds
    */
   public ArrayList<Rectangle> getPartMarkerBounds(final int zoomLevel) {
      return _markerPartBounds[zoomLevel];
   }

   public String getTileCustomPath() {
      return _tileCustomPath;
   }

   public String getTileKey() {
      return _tileKey;
   }

   public String getTileKey(final int xOffset, final int yOffset, final String projectionId) {
      return getTileKey(_mp, _zoom, _x + xOffset, _y + yOffset, _tileKeyCreatorId, null, projectionId);
   }

   public long getTimeEndLoading() {
      return _timeEndLoading;
   }

//   /**
//    * @return Returns <code>true</code> when this tile is a child of another tile
//    */
//   public boolean isChildTile() {
//      return fParentTile != null;
//   }

   public long getTimeIsQueued() {
      return _timeIsQueued;
   }

   public long getTimeStartLoading() {
      return _timeStartLoading;
   }

   /**
    * @return Returns the url which is used to load the tile, or null when it's not loaded
    */
   public String getUrl() {
      return _url;
   }

   /**
    * @param mapZoomLevel
    * @param isTourPaintMethodEnhanced
    *           When <code>true</code> the overlay image is painted with the enhanced method which
    *           is currently 3 x 3 parts.
    * @return Returns a list with rectangles for each way point in the tile or <code>null</code>
    *         when there are no way points within the tile.
    */
   public ArrayList<Rectangle> getWayPointBounds(final int mapZoomLevel, final boolean isTourPaintMethodEnhanced) {

      if (isTourPaintMethodEnhanced) {
         return _twpEnhancedBounds[mapZoomLevel];
      }

      return _twpSimpleBounds[mapZoomLevel];
   }

   public ArrayList<TourWayPoint> getWayPoints(final int _mapZoomLevel) {
      return _twp[_mapZoomLevel];
   }

   /**
    * @return Returns the tile position for the x-axis
    */
   public int getX() {
      return _x;
   }

   /**
    * @return Returns the tile position for the y-axis
    */
   public int getY() {
      return _y;
   }

   /**
    * @return the zoom level that this tile belongs in
    */
   public int getZoom() {
      return _zoom;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_tileKey == null) ? 0 : _tileKey.hashCode());
      return result;
   }

   /**
    * Increments the overlay content counter
    */
   public void incrementOverlayContent() {
      _overlayContent++;
   }

   /**
    * creates marker bounds array for the zoom level
    *
    * @param zoomLevel
    */
   private void initBounds(final int zoomLevel) {

      TILE_LOCK.lock();
      try {

         // check again
         if (_markerBounds[zoomLevel] == null) {

            // create bounds for current zoomlevel

            _twp[zoomLevel] = new ArrayList<>();

            _markerBounds[zoomLevel] = new ArrayList<>();
            _markerPartBounds[zoomLevel] = new ArrayList<>();
            _twpSimpleBounds[zoomLevel] = new ArrayList<>();
            _twpEnhancedBounds[zoomLevel] = new ArrayList<>();
         }

      } finally {
         TILE_LOCK.unlock();
      }
   }

   /**
    * @return Returns <code>true</code> when this is is a child tile, it is possible that the parent
    *         tile field {@link #_parentTile} was set to null, to keep the tile in a cache when the
    *         tile has loading errors
    */
   public boolean isChild() {
      return _isChild;
   }

   public boolean isImageValid() {

      if (_mapImage == null) {
         return false;
      }

      return getCheckedImage(_mapImage) != null;
   }

   /**
    * @return Returns <code>true</code> when the tile image is currently being loaded
    */
   public boolean isLoading() {
      return _isLoading;
   }

   /**
    * @return Returns <code>true</code> when loading of the tile image failed
    */
   public boolean isLoadingError() {
      return _loadingError != null && _loadingError.length() > 0;
   }

   /**
    * @return Returns <code>true</code> when the offline image is available in the file system,
    *         otherwise <code>false</code>
    */
   public boolean isOfflimeImageAvailable() {

      if (_isOfflineError) {
         return false;
      }

      return _isOfflineImageAvailable;
   }

   public boolean isOfflineError() {
      return _isOfflineError;
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

      _overlayTourState = OverlayTourState.TILE_IS_NOT_CHECKED;
      _overlayImageState = OverlayImageState.NOT_SET;

      _overlayContent = 0;

      _overlayImageDataResources = null;

      /*
       * reset all bounds for all zoomlevels, this is necessary when a new tour is displayed
       */
      for (int zoomLevel = 0; zoomLevel < MAX_BOUNDS; zoomLevel++) {

         final ArrayList<TourWayPoint> twpBounds = _twp[zoomLevel];
         if (twpBounds != null) {
            twpBounds.clear();
         }

         ArrayList<Rectangle> bounds = _markerBounds[zoomLevel];
         if (bounds != null) {
            bounds.clear();
         }

         bounds = _markerPartBounds[zoomLevel];
         if (bounds != null) {
            bounds.clear();
         }

         bounds = _twpSimpleBounds[zoomLevel];
         if (bounds != null) {
            bounds.clear();
         }

         bounds = _twpEnhancedBounds[zoomLevel];
         if (bounds != null) {
            bounds.clear();
         }
      }

   }

   /**
    * Sets the mercator bounding box for this tile
    *
    * @param info
    * @param projection
    */
   public void setBoundingBoxEPSG4326() {
      _boundingBox = BoundingBoxEPSG4326.tile2boundingBox(_x, _y, _zoom);
   }

   public void setChildLoadingError(final Tile childTile) {

      if (_childrenWithErrors == null) {

         PARENT_LOCK.lock();
         try {

            // check again
            if (_childrenWithErrors == null) {
               _childrenWithErrors = new ConcurrentHashMap<>();
            }

         } finally {
            PARENT_LOCK.unlock();
         }
      }

      _childrenWithErrors.put(childTile.getTileKey(), childTile);
   }

   /**
    * Set custom data which can be retrieved with {@link #getData()}
    *
    * @param customData
    */
   public void setData(final Object customData) {
      _customData = customData;
   }

   public void setFuture(final Future<?> future) {
      _future = future;
   }

   public void setIsOfflineImageAvailable(final boolean isOfflineImageAvailable) {
      _isOfflineImageAvailable = isOfflineImageAvailable;
   }

   /**
    * Set loading status
    *
    * @param loading
    */
   public void setLoading(final boolean loading) {
      _isLoading = loading;
   }

   /**
    * Set error message when loading of the image failed, an existing tile image will be disposed
    *
    * @param IMAGE_HAS_LOADING_ERROR
    */
   public void setLoadingError(final String loadingError) {

      _loadingError = loadingError;

      if ((_mapImage != null) && !_mapImage.isDisposed()) {

         try {
            _mapImage.dispose();
         } catch (final Exception e) {
            // ignore, this case happened that image was already disposed by another thread
         }

         _mapImage = null;
      }

      // create a copy because parent tile can be set to null
      final Tile parentTile = _parentTile;

      if (parentTile != null) {

         // this is a child tile, set error into the parent tile

         parentTile.setChildLoadingError(this);
      }
   }

   /**
    * Set the map image for this tile, the image is checked before it is set
    *
    * @param newImage
    * @return <code>true</code> when the image was set, <code>false</code> when the image is invalid
    */
   public boolean setMapImage(final Image newImage) {

//      if (newImage != null) {
//         int a = 0;
//         a++;
//      }

      _mapImage = getCheckedImage(newImage);

      return _mapImage != null;
   }

   public void setOfflineError(final boolean isOfflineError) {
      _isOfflineError = isOfflineError;
   }

   /**
    * sets the path which was used to load the offline image
    *
    * @param osTilePath
    */
   public void setOfflinePath(final String osTilePath) {
      _offlinePath = osTilePath;
   }

   public void setOverlayImage(final Image overlayImage) {
      _overlayImage = overlayImage;
   }

   public void setOverlayImageState(final OverlayImageState overlayImageState) {
      _overlayImageState = overlayImageState;
   }

   public void setOverlayTourStatus(final OverlayTourState overlayTourStatus) {
      _overlayTourState = overlayTourStatus;
   }

   public void setParentTile(final Tile parentTile) {
      _parentTile = parentTile;
      _isChild = true;
   }

   public void setTileCustomPath(final String tileCustomPath) {
      _tileCustomPath = tileCustomPath;
   }

   public void setTimeEndLoading(final long nanoTime) {
      _timeEndLoading = nanoTime;
   }

   public void setTimeIsQueued(final long nanoTime) {
      _timeIsQueued = nanoTime;
      _timeStartLoading = 0;
      _timeEndLoading = 0;
   }

   public void setTimeStartLoading(final long nanoTime) {
      _timeStartLoading = nanoTime;
   }

   public void setUrl(final String tileUrl) {
      _url = tileUrl;
   }

   @Override
   public String toString() {

      final boolean isImageOK = _mapImage == null ? //
            false
            : _mapImage.isDisposed() ? //
                  false
                  : true;

      return "" //$NON-NLS-1$

//            + " z=" + Integer.toString(_zoom).concat(COLUMN_2).substring(0, 2) // //$NON-NLS-1$
//            + " x=" + Integer.toString(_x).concat(COLUMN_5).substring(0, 5) //$NON-NLS-1$
//            + " y=" + Integer.toString(_y).concat(COLUMN_5).substring(0, 5) + NL //$NON-NLS-1$

            + " z=" + Integer.toString(_zoom) // //$NON-NLS-1$
            + " x=" + Integer.toString(_x) //$NON-NLS-1$
            + " y=" + Integer.toString(_y) + NL //$NON-NLS-1$

            + "allHoverRectangle: " + allPainted_HoverRectangle.size() + NL //$NON-NLS-1$

//
//            + (_isLoading ? " LOAD" : COLUMN_5) //$NON-NLS-1$
//            + " img=" + (isImageOK ? "OK" : COLUMN_2) //$NON-NLS-1$ //$NON-NLS-2$
//            + (isLoadingError() ? " ERR" : COLUMN_4) //$NON-NLS-1$
//            //
//            //                            0123456789012345678901234567890123456789
//            + (" key=" + _tileKey.concat("                                        ").substring(0, 40)) //$NON-NLS-1$ //$NON-NLS-2$
      //
      ;
   }

}
