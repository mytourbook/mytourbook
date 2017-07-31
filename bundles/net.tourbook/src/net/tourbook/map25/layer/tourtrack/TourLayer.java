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
package net.tourbook.map25.layer.tourtrack;

import gnu.trove.list.array.TIntArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.map25.Map25ConfigManager;

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
 * <p>
 * Example to handle track hover/selection
 * org.oscim.layers.marker.ItemizedLayer.activateSelectedItems(MotionEvent, ActiveItem)
 */
public class TourLayer extends Layer {

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

			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t\t\tupdate()"));
			// TODO remove SYSTEM.OUT.PRINTLN

			if (!isEnabled()) {
				return;
			}

			final int tz = 1 << v.pos.zoomLevel;
			final int tx = (int) (v.pos.x * tz);
			final int ty = (int) (v.pos.y * tz);

			/* update layers when map moved by at least one tile */
			if (tx != __curX || ty != __curY || tz != __curZ || _isUpdateLayer) {

				/*
				 * It took me many days to find this solution that a newly selected tour is
				 * displayed after the map position was moved/tilt/rotated. It works but I don't
				 * know exacly why.
				 */
				if (_isUpdateLayer) {
					_simpleWorker.cancel(true);
				}

				_isUpdateLayer = false;

				_simpleWorker.submit(0);

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

			final long start = System.nanoTime();

			int numPoints = __numPoints;

			if (_isUpdatePoints) {

				synchronized (_geoPoints) {

					_isUpdatePoints = false;
					__numPoints = numPoints = _geoPoints.length;

					double[] points = __preProjectedPoints;

					if (numPoints * 2 >= points.length) {
						points = __preProjectedPoints = new double[numPoints * 2];
						__projectedPoints = new float[numPoints * 2];
					}

					for (int pointIndex = 0; pointIndex < numPoints; pointIndex++) {
						MercatorProjection.project(_geoPoints[pointIndex], points, pointIndex);
					}
				}

			} else if (_geoBuffer != null) {

				final GeometryBuffer geoBuffer = _geoBuffer;
				_geoBuffer = null;
				numPoints = geoBuffer.index[0];

				double[] points = __preProjectedPoints;

				if (numPoints > points.length) {
					points = __preProjectedPoints = new double[numPoints * 2];
					__projectedPoints = new float[numPoints * 2];
				}

				for (int pointIndex = 0; pointIndex < numPoints; pointIndex += 2) {

					MercatorProjection.project(
							geoBuffer.points[pointIndex + 1],
							geoBuffer.points[pointIndex],
							points,
							pointIndex >> 1);
				}

				__numPoints = numPoints = numPoints >> 1;
			}

			if (numPoints == 0) {

				if (task.__renderBuckets.get() != null) {
					task.__renderBuckets.clear();
					mMap.render();
				}

				return true;
			}

			doWork_Rendering(task, numPoints);

			// trigger redraw to let renderer fetch the result.
			mMap.render();

//			System.out.println(
//					(UI.timeStampNano() + " " + this.getClass().getName() + " \t")
//							+ (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//			// TODO remove SYSTEM.OUT.PRINTLN

			return true;
		}

		private void doWork_Rendering(final PathLayerTask task, final int numPoints) {

			LineBucket lineBucket;

			if (_lineStyle.stipple == 0 && _lineStyle.texture == null) {
				lineBucket = task.__renderBuckets.getLineBucket(0);
			} else {
				lineBucket = task.__renderBuckets.getLineTexBucket(0);
			}

			lineBucket.line = _lineStyle;

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

			for (int pointIndex = 2; pointIndex < numPoints * 2; pointIndex += 2) {

				x = (int) ((__preProjectedPoints[pointIndex + 0] - mx) * scale);
				y = (int) ((__preProjectedPoints[pointIndex + 1] - my) * scale);

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
						lineBucket.addLine(projected, i, false);
					}

					__lineClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				if (pointIndex >= nextTourStartIndex) {

					// setup next tour
					nextTourStartIndex = getNextTourStartIndex(++tourIndex);

					// start a new line (copied from flip code)
					if (i > 2) {
						lineBucket.addLine(projected, i, false);
					}

					__lineClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				final int clip = __lineClipper.clipNext(x, y);
				if (clip < 1) {
					if (i > 2) {
						lineBucket.addLine(projected, i, false);
					}

					if (clip < 0) {
						/* add line segment */
						segment = __lineClipper.getLine(segment, 0);
						lineBucket.addLine(segment, 4, false);
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
				lineBucket.addLine(projected, i, false);
			}
		}

		private int getNextTourStartIndex(final int tourIndex) {

			if (_tourStarts.size() > tourIndex + 1) {
				return _tourStarts.get(tourIndex + 1) * 2;
			} else {
				return Integer.MAX_VALUE;
			}
		}
	}

	public TourLayer(final Map map) {

		super(map);

		_lineStyle = createLineStyle();

		_geoPoints = new GeoPoint[] {};
		_tourStarts = new TIntArrayList();

		mRenderer = new RenderPathLayer();
		_simpleWorker = new Worker(map);
	}

	private LineStyle createLineStyle() {

		final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		return LineStyle

				.builder()//

				.strokeWidth(trackConfig.outlineWidth)
				.color(ColorUtil.getARGB(trackConfig.outlineColor, 0xff))
				.cap(Cap.BUTT)

				// this is not yet working
				// .isOutline(true)

				.build();
	}

	public void onModifyConfig() {

		_lineStyle = createLineStyle();

		_simpleWorker.submit(0);

//		mMap.render();

//		if (trackConfig.isRecreateTracks()) {
//
//			// track data has changed
//
//			Map3Manager.getMap3View().showAllTours(false);
//
//		} else {
//
//			for (final Renderable renderable : getRenderables()) {
//
//				if (renderable instanceof ITrackPath) {
//					setPathAttributes((ITrackPath) renderable);
//				}
//			}
//
//			// ensure path modifications are redrawn
//			Map3Manager.getWWCanvas().redraw();
//		}
	}

	public void setPoints(final GeoPoint[] geoPoints, final TIntArrayList tourStarts) {

		synchronized (_geoPoints) {

			_tourStarts.clear();
			_tourStarts.addAll(tourStarts);

			_geoPoints = geoPoints;
		}

		updatePoints();
	}

	private void updatePoints() {

		_simpleWorker.cancel(true);

		_isUpdatePoints = true;
		_isUpdateLayer = true;
	}
}
