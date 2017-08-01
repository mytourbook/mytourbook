/*
 * Original: org.oscim.layers.marker.MarkerRenderer
 */
package net.tourbook.map25.layer.marker;

import java.util.Arrays;
import java.util.Comparator;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.layers.marker.utils.SparseIntArray;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.utils.FastMath;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

public class MarkerRenderer extends BucketRenderer {

	/**
	 * Max number to display inside a cluster icon
	 */
	private static final int					CLUSTER_MAXSIZE				= 10;

	/**
	 * default color of number inside the icon. Would be super-cool to cook this into the map theme
	 */
	private static final int					CLUSTER_COLOR_TEXT			= 0xff8000c0;

	/**
	 * default color of circle background
	 */
	private static final int					CLUSTER_COLOR_BACK			= 0xffffffff;

	/**
	 * Map Cluster Icon Size. This is the biggest size for clusters of CLUSTER_MAXSIZE elements.
	 * Smaller clusters will be slightly smaller
	 */
	public static final int						MAP_MARKER_CLUSTER_SIZE_DP	= 64;

	/**
	 * Clustering grid square size, decrease to cluster more aggresively. Ideally this value is the
	 * typical marker size
	 */
	private static final int					MAP_GRID_SIZE_DP			= 64;

	/**
	 * cached bitmaps database, we will cache cluster bitmaps from 1 to MAX_SIZE and always use same
	 * bitmap for efficiency
	 */
	private static Bitmap[]						_clusterBitmaps				= new Bitmap[CLUSTER_MAXSIZE + 1];

	static TimSort<ProjectedMarker>				ZSORT						= new TimSort<ProjectedMarker>();

	final static Comparator<ProjectedMarker>	zComparator;
	static {

		zComparator = new Comparator<ProjectedMarker>() {

			@Override
			public int compare(	final ProjectedMarker a,
								final ProjectedMarker b) {
				if (a.isVisible && b.isVisible) {
					if (a.dy > b.dy) {
						return -1;
					}
					if (a.dy < b.dy) {
						return 1;
					}
				} else if (a.isVisible) {
					return -1;
				} else if (b.isVisible) {
					return 1;
				}

				return 0;
			}
		};
	}

	MarkerSymbol				defaultMarkerSymbol;

	private final MarkerLayer	_markerLayer;
	private final SymbolBucket	_symbolBucket;

	private final float[]		_tmpBox					= new float[8];
	private final Point			_tmpPoint				= new Point();

	/**
	 * increase view to show items that are partially visible
	 */
	private int					_extents				= 100;

	/**
	 * flag to force update of markers
	 */
	private boolean				_isForceUpdateMarkers;

	private ProjectedMarker[]	_allProjectedMarker;

	private int					_clusterBackgroundColor	= CLUSTER_COLOR_BACK;
	private int					_clusterForegroundColor	= CLUSTER_COLOR_TEXT;

	/**
	 * Discrete scale step, used to trigger reclustering on significant scale change
	 */
	private int					_scaleSaved				= 0;

	/**
	 * We use a flat Sparse array to calculate the clusters. The sparse array models a 2D map where
	 * every (x,y) denotes a grid slot, ie. 64x64dp. For efficiency I use a linear sparsearray with
	 * ARRindex = SLOTypos * max_x + SLOTxpos"
	 */
	private SparseIntArray		_gridMap				= new SparseIntArray(200);		// initial space for 200 markers, that's not a lot of memory, and in most cases will avoid resizing the array

	private double				_clusterScale;

	private int					_clusterSymbolSizeDP	= MAP_MARKER_CLUSTER_SIZE_DP;
	private int					_clusterGridSizeDP		= MAP_GRID_SIZE_DP;

	/**
	 * When <code>true</code> all items are clustered, otherwise nothing is clustered.
	 */
	private boolean				_isClustering			= true;

	/**
	 * When <code>true</code> then the symbol is displayed as billboard, otherwise it is clamped to
	 * the ground.
	 */
	private boolean				_isBillboard;

	/**
	 * Class to wrap the cluster icon style properties
	 */
	public static class ClusterStyle {

		final int	background;
		final int	foreground;

		/**
		 * Creates the Cluster style
		 *
		 * @param fore
		 *            Foreground (border and text) color
		 * @param back
		 *            Background (circle) color
		 */

		public ClusterStyle(final int fore, final int back) {

			foreground = fore;
			background = back;
		}
	}

