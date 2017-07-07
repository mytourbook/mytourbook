/*
 * Copyright 2012 osmdroid authors: Viesturs Zarins, Martin Pearman
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 devemux86
 * Copyright 2016 Bezzu
 * Copyright 2016 Pedinel
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.geom.LineClipper;

/**
 * This class draws a path line in given color or texture.
 */
public class PathLayerMT extends Layer {

	/**
	 * Stores points, converted to the map projection.
	 */
	protected final ArrayList<GeoPoint>	mPoints;
	protected TIntArrayList				_tourStarts;

	protected boolean					mUpdatePoints;

	/**
	 * Line style
	 */
	LineStyle							mLineStyle;

	final Worker						mWorker;

	GeometryBuffer						mGeom;

	/***
	 * everything below runs on GL- and Worker-Thread
	 ***/
	final class RenderPath extends BucketRenderer {

		private int	mCurX	= -1;
		private int	mCurY	= -1;
		private int	mCurZ	= -1;

		@Override
		public synchronized void update(final GLViewport v) {

			final int tz = 1 << v.pos.zoomLevel;
			final int tx = (int) (v.pos.x * tz);
			final int ty = (int) (v.pos.y * tz);

			/* update layers when map moved by at least one tile */
			if ((tx != mCurX || ty != mCurY || tz != mCurZ)) {

				mWorker.submit(100);

				mCurX = tx;
				mCurY = ty;
				mCurZ = tz;
			}

			final Task t = mWorker.poll();
			if (t == null) {
				return;
			}

			/* keep position to render relative to current state */
			mMapPosition.copy(t.pos);

			/* compile new layers */
			buckets.set(t.bucket.get());
			compile();
		}
	}

	final static class Task {
		RenderBuckets	bucket	= new RenderBuckets();
		MapPosition		pos		= new MapPosition();
	}

	final class Worker extends SimpleWorker<Task> {

		private static final int	MIN_DIST		= 3;

		// limit coords
		private final int			max				= 2048;

		// pre-projected points
		private double[]			mPreprojected	= new double[2];

		// projected points
		private float[]				mPPoints;

		private final LineClipper	mClipper;
		private int					mNumPoints;

		public Worker(final Map map) {
			super(map, 0, new Task(), new Task());
			mClipper = new LineClipper(-max, -max, max, max);
			mPPoints = new float[0];
		}

		private int addPoint(final float[] points, int i, final int x, final int y) {
			points[i++] = x;
			points[i++] = y;
			return i;
		}

		@Override
		public void cleanup(final Task task) {
			task.bucket.clear();
		}

