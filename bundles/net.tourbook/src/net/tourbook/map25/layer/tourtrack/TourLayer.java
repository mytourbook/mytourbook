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
import net.tourbook.map25.renderer.BucketRendererMT;
import net.tourbook.map25.renderer.LineBucketMT;
import net.tourbook.map25.renderer.RenderBucketsMT;

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
import org.oscim.renderer.GLViewport;
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
 * <p>
 * Original code: org.oscim.layers.PathLayer
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

   private final class TourRenderer extends BucketRendererMT {

      private int __oldX = -1;
      private int __oldY = -1;
      private int __oldZ = -1;

      @Override
      public synchronized void update(final GLViewport viewport) {

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t\t\tupdate()"));
//			// TODO remove SYSTEM.OUT.PRINTLN

         if (!isEnabled()) {
            return;
         }

         final int currentZ = 1 << viewport.pos.zoomLevel;
         final int currentX = (int) (viewport.pos.x * currentZ);
         final int currentY = (int) (viewport.pos.y * currentZ);

         /* update layers when map moved by at least one tile */
         if (currentX != __oldX || currentY != __oldY || currentZ != __oldZ || _isUpdateLayer) {

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

            __oldX = currentX;
            __oldY = currentY;
            __oldZ = currentZ;
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

      RenderBucketsMT __renderBuckets = new RenderBucketsMT();
      MapPosition     __mapPos        = new MapPosition();
   }

   final class Worker extends SimpleWorker<TourRenderTask> {

      private static final int MIN_DIST = 3;

      // limit coords
      private final int __max                = 2048;

      /**
       * pre-projected points
       * <p>
       * Is projecting -180°...180° => 0...1 by using the {@link MercatorProjection}
       */
      private double[]  __preProjectedPoints = new double[2];

      // projected points
      private float[]           __projectedPoints;

      private final LineClipper __lineClipper;
      private int               __numGeoPoints;

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

         int numGeoPoints = __numGeoPoints;

         if (_isUpdatePoints) {

            synchronized (_geoPoints) {

               _isUpdatePoints = false;
               __numGeoPoints = numGeoPoints = _geoPoints.length;

               double[] preProjectedPoints = __preProjectedPoints;

               if (numGeoPoints * 2 >= preProjectedPoints.length) {
                  preProjectedPoints = __preProjectedPoints = new double[numGeoPoints * 2];
                  __projectedPoints = new float[numGeoPoints * 2];
               }

               for (int pointIndex = 0; pointIndex < numGeoPoints; pointIndex++) {
                  MercatorProjection.project(_geoPoints[pointIndex], preProjectedPoints, pointIndex);
               }
            }
         }

         if (numGeoPoints == 0) {

            if (task.__renderBuckets.get() != null) {

               task.__renderBuckets.clear();
               mMap.render();
            }

            return true;
         }

         doWork_Rendering(task, numGeoPoints);

         // trigger redraw to let renderer fetch the result.
         mMap.render();

         return true;
      }

      private void doWork_Rendering(final TourRenderTask task, final int numPoints) {

         LineBucketMT lineBucket;

//         if (_lineStyle.stipple == 0 && _lineStyle.texture == null) {
         lineBucket = task.__renderBuckets.getLineBucket(0);
//         } else {
//            lineBucket = task.__renderBuckets.getLineTexBucket(0);
//         }

         lineBucket.line = _lineStyle;

         //ll.scale = ll.line.width;

         final MapPosition mapPos = task.__mapPos;
         mMap.getMapPosition(mapPos);

         final int zoomlevel = mapPos.zoomLevel;
         mapPos.scale = 1 << zoomlevel;

         final double mapX = mapPos.x; // 0...1
         final double mapY = mapPos.y; // 0...1

         final double scale = Tile.SIZE * mapPos.scale;

         // flip around dateline
         int flip = 0;

         final int scaledMaxX = Tile.SIZE << (zoomlevel - 1);

         int scaledX = (int) ((__preProjectedPoints[0] - mapX) * scale);
         int scaledY = (int) ((__preProjectedPoints[1] - mapY) * scale);

         if (scaledX > scaledMaxX) {
            scaledX -= (scaledMaxX * 2);
            flip = -1;
         } else if (scaledX < -scaledMaxX) {
            scaledX += (scaledMaxX * 2);
            flip = 1;
         }

         // setup tour index
         int tourIndex = 0;
         int nextTourStartIndex = getNextTourStartIndex(tourIndex);

         // setup tour clipper
         __lineClipper.clipStart(scaledX, scaledY);

         final float[] projectedPoints = __projectedPoints;
         int projectedPointIndex = addPoint(projectedPoints, 0, scaledX, scaledY);

         float prevX = scaledX;
         float prevY = scaledY;

         float[] segment = null;

         for (int pointIndex = 2; pointIndex < numPoints * 2; pointIndex += 2) {

            scaledX = (int) ((__preProjectedPoints[pointIndex + 0] - mapX) * scale);
            scaledY = (int) ((__preProjectedPoints[pointIndex + 1] - mapY) * scale);

            int flipDirection = 0;
            if (scaledX > scaledMaxX) {
               scaledX -= scaledMaxX * 2;
               flipDirection = -1;
            } else if (scaledX < -scaledMaxX) {
               scaledX += scaledMaxX * 2;
               flipDirection = 1;
            }

//            System.out.println((System.currentTimeMillis() + " " + scaledX + " : " + scaledY));
//            // TODO remove SYSTEM.OUT.PRINTLN

            if (flip != flipDirection) {
               flip = flipDirection;
               if (projectedPointIndex > 2) {
                  lineBucket.addLine(projectedPoints, projectedPointIndex, false);
               }

               __lineClipper.clipStart(scaledX, scaledY);
               projectedPointIndex = addPoint(projectedPoints, 0, scaledX, scaledY);
               continue;
            }

            if (pointIndex >= nextTourStartIndex) {

               // setup next tour
               nextTourStartIndex = getNextTourStartIndex(++tourIndex);

               // start a new line (copied from flip code)
               if (projectedPointIndex > 2) {
                  lineBucket.addLine(projectedPoints, projectedPointIndex, false);
               }

               __lineClipper.clipStart(scaledX, scaledY);
               projectedPointIndex = addPoint(projectedPoints, 0, scaledX, scaledY);
               continue;
            }

            final int clipOutcode = __lineClipper.clipNext(scaledX, scaledY);
            if (clipOutcode != LineClipper.INSIDE) {

               if (projectedPointIndex > 2) {
                  lineBucket.addLine(projectedPoints, projectedPointIndex, false);
               }

               if (clipOutcode == LineClipper.INTERSECTION) {

                  /* add line segment */
                  segment = __lineClipper.getLine(segment, 0);
                  lineBucket.addLine(segment, 4, false);

                  // the prev point is the real point not the clipped point
                  //prevX = mClipper.outX2;
                  //prevY = mClipper.outY2;
                  prevX = scaledX;
                  prevY = scaledY;
               }

               projectedPointIndex = 0;

               // if the end point is inside, add it
               if (__lineClipper.getPrevOutcode() == LineClipper.INSIDE) {
                  projectedPoints[projectedPointIndex++] = prevX;
                  projectedPoints[projectedPointIndex++] = prevY;
               }
               continue;
            }

            final float diffX = scaledX - prevX;
            final float diffY = scaledY - prevY;
            if ((projectedPointIndex == 0) || FastMath.absMaxCmp(diffX, diffY, MIN_DIST)) {
               projectedPoints[projectedPointIndex++] = prevX = scaledX;
               projectedPoints[projectedPointIndex++] = prevY = scaledY;
            }
         }

         if (projectedPointIndex > 2) {
            lineBucket.addLine(projectedPoints, projectedPointIndex, false);
         }

         System.out.println((System.currentTimeMillis()
               + " " + numPoints
               + " " + projectedPoints.length
               + " " + projectedPointIndex));
         // TODO remove SYSTEM.OUT.PRINTLN

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

      final int lineColor = ColorUtil.getARGB(trackConfig.outlineColor, trackConfig.outlineOpacity);

      if (trackConfig.isShowDirectionArrow) {

         final MarkerToolkit _markertoolkit = new MarkerToolkit(MarkerShape.ARROW);

         final Bitmap _bitmapArrow = _markertoolkit.drawTrackArrow(40, lineColor);

         final TextureItem _tex = new TextureItem(_bitmapArrow);

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

         final int trackVerticalOffset = trackConfig.isTrackVerticalOffset
               ? trackConfig.trackVerticalOffset
               : 0;

         final LineStyle style = LineStyle.builder()

               .strokeWidth(trackConfig.outlineWidth)
               .color(lineColor)

               //.cap(Cap.BUTT)
               .cap(Paint.Cap.ROUND)

               // outline is not yet working
               // .isOutline(true)

               // "u_height" is above the ground -> this is the z axis
               .heightOffset(trackVerticalOffset)

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
