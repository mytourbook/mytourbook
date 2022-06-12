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

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to use the renderer.elements for drawing.
 * <p/>
 * All methods that modify 'buckets' MUST be synchronized!
 */
public class BucketRendererMT extends LayerRenderer {

   public static final Logger   log             = LoggerFactory.getLogger(BucketRendererMT.class);

   /**
    * Use mMapPosition.copy(position) to keep the position for which
    * the Overlay is *compiled*. NOTE: required by setMatrix utility
    * functions to draw this layer fixed to the map
    */
   protected MapPosition        mMapPosition;

   /**
    * Wrap around dateline
    */
   protected boolean            mFlipOnDateLine = true;

   /**
    * Buckets for rendering
    */
   public final RenderBucketsMT buckets;

   protected boolean            mInitialized;

   public BucketRendererMT() {
      buckets = new RenderBucketsMT();
      mMapPosition = new MapPosition();
   }

   /**
    * Compile all buckets into one BufferObject. Sets renderer to be ready
    * when successful. When no data is available (buckets.countVboSize() == 0)
    * then BufferObject will be released and buckets will not be rendered.
    */
   protected synchronized void compile() {
      final boolean ok = buckets.compile(true);
      setReady(ok);
   }

   /**
    * Render all 'buckets'
    */
   @Override
   public synchronized void render(final GLViewport v) {
      final MapPosition layerPos = mMapPosition;

      GLState.test(false, false);
      GLState.blend(true);

      final float div = (float) (v.pos.scale / layerPos.scale);

      boolean project = true;

      setMatrix(v, project);

      for (RenderBucketMT b = buckets.get(); b != null;) {

         buckets.bind();

         if (!project && b.type != SYMBOL) {
            project = true;
            setMatrix(v, project);
         }

         switch (b.type) {
//                case POLYGON:
//                    b = PolygonBucket.Renderer.draw(b, v, 1, true);
//                    break;
         case LINE:
            b = LineBucketMT.Renderer.draw(b, v, div, buckets);
            break;
//                case TEXLINE:
//                    b = LineTexBucket.Renderer.draw(b, v,
//                            FastMath.pow(layerPos.zoomLevel - v.pos.zoomLevel) * (float) layerPos.getZoomScale(),
//                            buckets);
//                    break;
//                case MESH:
//                    b = MeshBucket.Renderer.draw(b, v);
//                    break;
//                case HAIRLINE:
//                    b = HairLineBucket.Renderer.draw(b, v);
//                    break;
//                case BITMAP:
//                    b = BitmapBucket.Renderer.draw(b, v, 1, 1);
//                    break;
//                case SYMBOL:
//                    if (project) {
//                        project = false;
//                        setMatrix(v, project);
//                    }
//                    b = TextureBucket.Renderer.draw(b, v, div);
//                    break;
//                case CIRCLE:
//                    b = CircleBucket.Renderer.draw(b, v);
//                    break;
         default:
            log.error("invalid bucket {}", b.type);
            b = b.next;
            break;
         }
      }
   }

   protected void setMatrix(final GLMatrix mvp, final GLViewport v, final boolean project, final float coordScale) {
      final MapPosition oPos = mMapPosition;

      final double tileScale = Tile.SIZE * v.pos.scale;

      double x = oPos.x - v.pos.x;
      final double y = oPos.y - v.pos.y;

      if (mFlipOnDateLine) {
         //wrap around date-line
         while (x < 0.5) {
            x += 1.0;
         }
         while (x > 0.5) {
            x -= 1.0;
         }
      }

      mvp.setTransScale((float) (x * tileScale),
            (float) (y * tileScale),
            (float) (v.pos.scale / oPos.scale) / coordScale);

      mvp.multiplyLhs(project ? v.viewproj : v.view);
   }

   /**
    * Utility: Set matrices.mvp matrix relative to the difference of current
    * MapPosition and the last updated Overlay MapPosition and applies
    * view-projection-matrix.
    */
   protected void setMatrix(final GLViewport v) {
      setMatrix(v, true);
   }

   /**
    * Utility: Set matrices.mvp matrix relative to the difference of current
    * MapPosition and the last updated Overlay MapPosition.
    * Use this to 'stick' your layer to the map. Note: Vertex coordinates
    * are assumed to be scaled by MapRenderer.COORD_SCALE (== 8).
    *
    * @param v
    *           GLViewport
    * @param project
    *           if true apply view- and projection, or just view otherwise.
    */
   protected void setMatrix(final GLViewport v, final boolean project) {
      setMatrix(v, project, COORD_SCALE);
   }

   protected void setMatrix(final GLViewport v, final boolean project, final float coordScale) {
      setMatrix(v.mvp, v, project, coordScale);
   }

   /**
    * Default implementation:
    * Copy initial Viewport position and compile buckets.
    */
   @Override
   public void update(final GLViewport v) {
      if (!mInitialized) {
         mMapPosition.copy(v.pos);
         mInitialized = true;
         compile();
      }
   }
}