		@Override
		public boolean doWork(final Task task) {

			int size = mNumPoints;

			if (mUpdatePoints) {
				synchronized (mPoints) {
					mUpdatePoints = false;
					mNumPoints = size = mPoints.size();

					final ArrayList<GeoPoint> geopoints = mPoints;
					double[] points = mPreprojected;

					if (size * 2 >= points.length) {
						points = mPreprojected = new double[size * 2];
						mPPoints = new float[size * 2];
					}

					for (int i = 0; i < size; i++) {
						MercatorProjection.project(geopoints.get(i), points, i);
					}
				}

			} else if (mGeom != null) {
				final GeometryBuffer geom = mGeom;
				mGeom = null;
				size = geom.index[0];

				double[] points = mPreprojected;

				if (size > points.length) {
					points = mPreprojected = new double[size * 2];
					mPPoints = new float[size * 2];
				}

				for (int i = 0; i < size; i += 2) {
					MercatorProjection.project(
							geom.points[i + 1],
							geom.points[i],
							points,
							i >> 1);
				}
				mNumPoints = size = size >> 1;

			}
			if (size == 0) {
				if (task.bucket.get() != null) {
					task.bucket.clear();
					mMap.render();
				}
				return true;
			}

			LineBucket ll;

			if (mLineStyle.stipple == 0 && mLineStyle.texture == null) {
				ll = task.bucket.getLineBucket(0);
			} else {
				ll = task.bucket.getLineTexBucket(0);
			}

			ll.line = mLineStyle;

			//ll.scale = ll.line.width;

			mMap.getMapPosition(task.pos);

			final int zoomlevel = task.pos.zoomLevel;
			task.pos.scale = 1 << zoomlevel;

			final double mx = task.pos.x;
			final double my = task.pos.y;
			final double scale = Tile.SIZE * task.pos.scale;

			// flip around dateline
			int flip = 0;
			final int maxx = Tile.SIZE << (zoomlevel - 1);

			int x = (int) ((mPreprojected[0] - mx) * scale);
			int y = (int) ((mPreprojected[1] - my) * scale);

			if (x > maxx) {
				x -= (maxx * 2);
				flip = -1;
			} else if (x < -maxx) {
				x += (maxx * 2);
				flip = 1;
			}

			/*
			 * Setup tour clipper
			 */
			int tourIndex = 0;
			int nextTourStartIndex = getNextTourStartIndex(tourIndex);

			mClipper.clipStart(x, y);

			final float[] projected = mPPoints;
			int i = addPoint(projected, 0, x, y);

			float prevX = x;
			float prevY = y;

			float[] segment = null;

			for (int j = 2; j < size * 2; j += 2) {

				x = (int) ((mPreprojected[j + 0] - mx) * scale);
				y = (int) ((mPreprojected[j + 1] - my) * scale);

				int flipDirection = 0;
				if (x > maxx) {
					x -= maxx * 2;
					flipDirection = -1;
				} else if (x < -maxx) {
					x += maxx * 2;
					flipDirection = 1;
				}

				if (flip != flipDirection) {
					flip = flipDirection;
					if (i > 2) {
						ll.addLine(projected, i, false);
					}

					mClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				if (j >= nextTourStartIndex) {

					// setup next tour
					nextTourStartIndex = getNextTourStartIndex(++tourIndex);

					// start a new line (copied from flip code)
					if (i > 2) {
						ll.addLine(projected, i, false);
					}

					mClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				final int clip = mClipper.clipNext(x, y);
				if (clip < 1) {
					if (i > 2) {
						ll.addLine(projected, i, false);
					}

					if (clip < 0) {
						/* add line segment */
						segment = mClipper.getLine(segment, 0);
						ll.addLine(segment, 4, false);
						// the prev point is the real point not the clipped point
						//prevX = mClipper.outX2;
						//prevY = mClipper.outY2;
						prevX = x;
						prevY = y;
					}
					i = 0;
					// if the end point is inside, add it
					if (mClipper.getPrevOutcode() == 0) {
						projected[i++] = prevX;
						projected[i++] = prevY;
					}
					continue;
				}

				final float dx = x - prevX;
				final float dy = y - prevY;
				if ((i == 0) || FastMath.absMaxCmp(dx, dy, MIN_DIST)) {
					projected[i++] = prevX = x;
					projected[i++] = prevY = y;
				}
			}
			if (i > 2) {
				ll.addLine(projected, i, false);
			}

			// trigger redraw to let renderer fetch the result.
			mMap.render();
//			mMap.updateMap(true);

			return true;
		}

		private int getNextTourStartIndex(final int tourIndex) {

			if (_tourStarts.size() > tourIndex + 1) {
				return _tourStarts.get(tourIndex + 1) * 2;
			} else {
				return Integer.MAX_VALUE;
			}
		}
	}

	public PathLayerMT(final Map map, final int lineColor) {
		this(map, lineColor, 2);
	}

	public PathLayerMT(final Map map, final int lineColor, final float lineWidth) {
		this(map, new LineStyle(lineColor, lineWidth, Cap.BUTT));
	}

	public PathLayerMT(final Map map, final LineStyle style) {

		super(map);

		mLineStyle = style;

		mPoints = new ArrayList<>();
		_tourStarts = new TIntArrayList();

		mRenderer = new RenderPath();
		mWorker = new Worker(map);
	}

	/**
	 * Draw a great circle. Calculate a point for every 100km along the path.
	 *
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 */
	public void addGreatCircle(final GeoPoint startPoint, final GeoPoint endPoint) {
		synchronized (mPoints) {

			/* get the great circle path length in meters */
			final double length = startPoint.sphericalDistance(endPoint);

			/* add one point for every 100kms of the great circle path */
			final int numberOfPoints = (int) (length / 100000);

			addGreatCircle(startPoint, endPoint, numberOfPoints);
		}
	}

	/**
	 * Draw a great circle.
	 *
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 * @param numberOfPoints
	 *            number of points to calculate along the path
	 */
	public void addGreatCircle(	final GeoPoint startPoint,
								final GeoPoint endPoint,
								final int numberOfPoints) {
		// adapted from page
		// http://compastic.blogspot.co.uk/2011/07/how-to-draw-great-circle-on-map-in.html
		// which was adapted from page http://maps.forum.nu/gm_flight_path.html

		// convert to radians
		final double lat1 = startPoint.getLatitude() * Math.PI / 180;
		final double lon1 = startPoint.getLongitude() * Math.PI / 180;
		final double lat2 = endPoint.getLatitude() * Math.PI / 180;
		final double lon2 = endPoint.getLongitude() * Math.PI / 180;

		final double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2)
				+ Math.cos(lat1) * Math.cos(lat2)
						* Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
		double bearing = Math.atan2(
				Math.sin(lon1 - lon2) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
						* Math.cos(lat2)
						* Math.cos(lon1 - lon2))
				/ -(Math.PI / 180);
		bearing = bearing < 0 ? 360 + bearing : bearing;

		for (int i = 0, j = numberOfPoints + 1; i < j; i++) {
			final double f = 1.0 / numberOfPoints * i;
			final double A = Math.sin((1 - f) * d) / Math.sin(d);
			final double B = Math.sin(f * d) / Math.sin(d);
			final double x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2)
					* Math.cos(lon2);
			final double y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2)
					* Math.sin(lon2);
			final double z = A * Math.sin(lat1) + B * Math.sin(lat2);