	/**
	 * Constructs a clustered marker renderer
	 *
	 * @param markerLayer
	 *            The owner layer
	 * @param defaultSymbol
	 *            The default symbol
	 * @param style
	 *            The desired style, or NULL to disable clustering
	 */
	public MarkerRenderer(	final MarkerLayer markerLayer,
							final MarkerSymbol defaultSymbol) {

		_markerLayer = markerLayer;
		defaultMarkerSymbol = defaultSymbol;

		_symbolBucket = new SymbolBucket();

		final ClusterStyle style = new MarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE);

		setClusterStyle(style.foreground, style.background);

		for (int k = 0; k <= CLUSTER_MAXSIZE; k++) {
			// cache bitmaps so render thread never creates them
			// we create CLUSTER_MAXSIZE bitmaps. Bigger clusters will show like "+"
			getClusterBitmap(k);
		}
	}

	public static void sort(final ProjectedMarker[] a, final int lo, final int hi) {

		final int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}

		ZSORT.doSort(a, zComparator, lo, hi);
	}

	/**
	 * Gets a bitmap for a given cluster size
	 *
	 * @param size
	 *            The cluster size. Can be greater than CLUSTER_MAXSIZE.
	 * @return A somewhat cool bitmap to be used as the cluster marker
	 */
	private Bitmap getClusterBitmap(int size) {

		final String strValue;

		if (size >= CLUSTER_MAXSIZE) {

			// restrict cluster indicator size. Bigger clusters will show as "+" instead of ie. "45"
			size = CLUSTER_MAXSIZE;
			strValue = "+";

		} else {

			strValue = String.valueOf(size);
		}

		// return cached bitmap if exists. cache hit !
		if (_clusterBitmaps[size] != null) {
			return _clusterBitmaps[size];
		}

		// create and cache bitmap. This is unacceptable inside the GL thread,
		// so we'll call this routine at the beginning to pre-cache all bitmaps

		final ScreenUtils.ClusterDrawable drawable = new ScreenUtils.ClusterDrawable(

				_clusterSymbolSizeDP - CLUSTER_MAXSIZE + size, // make size dependent on cluster size

				_clusterForegroundColor,
				_clusterBackgroundColor,

				strValue);

		_clusterBitmaps[size] = drawable.getBitmap();

		return _clusterBitmaps[size];
	}

	void populate(final int size) {

		repopulateCluster(size, _clusterScale);
	}

	/**
	 * Repopulates item list clustering close markers. This is triggered from update() when a
	 * significant change in scale has happened.
	 *
	 * @param numMarkers
	 *            Item list size
	 * @param mapScale
	 *            current map scale
	 */
	private void repopulateCluster(final int numMarkers, final double mapScale) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\trepopulateCluster()"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		/*
		 * the grid slot size in px. increase to group more aggressively. currently set to marker
		 * size
		 */
		final int GRIDSIZE = ScreenUtils.getPixels(_clusterGridSizeDP);

		/* the factor to map into Grid Coordinates (discrete squares of GRIDSIZE x GRIDSIZE) */
		final double factor = (mapScale / GRIDSIZE);

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tfactor:" + factor));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final ProjectedMarker[] tmp = new ProjectedMarker[numMarkers];

		// clear grid map to count items that share the same "grid slot"
		_gridMap.clear();

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tGRIDSIZE:" + GRIDSIZE));
		// TODO remove SYSTEM.OUT.PRINTLN

		for (int markerIndex = 0; markerIndex < numMarkers; markerIndex++) {

			final ProjectedMarker projectedMarker = tmp[markerIndex] = new ProjectedMarker();

			projectedMarker.mapMarker = _markerLayer.getMarker(markerIndex);

			/* pre-project points */
			MercatorProjection.project(projectedMarker.mapMarker.geoPoint, _tmpPoint);
			projectedMarker.px = _tmpPoint.x;
			projectedMarker.py = _tmpPoint.y;

			// items can be declared non-clusterable
			if (_isClustering) {

				// absolute item X position in the grid
				final int absposx = (int) (projectedMarker.px * factor);
				final int absposy = (int) (projectedMarker.py * factor); // absolute item Y position

				// Grid number of columns
				final int maxcols = (int) factor;

				// Index in the sparsearray map
				final int itemGridIndex = absposx + absposy * maxcols;

				// we store in the linear sparsearray the index of the marker,
				// ie, index = y * maxcols + x; array[index} = markerIndex

				// Lets check if there's already an item in the grid slot
				final int storedIndexInGridSlot = _gridMap.get(itemGridIndex, -1);

//				System.out.println(
//						(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//								+ ("\titemGridIndex:" + itemGridIndex)
//								+ ("\tabsposx:" + absposx)
//								+ ("\tabsposy:" + absposy)
//								+ ("\tmaxcols:" + maxcols)
//								+ ("\tfactor:" + factor)
////								+ ("\t:" + )
//
//				);
				// TODO remove SYSTEM.OUT.PRINTLN

				if (storedIndexInGridSlot == -1) {

					// no item at that grid position. The grid slot is free so let's
					// store this item "i" (we identify every item by its InternalItem index)

					_gridMap.put(itemGridIndex, markerIndex);
					//Log.v(TAG, "UNclustered item at " + itemGridIndex);

				} else {

					// at that grid position there's already a marker index
					// mark this item as clustered out, so it will be skipped in the update() call

					projectedMarker.isClusteredOut = true;

					// and increment the count on its "parent" that will from now on act as a cluster
					tmp[storedIndexInGridSlot].clusterSize++;

					//Log.v(TAG, "Clustered item at " + itemGridIndex + ", \'parent\' size " + (tmp[storedIndexInGridSlot].clusterSize));
				}
			}
		}

