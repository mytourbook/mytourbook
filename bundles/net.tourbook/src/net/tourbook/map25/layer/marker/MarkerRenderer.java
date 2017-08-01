/*
 * Original: org.oscim.layers.marker.MarkerRenderer
 */
package net.tourbook.map25.layer.marker;

import java.util.Comparator;

import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

public class MarkerRenderer extends BucketRenderer {

	static TimSort<ProjectedMarker>				ZSORT	= new TimSort<ProjectedMarker>();

	final static Comparator<ProjectedMarker>	zComparator;

	static {

		zComparator = new Comparator<ProjectedMarker>() {

			@Override
			public int compare(	final ProjectedMarker a,
								final ProjectedMarker b) {
				if (a.visible && b.visible) {
					if (a.dy > b.dy) {
						return -1;
					}
					if (a.dy < b.dy) {
						return 1;
					}
				} else if (a.visible) {
					return -1;
				} else if (b.visible) {
					return 1;
				}

				return 0;
			}
		};
	}

	protected MarkerSymbol			defaultMarkerSymbol;
	protected final SymbolBucket	symbolBucket;

	protected final float[]			mBox		= new float[8];

	protected final MarkerLayer		mMarkerLayer;

	protected final Point			mMapPoint	= new Point();

	/**
	 * increase view to show items that are partially visible
	 */
	protected int					mExtents	= 100;

	/**
	 * flag to force update of markers
	 */
	protected boolean				isForceUpdateMarkers;

	protected ProjectedMarker[]		allProjectedMarker;

	public MarkerRenderer(final MarkerLayer markerLayer, final MarkerSymbol defaultSymbol) {

		mMarkerLayer = markerLayer;
		defaultMarkerSymbol = defaultSymbol;

		symbolBucket = new SymbolBucket();
	}

	public static void sort(final ProjectedMarker[] a, final int lo, final int hi) {

		final int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}

		ZSORT.doSort(a, zComparator, lo, hi);
	}

	protected void populate(final int size) {

		final ProjectedMarker[] tmp = new ProjectedMarker[size];

		for (int markerIndex = 0; markerIndex < size; markerIndex++) {

			final ProjectedMarker projectedMarker = new ProjectedMarker();
			tmp[markerIndex] = projectedMarker;
			projectedMarker.item = mMarkerLayer.createItem(markerIndex);

			/* pre-project points */
			MercatorProjection.project(projectedMarker.item.getGeoPoint(), mMapPoint);
			projectedMarker.px = mMapPoint.x;
			projectedMarker.py = mMapPoint.y;
		}

		synchronized (this) {
			isForceUpdateMarkers = true;
			allProjectedMarker = tmp;
		}
	}

	public void update() {
		isForceUpdateMarkers = true;
	}

	@Override
	public synchronized void update(final GLViewport v) {

		if (!v.changed() && !isForceUpdateMarkers) {
			return;
		}

		isForceUpdateMarkers = false;

		final double mx = v.pos.x;
		final double my = v.pos.y;
		final double scale = Tile.SIZE * v.pos.scale;

		//int changesInvisible = 0;
		//int changedVisible = 0;
		int numVisible = 0;

		mMarkerLayer.map().viewport().getMapExtents(mBox, mExtents);

		final long flip = (long) (Tile.SIZE * v.pos.scale) >> 1;

		if (allProjectedMarker == null) {

			if (buckets.get() != null) {
				buckets.clear();
				compile();
			}

			return;
		}

		final double angle = Math.toRadians(v.pos.bearing);
		final float cos = (float) Math.cos(angle);
		final float sin = (float) Math.sin(angle);

		/* check visibility */
		for (final ProjectedMarker projectedMarker : allProjectedMarker) {

			projectedMarker.changes = false;
			projectedMarker.x = (float) ((projectedMarker.px - mx) * scale);
			projectedMarker.y = (float) ((projectedMarker.py - my) * scale);

			if (projectedMarker.x > flip) {
				projectedMarker.x -= (flip << 1);
			} else if (projectedMarker.x < -flip) {
				projectedMarker.x += (flip << 1);
			}

			if (!GeometryUtils.pointInPoly(projectedMarker.x, projectedMarker.y, mBox, 8, 0)) {

				if (projectedMarker.visible) {
					projectedMarker.changes = true;
					//changesInvisible++;
				}

				continue;
			}

			projectedMarker.dy = sin * projectedMarker.x + cos * projectedMarker.y;

			if (!projectedMarker.visible) {
				projectedMarker.visible = true;
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
		//    return;
		buckets.clear();

		if (numVisible == 0) {
			compile();
			return;
		}

		/* keep position for current state */
		mMapPosition.copy(v.pos);
		mMapPosition.bearing = -mMapPosition.bearing;

		sort(allProjectedMarker, 0, allProjectedMarker.length);

		//log.debug(Arrays.toString(mItems));
		for (final ProjectedMarker it : allProjectedMarker) {

			if (!it.visible) {
				continue;
			}

			if (it.changes) {
				it.visible = false;
				continue;
			}

			MarkerSymbol marker = it.item.getMarkerSymbol();
			if (marker == null) {
				marker = defaultMarkerSymbol;
			}

			final SymbolItem s = SymbolItem.pool.get();
			if (marker.isBitmap()) {
				s.set(it.x, it.y, marker.getBitmap(), marker.rotation, marker.isBillboard());
			} else {
				s.set(it.x, it.y, marker.getTextureRegion(), marker.rotation, marker.isBillboard());
			}

			s.offset = marker.getHotspot();
			symbolBucket.pushSymbol(s);
		}

		buckets.set(symbolBucket);
		buckets.prepare();

		compile();
	}
}