			final double latN = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
			final double lonN = Math.atan2(y, x);
			addPoint((int) (latN / (Math.PI / 180) * 1E6), (int) (lonN / (Math.PI / 180) * 1E6));
		}
	}

	public void addPoint(final GeoPoint pt) {
		synchronized (mPoints) {
			mPoints.add(pt);
		}
		updatePoints();
	}

	public void addPoint(final int latitudeE6, final int longitudeE6) {
		synchronized (mPoints) {
			mPoints.add(new GeoPoint(latitudeE6, longitudeE6));
		}
		updatePoints();
	}

	public void addPoints(final Collection<? extends GeoPoint> pts) {
		synchronized (mPoints) {
			mPoints.addAll(pts);
		}
		updatePoints();
	}

	public void clearPath() {
		if (mPoints.isEmpty()) {
			return;
		}

		synchronized (mPoints) {
			mPoints.clear();
		}
		updatePoints();
	}

	public List<GeoPoint> getPoints() {
		return mPoints;
	}

	/**
	 * FIXME To be removed
	 *
	 * @deprecated
	 */
	@Deprecated
	public void setGeom(final GeometryBuffer geom) {
		mGeom = geom;
		mWorker.submit(10);
	}

	public void setPoints(final Collection<? extends GeoPoint> pts, final TIntArrayList tourStarts) {

		synchronized (mPoints) {

			mPoints.clear();
			mPoints.addAll(pts);

			_tourStarts.clear();
			_tourStarts.addAll(tourStarts);
		}

		updatePoints();
	}

	public void setStyle(final LineStyle style) {
		mLineStyle = style;
	}

	private void updatePoints() {

		mWorker.submit(1);
		mUpdatePoints = true;
	}
}