//		System.out.println();
//		// TODO remove SYSTEM.OUT.PRINTLN

		/* All ready for update. */
		synchronized (this) {

			_isForceUpdateMarkers = true;
			_allProjectedMarker = tmp;
		}
	}

	/**
	 * @param isEnabled
	 *            When <code>true</code> then all items are clustered, otherwise nothing is
	 *            clustered.
	 * @param clusterGridSize
	 */
	public void setClusteringEnabled(final boolean isEnabled, final int clusterGridSize) {

		// check if modified
		if (isEnabled == _isClustering && clusterGridSize == _clusterGridSizeDP) {
			return;
		}

		_isClustering = isEnabled;
		_clusterGridSizeDP = clusterGridSize;

		// post repopulation to the main thread
		_markerLayer.map().post(new Runnable() {
			@Override
			public void run() {
				repopulateCluster(_allProjectedMarker.length, _clusterScale);
			}
		});
	}

	/**
	 * Configures the cluster icon style. This is called by the constructor and cannot be made
	 * public because we pre-cache the icons at construction time so the renderer does not have to
	 * create them while rendering
	 *
	 * @param backgroundColor
	 *            Background color
	 * @param foregroundColor
	 *            text & border color
	 */
	private void setClusterStyle(final int foregroundColor, final int backgroundColor) {

		_clusterBackgroundColor = backgroundColor;
		_clusterForegroundColor = foregroundColor;
	}

	public synchronized void setClusterSymbolConfig(final int clusterSymbolSize,
													final int clusterForegroundColor,
													final int clusterBackgroundColor,
													final boolean isBillboard) {

		_clusterSymbolSizeDP = clusterSymbolSize;
		_isBillboard = isBillboard;

		// remove cached bitmaps
		Arrays.fill(_clusterBitmaps, null);

		_clusterForegroundColor = clusterForegroundColor;
		_clusterBackgroundColor = clusterBackgroundColor;

		_isForceUpdateMarkers = true;
	}

	public void setDefaultMarker(final MarkerSymbol defaultMarker) {

		defaultMarkerSymbol = defaultMarker;
		_isForceUpdateMarkers = true;
	}

	public void update() {
		_isForceUpdateMarkers = true;
	}

	@Override
	public synchronized void update(final GLViewport viewport) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tupdate()"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final MapPosition mapPosition = viewport.pos;
		final double mapScale = mapPosition.scale;
		final float mapRotation = mapPosition.bearing;

		final double clusterScale = Tile.SIZE * mapScale;

		/*
		 * Clustering check: If clustering is enabled and there's been a significant scale change
		 * trigger repopulation and return. After repopulation, this will be called again
		 */

		// (int) log of scale gives us adequate steps to trigger clustering
		final int scaleSaved = FastMath.log2((int) clusterScale);

		if (scaleSaved != _scaleSaved) {

			_scaleSaved = scaleSaved;
			_clusterScale = clusterScale;

			// post repopulation to the main thread
			_markerLayer.map().post(new Runnable() {
				@Override
				public void run() {
					repopulateCluster(_allProjectedMarker.length, clusterScale);
				}
			});

			// wait and see
			return;
		}

		if (!viewport.changed() && !_isForceUpdateMarkers) {
			return;
		}

		_isForceUpdateMarkers = false;

		final double mx = mapPosition.x;
		final double my = mapPosition.y;

		//int changesInvisible = 0;
		//int changedVisible = 0;
		int numVisible = 0;

		_markerLayer.map().viewport().getMapExtents(_tmpBox, _extents);

		final long flip = (long) (Tile.SIZE * mapScale) >> 1;

		if (_allProjectedMarker == null) {

			if (buckets.get() != null) {
				buckets.clear();
				compile();
			}

			return;
		}

		final double angle = Math.toRadians(mapPosition.bearing);
		final float cos = (float) Math.cos(angle);
		final float sin = (float) Math.sin(angle);

		/* check visibility */
		for (final ProjectedMarker projectedMarker : _allProjectedMarker) {

			projectedMarker.isModified = false;

			projectedMarker.x = (float) ((projectedMarker.px - mx) * clusterScale);
			projectedMarker.y = (float) ((projectedMarker.py - my) * clusterScale);

			if (projectedMarker.x > flip) {
				projectedMarker.x -= (flip << 1);
			} else if (projectedMarker.x < -flip) {
				projectedMarker.x += (flip << 1);
			}

			if (projectedMarker.isClusteredOut || !GeometryUtils.pointInPoly(
					projectedMarker.x,
					projectedMarker.y,
					_tmpBox,
					8,
					0)) {

				// either properly invisible, or clustered out. Items marked as clustered out mean there's another item
				// on the same-ish position that will be promoted to cluster marker, so this particular item is considered
				// invisible

				if (projectedMarker.isVisible && (!projectedMarker.isClusteredOut)) {

					// it was previously visible, but now it won't
					projectedMarker.isModified = true;

					// changes to invible
					//changesInvisible++;
				}

				continue;
			}

			// item IS definitely visible
			projectedMarker.dy = sin * projectedMarker.x + cos * projectedMarker.y;

			if (!projectedMarker.isVisible) {
				projectedMarker.isVisible = true;
				//changedVisible++;
			}

			numVisible++;
		}

		//log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

		/*
		 * only update when zoomlevel changed, new items are visible or more than 10 of the current
		 * items became invisible
		 */
		//if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
		//	return;
		buckets.clear();

		if (numVisible == 0) {
			compile();
			return;
		}

		/* keep position for current state */
		mMapPosition.copy(mapPosition);
		mMapPosition.bearing = -mMapPosition.bearing;

		// why do we sort ? z-index?
		sort(_allProjectedMarker, 0, _allProjectedMarker.length);

		//log.debug(Arrays.toString(mItems));

		for (final ProjectedMarker projectedMarker : _allProjectedMarker) {

			// skip invisible AND clustered-out
			if (!projectedMarker.isVisible || projectedMarker.isClusteredOut) {
				continue;
			}

			if (projectedMarker.isModified) {

				projectedMarker.isVisible = false;

				continue;
			}

			final SymbolItem markerSymbol = SymbolItem.pool.get();

			if (projectedMarker.clusterSize > 0) {

				// this item will act as a cluster, just use a proper bitmap
				// depending on cluster size, instead of its marker

				final Bitmap bitmap = getClusterBitmap(projectedMarker.clusterSize + 1);

				markerSymbol.set(projectedMarker.x, projectedMarker.y, bitmap, true);
				markerSymbol.offset = new PointF(0.5f, 0.5f);
				markerSymbol.billboard = _isBillboard;

				if (!_isBillboard) {
// CLUSTERING IS VERY BUGGY WHEN ROTATION IS SET
//					markerSymbol.rotation = -mapRotation;
				}

			} else {

				// normal item, use its marker

				MarkerSymbol symbol = projectedMarker.mapMarker.markerSymbol;

				if (symbol == null) {
					symbol = defaultMarkerSymbol;
				}

				markerSymbol.set(projectedMarker.x, projectedMarker.y, symbol.getBitmap(), true);
				markerSymbol.offset = symbol.getHotspot();
				markerSymbol.billboard = symbol.isBillboard();
			}

			_symbolBucket.pushSymbol(markerSymbol);
		}

		buckets.set(_symbolBucket);
		buckets.prepare();

		compile();
	}

}
