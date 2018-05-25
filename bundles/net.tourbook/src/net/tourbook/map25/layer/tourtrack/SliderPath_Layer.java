/*
 * Original: org.oscim.layers.PathLayer
 */
package net.tourbook.map25.layer.tourtrack;

import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map25.Map25ConfigManager;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
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
public class SliderPath_Layer extends Layer {

	private AtomicInteger	_eventCounter	= new AtomicInteger();
	private int				_geoPointCounter;

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

	private boolean			_isUpdateLayer;
	private int				_firstSliderValueIndex;
	private int				_lastSliderValueIndex;

	private final class TourRenderer extends BucketRenderer {

		private int	__curX	= -1;
		private int	__curY	= -1;
		private int	__curZ	= -1;

		@Override
		public synchronized void update(final GLViewport v) {

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

			final TourRenderTask workerTask = _simpleWorker.poll();
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

	private final static class TourRenderTask {

		RenderBuckets	__renderBuckets	= new RenderBuckets();
		MapPosition		__mapPos		= new MapPosition();
	}

	final class Worker extends SimpleWorker<TourRenderTask> {

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

			super(map, 55, new TourRenderTask(), new TourRenderTask());

			__lineClipper = new LineClipper(-__max, -__max, __max, __max);
			__projectedPoints = new float[0];
		}

		private int addPoint(final float[] points, int i, final int x, final int y) {

			points[i++] = x;
			points[i++] = y;

			return i;
		}

		@Override
		public void cleanup(final TourRenderTask task) {
			task.__renderBuckets.clear();
		}

		@Override
		public boolean doWork(final TourRenderTask task) {

			int numPoints = __numPoints;

			if (_isUpdatePoints) {

				synchronized (_geoPoints) {

					final int eventCounter = _eventCounter.get();

//					System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] doWork()")
//							+ ("\tcounter diff: " + (eventCounter - _geoPointCounter))
////							+ ("\t_geoPointCounter: " + _geoPointCounter)
//					);
//// TODO remove SYSTEM.OUT.PRINTLN

					if (eventCounter > _geoPointCounter) {
						return false;
					}

					_isUpdatePoints = false;

					final int firstSliderValueIndex = _firstSliderValueIndex;
					final int lastSliderValueIndex = _lastSliderValueIndex;

					__numPoints = numPoints = lastSliderValueIndex - firstSliderValueIndex;

					double[] points = __preProjectedPoints;

					if (numPoints * 2 >= points.length) {
						points = __preProjectedPoints = new double[numPoints * 2];
						__projectedPoints = new float[numPoints * 2];
					}

					for (int pointIndex = 0; pointIndex < numPoints; pointIndex++) {
						MercatorProjection.project(_geoPoints[firstSliderValueIndex + pointIndex], points, pointIndex);
					}
				}
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

			return true;
		}

		private void doWork_Rendering(final TourRenderTask task, final int numPoints) {

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
				if (clip != LineClipper.INSIDE) {

					if (i > 2) {
						lineBucket.addLine(projected, i, false);
					}

					if (clip == LineClipper.INTERSECTION) {

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
					if (__lineClipper.getPrevOutcode() == LineClipper.INSIDE) {
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

	public SliderPath_Layer(final Map map) {

		super(map);

		_lineStyle = createLineStyle();

		_geoPoints = new GeoPoint[] {};
		_tourStarts = new TIntArrayList();

		mRenderer = new TourRenderer();
		_simpleWorker = new Worker(map);
	}

	private LineStyle createLineStyle() {

		final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		return LineStyle

				.builder()//

				.strokeWidth(trackConfig.sliderPath_LineWidth)
				.color(ColorUtil.getARGB(trackConfig.sliderPath_Color, trackConfig.sliderPath_Opacity * 0xff / 100))
				.cap(Cap.BUTT)

				// this is not yet working
				// .isOutline(true)

				.build();
	}

	public void onModifyConfig() {

		final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		setEnabled(activeTourTrackConfig.isShowSliderPath);

		_lineStyle = createLineStyle();

		_simpleWorker.submit(0);
	}

	public void setPoints(	final GeoPoint[] geoPoints,
							final TIntArrayList tourStarts,
							final int leftSliderValueIndexRaw,
							final int rightSliderValueIndexRaw) {

		final int numPoints = geoPoints.length;

		// force array bounds
		final int leftSliderValueIndex = Math.min(Math.max(leftSliderValueIndexRaw, 0), numPoints - 1);
		final int rightSliderValueIndex = Math.min(Math.max(rightSliderValueIndexRaw, 0), numPoints - 1);

		int firstSliderValueIndex;
		int lastSliderValueIndex;

		if (leftSliderValueIndex > rightSliderValueIndex) {
			firstSliderValueIndex = rightSliderValueIndex;
			lastSliderValueIndex = leftSliderValueIndex;
		} else {
			firstSliderValueIndex = leftSliderValueIndex;
			lastSliderValueIndex = rightSliderValueIndex;
		}

		synchronized (_geoPoints) {

			_geoPointCounter = _eventCounter.incrementAndGet();

			_tourStarts.clear();
			_tourStarts.addAll(tourStarts);

			_geoPoints = geoPoints;

			_firstSliderValueIndex = firstSliderValueIndex;
			_lastSliderValueIndex = lastSliderValueIndex;
		}

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] setPoints()")
//				+ ("\tsimpleWorker.isRunning: " + _simpleWorker.isRunning())
////				+ ("\t: " + )
//		);
//// TODO remove SYSTEM.OUT.PRINTLN

		_simpleWorker.cancel(true);

		_isUpdatePoints = true;
		_isUpdateLayer = true;

	}
}
