/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 * Copyright (C) 2018, 2021 Thomas Theussing
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

/*
 * Original: org.oscim.layers.PathLayer
 */
package net.tourbook.map25.layer.tourtrack;

import gnu.trove.list.array.TIntArrayList;

import net.tourbook.common.color.ColorUtil;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.layer.marker.MarkerShape;
import net.tourbook.map25.layer.marker.MarkerToolkit;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
//import org.oscim.layers.vector.
import org.oscim.map.Map;
import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.TextureItem;
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

   private static final int RENDERING_DELAY = 0;
   /**
    * Stores points, converted to the map projection.
    */
   protected GeoPoint[]     _geoPoints;
   protected TIntArrayList  _tourStarts;

   protected boolean        _isUpdatePoints;

   /**
    * Line style
    */
   LineStyle                _lineStyle;

   final Worker             _simpleWorker;

   private boolean          _isUpdateLayer;

   private final class TourRenderer extends BucketRenderer {

      private int __curX = -1;
      private int __curY = -1;
      private int __curZ = -1;

      @Override
      public synchronized void update(final GLViewport v) {

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t\t\tupdate()"));
//			// TODO remove SYSTEM.OUT.PRINTLN

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

            _simpleWorker.submit(RENDERING_DELAY);

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

      RenderBuckets __renderBuckets = new RenderBuckets();
      MapPosition   __mapPos        = new MapPosition();
   }

   final class Worker extends SimpleWorker<TourRenderTask> {

      private static final int MIN_DIST = 3;

      // limit coords
      private final int __max = 2048;

      // pre-projected points
      private double[] __preProjectedPoints = new double[2];

      // projected points
      private float[]           __projectedPoints;

      private final LineClipper __lineClipper;
      private int               __numPoints;

      public Worker(final Map map) {

         super(map, 50, new TourRenderTask(), new TourRenderTask());

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

   public TourLayer(final Map map) {

      super(map);

      _lineStyle = createLineStyle();

      _geoPoints = new GeoPoint[] {};
      _tourStarts = new TIntArrayList();

      mRenderer = new TourRenderer();
      _simpleWorker = new Worker(map);
   }

   private LineStyle createLineStyle() {

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      
      int lineColor = ColorUtil.getARGB(trackConfig.outlineColor, trackConfig.outlineOpacity);

      if (trackConfig.isShowDirectionArrow) {

         MarkerToolkit _markertoolkit = new MarkerToolkit(MarkerShape.ARROW);

         Bitmap _bitmapArrow = _markertoolkit.drawTrackArrow(40, lineColor);

         TextureItem _tex = new TextureItem(_bitmapArrow);

         //width must not to tiny, otherwise no place that arrow can be painted
         final float faterOutlineWidth = Math.max(trackConfig.outlineWidth * 2, 5f);

         final LineStyle style = LineStyle.builder()
               .stippleColor(lineColor)
               .stipple(20)
               .strokeWidth(faterOutlineWidth)
               .strokeColor(lineColor)
               .fixed(true)
               .texture(_tex)
               .randomOffset(false)
               .color(lineColor)
               .cap(Cap.BUTT)

               .build();

         return style;

      } else {

         final LineStyle style = LineStyle.builder()
               .strokeWidth(trackConfig.outlineWidth)
               .color(lineColor)
               //.cap(Cap.BUTT)
               .cap(Paint.Cap.ROUND)
               // this is not yet working
               // .isOutline(true)
               .build();

         return style;
      }
   }

   public void onModifyConfig() {

      _lineStyle = createLineStyle();

      _simpleWorker.submit(RENDERING_DELAY);
   }

   public void setPoints(final GeoPoint[] geoPoints, final TIntArrayList tourStarts) {

      synchronized (_geoPoints) {

         _tourStarts.clear();
         _tourStarts.addAll(tourStarts);

         _geoPoints = geoPoints;
      }

      _simpleWorker.cancel(true);

      _isUpdatePoints = true;
      _isUpdateLayer = true;
   }
}
