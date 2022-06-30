/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
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
package net.tourbook.map25.renderer;

import static org.oscim.renderer.MapRenderer.COORD_SCALE;
import static org.oscim.renderer.bucket.RenderBucket.LINE;
import static org.oscim.renderer.bucket.RenderBucket.SYMBOL;
import static org.oscim.renderer.bucket.RenderBucket.TEXLINE;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to use the renderer.elements for drawing.
 * <p/>
 * All methods that modify 'buckets' MUST be synchronized!
 */
public class BucketRendererMT extends LayerRenderer {

   public static final Logger         log             = LoggerFactory.getLogger(BucketRendererMT.class);

   /**
    * Use mMapPosition.copy(position) to keep the position for which
    * the Overlay is *compiled*. NOTE: required by setMatrix utility
    * functions to draw this layer fixed to the map
    */
   protected MapPosition              mMapPosition;

   /**
    * Wrap around dateline
    */
   protected boolean                  mFlipOnDateLine = true;

   /**
    * Buckets for rendering
    */
   protected final RenderBuckets_AllMT allBuckets;

   protected boolean                  mInitialized;

   public BucketRendererMT() {

      allBuckets = new RenderBuckets_AllMT();
      mMapPosition = new MapPosition();
   }

   /**
    * Compile all buckets into one BufferObject. Sets renderer to be ready
    * when successful. When no data is available (buckets.countVboSize() == 0)
    * then BufferObject will be released and buckets will not be rendered.
    */
   protected synchronized void compile() {

      final boolean ok = allBuckets.compile(true);
      setReady(ok);
   }

   /**
    * Render all 'buckets'
    */
   @Override
   public synchronized void render(final GLViewport viewport) {

      final MapPosition mapPosition = mMapPosition;

      GLState.test(false, false);
      GLState.blend(true);

      // viewport scale 2 map scale is between 1...2
      final float vp2mpScale = (float) (viewport.pos.scale / mapPosition.scale);

      boolean isProjected = true;

      setMatrix(viewport, isProjected);

      for (RenderBucketMT bucket = allBuckets.get(); bucket != null;) {

         // performs GL.bindBuffer() for the vbo
         allBuckets.bind();

         if (isProjected == false && bucket.type != SYMBOL) {
            isProjected = true;
            setMatrix(viewport, isProjected);
         }

         switch (bucket.type) {

//       case POLYGON:
//           b = PolygonBucket.Renderer.draw(b, v, 1, true);
//           break;

         case LINE:
            bucket = LineBucketMT.Renderer.draw(bucket, viewport, vp2mpScale, allBuckets);
            break;

         case TEXLINE:
            bucket = LineTexBucketMT.Renderer.draw(bucket,
                  viewport,
                  FastMath.pow(mapPosition.zoomLevel - viewport.pos.zoomLevel) * (float) mapPosition.getZoomScale(),
                  allBuckets);
            break;

//       case MESH:
//           b = MeshBucket.Renderer.draw(b, v);
//           break;
//       case HAIRLINE:
//           b = HairLineBucket.Renderer.draw(b, v);
//           break;
//       case BITMAP:
//           b = BitmapBucket.Renderer.draw(b, v, 1, 1);
//           break;
//       case SYMBOL:
//           if (project) {
//               project = false;
//               setMatrix(v, project);
//           }
//           b = TextureBucket.Renderer.draw(b, v, div);
//           break;
//       case CIRCLE:
//           b = CircleBucket.Renderer.draw(b, v);
//           break;

         default:
            log.error("Invalid bucket {}", bucket.type);
            bucket = bucket.next;
            break;
         }
      }
   }

   protected void setMatrix(final GLMatrix mvp,
                            final GLViewport viewport,
                            final boolean isProjected,
                            final float coordScale) {

      final MapPosition mapPosition = mMapPosition;

      final double tileScale = Tile.SIZE * viewport.pos.scale;

      double x = mapPosition.x - viewport.pos.x;
      final double y = mapPosition.y - viewport.pos.y;

      if (mFlipOnDateLine) {

         //wrap around date-line
         while (x < 0.5) {
            x += 1.0;
         }

         while (x > 0.5) {
            x -= 1.0;
         }
      }

      mvp.setTransScale(
            (float) (x * tileScale),
            (float) (y * tileScale),
            (float) (viewport.pos.scale / mapPosition.scale) / coordScale);

      mvp.multiplyLhs(isProjected

            ? viewport.viewproj
            : viewport.view);
   }

   /**
    * Utility: Set matrices.mvp matrix relative to the difference of current
    * MapPosition and the last updated Overlay MapPosition and applies
    * view-projection-matrix.
    */
   protected void setMatrix(final GLViewport viewport) {

      setMatrix(viewport, true);
   }

   /**
    * Utility: Set matrices.mvp matrix relative to the difference of current
    * MapPosition and the last updated Overlay MapPosition.
    * <p>
    * Use this to 'stick' your layer to the map. Note: Vertex coordinates
    * are assumed to be scaled by MapRenderer.COORD_SCALE (== 8).
    *
    * @param viewport
    *           GLViewport
    * @param isProjected
    *           if true apply view- and projection, or just view otherwise.
    */
   protected void setMatrix(final GLViewport viewport,
                            final boolean isProjected) {

      setMatrix(viewport, isProjected, COORD_SCALE);
   }

   protected void setMatrix(final GLViewport viewport,
                            final boolean isProjected,
                            final float coordScale) {

      setMatrix(viewport.mvp, viewport, isProjected, coordScale);
   }

   /**
    * Default implementation:
    * Copy initial Viewport position and compile buckets.
    */
   @Override
   public void update(final GLViewport viewport) {

      if (!mInitialized) {

         mMapPosition.copy(viewport.pos);
         mInitialized = true;

         compile();
      }
   }
}
