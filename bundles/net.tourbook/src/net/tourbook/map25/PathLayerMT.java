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

import java.time.LocalTime;

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
	protected GeoPoint[]	_geoPoints;
	protected TIntArrayList	_tourStarts;

	protected boolean		_isUpdatePoints;

	/**
	 * Line style
	 */
	LineStyle				_lineStyle;

	final Worker			_simpleWorker;

	GeometryBuffer			_geoBuffer;

	private boolean			_isUpdateLayer;

	/*
	 * everything below runs on GL- and Worker-Thread
	 */

	final static class PathLayerTask {

		RenderBuckets	__renderBuckets	= new RenderBuckets();
		MapPosition		__mapPos		= new MapPosition();
	}

	final class RenderPathLayer extends BucketRenderer {

		private int	__curX	= -1;
		private int	__curY	= -1;
		private int	__curZ	= -1;

		@Override
		public synchronized void update(final GLViewport v) {

			final int tz = 1 << v.pos.zoomLevel;
			final int tx = (int) (v.pos.x * tz);
			final int ty = (int) (v.pos.y * tz);

			/* update layers when map moved by at least one tile */
			if (tx != __curX || ty != __curY || tz != __curZ || _isUpdateLayer) {

				_isUpdateLayer = false;

				_simpleWorker.submit(007);

				__curX = tx;
				__curY = ty;
				__curZ = tz;
			}

			final PathLayerTask workerTask = _simpleWorker.poll();
			if (workerTask == null) {
				return;
			}

			/* keep position to render relative to current state */
			mMapPosition.copy(workerTask.__mapPos);

			/* compile new layers */
			buckets.set(workerTask.__renderBuckets.get());
			compile();
		}
	}

	final class Worker extends SimpleWorker<PathLayerTask> {

		private static final int	MIN_DIST				= 3;

		// limit coords
		private final int			__max					= 2048;

		// pre-projected points
		private double[]			__preProjectedPoints	= new double[2];

		// projected points
		private float[]				__projectedPoints;

		private final LineClipper	__lineClipper;
		private int					__numPoints;

		public Worker(final Map map) {

			super(map, 55, new PathLayerTask(), new PathLayerTask());

			__lineClipper = new LineClipper(-__max, -__max, __max, __max);
			__projectedPoints = new float[0];
		}

		private int addPoint(final float[] points, int i, final int x, final int y) {

			points[i++] = x;
			points[i++] = y;

			return i;
		}

		@Override
		public void cleanup(final PathLayerTask task) {
			task.__renderBuckets.clear();
		}

		@Override
		public boolean doWork(final PathLayerTask task) {

			int size = __numPoints;

			if (_isUpdatePoints) {

				synchronized (_geoPoints) {

					_isUpdatePoints = false;
					__numPoints = size = _geoPoints.length;

					double[] points = __preProjectedPoints;

					if (size * 2 >= points.length) {
						points = __preProjectedPoints = new double[size * 2];
						__projectedPoints = new float[size * 2];
					}

					for (int pointIndex = 0; pointIndex < size; pointIndex++) {
						MercatorProjection.project(_geoPoints[pointIndex], points, pointIndex);
					}
				}

			} else if (_geoBuffer != null) {

				final GeometryBuffer geoBuffer = _geoBuffer;
				_geoBuffer = null;
				size = geoBuffer.index[0];

				double[] points = __preProjectedPoints;

				if (size > points.length) {
					points = __preProjectedPoints = new double[size * 2];
					__projectedPoints = new float[size * 2];
				}

				for (int pointIndex = 0; pointIndex < size; pointIndex += 2) {

					MercatorProjection.project(
							geoBuffer.points[pointIndex + 1],
							geoBuffer.points[pointIndex],
							points,
							pointIndex >> 1);
				}

				__numPoints = size = size >> 1;
			}

			if (size == 0) {

				if (task.__renderBuckets.get() != null) {
					task.__renderBuckets.clear();
					mMap.render();
				}

				return true;
			}

			LineBucket ll;

			if (_lineStyle.stipple == 0 && _lineStyle.texture == null) {
				ll = task.__renderBuckets.getLineBucket(0);
			} else {
				ll = task.__renderBuckets.getLineTexBucket(0);
			}

			ll.line = _lineStyle;

			//ll.scale = ll.line.width;

			mMap.getMapPosition(task.__mapPos);

			final int zoomlevel = task.__mapPos.zoomLevel;
			task.__mapPos.scale = 1 << zoomlevel;

			final double mx = task.__mapPos.x;
			final double my = task.__mapPos.y;
			final double scale = Tile.SIZE * task.__mapPos.scale;

			// flip around dateline
			int flip = 0;
			final int maxx = Tile.SIZE << (zoomlevel - 1);

			int x = (int) ((__preProjectedPoints[0] - mx) * scale);
			int y = (int) ((__preProjectedPoints[1] - my) * scale);

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

			__lineClipper.clipStart(x, y);

			final float[] projected = __projectedPoints;
			int i = addPoint(projected, 0, x, y);

			float prevX = x;
			float prevY = y;

			float[] segment = null;

			for (int j = 2; j < size * 2; j += 2) {

				x = (int) ((__preProjectedPoints[j + 0] - mx) * scale);
				y = (int) ((__preProjectedPoints[j + 1] - my) * scale);

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

					__lineClipper.clipStart(x, y);
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

					__lineClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				final int clip = __lineClipper.clipNext(x, y);
				if (clip < 1) {
					if (i > 2) {
						ll.addLine(projected, i, false);
					}

					if (clip < 0) {
						/* add line segment */
						segment = __lineClipper.getLine(segment, 0);
						ll.addLine(segment, 4, false);
						// the prev point is the real point not the clipped point
						//prevX = mClipper.outX2;
						//prevY = mClipper.outY2;
						prevX = x;
						prevY = y;
					}
					i = 0;
					// if the end point is inside, add it
					if (__lineClipper.getPrevOutcode() == 0) {
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

		_lineStyle = style;

		_geoPoints = new GeoPoint[] {};
		_tourStarts = new TIntArrayList();

		mRenderer = new RenderPathLayer();
		_simpleWorker = new Worker(map);
	}

	public void setPoints(final GeoPoint[] geoPoints, final TIntArrayList tourStarts) {

		System.out.println(
				(LocalTime.now().toString() + " [" + getClass().getSimpleName() + "] ") + ("\tupdatePoints()"));
		// TODO remove SYSTEM.OUT.PRINTLN

		synchronized (_geoPoints) {

			_geoPoints = geoPoints;

			_tourStarts.clear();
			_tourStarts.addAll(tourStarts);
		}

		updatePoints();
	}

	public void setStyle(final LineStyle style) {
		_lineStyle = style;
	}

	private void updatePoints() {

//		synchronized (_simpleWorker) {
//			if (_simpleWorker.isRunning()) {
//				_simpleWorker.cancel(true);
//			}
//		}

//		_simpleWorker.submit(66);

		_isUpdatePoints = true;
		_isUpdateLayer = true;
	}
}
